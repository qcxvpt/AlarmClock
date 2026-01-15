package com.Rez1n.smartalarm

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri

/**
 * Simple singleton to play/stop alarm ringtone with fallbacks.
 */
object AlarmPlayer {
    private var ringtone: Ringtone? = null

    fun start(context: Context) {
        val appContext = context.applicationContext
        if (ringtone?.isPlaying == true) return

        val uri = pickAlarmUri()
        ringtone = RingtoneManager.getRingtone(appContext, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            play()
        }
    }

    fun stop() {
        ringtone?.stop()
        ringtone = null
    }

    private fun pickAlarmUri(): Uri {
        val alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alarm != null) return alarm

        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (notification != null) return notification

        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }
}
