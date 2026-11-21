package com.german.wakeapp

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ProfileManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wakeapp_profile", Context.MODE_PRIVATE)

    private val achievementManager = AchievementManager(context)
    private val streakManager = StreakManager(context)

    // ---------- Estados Reactivos (para Compose) ----------
    var equippedAvatar by mutableStateOf(
        prefs.getString("equipped_AVATAR", null) ?: ProfileCatalog.defaultFor(ProfileCategory.AVATAR)
    )
        private set

    // ---------- Nombre ----------
    fun getName(): String = prefs.getString("name", "") ?: ""
    fun setName(name: String) {
        prefs.edit().putString("name", name).apply()
    }

    // ---------- Equipados ----------
    fun getEquipped(category: ProfileCategory): String {
        return when (category) {
            ProfileCategory.AVATAR -> equippedAvatar
        }
    }

    fun setEquipped(category: ProfileCategory, itemId: String) {
        prefs.edit().putString("equipped_${category.name}", itemId).apply()
        when (category) {
            ProfileCategory.AVATAR -> equippedAvatar = itemId
        }
    }

    // ---------- Desbloqueos ----------
    fun isUnlocked(itemId: String): Boolean {
        val item = ProfileCatalog.byId(itemId) ?: return false
        if (item.requirement is UnlockRequirement.Free) return true
        return prefs.getBoolean("unlocked_$itemId", false)
    }

    fun unlock(itemId: String): Boolean {
        if (isUnlocked(itemId)) return false
        prefs.edit().putBoolean("unlocked_$itemId", true).apply()
        return true
    }

    fun refreshAutoUnlocks(): List<ProfileItem> {
        val newlyUnlocked = mutableListOf<ProfileItem>()
        val currentStreak = streakManager.getStreak()
        val currentLevel = streakManager.getLevel()

        ProfileCatalog.all().forEach { item ->
            if (isUnlocked(item.id)) return@forEach
            val shouldUnlock = when (val req = item.requirement) {
                is UnlockRequirement.Free -> true
                is UnlockRequirement.Achievement -> achievementManager.isUnlocked(req.achievementId)
                is UnlockRequirement.Streak -> currentStreak >= req.days
                is UnlockRequirement.Level -> currentLevel >= req.level
                is UnlockRequirement.Purchase -> false
            }
            if (shouldUnlock && unlock(item.id)) newlyUnlocked.add(item)
        }
        return newlyUnlocked
    }

    // ---------- Stats ----------
    fun getTotalWakeUps(): Int = streakManager.getWakeHistory().size

    fun getBestStreak(): Int = prefs.getInt("best_streak", streakManager.getStreak()).also {
        val current = streakManager.getStreak()
        if (current > it) prefs.edit().putInt("best_streak", current).apply()
    }

    fun updateBestStreak() {
        val current = streakManager.getStreak()
        val best = prefs.getInt("best_streak", 0)
        if (current > best) prefs.edit().putInt("best_streak", current).apply()
    }

    fun getAlarmsCreated(): Int = prefs.getInt("alarms_created", 0)
    fun incrementAlarmsCreated() = prefs.edit().putInt("alarms_created", getAlarmsCreated() + 1).apply()

    fun getProfileUnlockedCount(): Int = ProfileCatalog.all().count { isUnlocked(it.id) }
    fun getProfileTotalCount(): Int = ProfileCatalog.all().size
}