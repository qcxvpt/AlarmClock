package com.Rez1n.smartalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object AlarmNotifier {
    private const val CHANNEL_ID = "alarm_channel"
    private const val NOTIFICATION_ID = 1001
    private const val PERIOD_MS = 5000L

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val reposter = object : Runnable {
        override fun run() {
            currentContext?.let { ctx ->
                postNotification(ctx)
                handler.postDelayed(this, PERIOD_MS)
            }
        }
    }

    private var currentContext: Context? = null

    fun start(context: Context) {
        val appCtx = context.applicationContext
        currentContext = appCtx
        if (!isRunning) {
            postNotification(appCtx)
            handler.postDelayed(reposter, PERIOD_MS)
            isRunning = true
        }
    }

    fun stop(context: Context) {
        handler.removeCallbacksAndMessages(null)
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        isRunning = false
        currentContext = null
    }

    private fun postNotification(context: Context) {
        ensureChannel(context)

        val fullScreenIntent = Intent(context, StopAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            1,
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Будильник")
            .setContentText("Нажмите, чтобы остановить")
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setSound(null) // звук воспроизводится AlarmPlayer
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Будильники",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления будильника"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }
}
