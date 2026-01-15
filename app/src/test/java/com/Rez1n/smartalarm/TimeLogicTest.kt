package com.Rez1n.smartalarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TimeLogicTest {

    @Test
    fun testCalculateTriggerTime_futureToday() {
        val now = Calendar.getInstance()
        val hour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24 // время через 1 час
        val minute = now.get(Calendar.MINUTE)

        val triggerTime = TimeLogic.calculateTriggerTime(hour, minute)

        // Если время позже, день должен быть сегодня
        assertEquals(now.get(Calendar.DAY_OF_YEAR), triggerTime.get(Calendar.DAY_OF_YEAR))
        assertEquals(hour, triggerTime.get(Calendar.HOUR_OF_DAY))
        assertEquals(minute, triggerTime.get(Calendar.MINUTE))
        assertEquals(0, triggerTime.get(Calendar.SECOND))
        assertEquals(0, triggerTime.get(Calendar.MILLISECOND))
    }

    @Test
    fun testCalculateTriggerTime_pastToday() {
        val now = Calendar.getInstance()
        val hour = (now.get(Calendar.HOUR_OF_DAY) + 23) % 24 // время, которое уже прошло
        val minute = now.get(Calendar.MINUTE)

        val triggerTime = TimeLogic.calculateTriggerTime(hour, minute)

        // Если время уже прошло, день должен быть завтра
        val expected = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertEquals(expected.get(Calendar.DAY_OF_YEAR), triggerTime.get(Calendar.DAY_OF_YEAR))
        assertEquals(hour, triggerTime.get(Calendar.HOUR_OF_DAY))
        assertEquals(minute, triggerTime.get(Calendar.MINUTE))
        assertEquals(0, triggerTime.get(Calendar.SECOND))
        assertEquals(0, triggerTime.get(Calendar.MILLISECOND))
    }
}
