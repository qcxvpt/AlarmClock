package com.Rez1n.smartalarm

import java.util.Calendar

object TimeLogic {

    fun calculateTriggerTime(hour: Int, minute: Int): Calendar {
        return calculateNextTrigger(hour, minute, emptySet(), null)
    }

    /**
     * Рассчитывает ближайшее время будильника с учётом выбранных дней недели и опциональной даты.
     * @param weekdays Набор дней недели Calendar.MONDAY..SUNDAY. Пустой набор = одноразовый будильник (сегодня/завтра).
     * @param specificDateMillis Опциональная конкретная дата (UTC millis). Если задана, используется она.
     */
    fun calculateNextTrigger(
        hour: Int,
        minute: Int,
        weekdays: Set<Int>,
        specificDateMillis: Long?
    ): Calendar {
        val now = Calendar.getInstance()

        // Конкретная дата имеет приоритет
        if (specificDateMillis != null) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = specificDateMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.before(now)) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal
        }

        // Повтор по дням недели
        if (weekdays.isNotEmpty()) {
            for (offset in 0..6) {
                val candidate = Calendar.getInstance().apply {
                    timeInMillis = now.timeInMillis
                    add(Calendar.DAY_OF_YEAR, offset)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val day = candidate.get(Calendar.DAY_OF_WEEK)
                if (day in weekdays && !candidate.before(now)) {
                    return candidate
                }
            }
            // На всякий случай — следующий доступный через неделю
            return Calendar.getInstance().apply {
                timeInMillis = now.timeInMillis
                add(Calendar.DAY_OF_YEAR, 7)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        // Обычный одноразовый будильник (сегодня/завтра)
        val triggerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (triggerTime.before(now)) {
            triggerTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        return triggerTime
    }
}
