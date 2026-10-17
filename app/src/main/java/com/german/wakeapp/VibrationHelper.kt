package com.german.wakeapp

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationHelper {

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun buzz(context: Context, pattern: LongArray, amplitudes: IntArray? = null) {
        val vib = getVibrator(context) ?: return
        if (!vib.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (amplitudes != null) {
                VibrationEffect.createWaveform(pattern, amplitudes, -1)
            } else {
                VibrationEffect.createWaveform(pattern, -1)
            }
            vib.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(pattern, -1)
        }
    }

    fun success(context: Context) = buzz(
        context,
        longArrayOf(0, 40, 60, 40, 60, 80),
        intArrayOf(0, 120, 0, 120, 0, 255)
    )

    fun error(context: Context) = buzz(
        context,
        longArrayOf(0, 120, 80, 120),
        intArrayOf(0, 255, 0, 255)
    )

    fun tick(context: Context) = buzz(context, longArrayOf(0, 20))

    fun alarmPulse(context: Context) = buzz(
        context,
        longArrayOf(0, 400, 200, 400, 200, 400),
        intArrayOf(0, 255, 0, 255, 0, 255)
    )
}