package com.german.wakeapp

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

// ============================================================
//  PROFILE MANAGER — Persistencia de perfil y stats
// ============================================================
class ProfileManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wakeapp_profile", Context.MODE_PRIVATE)

    private val achievementManager = AchievementManager(context)
    private val streakManager = StreakManager(context)

    // ---------- Nombre ----------
    fun getName(): String = prefs.getString("name", "") ?: ""
    fun setName(name: String) = prefs.edit().putString("name", name).apply()

    // ---------- Bio ----------
    fun getBio(): String = prefs.getString("bio", "") ?: ""
    fun setBio(bio: String) = prefs.edit().putString("bio", bio.take(80)).apply()

    // ---------- Equipados ----------
    fun getEquipped(category: ProfileCategory): String =
        prefs.getString("equipped_${category.name}", null)
            ?: ProfileCatalog.defaultFor(category)

    fun setEquipped(category: ProfileCategory, itemId: String) {
        prefs.edit().putString("equipped_${category.name}", itemId).apply()
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

    /** Marca como desbloqueados todos los ítems cuyo requisito ya se cumple. */
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

    // ---------- Desbloqueos por compra ----------
    fun canPurchase(itemId: String, points: Int): Boolean {
        val item = ProfileCatalog.byId(itemId) ?: return false
        val req = item.requirement
        if (req !is UnlockRequirement.Purchase) return false
        return !isUnlocked(itemId) && points >= req.price
    }

    // ---------- Stats para la pantalla de perfil ----------
    fun getTotalWakeUps(): Int {
        // Total de días con wake en el historial
        return streakManager.getWakeHistory().size
    }

    fun getBestStreak(): Int = prefs.getInt("best_streak", streakManager.getStreak()).also {
        // Actualizamos si la racha actual es mayor
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

    // ---------- Contadores de progreso de perfil ----------
    fun getProfileUnlockedCount(): Int =
        ProfileCatalog.all().count { isUnlocked(it.id) }

    fun getProfileTotalCount(): Int = ProfileCatalog.all().size
}