package com.Rez1n.smartalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.Rez1n.smartalarm.TimeLogic
import com.Rez1n.smartalarm.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedDateMillis: Long? = null

    private val weekdayChips by lazy {
        mapOf(
            binding.chipMon to Calendar.MONDAY,
            binding.chipTue to Calendar.TUESDAY,
            binding.chipWed to Calendar.WEDNESDAY,
            binding.chipThu to Calendar.THURSDAY,
            binding.chipFri to Calendar.FRIDAY,
            binding.chipSat to Calendar.SATURDAY,
            binding.chipSun to Calendar.SUNDAY
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка формата времени
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val is24h = prefs.getBoolean("is24h", true)
        binding.timePicker.setIs24HourView(is24h)
        binding.switch24h.isChecked = is24h

        binding.switch24h.setOnCheckedChangeListener { _, checked ->
            binding.timePicker.setIs24HourView(checked)
            prefs.edit().putBoolean("is24h", checked).apply()
        }

        binding.btnSelectDate.setOnClickListener { openDatePicker() }
        binding.btnClearDate.setOnClickListener { clearDateSelection() }

        binding.btnSetAlarm.setOnClickListener {
            val triggerTime = TimeLogic.calculateNextTrigger(
                binding.timePicker.hour,
                binding.timePicker.minute,
                getSelectedWeekdays(),
                selectedDateMillis
            )

            setAlarm(triggerTime.timeInMillis)
            // Сохраняем время для показа после перезапуска
            val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putLong("alarm_time", triggerTime.timeInMillis)
                .putString("alarm_weekdays", serializeWeekdays(getSelectedWeekdays()))
                .putLong("alarm_date", selectedDateMillis ?: -1L)
                .apply()
            showAlarmInfo(triggerTime)
        }

        binding.btnCancelAlarm.setOnClickListener {
            cancelAlarm()
        }

        binding.btnInlineCancel.setOnClickListener {
            cancelAlarm()
        }

        // При старте проверяем установлен ли будильник
        if (isAlarmSet() && prefs.contains("alarm_time")) {
            val time = prefs.getLong("alarm_time", 0L)
            val cal = Calendar.getInstance().apply { timeInMillis = time }
            val savedDate = prefs.getLong("alarm_date", -1L)
            selectedDateMillis = if (savedDate > 0) savedDate else null
            restoreWeekdays(prefs.getString("alarm_weekdays", ""))
            updateDateButtonText()
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
        prefs.edit().remove("alarm_weekdays").apply()
        prefs.edit().remove("alarm_date").apply()
        clearDateSelection()
        clearWeekdays()

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
        val dateStr = selectedDateMillis?.let {
            val cal = Calendar.getInstance().apply { timeInMillis = it }
            "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH)+1}.${cal.get(Calendar.YEAR)}"
        }
        val weekdaysStr = formatWeekdays(getSelectedWeekdays())
        val extra = when {
            dateStr != null -> " (дата: $dateStr)"
            weekdaysStr.isNotEmpty() -> " ($weekdaysStr)"
            else -> ""
        }
        binding.tvSelectedTime.text = "Будильник на: $hour:$minuteStr$extra"
        binding.tvAlarmInfo.text = "Будильник на $hour:$minuteStr$extra"
        binding.alarmInfoContainer.visibility = android.view.View.VISIBLE
    }

    private fun hideAlarmInfo() {
        binding.tvSelectedTime.text = "Будильник не установлен"
        binding.alarmInfoContainer.visibility = android.view.View.GONE
    }

    private fun openDatePicker() {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val day = now.get(Calendar.DAY_OF_MONTH)

        val dialog = android.app.DatePickerDialog(this, { _: DatePicker, y: Int, m: Int, d: Int ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, y)
                set(Calendar.MONTH, m)
                set(Calendar.DAY_OF_MONTH, d)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selectedDateMillis = cal.timeInMillis
            updateDateButtonText()
        }, year, month, day)
        dialog.datePicker.minDate = now.timeInMillis
        dialog.show()
    }

    private fun updateDateButtonText() {
        binding.btnSelectDate.text = selectedDateMillis?.let {
            val cal = Calendar.getInstance().apply { timeInMillis = it }
            "Дата: ${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH)+1}.${cal.get(Calendar.YEAR)}"
        } ?: "Выбрать дату"
    }

    private fun clearDateSelection() {
        selectedDateMillis = null
        updateDateButtonText()
    }

    private fun getSelectedWeekdays(): Set<Int> {
        return weekdayChips.filter { (chip, _) -> chip.isChecked }.values.toSet()
    }

    private fun clearWeekdays() {
        weekdayChips.keys.forEach { it.isChecked = false }
    }

    private fun serializeWeekdays(days: Set<Int>): String = days.joinToString(",")

    private fun restoreWeekdays(serialized: String?) {
        if (serialized.isNullOrBlank()) {
            clearWeekdays()
            return
        }
        val set = serialized.split(",").mapNotNull { it.toIntOrNull() }.toSet()
        weekdayChips.forEach { (chip, day) -> chip.isChecked = day in set }
    }

    private fun formatWeekdays(days: Set<Int>): String {
        if (days.isEmpty()) return ""
        val names = mapOf(
            Calendar.MONDAY to "Пн",
            Calendar.TUESDAY to "Вт",
            Calendar.WEDNESDAY to "Ср",
            Calendar.THURSDAY to "Чт",
            Calendar.FRIDAY to "Пт",
            Calendar.SATURDAY to "Сб",
            Calendar.SUNDAY to "Вс"
        )
        return days.sorted().joinToString(",") { names[it] ?: "" }
    }
}