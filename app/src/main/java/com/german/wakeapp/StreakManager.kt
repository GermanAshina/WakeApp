package com.german.wakeapp

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import kotlin.math.pow
import kotlin.math.roundToInt

class StreakManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wakeapp_prefs", Context.MODE_PRIVATE)

    companion object {
        fun xpForLevel(level: Int): Int {
            return (100.0 * level.toDouble().pow(1.5)).roundToInt()
        }
    }

    fun registerSuccessfulWakeUp(): Int {
        val currentStreak = getStreak() + 1
        val currentPoints = getPoints() + 50
        prefs.edit()
            .putInt("STREAK_KEY", currentStreak)
            .putInt("POINTS_KEY", currentPoints)
            .apply()
        return currentStreak
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

    // ---------------- XP / NIVELES ----------------

    fun getLevel(): Int = prefs.getInt("LEVEL_KEY", 1)
    fun getXp(): Int = prefs.getInt("XP_KEY", 0)

    /**
     * Sube niveles DIRECTAMENTE, sin tocar la XP acumulada.
     * La barra exponencial se recalcula con `xpForLevel(newLevel)`.
     */
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
        if (levelsUp > 0) {
            addPoints(levelsUp * 50)
        }
        prefs.edit()
            .putInt("LEVEL_KEY", level)
            .putInt("XP_KEY", xp)
            .apply()
        return levelsUp
    }

    // ---------------- RECOMPENSA PENDIENTE ----------------

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