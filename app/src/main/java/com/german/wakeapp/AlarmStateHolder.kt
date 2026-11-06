package com.german.wakeapp

import android.content.Context
import android.content.SharedPreferences

object AlarmStateHolder {

    private const val PREFS = "wakeapp_alarm_state"
    private const val KEY_ACTIVE = "alarm_active"
    private const val KEY_CHALLENGE = "alarm_challenge"
    private const val KEY_DIFFICULTY = "alarm_difficulty"
    private const val KEY_ROUNDS = "alarm_rounds"
    private const val KEY_XP_BOOST = "alarm_xp_boost"
    private const val KEY_SOUND_URI = "alarm_sound_uri"
    private const val KEY_SOUND_NAME = "alarm_sound_name"

    @Volatile
    var isRinging: Boolean = false
        private set

    @Volatile
    var lastChallengeType: String = "MATH"
        private set

    @Volatile
    var lastDifficulty: String = "MEDIUM"
        private set

    @Volatile
    var lastRounds: Int = 1
        private set

    @Volatile
    var lastXpBoost: Boolean = false
        private set

    @Volatile
    var lastSoundUri: String? = null
        private set

    @Volatile
    var lastSoundName: String? = null
        private set

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun markRinging(
        context: Context,
        challengeType: String,
        difficulty: String,
        rounds: Int,
        xpBoost: Boolean,
        soundUri: String?,
        soundName: String?
    ) {
        isRinging = true
        lastChallengeType = challengeType
        lastDifficulty = difficulty
        lastRounds = rounds
        lastXpBoost = xpBoost
        lastSoundUri = soundUri
        lastSoundName = soundName

        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_CHALLENGE, challengeType)
            .putString(KEY_DIFFICULTY, difficulty)
            .putInt(KEY_ROUNDS, rounds)
            .putBoolean(KEY_XP_BOOST, xpBoost)
            .putString(KEY_SOUND_URI, soundUri)
            .putString(KEY_SOUND_NAME, soundName)
            .apply()
    }

    fun markStopped(context: Context) {
        isRinging = false
        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, false)
            .apply()
    }

    fun restoreIfNeeded(context: Context) {
        if (isRinging) return
        val p = prefs(context)
        if (p.getBoolean(KEY_ACTIVE, false)) {
            isRinging = true
            lastChallengeType = p.getString(KEY_CHALLENGE, "MATH") ?: "MATH"
            lastDifficulty = p.getString(KEY_DIFFICULTY, "MEDIUM") ?: "MEDIUM"
            lastRounds = p.getInt(KEY_ROUNDS, 1)
            lastXpBoost = p.getBoolean(KEY_XP_BOOST, false)
            lastSoundUri = p.getString(KEY_SOUND_URI, null)
            lastSoundName = p.getString(KEY_SOUND_NAME, null)
        }
    }
}