package com.dawaalarm.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    const val TYPE_PRE = "pre"
    const val TYPE_MAIN = "main"

    fun scheduleMedicineAlarms(context: Context, med: Medicine) {
        cancelMedicineAlarms(context, med)
        if (!med.enabled) return

        val (preH, preM) = med.getPreAlarmHourMinute()
        scheduleSingle(context, med, TYPE_PRE, preH, preM, med.id * 2)
        scheduleSingle(context, med, TYPE_MAIN, med.hour, med.minute, med.id * 2 + 1)
    }

    fun scheduleSingle(
        context: Context,
        med: Medicine,
        type: String,
        targetHour: Int,
        targetMinute: Int,
        requestCode: Int
    ) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        scheduleAtMillis(context, med, type, cal.timeInMillis, requestCode)
    }

    fun scheduleSnooze(context: Context, med: Medicine, type: String, delayMinutes: Int = 5) {
        val triggerAt = System.currentTimeMillis() + delayMinutes * 60 * 1000L
        scheduleAtMillis(context, med, type, triggerAt, med.id * 10 + 9)
    }

    private fun scheduleAtMillis(
        context: Context,
        med: Medicine,
        type: String,
        triggerAtMillis: Long,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("med_id", med.id)
            putExtra("med_name", med.name)
            putExtra("med_hour", med.hour)
            putExtra("med_minute", med.minute)
            putExtra("med_note", med.note)
            putExtra("alarm_type", type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelMedicineAlarms(context: Context, med: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(med.id * 2, med.id * 2 + 1).forEach { reqCode ->
            val intent = Intent(context, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pi != null) {
                alarmManager.cancel(pi)
                pi.cancel()
            }
        }
    }

    fun rescheduleAll(context: Context) {
        val meds = MedicineStorage.getAll(context)
        for (med in meds) {
            if (med.enabled) {
                scheduleMedicineAlarms(context, med)
            }
        }
    }
}
