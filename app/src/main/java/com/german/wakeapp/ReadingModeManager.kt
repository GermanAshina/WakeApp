package com.german.wakeapp

import android.content.Context
import android.content.SharedPreferences
import android.view.Window
import android.view.WindowManager

/**
 * Gestiona el "Modo Lectura": baja el brillo de la pantalla al mínimo (1%),
 * aplica un tinte cálido (filtro rojizo) y guarda la preferencia
 * para que se recuerde entre sesiones.
 */
class ReadingModeManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wakeapp_reading", Context.MODE_PRIVATE)

    companion object {
        /** Brillo mínimo práctico (1%). Por debajo Android puede ignorarlo. */
        const val MIN_BRIGHTNESS = 0.01f
    }

    fun isEnabled(): Boolean = prefs.getBoolean("reading_enabled", false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("reading_enabled", enabled).apply()
    }

    /**
     * Aplica el modo lectura a una ventana dada.
     * - Si está activo: baja el brillo al 1% y aplica un overlay rojizo.
     * - Si está desactivado: restaura el brillo normal y quita el overlay.
     */
    fun applyToWindow(window: Window, enabled: Boolean) {
        val params = window.attributes
        if (enabled) {
            params.screenBrightness = getCustomBrightness()
        } else {
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        window.attributes = params
    }

    /**
     * Devuelve el valor de brillo manual si el usuario lo ha configurado.
     * Por defecto 1%.
     */
    fun getCustomBrightness(): Float =
        prefs.getFloat("reading_brightness", MIN_BRIGHTNESS)

    fun setCustomBrightness(value: Float) {
        prefs.edit().putFloat("reading_brightness", value.coerceIn(0.01f, 1f)).apply()
    }
}