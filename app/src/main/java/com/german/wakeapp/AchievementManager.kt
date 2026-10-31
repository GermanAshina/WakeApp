package com.german.wakeapp

import android.content.Context
import android.content.SharedPreferences

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val rewardPoints: Int,
    val required: Int   // 1, 3 o 7 (días de racha necesarios)
)

object AchievementCatalog {
    val all = listOf(
        Achievement("first_wake", "Primera sangre", "Despierta por primera vez.", "🥇", 50, 1),
        Achievement("streak_3", "Racha de 3", "3 días seguidos despertando.", "🔥", 100, 3),
        Achievement("streak_7", "Una semana", "7 días seguidos. Imparable.", "🔥🔥", 250, 7)
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

    /**
     * Devuelve el progreso actual (0..required) para un logro.
     * Si ya está desbloqueado, devuelve `required` (mostrará 3/3, 7/7…).
     */
    fun getProgress(achievement: Achievement, currentStreak: Int): Int {
        if (isUnlocked(achievement.id)) return achievement.required
        return currentStreak.coerceIn(0, achievement.required)
    }

    /**
     * Comprueba los 3 logros de racha contra la racha actual.
     * Devuelve SOLO los que se han desbloqueado AHORA (no los que ya estaban).
     */
    fun checkStreakAchievements(streak: Int): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (streak >= 1 && unlock("first_wake")) newly.add(byId("first_wake"))
        if (streak >= 3 && unlock("streak_3")) newly.add(byId("streak_3"))
        if (streak >= 7 && unlock("streak_7")) newly.add(byId("streak_7"))
        return newly
    }

    private fun byId(id: String): Achievement =
        AchievementCatalog.all.first { it.id == id }
}