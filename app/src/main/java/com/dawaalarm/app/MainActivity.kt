package com.dawaalarm.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var permissionBanner: LinearLayout
    private lateinit var btnGrantExactAlarm: Button
    private lateinit var etMedName: EditText
    private lateinit var etMedNote: EditText
    private lateinit var timePicker: TimePicker
    private lateinit var tvPreAlarmPreview: TextView
    private lateinit var btnSaveMed: Button
    private lateinit var medListContainer: LinearLayout

    private var medicines: MutableList<Medicine> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionBanner = findViewById(R.id.permissionBanner)
        btnGrantExactAlarm = findViewById(R.id.btnGrantExactAlarm)
        etMedName = findViewById(R.id.etMedName)
        etMedNote = findViewById(R.id.etMedNote)
        timePicker = findViewById(R.id.timePicker)
        tvPreAlarmPreview = findViewById(R.id.tvPreAlarmPreview)
        btnSaveMed = findViewById(R.id.btnSaveMed)
        medListContainer = findViewById(R.id.medListContainer)

        requestNotificationPermissionIfNeeded()

        btnGrantExactAlarm.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }

        updatePreAlarmPreview(timePicker.hour, timePicker.minute)
        timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            updatePreAlarmPreview(hourOfDay, minute)
        }

        btnSaveMed.setOnClickListener {
            val name = etMedName.text.toString().trim()
            val note = etMedNote.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "कृपया दवा का नाम लिखें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val h = timePicker.hour
            val m = timePicker.minute
            val newMed = Medicine(
                id = MedicineStorage.nextId(this),
                name = name,
                hour = h,
                minute = m,
                note = note,
                enabled = true
            )

            medicines.add(newMed)
            MedicineStorage.saveAll(this, medicines)
            AlarmScheduler.scheduleMedicineAlarms(this, newMed)

            val (preH, preM) = newMed.getPreAlarmHourMinute()
            Toast.makeText(
                this,
                "सेव हुआ! अलार्म: ${Medicine.formatTime12(preH, preM)} और ${Medicine.formatTime12(h, m)}",
                Toast.LENGTH_LONG
            ).show()

            etMedName.text.clear()
            etMedNote.text.clear()
            renderMedicineList()
        }

        medicines = MedicineStorage.getAll(this)
        renderMedicineList()
    }

    override fun onResume() {
        super.onResume()
        checkExactAlarmPermission()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            permissionBanner.visibility = if (am.canScheduleExactAlarms()) View.GONE else View.VISIBLE
        } else {
            permissionBanner.visibility = View.GONE
        }
    }

    private fun updatePreAlarmPreview(h: Int, m: Int) {
        val temp = Medicine(0, "", h, m, "")
        val (preH, preM) = temp.getPreAlarmHourMinute()
        tvPreAlarmPreview.text = "15 मिनट पहले का अलार्म: ${Medicine.formatTime12(preH, preM)}"
    }

    private fun renderMedicineList() {
        medListContainer.removeAllViews()

        if (medicines.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "अभी कोई दवा सेव नहीं है। ऊपर से अपनी पहली दवा जोड़ें।"
                setTextColor(Color.parseColor("#64748B"))
                textSize = 14f
                setPadding(12, 24, 12, 24)
            }
            medListContainer.addView(emptyTv)
            return
        }

        val sorted = medicines.sortedBy { it.hour * 60 + it.minute }
        for (med in sorted) {
            val (preH, preM) = med.getPreAlarmHourMinute()
            val preStr = Medicine.formatTime12(preH, preM)
            val mainStr = Medicine.formatTime12(med.hour, med.minute)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(36, 30, 36, 30)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 24
                layoutParams = lp
                elevation = 3f
            }

            val titleTv = TextView(this).apply {
                text = med.name
                textSize = 18f
                setTextColor(Color.parseColor("#0F172A"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            card.addView(titleTv)

            if (med.note.isNotBlank()) {
                val noteTv = TextView(this).apply {
                    text = med.note
                    textSize = 13f
                    setTextColor(Color.parseColor("#475569"))
                    setPadding(0, 6, 0, 8)
                }
                card.addView(noteTv)
            }

            val timesTv = TextView(this).apply {
                text = "⏰ 15 मिनट पहले: $preStr\n💊 दवा का समय: $mainStr"
                textSize = 15f
                setTextColor(Color.parseColor("#0F766E"))
                setPadding(0, 10, 0, 14)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            card.addView(timesTv)

            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val btnTestPre = Button(this).apply {
                text = "टेस्ट (15m)"
                textSize = 12f
                setBackgroundColor(Color.parseColor("#FEF3C7"))
                setTextColor(Color.parseColor("#92400E"))
                setOnClickListener { openTestAlarm(med, AlarmScheduler.TYPE_PRE) }
            }

            val btnTestMain = Button(this).apply {
                text = "टेस्ट (समय)"
                textSize = 12f
                setBackgroundColor(Color.parseColor("#CCFBF1"))
                setTextColor(Color.parseColor("#0F766E"))
                setOnClickListener { openTestAlarm(med, AlarmScheduler.TYPE_MAIN) }
            }

            val btnDelete = Button(this).apply {
                text = "हटाएं"
                textSize = 12f
                setBackgroundColor(Color.parseColor("#FFE4E6"))
                setTextColor(Color.parseColor("#BE123C"))
                setOnClickListener {
                    AlarmScheduler.cancelMedicineAlarms(this@MainActivity, med)
                    medicines.removeAll { it.id == med.id }
                    MedicineStorage.saveAll(this@MainActivity, medicines)
                    renderMedicineList()
                    Toast.makeText(this@MainActivity, "${med.name} हटा दी गई", Toast.LENGTH_SHORT).show()
                }
            }

            val lpBtn = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 10
            }
            btnTestPre.layoutParams = lpBtn
            btnTestMain.layoutParams = lpBtn
            btnDelete.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            btnRow.addView(btnTestPre)
            btnRow.addView(btnTestMain)
            btnRow.addView(btnDelete)
            card.addView(btnRow)

            medListContainer.addView(card)
        }
    }

    private fun openTestAlarm(med: Medicine, type: String) {
        val intent = Intent(this, AlarmActivity::class.java).apply {
            putExtra("med_id", med.id)
            putExtra("med_name", med.name)
            putExtra("med_hour", med.hour)
            putExtra("med_minute", med.minute)
            putExtra("med_note", med.note)
            putExtra("alarm_type", type)
        }
        startActivity(intent)
    }
}
