package com.german.wakeapp

import android.content.Context
import android.content.SharedPreferences

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val rewardPoints: Int,
    val required: Int
)

object AchievementCatalog {
    val all = listOf(
        // Rachas
        Achievement("first_wake", "Una alarma al año no hace daño", "Despierta por primera vez.", "🥇", 50, 1),
        Achievement("streak_3", "Tres son multitud", "3 días seguidos despertando.", "🔥", 100, 3),
        Achievement("streak_7", "No hay semana sin siete días", "7 días seguidos.", "🔥🔥", 250, 7),
        Achievement("streak_21", "Nuevo Hábito Adquirido", "21 días seguidos. Ya es rutina.", "🧠", 500, 21),
        Achievement("streak_100", "Ambicioso", "100 días seguidos. Leyenda.", "👑", 2000, 100),

        // Minijuegos (Totales)
        Achievement("all_games", "En la variedad está el gusto", "Completa los 5 tipos de desafíos.", "🎯", 300, 5),
        Achievement("math_10", "2x2=?", "Completa 10 desafíos de matemáticas.", "🧮", 150, 10),
        Achievement("text_10", "Despacito y con buena letra", "Completa 10 desafíos de escribir.", "✍️", 150, 10),
        Achievement("tracing_10", "No te pases de la raya", "Completa 10 desafíos de delinear.", "✏️", 150, 10),

        // Otros
        Achievement("early_7am", "Madrugador", "Levántate a las 7:00h o antes.", "🌅", 150, 1),
        Achievement("shop_buyer", "Comprador", "Compra un objeto en la Tienda.", "🛒", 100, 1),
        Achievement("rich_10k", "Avaricioso", "Acumula 10,000 puntos.", "💰", 500, 10000),
        Achievement("ad_watcher", "Caja Tonta", "Ve un anuncio recompensado.", "📺", 100, 1)
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

    fun getProgress(achievement: Achievement, currentStreak: Int, currentPoints: Int): Int {
        if (isUnlocked(achievement.id)) return achievement.required
        return when (achievement.id) {
            "rich_10k" -> currentPoints.coerceIn(0, achievement.required)
            "math_10" -> prefs.getInt("count_MATH", 0).coerceIn(0, achievement.required)
            "text_10" -> prefs.getInt("count_TEXT", 0).coerceIn(0, achievement.required)
            "tracing_10" -> prefs.getInt("count_TRACING", 0).coerceIn(0, achievement.required)
            "all_games" -> prefs.getInt("count_unique_games", 0).coerceIn(0, achievement.required)
            else -> currentStreak.coerceIn(0, achievement.required)
        }
    }

    fun checkStreakAchievements(streak: Int): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (streak >= 1 && unlock("first_wake")) newly.add(byId("first_wake"))
        if (streak >= 3 && unlock("streak_3")) newly.add(byId("streak_3"))
        if (streak >= 7 && unlock("streak_7")) newly.add(byId("streak_7"))
        if (streak >= 21 && unlock("streak_21")) newly.add(byId("streak_21"))
        if (streak >= 100 && unlock("streak_100")) newly.add(byId("streak_100"))
        return newly
    }

    fun checkEarlyBird(hour: Int): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (hour <= 7 && unlock("early_7am")) newly.add(byId("early_7am"))
        return newly
    }

    fun checkShopBuyer(): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (unlock("shop_buyer")) newly.add(byId("shop_buyer"))
        return newly
    }

    fun checkAdWatch(): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (unlock("ad_watcher")) newly.add(byId("ad_watcher"))
        return newly
    }

    fun checkPointsAchievement(points: Int): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (points >= 10000 && unlock("rich_10k")) newly.add(byId("rich_10k"))
        return newly
    }

    fun checkChallengeAchievements(challengeType: String): List<Achievement> {
        val newly = mutableListOf<Achievement>()

        // 1. Contar desafío específico
        val key = "count_$challengeType"
        val count = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, count).apply()

        if (challengeType == "MATH" && count >= 10 && unlock("math_10")) newly.add(byId("math_10"))
        if (challengeType == "TEXT" && count >= 10 && unlock("text_10")) newly.add(byId("text_10"))
        if (challengeType == "TRACING" && count >= 10 && unlock("tracing_10")) newly.add(byId("tracing_10"))

        // 2. Contar tipos únicos usados (para "En la variedad...")
        val uniqueKey = "unique_games_$challengeType"
        if (!prefs.getBoolean(uniqueKey, false)) {
            prefs.edit().putBoolean(uniqueKey, true).apply()
            val uniqueCount = prefs.getInt("count_unique_games", 0) + 1
            prefs.edit().putInt("count_unique_games", uniqueCount).apply()
            if (uniqueCount >= 5 && unlock("all_games")) newly.add(byId("all_games"))
        }

        return newly
    }

    private fun byId(id: String): Achievement =
        AchievementCatalog.all.first { it.id == id }
}