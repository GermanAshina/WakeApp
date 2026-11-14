package com.german.wakeapp

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.util.Calendar
import kotlin.math.pow
import kotlin.math.roundToInt

class StreakManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wakeapp_prefs", Context.MODE_PRIVATE)

    companion object {
        fun xpForLevel(level: Int): Int {
            return (100.0 * level.toDouble().pow(1.5)).roundToInt()
        }

        /**
         * Clave del "día lógico". Un día va de 6:59 a 6:58 del día siguiente.
         * Si el timestamp cae antes de las 6:59, pertenece al día anterior.
         */
        fun logicalDayKey(timestamp: Long): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val minutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            if (minutesOfDay < 6 * 60 + 59) {
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
            return cal.get(Calendar.YEAR).toLong() * 1000L + cal.get(Calendar.DAY_OF_YEAR).toLong()
        }
    }

    // ---- COMPRAS DE LA TIENDA NUEVA ----
    fun hasInfiniteAlarms(): Boolean = prefs.getBoolean("upgrade_infinite_alarms", false)
    fun unlockInfiniteAlarms() = prefs.edit().putBoolean("upgrade_infinite_alarms", true).apply()

    fun hasExtraColors(): Boolean = prefs.getBoolean("upgrade_extra_colors", false)
    fun unlockExtraColors() = prefs.edit().putBoolean("upgrade_extra_colors", true).apply()

    fun hasExtraRounds(): Boolean = prefs.getBoolean("upgrade_extra_rounds", false)
    fun unlockExtraRounds() = prefs.edit().putBoolean("upgrade_extra_rounds", true).apply()

    fun isNoAdsUnlocked(): Boolean = prefs.getBoolean("upgrade_no_ads", false)
    fun unlockNoAds() = prefs.edit().putBoolean("upgrade_no_ads", true).apply()

    fun isAvatarUnlocked(): Boolean = prefs.getBoolean("upgrade_avatar", false)
    fun unlockAvatar() = prefs.edit().putBoolean("upgrade_avatar", true).apply()

    fun isXpMultiplierUnlocked(): Boolean = prefs.getBoolean("upgrade_xp_multiplier", false)
    fun unlockXpMultiplier() = prefs.edit().putBoolean("upgrade_xp_multiplier", true).apply()

    fun hasCompletePack(): Boolean =
        hasInfiniteAlarms() && hasExtraColors() && hasExtraRounds() &&
                isNoAdsUnlocked() && isAvatarUnlocked() && isXpMultiplierUnlocked()

    fun unlockCompletePack() {
        prefs.edit()
            .putBoolean("upgrade_infinite_alarms", true)
            .putBoolean("upgrade_extra_colors", true)
            .putBoolean("upgrade_extra_rounds", true)
            .putBoolean("upgrade_no_ads", true)
            .putBoolean("upgrade_avatar", true)
            .putBoolean("upgrade_xp_multiplier", true)
            .apply()
    }

    // Inventario clásico (por si acaso)
    fun getInventoryCount(item: String): Int = prefs.getInt("inv_$item", 0)
    fun addInventoryItem(item: String, amount: Int = 1) {
        val current = getInventoryCount(item)
        prefs.edit().putInt("inv_$item", current + amount).apply()
    }
    fun consumeInventoryItem(item: String): Boolean {
        val current = getInventoryCount(item)
        if (current > 0) {
            prefs.edit().putInt("inv_$item", current - 1).apply()
            return true
        }
        return false
    }

    /**
     * Registra un despertar. Devuelve el nuevo total de "días distintos".
     * Guarda también el registro histórico en un JSON para el calendario.
     */
    fun registerSuccessfulWakeUp(): Int {
        val now = System.currentTimeMillis()
        val currentDayKey = logicalDayKey(now)
        val lastDayKey = prefs.getLong("LAST_WAKE_DAY_KEY", -1L)
        val currentDays = getStreak()

        val newDays = if (currentDayKey != lastDayKey) {
            saveWakeHistory(currentDayKey)
            currentDays + 1
        } else {
            currentDays
        }

        prefs.edit()
            .putInt("STREAK_KEY", newDays)
            .putLong("LAST_WAKE_DAY_KEY", currentDayKey)
            .putLong("LAST_WAKE_TIME", now)
            .apply()

        return newDays
    }

    private fun saveWakeHistory(dayKey: Long) {
        val raw = prefs.getString("WAKE_HISTORY", "[]") ?: "[]"
        try {
            val arr = JSONArray(raw)
            // Evitar duplicados (por seguridad)
            for (i in 0 until arr.length()) {
                if (arr.getLong(i) == dayKey) return
            }
            arr.put(dayKey)
            prefs.edit().putString("WAKE_HISTORY", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getWakeHistory(): List<Long> {
        val raw = prefs.getString("WAKE_HISTORY", "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i -> arr.getLong(i) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun checkStreakExpiration() {
        // No-op: los "días distintos" no caducan por inactividad.
    }

    fun addPoints(amount: Int) {
        val newPoints = getPoints() + amount
        prefs.edit().putInt("POINTS_KEY", newPoints).apply()
    }

    fun getStreak(): Int = prefs.getInt("STREAK_KEY", 0)
    fun getPoints(): Int = prefs.getInt("POINTS_KEY", 0)

    fun spendPoints(amount: Int): Boolean {
        val points = getPoints()
        return if (points >= amount) {
            prefs.edit().putInt("POINTS_KEY", points - amount).apply()
            true
        } else false
    }

    fun getLevel(): Int = prefs.getInt("LEVEL_KEY", 1)
    fun getXp(): Int = prefs.getInt("XP_KEY", 0)

    fun addLevels(amount: Int) {
        if (amount <= 0) return
        val newLevel = getLevel() + amount
        prefs.edit().putInt("LEVEL_KEY", newLevel).apply()
    }

    fun addXp(amount: Int): Int {
        var level = getLevel()
        var xp = getXp() + amount
        var levelsUp = 0
        var needed = xpForLevel(level)
        while (xp >= needed) {
            xp -= needed
            level += 1
            levelsUp += 1
            needed = xpForLevel(level)
        }
        prefs.edit()
            .putInt("LEVEL_KEY", level)
            .putInt("XP_KEY", xp)
            .apply()
        return levelsUp
    }

    fun setPendingReward(
        xp: Int,
        levelUp: Boolean,
        oldLevel: Int,
        newLevel: Int,
        achievementsJson: String? = null,
        bonusLevels: Int = 0,
        bonusPoints: Int = 0
    ) {
        prefs.edit()
            .putInt("PENDING_XP", xp)
            .putBoolean("PENDING_LEVEL_UP", levelUp)
            .putInt("PENDING_OLD_LEVEL", oldLevel)
            .putInt("PENDING_NEW_LEVEL", newLevel)
            .putBoolean("PENDING_REWARD", true)
            .putString("PENDING_ACHIEVEMENTS", achievementsJson)
            .putInt("PENDING_BONUS_LEVELS", bonusLevels)
            .putInt("PENDING_BONUS_POINTS", bonusPoints)
            .apply()
    }

    fun consumePendingReward(): RewardData? {
        if (!prefs.getBoolean("PENDING_REWARD", false)) return null

        val achievementsJson = prefs.getString("PENDING_ACHIEVEMENTS", null)
        val achievements = mutableListOf<RewardAchievement>()
        if (achievementsJson != null) {
            try {
                val arr = JSONArray(achievementsJson)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    achievements.add(
                        RewardAchievement(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            emoji = o.getString("emoji"),
                            rewardPoints = o.getInt("rewardPoints")
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        val data = RewardData(
            xp = prefs.getInt("PENDING_XP", 0),
            levelUp = prefs.getBoolean("PENDING_LEVEL_UP", false),
            oldLevel = prefs.getInt("PENDING_OLD_LEVEL", 1),
            newLevel = prefs.getInt("PENDING_NEW_LEVEL", 1),
            achievements = achievements,
            bonusLevels = prefs.getInt("PENDING_BONUS_LEVELS", 0),
            bonusPoints = prefs.getInt("PENDING_BONUS_POINTS", 0)
        )

        prefs.edit()
            .putBoolean("PENDING_REWARD", false)
            .remove("PENDING_XP")
            .remove("PENDING_LEVEL_UP")
            .remove("PENDING_OLD_LEVEL")
            .remove("PENDING_NEW_LEVEL")
            .remove("PENDING_ACHIEVEMENTS")
            .remove("PENDING_BONUS_LEVELS")
            .remove("PENDING_BONUS_POINTS")
            .apply()

        return data
    }
}

data class RewardAchievement(
    val id: String,
    val title: String,
    val emoji: String,
    val rewardPoints: Int
)

data class RewardData(
    val xp: Int,
    val levelUp: Boolean,
    val oldLevel: Int,
    val newLevel: Int,
    val achievements: List<RewardAchievement> = emptyList(),
    val bonusLevels: Int = 0,
    val bonusPoints: Int = 0
)