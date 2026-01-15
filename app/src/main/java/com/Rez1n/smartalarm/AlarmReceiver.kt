package com.Rez1n.smartalarm

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.Rez1n.smartalarm.R

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "alarm_channel"
    }

    @SuppressLint("MissingPermission") // POST_NOTIFICATIONS должен быть предоставлен вручную на 13+
    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context ?: return

        Toast.makeText(ctx, "Будильник звонит!", Toast.LENGTH_LONG).show()

        startVibration(ctx)
        AlarmPlayer.start(ctx)
        showNotification(ctx)
    }

    private fun startVibration(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(800, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(800)
        }
    }

    private fun showNotification(context: Context) {
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

        NotificationManagerCompat.from(context).notify(1001, notification)
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
            setSound(null, null) // звук будет запускаться отдельно через AlarmPlayer
            enableVibration(true)
        }

        manager.createNotificationChannel(channel)
    }
}
