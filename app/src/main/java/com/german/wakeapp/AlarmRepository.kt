package com.german.wakeapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persiste la lista de alarmas en SharedPreferences para que sobrevivan
 * al cierre de la app, a que el sistema mate el proceso, y a reinicios.
 */
class AlarmRepository(context: Context) {

    private val prefs = context.getSharedPreferences("wakeapp_alarms", Context.MODE_PRIVATE)

    fun saveAll(alarms: List<AlarmItem>) {
        val arr = JSONArray()
        alarms.forEach { a ->
            val obj = JSONObject().apply {
                put("id", a.id)
                put("hour", a.hour)
                put("minute", a.minute)
                put("isEnabled", a.isEnabled)
                put("challengeType", a.challengeType)
                put("difficulty", a.difficulty)
                if (a.soundUri != null) put("soundUri", a.soundUri) else put("soundUri", JSONObject.NULL)
                if (a.soundName != null) put("soundName", a.soundName) else put("soundName", JSONObject.NULL)
            }
            arr.put(obj)
        }
        prefs.edit().putString("alarms_json", arr.toString()).apply()
    }

    fun loadAll(): List<AlarmItem> {
        val raw = prefs.getString("alarms_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                AlarmItem(
                    id = o.getLong("id"),
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    isEnabled = o.optBoolean("isEnabled", true),
                    challengeType = o.optString("challengeType", "MATH"),
                    difficulty = o.optString("difficulty", "MEDIUM"),
                    soundUri = if (o.isNull("soundUri")) null else o.getString("soundUri"),
                    soundName = if (o.isNull("soundName")) null else o.getString("soundName")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}