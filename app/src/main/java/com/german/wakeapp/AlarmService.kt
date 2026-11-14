package com.german.wakeapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentChallengeTypes: List<String> = listOf("MATH")
    private var currentDifficulty: String = "MEDIUM"
    private var currentRounds: Int = 1
    private var currentXpBoost: Boolean = false
    private var currentSoundUri: String? = null
    private var currentSoundName: String? = null
    private var currentAlarmLabel: String? = null

    companion object {
        const val CHANNEL_ALARM = "ALARM_SERVICE_CHANNEL"
        const val NOTIF_ID = 1
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val list = it.getStringArrayListExtra("CHALLENGE_TYPES")
            if (!list.isNullOrEmpty()) {
                currentChallengeTypes = list
            } else {
                it.getStringExtra("CHALLENGE_TYPE")?.let { v -> currentChallengeTypes = listOf(v) }
            }
            it.getStringExtra("DIFFICULTY")?.let { v -> currentDifficulty = v }
            it.getIntExtra("ROUNDS", currentChallengeTypes.size).takeIf { r -> r > 0 }?.let { v -> currentRounds = v }
            it.getBooleanExtra("XP_BOOST", false).let { v -> currentXpBoost = v }
            it.getStringExtra("SOUND_URI")?.let { v -> currentSoundUri = v }
            it.getStringExtra("SOUND_NAME")?.let { v -> currentSoundName = v }
            it.getStringExtra("ALARM_LABEL")?.let { v -> currentAlarmLabel = v }
        }
        if (currentRounds <= 0) currentRounds = currentChallengeTypes.size
        if (currentRounds > currentChallengeTypes.size) currentRounds = currentChallengeTypes.size

        createChannelIfNeeded()

        if (mediaPlayer?.isPlaying == true) {
            AlarmStateHolder.markRinging(
                this, currentChallengeTypes, currentDifficulty,
                currentRounds, currentXpBoost, currentSoundUri, currentSoundName
            )
            return START_STICKY
        }

        AlarmStateHolder.markRinging(
            this, currentChallengeTypes, currentDifficulty,
            currentRounds, currentXpBoost, currentSoundUri, currentSoundName
        )

        startForeground(NOTIF_ID, buildNotification())
        playAlarmSound(currentSoundUri)
        launchChallengeActivity()

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val challengeIntent = challengeIntent()
        val pendingIntent = PendingIntent.getActivity(
            this, 0, challengeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_lock_idle_alarm,
            "Resolver desafío",
            pendingIntent
        ).build()

        val roundsText = if (currentRounds > 1) " · $currentRounds rondas" else ""
        val labelText = if (!currentAlarmLabel.isNullOrBlank()) "🏷 $currentAlarmLabel · " else ""
        val notifText = if (!currentSoundName.isNullOrBlank())
            "$labelText🎵 $currentSoundName$roundsText — Toca para resolver el desafío."
        else
            "$labelText Toca para resolver el desafío$roundsText."

        return NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setContentTitle("¡Alarma Sonando!")
            .setContentText(notifText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .addAction(openAction)
            .build()
    }

    private fun challengeIntent(): Intent =
        Intent(this, ChallengeActivity::class.java).apply {
            putStringArrayListExtra("CHALLENGE_TYPES", ArrayList(currentChallengeTypes))
            putExtra("CHALLENGE_TYPE", currentChallengeTypes.firstOrNull() ?: "MATH")
            putExtra("DIFFICULTY", currentDifficulty)
            putExtra("ROUNDS", currentRounds)
            putExtra("XP_BOOST", currentXpBoost)
            putExtra("ALARM_LABEL", currentAlarmLabel)
            putExtra("FROM_SERVICE", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

    private fun launchChallengeActivity() {
        try {
            startActivity(challengeIntent())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ALARM)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ALARM,
                    "Servicio de Alarma Activo",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun playAlarmSound(customUri: String?) {
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (!customUri.isNullOrBlank()) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, Uri.parse(customUri))
                    setAudioAttributes(audioAttrs)
                    isLooping = true
                    prepare()
                    start()
                }
                return
            } catch (e: Exception) {
                e.printStackTrace()
                mediaPlayer = null
            }
        }

        try {
            var alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(audioAttrs)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null
        AlarmStateHolder.markStopped(this)
    }
}