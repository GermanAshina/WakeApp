package com.german.wakeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val valid = action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
                action == "android.intent.action.QUICKBOOT_POWERON" ||
                action == "com.htc.intent.action.QUICKBOOT_POWERON"

        if (!valid) return

        val repo = AlarmRepository(context)
        val alarms = repo.loadAll()
        alarms.filter { it.isEnabled }.forEach { alarm ->
            scheduleAlarm(context, alarm)
        }
    }
}