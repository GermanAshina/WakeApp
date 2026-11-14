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
                // Nuevo: lista de tipos
                put("challengeTypes", JSONArray(a.challengeTypes))
                // Legacy, por si acaso
                put("challengeType", a.challengeTypes.firstOrNull() ?: "MATH")
                put("difficulty", a.difficulty)
                put("rounds", a.rounds)
                put("xpBoost", a.xpBoost)
                put("colorHex", a.colorHex)
                if (a.soundUri != null) put("soundUri", a.soundUri) else put("soundUri", JSONObject.NULL)
                if (a.soundName != null) put("soundName", a.soundName) else put("soundName", JSONObject.NULL)
                if (a.label != null) put("label", a.label) else put("label", JSONObject.NULL)
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

                // Nuevo formato: array de tipos
                val typesFromArray: List<String>? = if (o.has("challengeTypes")) {
                    val ta = o.optJSONArray("challengeTypes")
                    if (ta != null) {
                        (0 until ta.length()).mapNotNull { idx -> ta.optString(idx, null) }
                            .filter { it.isNotBlank() }
                            .ifEmpty { null }
                    } else null
                } else null

                // Fallback legacy: un único tipo
                val legacyType = o.optString("challengeType", "MATH")

                val baseTypes = typesFromArray ?: listOf(legacyType)
                val sanitized = baseTypes.map { t ->
                    if (t == "STROOP" || t == "SHAKE" || t == "MAZE") "MATH" else t
                }.filter { t -> ChallengeCatalog.all.any { it.id == t } }
                    .ifEmpty { listOf("MATH") }

                val rounds = o.optInt("rounds", sanitized.size).coerceIn(1, 10)
                // Ajustar lista de tipos a las rondas (ciclar o truncar)
                val types = ChallengeCatalog.buildSequence(sanitized, rounds)

                AlarmItem(
                    id = o.getLong("id"),
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    isEnabled = o.optBoolean("isEnabled", true),
                    challengeTypes = types,
                    difficulty = o.optString("difficulty", "MEDIUM"),
                    rounds = rounds,
                    xpBoost = o.optBoolean("xpBoost", false),
                    soundUri = if (o.isNull("soundUri")) null else o.getString("soundUri"),
                    soundName = if (o.isNull("soundName")) null else o.getString("soundName"),
                    colorHex = o.optString("colorHex", "DEFAULT"),
                    label = if (o.isNull("label")) null else o.getString("label")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
        return loaded
    }
}