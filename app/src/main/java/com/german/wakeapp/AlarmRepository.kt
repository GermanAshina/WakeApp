package com.german.wakeapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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
                put("rounds", a.rounds)
                put("xpBoost", a.xpBoost)
                put("colorHex", a.colorHex) // <-- NUEVO
                if (a.soundUri != null) put("soundUri", a.soundUri) else put("soundUri", JSONObject.NULL)
                if (a.soundName != null) put("soundName", a.soundName) else put("soundName", JSONObject.NULL)
            }
            arr.put(obj)
        }
        prefs.edit().putString("alarms_json", arr.toString()).apply()
    }

    fun loadAll(): List<AlarmItem> {
        val raw = prefs.getString("alarms_json", null) ?: return emptyList()
        val loaded = try {
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
                    rounds = o.optInt("rounds", 1).coerceIn(1, 10),
                    xpBoost = o.optBoolean("xpBoost", false),
                    soundUri = if (o.isNull("soundUri")) null else o.getString("soundUri"),
                    soundName = if (o.isNull("soundName")) null else o.getString("soundName"),
                    colorHex = o.optString("colorHex", "DEFAULT") // <-- NUEVO (con fallback para no romper alarmas viejas)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }

        return loaded.map { alarm ->
            if (alarm.challengeType == "STROOP" || alarm.challengeType == "SHAKE") {
                alarm.copy(challengeType = "MATH")
            } else {
                alarm
            }
        }
    }
}