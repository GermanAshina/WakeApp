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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "ALARM_SERVICE_CHANNEL"

        val challengeType = intent?.getStringExtra("CHALLENGE_TYPE") ?: "MATH"
        val difficulty = intent?.getStringExtra("DIFFICULTY") ?: "MEDIUM"
        val soundUri = intent?.getStringExtra("SOUND_URI")
        val soundName = intent?.getStringExtra("SOUND_NAME")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Servicio de Alarma Activo",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val challengeIntent = Intent(this, ChallengeActivity::class.java).apply {
            putExtra("CHALLENGE_TYPE", challengeType)
            putExtra("DIFFICULTY", difficulty)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            challengeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifText = if (!soundName.isNullOrBlank())
            "🎵 $soundName — Toca para resolver el desafío."
        else
            "Toca para resolver el desafío y apagarla."

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("¡Alarma Sonando!")
            .setContentText(notifText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
        playAlarmSound(soundUri)
        startActivity(challengeIntent)

        return START_STICKY
    }

    private fun playAlarmSound(customUri: String?) {
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // 1) Intento con sonido personalizado
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
                // Cae al sonido del sistema si falla
            }
        }

        // 2) Fallback: sonido del sistema
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

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}