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
        // Días distintos
        Achievement("first_wake", "Una alarma al año no hace daño", "Despierta por primera vez.", "🥇", 10, 1),
        Achievement("streak_3", "Tres son multitud", "3 días distintos despertando.", "🔥", 20, 3),
        Achievement("streak_7", "No hay semana sin siete días", "7 días distintos despertando.", "💪", 50, 7),
        Achievement("streak_21", "Nuevo Hábito Adquirido", "21 días distintos. Ya es rutina.", "🧠", 100, 21),
        Achievement("streak_30", "Mes de Hierro", "30 días distintos despertando.", "🛡️", 150, 30),
        Achievement("streak_100", "Ambicioso", "100 días distintos. Leyenda.", "👑", 500, 100),

        // Minijuegos (Totales)
        Achievement("math_10", "2x2=?", "Completa 10 desafíos de matemáticas.", "🧮", 30, 10),
        Achievement("text_10", "Despacito y con buena letra", "Completa 10 desafíos de escribir.", "✍️", 30, 10),
        Achievement("tracing_10", "No te pases de la raya", "Completa 10 desafíos de delinear.", "✏️", 30, 10),

        // Rondas múltiples
        Achievement("rounds_4", "Cuádruple o nada", "Supera una alarma de 4 rondas.", "🎯", 40, 1),

        // Otros
        Achievement("early_7am", "Madrugador", "Levántate a las 7:00h o antes.", "🌅", 30, 1),
        Achievement("shop_buyer", "Comprador", "Compra un objeto en la Tienda.", "🛒", 20, 1),
        Achievement("shop_10", "Coleccionista", "Compra 10 objetos en la Tienda.", "🧺", 80, 10),
        Achievement("rich_10k", "Avaricioso", "Acumula 5,000 puntos.", "💰", 100, 5000),
        Achievement("ad_watcher", "Caja Tonta", "Ve un anuncio recompensado.", "📺", 20, 1)
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
            // --- Progreso basado en la Racha ---
            "first_wake", "streak_3", "streak_7", "streak_21", "streak_30", "streak_100" ->
                currentStreak.coerceIn(0, achievement.required)

            // --- Progreso basado en Puntos ---
            "rich_10k" ->
                currentPoints.coerceIn(0, achievement.required)

            // --- Progreso basado en contadores de minijuegos ---
            "math_10" -> prefs.getInt("count_MATH", 0).coerceIn(0, achievement.required)
            "text_10" -> prefs.getInt("count_TEXT", 0).coerceIn(0, achievement.required)
            "tracing_10" -> prefs.getInt("count_TRACING", 0).coerceIn(0, achievement.required)

            // --- Progreso basado en compras ---
            "shop_10" -> prefs.getInt("shop_purchases", 0).coerceIn(0, achievement.required)

            // --- Logros de acción única (0 si no está desbloqueado) ---
            "rounds_4", "early_7am", "shop_buyer", "ad_watcher" -> 0

            // --- Fallback por seguridad ---
            else -> 0
        }
    }

    fun checkStreakAchievements(streak: Int): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (streak >= 1 && unlock("first_wake")) newly.add(byId("first_wake"))
        if (streak >= 3 && unlock("streak_3")) newly.add(byId("streak_3"))
        if (streak >= 7 && unlock("streak_7")) newly.add(byId("streak_7"))
        if (streak >= 21 && unlock("streak_21")) newly.add(byId("streak_21"))
        if (streak >= 30 && unlock("streak_30")) newly.add(byId("streak_30"))
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
        val count = prefs.getInt("shop_purchases", 0) + 1
        prefs.edit().putInt("shop_purchases", count).apply()

        if (unlock("shop_buyer")) newly.add(byId("shop_buyer"))
        if (count >= 10 && unlock("shop_10")) newly.add(byId("shop_10"))
        return newly
    }

    fun checkAdWatch(): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (unlock("ad_watcher")) newly.add(byId("ad_watcher"))
        return newly
    }

    fun checkPointsAchievement(points: Int): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (points >= 5000 && unlock("rich_10k")) newly.add(byId("rich_10k"))
        return newly
    }

    fun checkChallengeAchievements(challengeType: String): List<Achievement> {
        val newly = mutableListOf<Achievement>()

        val key = "count_$challengeType"
        val count = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, count).apply()

        if (challengeType == "MATH" && count >= 10 && unlock("math_10")) newly.add(byId("math_10"))
        if (challengeType == "TEXT" && count >= 10 && unlock("text_10")) newly.add(byId("text_10"))
        if (challengeType == "TRACING" && count >= 10 && unlock("tracing_10")) newly.add(byId("tracing_10"))

        return newly
    }

    fun checkMultiRoundAchievements(rounds: Int): List<Achievement> {
        val newly = mutableListOf<Achievement>()
        if (rounds >= 4 && unlock("rounds_4")) newly.add(byId("rounds_4"))
        return newly
    }

    private fun byId(id: String): Achievement =
        AchievementCatalog.all.first { it.id == id }
}