package com.german.wakeapp

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.german.wakeapp.ui.theme.*

data class AppTheme(
    val id: String,
    val name: String,
    val primary: Color,
    val accent: Color,
    val price: Int,
    val emoji: String
)

object ThemeCatalog {
    val all = listOf(
        AppTheme("purple", "Púrpura Neón", PurplePrimary, PurpleAccent, 0, "💜"),
        AppTheme("cyan", "Cian Eléctrico", NeonCyan, Color(0xFF00B8D4), 300, "💙"),
        AppTheme("green", "Verde Matrix", NeonGreen, Color(0xFF00C853), 300, "💚"),
        AppTheme("pink", "Rosa Cyberpunk", Color(0xFFFF4D8D), Color(0xFFFF80AB), 500, "💗"),
        AppTheme("gold", "Aurum Legendario", Color(0xFFFFD700), Color(0xFFFFB300), 1000, "👑")
    )
}

class ThemeManager(context: Context) {

    private val prefs = context.getSharedPreferences("wakeapp_themes", Context.MODE_PRIVATE)

    fun isUnlocked(id: String): Boolean = prefs.getBoolean("theme_$id", id == "purple")

    fun unlock(id: String): Boolean {
        if (isUnlocked(id)) return false
        prefs.edit().putBoolean("theme_$id", true).apply()
        return true
    }

    fun getActive(): AppTheme {
        val id = prefs.getString("active_theme", "purple") ?: "purple"
        return ThemeCatalog.all.firstOrNull { it.id == id } ?: ThemeCatalog.all.first()
    }

    fun setActive(id: String) {
        prefs.edit().putString("active_theme", id).apply()
    }
}