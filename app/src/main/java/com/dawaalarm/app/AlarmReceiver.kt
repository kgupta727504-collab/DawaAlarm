package com.dawaalarm.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "dawa_alarm_channel_v1"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getIntExtra("med_id", 0)
        val medName = intent.getStringExtra("med_name") ?: "दवा"
        val medHour = intent.getIntExtra("med_hour", 9)
        val medMinute = intent.getIntExtra("med_minute", 0)
        val medNote = intent.getStringExtra("med_note") ?: ""
        val alarmType = intent.getStringExtra("alarm_type") ?: AlarmScheduler.TYPE_MAIN

        // Wake CPU briefly while launching full-screen alarm
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DawaAlarm::AlarmWakeLock"
        )
        wakeLock.acquire(10_000L)

        try {
            val activityIntent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("med_id", medId)
                putExtra("med_name", medName)
                putExtra("med_hour", medHour)
                putExtra("med_minute", medMinute)
                putExtra("med_note", medNote)
                putExtra("alarm_type", alarmType)
            }

            val fullScreenPi = PendingIntent.getActivity(
                context,
                medId * 10 + (if (alarmType == AlarmScheduler.TYPE_PRE) 1 else 2),
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            createNotificationChannel(context)

            val timeText = Medicine.formatTime12(medHour, medMinute)
            val title = if (alarmType == AlarmScheduler.TYPE_PRE) {
                "⏰ 15 मिनट बाद दवा लेनी है: $medName"
            } else {
                "💊 दवा लेने का समय: $medName"
            }
            val body = "दवा का समय: $timeText" + if (medNote.isNotBlank()) " • $medNote" else ""

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pill)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPi, true)
                .setContentIntent(fullScreenPi)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(medId * 10 + 1, notification)

            try {
                context.startActivity(activityIntent)
            } catch (_: Exception) {
            }

            // Re-schedule for next day automatically
            val allMeds = MedicineStorage.getAll(context)
            val found = allMeds.find { it.id == medId }
            if (found != null && found.enabled) {
                if (alarmType == AlarmScheduler.TYPE_PRE) {
                    val (preH, preM) = found.getPreAlarmHourMinute()
                    AlarmScheduler.scheduleSingle(context, found, AlarmScheduler.TYPE_PRE, preH, preM, found.id * 2)
                } else {
                    AlarmScheduler.scheduleSingle(context, found, AlarmScheduler.TYPE_MAIN, found.hour, found.minute, found.id * 2 + 1)
                }
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "दवा अलार्म (Medicine Alarms)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "15 मिनट पहले और दवा के समय के अलार्म"
                enableVibration(true)
                setSound(alarmSound, audioAttr)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }
}
