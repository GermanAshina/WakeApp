package com.german.wakeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val challengeType = intent.getStringExtra("CHALLENGE_TYPE") ?: "MATH"
        val difficulty = intent.getStringExtra("DIFFICULTY") ?: "MEDIUM"
        val soundUri = intent.getStringExtra("SOUND_URI")
        val soundName = intent.getStringExtra("SOUND_NAME")

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("CHALLENGE_TYPE", challengeType)
            putExtra("DIFFICULTY", difficulty)
            putExtra("SOUND_URI", soundUri)
            putExtra("SOUND_NAME", soundName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}