package com.Rez1n.smartalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.Rez1n.smartalarm.TimeLogic
import com.Rez1n.smartalarm.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Всегда показываем 24-часовой формат (европейский)
        binding.timePicker.setIs24HourView(true)

        binding.btnSetAlarm.setOnClickListener {
            val triggerTime = TimeLogic.calculateTriggerTime(
                binding.timePicker.hour,
                binding.timePicker.minute
            )

            setAlarm(triggerTime.timeInMillis)
            // Сохраняем время для показа после перезапуска
            val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            prefs.edit().putLong("alarm_time", triggerTime.timeInMillis).apply()
            showAlarmInfo(triggerTime)
        }

        binding.btnCancelAlarm.setOnClickListener {
            cancelAlarm()
        }

        binding.btnInlineCancel.setOnClickListener {
            cancelAlarm()
        }

        // При старте проверяем установлен ли будильник
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        if (isAlarmSet() && prefs.contains("alarm_time")) {
            val time = prefs.getLong("alarm_time", 0L)
            val cal = Calendar.getInstance().apply { timeInMillis = time }
            showAlarmInfo(cal)
        } else {
            hideAlarmInfo()
        }
    }

    private fun setAlarm(timeInMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Проверка возможности ставить exact alarms (Android 12+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Показываем диалог с возможностью открыть системные настройки для выдачи разрешения
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Требуется разрешение")
                    .setMessage("Для установки точного будильника требуется разрешение 'Schedule exact alarms'. Открыть настройки, чтобы предоставить его?")
                    .setPositiveButton("Открыть настройки") { _, _ ->
                        try {
                            val intent = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                .let { Intent(it) }
                            startActivity(intent)
                        } catch (e: Exception) {
                            // fallback: открываем общие настройки приложения
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:$packageName")
                            }
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
                return
            }
        }

        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
        Toast.makeText(this, "Будильник установлен!", Toast.LENGTH_SHORT).show()
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        // Удаляем сохранённое время
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("alarm_time").apply()

        hideAlarmInfo()
        Toast.makeText(this, "Будильник отменён", Toast.LENGTH_SHORT).show()
    }

    private fun isAlarmSet(): Boolean {
        val intent = Intent(this, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        return pending != null
    }

    private fun showAlarmInfo(calendar: Calendar) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val minuteStr = "%02d".format(minute)
        binding.tvSelectedTime.text = "Будильник на: $hour:$minuteStr"
        binding.tvAlarmInfo.text = "Будильник на $hour:$minuteStr"
        binding.alarmInfoContainer.visibility = android.view.View.VISIBLE
    }

    private fun hideAlarmInfo() {
        binding.tvSelectedTime.text = "Будильник не установлен"
        binding.alarmInfoContainer.visibility = android.view.View.GONE
    }
}