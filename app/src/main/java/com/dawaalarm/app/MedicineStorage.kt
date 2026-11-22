package com.dawaalarm.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object MedicineStorage {
    private const val PREFS_NAME = "dawa_alarm_prefs"
    private const val KEY_MEDS = "medicines_json"
    private const val KEY_NEXT_ID = "next_med_id"

    fun getAll(context: Context): MutableList<Medicine> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_MEDS, "[]") ?: "[]"
        val list = mutableListOf<Medicine>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Medicine(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        hour = obj.getInt("hour"),
                        minute = obj.getInt("minute"),
                        note = obj.optString("note", ""),
                        enabled = obj.optBoolean("enabled", true)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }

    fun saveAll(context: Context, list: List<Medicine>) {
        val arr = JSONArray()
        for (med in list) {
            val obj = JSONObject()
            obj.put("id", med.id)
            obj.put("name", med.name)
            obj.put("hour", med.hour)
            obj.put("minute", med.minute)
            obj.put("note", med.note)
            obj.put("enabled", med.enabled)
            arr.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MEDS, arr.toString()).apply()
    }

    fun nextId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getInt(KEY_NEXT_ID, 1)
        prefs.edit().putInt(KEY_NEXT_ID, id + 1).apply()
        return id
    }
}
