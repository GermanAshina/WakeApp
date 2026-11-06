package com.german.wakeapp

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestiona las preferencias reales de la app:
 * - Vibración al resolver desafíos
 * - Sonido de alarma por defecto
 * - Autosilenciar alarma tras X minutos (fallback de seguridad)
 * - Volumen de alarma
 * - Formato de hora 12/24h
 * - Confirmación al borrar alarmas
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wakeapp_settings", Context.MODE_PRIVATE)

    // ---- Vibración ----
    fun isVibrationEnabled(): Boolean = prefs.getBoolean("vibration_enabled", true)
    fun setVibrationEnabled(enabled: Boolean) =
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()

    // ---- Sonido por defecto (si no hay personalizado) ----
    // "alarm" | "notification" | "ringtone"
    fun getDefaultSoundType(): String = prefs.getString("default_sound", "alarm") ?: "alarm"
    fun setDefaultSoundType(type: String) = prefs.edit().putString("default_sound", type).apply()

    // ---- Auto-silenciar tras N minutos (0 = desactivado) ----
    fun getAutoSilenceMinutes(): Int = prefs.getInt("auto_silence_min", 15)
    fun setAutoSilenceMinutes(min: Int) =
        prefs.edit().putInt("auto_silence_min", min.coerceIn(0, 60)).apply()

    // ---- Volumen de alarma (0..1) ----
    fun getAlarmVolume(): Float = prefs.getFloat("alarm_volume", 1f)
    fun setAlarmVolume(v: Float) =
        prefs.edit().putFloat("alarm_volume", v.coerceIn(0f, 1f)).apply()

    // ---- Formato 24h ----
    fun is24HourFormat(): Boolean = prefs.getBoolean("hour_24", true)
    fun set24HourFormat(enabled: Boolean) =
        prefs.edit().putBoolean("hour_24", enabled).apply()

    // ---- Confirmar borrado de alarma ----
    fun isConfirmDeleteEnabled(): Boolean = prefs.getBoolean("confirm_delete", true)
    fun setConfirmDeleteEnabled(enabled: Boolean) =
        prefs.edit().putBoolean("confirm_delete", enabled).apply()

    // ---- Reset total (para "Restablecer app") ----
    fun clearAll() = prefs.edit().clear().apply()
}