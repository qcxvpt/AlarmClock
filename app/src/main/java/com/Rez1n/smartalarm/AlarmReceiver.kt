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
        // Сразу открываем экран остановки
        val fullScreenIntent = Intent(ctx, StopAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        ctx.startActivity(fullScreenIntent)

        // Запускаем периодические пуши каждые 5 секунд, включая первый
        AlarmNotifier.start(ctx)
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

}
