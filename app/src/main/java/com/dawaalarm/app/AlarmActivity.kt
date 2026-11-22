package com.dawaalarm.app

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turn screen ON and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_alarm)

        val medId = intent.getIntExtra("med_id", 0)
        val medName = intent.getStringExtra("med_name") ?: "दवा"
        val medHour = intent.getIntExtra("med_hour", 9)
        val medMinute = intent.getIntExtra("med_minute", 0)
        val medNote = intent.getStringExtra("med_note") ?: ""
        val alarmType = intent.getStringExtra("alarm_type") ?: AlarmScheduler.TYPE_MAIN

        val rootLayout = findViewById<LinearLayout>(R.id.alarmRootLayout)
        val tvBadge = findViewById<TextView>(R.id.tvAlarmBadge)
        val tvMedName = findViewById<TextView>(R.id.tvAlarmMedName)
        val tvMedTime = findViewById<TextView>(R.id.tvAlarmMedTime)
        val tvNote = findViewById<TextView>(R.id.tvAlarmNote)
        val btnStop = findViewById<Button>(R.id.btnStopAlarm)
        val btnSnooze = findViewById<Button>(R.id.btnSnoozeAlarm)

        val timeText = Medicine.formatTime12(medHour, medMinute)
        tvMedName.text = medName
        tvMedTime.text = "दवा का समय: $timeText"
        tvNote.text = medNote

        if (alarmType == AlarmScheduler.TYPE_PRE) {
            rootLayout.setBackgroundColor(Color.parseColor("#B45309"))
            tvBadge.text = "⏰ 15 मिनट पहले की चेतावनी"
            tvBadge.setTextColor(Color.parseColor("#B45309"))
            btnStop.text = "ठीक है, समझ गया (बंद करें)"
        } else {
            rootLayout.setBackgroundColor(Color.parseColor("#0F766E"))
            tvBadge.text = "💊 दवा लेने का समय हो गया है!"
            tvBadge.setTextColor(Color.parseColor("#0F766E"))
            btnStop.text = "दवा ले ली / अलार्म बंद करें"
        }

        startSoundAndVibration()

        btnStop.setOnClickListener {
            stopSoundAndVibration()
            finish()
        }

        btnSnooze.setOnClickListener {
            stopSoundAndVibration()
            val med = Medicine(medId, medName, medHour, medMinute, medNote, true)
            AlarmScheduler.scheduleSnooze(this, med, alarmType, 5)
            Toast.makeText(this, "5 मिनट बाद फिर अलार्म बजेगा", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startSoundAndVibration() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmActivity, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
        }

        try {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 600, 400, 600, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun stopSoundAndVibration() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {
        }
        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        stopSoundAndVibration()
        super.onDestroy()
    }
}
