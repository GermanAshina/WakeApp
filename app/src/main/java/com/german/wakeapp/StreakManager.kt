package com.german.wakeapp

import android.content.Context
import android.content.SharedPreferences

class StreakManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wakeapp_prefs", Context.MODE_PRIVATE)

    fun registerSuccessfulWakeUp() {
        val currentStreak = getStreak() + 1
        val currentPoints = getPoints() + 50

        prefs.edit()
            .putInt("STREAK_KEY", currentStreak)
            .putInt("POINTS_KEY", currentPoints)
            .apply()
    }

    // Función que falta para el botón Dev (+10 ⚡)
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
        } else {
            false
        }
    }
}