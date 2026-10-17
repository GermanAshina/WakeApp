package com.german.wakeapp

import android.content.Context
import android.content.SharedPreferences

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val rewardPoints: Int
)

object AchievementCatalog {
    val all = listOf(
        Achievement("first_wake", "Primera sangre", "Despierta por primera vez.", "🥇", 50),
        Achievement("streak_3", "Racha de 3", "3 días seguidos despertando.", "🔥", 100),
        Achievement("streak_7", "Una semana", "7 días seguidos. Imparable.", "🔥🔥", 250),
        Achievement("streak_30", "Mes de hierro", "30 días seguidos.", "🏆", 1000),
        Achievement("early_bird", "Madrugador", "Despierta antes de las 6:00.", "🌅", 75),
        Achievement("night_owl", "Noctámbulo", "Despierta después de las 23:00.", "🦉", 75),
        Achievement("perfectionist", "Perfeccionista", "Completa 10 desafíos sin fallar.", "💎", 200),
        Achievement("all_challenges", "Polivalente", "Usa los 8 tipos de desafío.", "🎯", 300),
        Achievement("shake_master", "Sacudidor", "Completa 5 desafíos de sacudida.", "📳", 150),
        Achievement("math_genius", "Genio matemático", "Completa 10 desafíos de cálculo.", "🧮", 150),
        Achievement("points_1000", "Millonario", "Acumula 1000 puntos.", "💰", 100),
        Achievement("no_snooze", "Sin excusas", "Completa un desafío EXTREME.", "💀", 500)
    )
}

class AchievementManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wakeapp_achievements", Context.MODE_PRIVATE)

    fun isUnlocked(id: String): Boolean = prefs.getBoolean("ach_$id", false)

    fun unlock(id: String): Boolean {
        if (isUnlocked(id)) return false
        prefs.edit().putBoolean("ach_$id", true).apply()
        return true
    }

    fun unlockedCount(): Int = AchievementCatalog.all.count { isUnlocked(it.id) }

    fun checkStreakAchievements(streak: Int): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (streak >= 1 && unlock("first_wake")) newly.add(byId("first_wake"))
        if (streak >= 3 && unlock("streak_3")) newly.add(byId("streak_3"))
        if (streak >= 7 && unlock("streak_7")) newly.add(byId("streak_7"))
        if (streak >= 30 && unlock("streak_30")) newly.add(byId("streak_30"))
        return newly
    }

    fun checkPointsAchievements(points: Int): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (points >= 1000 && unlock("points_1000")) newly.add(byId("points_1000"))
        return newly
    }

    fun checkChallengeAchievement(challengeId: String, difficulty: String): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        val key = "challenge_count_$challengeId"
        val count = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, count).apply()

        // Registro de desafíos usados
        val usedKey = "used_challenges"
        val used = prefs.getStringSet(usedKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        used.add(challengeId)
        prefs.edit().putStringSet(usedKey, used).apply()

        if (used.size >= 8 && unlock("all_challenges")) newly.add(byId("all_challenges"))
        if (challengeId == "SHAKE" && count >= 5 && unlock("shake_master")) newly.add(byId("shake_master"))
        if (challengeId == "MATH" && count >= 10 && unlock("math_genius")) newly.add(byId("math_genius"))
        if (difficulty == "EXTREME" && unlock("no_snooze")) newly.add(byId("no_snooze"))
        return newly
    }

    fun checkEarlyBird(hour: Int): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (hour < 6 && unlock("early_bird")) newly.add(byId("early_bird"))
        if (hour >= 23 && unlock("night_owl")) newly.add(byId("night_owl"))
        return newly
    }

    private fun byId(id: String): Achievement =
        AchievementCatalog.all.first { it.id == id }
}