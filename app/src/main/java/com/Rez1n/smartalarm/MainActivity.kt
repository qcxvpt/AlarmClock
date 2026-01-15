package com.Rez1n.smartalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.Rez1n.smartalarm.R
import com.Rez1n.smartalarm.TimeLogic
import com.Rez1n.smartalarm.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedDateMillis: Long? = null
    private val alarms = mutableListOf<AlarmItem>()

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
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        applySavedTheme(prefs)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка формата времени
        val is24h = prefs.getBoolean("is24h", true)
        binding.timePicker.setIs24HourView(is24h)
        binding.switch24h.isChecked = is24h

        binding.switch24h.setOnCheckedChangeListener { _, checked ->
            binding.timePicker.setIs24HourView(checked)
            prefs.edit().putBoolean("is24h", checked).apply()
        }

        binding.btnSettings.setOnClickListener {
            showThemeDialog(prefs)
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

            val weekdays = getSelectedWeekdays()
            addAlarm(triggerTime.timeInMillis, weekdays, selectedDateMillis)
            showAlarmInfo()
        }

        binding.btnCancelAlarm.setOnClickListener {
            cancelAllAlarms()
        }

        binding.btnInlineCancel.setOnClickListener {
            cancelAllAlarms()
        }

        loadAlarms()
        renderAlarms()
        if (alarms.isEmpty()) {
            hideAlarmInfo()
        } else {
            showAlarmInfo()
        }
    }

    private fun addAlarm(timeInMillis: Long, weekdays: Set<Int>, dateMillis: Long?) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Требуется разрешение")
                    .setMessage("Для установки точного будильника требуется разрешение 'Schedule exact alarms'. Открыть настройки, чтобы предоставить его?")
                    .setPositiveButton("Открыть настройки") { _, _ ->
                        try {
                            val intent = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                .let { Intent(it) }
                            startActivity(intent)
                        } catch (e: Exception) {
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

        val alarmId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("time", timeInMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val clockInfo = AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent)
        // setAlarmClock даёт более надёжный запуск на некоторых прошивках и показывает системный индикатор будильника
        alarmManager.setAlarmClock(clockInfo, pendingIntent)

        val item = AlarmItem(alarmId, timeInMillis, weekdays, dateMillis)
        alarms.add(item)
        saveAlarms()
        renderAlarms()
        Toast.makeText(this, "Будильник добавлен", Toast.LENGTH_SHORT).show()
    }

    private fun cancelAllAlarms() {
        if (alarms.isEmpty()) {
            hideAlarmInfo()
            Toast.makeText(this, "Будильников нет", Toast.LENGTH_SHORT).show()
            return
        }

        alarms.toList().forEach { cancelAlarm(it) }
        alarms.clear()
        saveAlarms()
        renderAlarms()
        hideAlarmInfo()
        Toast.makeText(this, "Все будильники отменены", Toast.LENGTH_SHORT).show()
    }

    private fun cancelAlarm(item: AlarmItem) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val existing = PendingIntent.getBroadcast(
            this,
            item.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        existing?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        alarms.removeAll { it.id == item.id }
        saveAlarms()
        renderAlarms()
    }

    private fun showAlarmInfo() {
        if (alarms.isEmpty()) {
            hideAlarmInfo()
            return
        }
        val last = alarms.last()
        val cal = Calendar.getInstance().apply { timeInMillis = last.timeInMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeStr = "%02d:%02d".format(hour, minute)
        val dateStr = last.dateMillis?.let {
            val c = Calendar.getInstance().apply { timeInMillis = it }
            "${c.get(Calendar.DAY_OF_MONTH)}.${c.get(Calendar.MONTH)+1}.${c.get(Calendar.YEAR)}"
        }
        val weekdaysStr = formatWeekdays(last.weekdays)
        val extra = when {
            dateStr != null -> " (дата: $dateStr)"
            weekdaysStr.isNotEmpty() -> " ($weekdaysStr)"
            else -> ""
        }
        binding.tvSelectedTime.text = "Активных будильников: ${alarms.size}. Последний: $timeStr$extra"
        binding.tvAlarmInfo.text = "Активных будильников: ${alarms.size}"
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

    private fun saveAlarms() {
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val json = JSONArray()
        alarms.forEach { alarm ->
            val obj = JSONObject()
            obj.put("id", alarm.id)
            obj.put("time", alarm.timeInMillis)
            obj.put("date", alarm.dateMillis ?: JSONObject.NULL)
            obj.put("weekdays", JSONArray(alarm.weekdays.toList()))
            json.put(obj)
        }
        prefs.edit().putString("alarms_json", json.toString()).apply()
    }

    private fun loadAlarms() {
        val prefs = getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val serialized = prefs.getString("alarms_json", null) ?: return
        runCatching {
            val arr = JSONArray(serialized)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.getInt("id")
                val time = obj.getLong("time")
                val dateMillis = if (obj.isNull("date")) null else obj.getLong("date")
                val weekdaysJson = obj.optJSONArray("weekdays") ?: JSONArray()
                val weekdaysSet = mutableSetOf<Int>()
                for (j in 0 until weekdaysJson.length()) {
                    weekdaysSet.add(weekdaysJson.getInt(j))
                }
                alarms.add(AlarmItem(id, time, weekdaysSet, dateMillis))
            }
        }.onFailure { alarms.clear() }
    }

    private fun renderAlarms() {
        binding.alarmsContainer.removeAllViews()
        if (alarms.isEmpty()) return

        val inflater = layoutInflater
        alarms.sortedBy { it.timeInMillis }.forEach { alarm ->
            val view = inflater.inflate(R.layout.item_alarm, binding.alarmsContainer, false)
            val info = view.findViewById<android.widget.TextView>(R.id.tvAlarmInfo)
            val btnCancel = view.findViewById<android.widget.Button>(R.id.btnCancelAlarmItem)

            val cal = Calendar.getInstance().apply { timeInMillis = alarm.timeInMillis }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val timeStr = "%02d:%02d".format(hour, minute)
            val dateStr = alarm.dateMillis?.let {
                val c = Calendar.getInstance().apply { timeInMillis = it }
                "${c.get(Calendar.DAY_OF_MONTH)}.${c.get(Calendar.MONTH)+1}.${c.get(Calendar.YEAR)}"
            }
            val weekdaysStr = formatWeekdays(alarm.weekdays)
            val extra = when {
                dateStr != null -> " (дата: $dateStr)"
                weekdaysStr.isNotEmpty() -> " ($weekdaysStr)"
                else -> ""
            }
            info.text = "$timeStr$extra"

            btnCancel.setOnClickListener {
                cancelAlarm(alarm)
                showAlarmInfo()
            }

            binding.alarmsContainer.addView(view)
        }
    }

    data class AlarmItem(
        val id: Int,
        val timeInMillis: Long,
        val weekdays: Set<Int>,
        val dateMillis: Long?
    )

    private fun applySavedTheme(prefs: android.content.SharedPreferences) {
        val mode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun showThemeDialog(prefs: android.content.SharedPreferences) {
        val modes = listOf(
            "Как в системе" to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            "Светлая" to AppCompatDelegate.MODE_NIGHT_NO,
            "Тёмная" to AppCompatDelegate.MODE_NIGHT_YES
        )
        val current = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val checked = modes.indexOfFirst { it.second == current }.coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle("Тема")
            .setSingleChoiceItems(modes.map { it.first }.toTypedArray(), checked) { dialog, which ->
                val mode = modes[which].second
                prefs.edit().putInt("theme_mode", mode).apply()
                AppCompatDelegate.setDefaultNightMode(mode)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}