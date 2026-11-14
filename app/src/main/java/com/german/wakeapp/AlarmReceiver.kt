package com.german.wakeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val challengeTypes: List<String> = intent.getStringArrayListExtra("CHALLENGE_TYPES")
            ?: run {
                val legacy = intent.getStringExtra("CHALLENGE_TYPE") ?: "MATH"
                listOf(legacy)
            }

        val difficulty = intent.getStringExtra("DIFFICULTY") ?: "MEDIUM"
        val rounds = intent.getIntExtra("ROUNDS", challengeTypes.size)
        val xpBoost = intent.getBooleanExtra("XP_BOOST", false)
        val soundUri = intent.getStringExtra("SOUND_URI")
        val soundName = intent.getStringExtra("SOUND_NAME")
        val alarmLabel = intent.getStringExtra("ALARM_LABEL")

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putStringArrayListExtra("CHALLENGE_TYPES", ArrayList(challengeTypes))
            putExtra("CHALLENGE_TYPE", challengeTypes.firstOrNull() ?: "MATH")
            putExtra("DIFFICULTY", difficulty)
            putExtra("ROUNDS", rounds)
            putExtra("XP_BOOST", xpBoost)
            putExtra("SOUND_URI", soundUri)
            putExtra("SOUND_NAME", soundName)
            putExtra("ALARM_LABEL", alarmLabel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}