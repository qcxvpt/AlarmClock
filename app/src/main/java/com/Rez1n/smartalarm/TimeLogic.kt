package com.Rez1n.smartalarm

import java.util.Calendar

object TimeLogic {

    fun calculateTriggerTime(hour: Int, minute: Int): Calendar {
        val now = Calendar.getInstance()

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
