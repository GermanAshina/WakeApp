package com.german.wakeapp

import android.content.Context
import androidx.compose.ui.graphics.Color

data class AppTheme(
    val id: String,
    val name: String,
    val primary: Color,
    val accent: Color,
    val price: Int,
    val emoji: String
)

object ThemeCatalog {
    const val DEFAULT_ID = "default"
    const val THEME_PRICE = 3000

    /** Azul original de la app. */
    val DEFAULT_ACCENT = Color(0xFF1D9BF0)

    val all = listOf(
        AppTheme(DEFAULT_ID, "Por defecto", DEFAULT_ACCENT, Color(0xFF1D9BF0), 0, "🔵"),
        AppTheme("gold", "Aurum Legendario", Color(0xFFE5A100), Color(0xFFFFB300), THEME_PRICE, "👑"),
        AppTheme("pink", "Rosa Cyberpunk", Color(0xFFFF4D8D), Color(0xFFFF80AB), THEME_PRICE, "💗"),
        AppTheme("green", "Verde Matrix", Color(0xFF00C853), Color(0xFF00E676), THEME_PRICE, "💚")
    )

    /** Temas que se venden en la Tienda (todos menos el de por defecto). */
    val purchasable: List<AppTheme> get() = all.filter { it.price > 0 }

    fun byId(id: String): AppTheme = all.firstOrNull { it.id == id } ?: all.first()
}

class ThemeManager(context: Context) {

    private val prefs = context.getSharedPreferences("wakeapp_themes", Context.MODE_PRIVATE)

    fun isUnlocked(id: String): Boolean =
        id == ThemeCatalog.DEFAULT_ID || prefs.getBoolean("theme_$id", false)

    fun unlock(id: String): Boolean {
        if (isUnlocked(id)) return false
        prefs.edit().putBoolean("theme_$id", true).apply()
        return true
    }

    /**
     * Tema activo. Si el guardado ya no existe (p. ej. los temas antiguos
     * "purple" o "cyan") o no está desbloqueado, vuelve a "Por defecto".
     */
    fun getActive(): AppTheme {
        val id = prefs.getString("active_theme", ThemeCatalog.DEFAULT_ID) ?: ThemeCatalog.DEFAULT_ID
        val theme = ThemeCatalog.all.firstOrNull { it.id == id } ?: return ThemeCatalog.all.first()
        return if (isUnlocked(theme.id)) theme else ThemeCatalog.all.first()
    }

    fun setActive(id: String) {
        prefs.edit().putString("active_theme", id).apply()
    }
}