package com.dawaalarm.app

data class Medicine(
    val id: Int,
    val name: String,
    val hour: Int,
    val minute: Int,
    val note: String,
    val enabled: Boolean = true
) {
    fun getPreAlarmHourMinute(): Pair<Int, Int> {
        var totalMinutes = hour * 60 + minute - 15
        if (totalMinutes < 0) totalMinutes += 24 * 60
        return Pair((totalMinutes / 60) % 24, totalMinutes % 60)
    }

    companion object {
        fun formatTime12(h: Int, m: Int): String {
            val amPm = if (h >= 12) "PM" else "AM"
            val hour12 = when {
                h % 12 == 0 -> 12
                else -> h % 12
            }
            return String.format("%02d:%02d %s", hour12, m, amPm)
        }
    }
}
