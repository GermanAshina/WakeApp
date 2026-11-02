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

    // Inventario de la tienda
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

    // Racha Congelada
    fun isStreakFrozen(): Boolean {
        val frozenUntil = prefs.getLong("FROZEN_UNTIL", 0L)
        return System.currentTimeMillis() < frozenUntil
    }

    fun getFreezeRemainingMillis(): Long {
        val frozenUntil = prefs.getLong("FROZEN_UNTIL", 0L)
        val remaining = frozenUntil - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    fun freezeStreakFor24h() {
        val until = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
        prefs.edit().putLong("FROZEN_UNTIL", until).apply()
    }

    fun checkStreakExpiration() {
        if (isStreakFrozen()) return

        val lastWake = prefs.getLong("LAST_WAKE_TIME", 0L)
        val currentStreak = getStreak()
        if (lastWake > 0 && currentStreak > 0) {
            val now = System.currentTimeMillis()
            if (now - lastWake > 24 * 60 * 60 * 1000L) {
                prefs.edit().putInt("STREAK_KEY", 0).apply()
            }
        }
    }

    fun registerSuccessfulWakeUp(): Int {
        val currentStreak = getStreak() + 1
        val currentPoints = getPoints() + 50
        prefs.edit()
            .putInt("STREAK_KEY", currentStreak)
            .putInt("POINTS_KEY", currentPoints)
            .putLong("LAST_WAKE_TIME", System.currentTimeMillis())
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

    fun isCyberpunkThemeUnlocked(): Boolean = prefs.getBoolean("theme_cyberpunk", false)
    fun unlockCyberpunkTheme() {
        prefs.edit().putBoolean("theme_cyberpunk", true).apply()
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
        if (levelsUp > 0) {
            addPoints(levelsUp * 50)
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