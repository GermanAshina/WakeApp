package com.german.wakeapp

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wakeapp_settings", Context.MODE_PRIVATE)

    // ---- ONBOARDING ----
    fun isOnboardingCompleted(): Boolean = prefs.getBoolean("onboarding_completed", false)
    fun setOnboardingCompleted(completed: Boolean) =
        prefs.edit().putBoolean("onboarding_completed", completed).apply()

    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    fun setUserName(name: String) = prefs.edit().putString("user_name", name).apply()

    fun getLanguage(): String = prefs.getString("language", "es") ?: "es"
    fun setLanguage(lang: String) = prefs.edit().putString("language", lang).apply()

    fun getPurpose(): String = prefs.getString("user_purpose", "") ?: ""
    fun setPurpose(purpose: String) = prefs.edit().putString("user_purpose", purpose).apply()

    // ---- NUEVOS: Dificultad por defecto y hora habitual ----
    fun getDefaultDifficulty(): String = prefs.getString("default_difficulty", "MEDIUM") ?: "MEDIUM"
    fun setDefaultDifficulty(difficulty: String) =
        prefs.edit().putString("default_difficulty", difficulty).apply()

    fun getHabitualWakeHour(): Int = prefs.getInt("habitual_wake_hour", 7)
    fun getHabitualWakeMinute(): Int = prefs.getInt("habitual_wake_minute", 0)
    fun setHabitualWakeTime(hour: Int, minute: Int) =
        prefs.edit()
            .putInt("habitual_wake_hour", hour)
            .putInt("habitual_wake_minute", minute)
            .apply()

    // ---- Vibración ----
    fun isVibrationEnabled(): Boolean = prefs.getBoolean("vibration_enabled", true)
    fun setVibrationEnabled(enabled: Boolean) =
        prefs.edit().putBoolean("vibration_enabled", enabled).apply()

    // ---- Sonido por defecto ----
    fun getDefaultSoundType(): String = prefs.getString("default_sound", "alarm") ?: "alarm"
    fun setDefaultSoundType(type: String) = prefs.edit().putString("default_sound", type).apply()

    // ---- Auto-silenciar tras N minutos (0 = desactivado) ----
    fun getAutoSilenceMinutes(): Int = prefs.getInt("auto_silence_min", 15)
    fun setAutoSilenceMinutes(min: Int) =
        prefs.edit().putInt("auto_silence_min", min.coerceIn(0, 60)).apply()

    // ---- Volumen de alarma ----
    fun getAlarmVolume(): Float = prefs.getFloat("alarm_volume", 1f)
    fun setAlarmVolume(v: Float) =
        prefs.edit().putFloat("alarm_volume", v.coerceIn(0f, 1f)).apply()

    // ---- Formato 24h ----
    fun is24HourFormat(): Boolean = prefs.getBoolean("hour_24", true)
    fun set24HourFormat(enabled: Boolean) =
        prefs.edit().putBoolean("hour_24", enabled).apply()

    // ---- Confirmar borrado ----
    fun isConfirmDeleteEnabled(): Boolean = prefs.getBoolean("confirm_delete", true)
    fun setConfirmDeleteEnabled(enabled: Boolean) =
        prefs.edit().putBoolean("confirm_delete", enabled).apply()

    // ---- Modo claro / oscuro (false = oscuro, por defecto) ----
    fun isLightMode(): Boolean = prefs.getBoolean("light_mode", false)
    fun setLightMode(enabled: Boolean) =
        prefs.edit().putBoolean("light_mode", enabled).apply()

    // ---- Reset ----
    fun clearAll() = prefs.edit().clear().apply()
}