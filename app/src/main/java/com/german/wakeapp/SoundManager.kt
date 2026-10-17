package com.german.wakeapp

import android.content.Context
import android.media.MediaPlayer

class SoundManager(private val context: Context) {

    private var bgmPlayer: MediaPlayer? = null

    fun startShopMusic() {
        try {
            val resId = context.resources.getIdentifier("shop_bgm", "raw", context.packageName)
            if (resId != 0) {
                bgmPlayer = MediaPlayer.create(context, resId).apply {
                    isLooping = true
                    setVolume(0.5f, 0.5f)
                    start()
                }
            }
        } catch (_: Exception) {}
    }

    fun stopShopMusic() {
        bgmPlayer?.stop()
        bgmPlayer?.release()
        bgmPlayer = null
    }

    fun playSound(soundName: String) {
        try {
            val resId = context.resources.getIdentifier(soundName, "raw", context.packageName)
            if (resId != 0) {
                val mp = MediaPlayer.create(context, resId)
                mp.setOnCompletionListener { it.release() }
                mp.start()
            }
        } catch (_: Exception) {}
    }
}