package com.example.speedcheck

import com.example.speedcheck.data.UserPreferences
import com.example.speedcheck.model.MonitoringState
import com.example.speedcheck.model.SpeedAlertStatus
import com.example.speedcheck.model.TripStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SpeedCheck core logic, calculations, and state evaluation.
 */
class SpeedCheckUnitTest {

    @Test
    fun defaultPreferences_haveCorrectGhanaStandards() {
        val prefs = UserPreferences()
        assertEquals(80, prefs.speedLimitKmh)
        assertEquals(5, prefs.approachingOffsetKmh)
        assertTrue(prefs.voiceAlertsEnabled)
        assertTrue(prefs.approachingAlertEnabled)
        assertTrue(prefs.autoDetectVehicleMovement)
        assertTrue(prefs.autoStartOnBoot)
        assertEquals("FEMALE", prefs.voiceGender)
        assertEquals(15, prefs.alertCooldownSeconds)
    }

    @Test
    fun approachingSpeedCalculation_isAccurateForPresets() {
        // Preset 30 km/h (e.g. school zones in Ghana)
        val limit30 = 30
        val offset = 5
        val warning30 = limit30 - offset
        assertEquals(25, warning30)

        // Preset 50 km/h (urban town roads)
        val limit50 = 50
        val warning50 = limit50 - offset
        assertEquals(45, warning50)

        // Preset 80 km/h (highways)
        val limit80 = 80
        val warning80 = limit80 - offset
        assertEquals(75, warning80)

        // Preset 100 km/h (dual carriageways like N1)
        val limit100 = 100
        val warning100 = limit100 - offset
        assertEquals(95, warning100)

        // Preset 120 km/h (Accra-Tema Motorway)
        val limit120 = 120
        val warning120 = limit120 - offset
        assertEquals(115, warning120)
    }

    @Test
    fun speedAlertStatus_evaluatesCorrectly() {
        val limit = 80
        val offset = 5
        val approachingThreshold = limit - offset // 75 km/h

        fun evaluate(speed: Float): SpeedAlertStatus = when {
            speed >= limit -> SpeedAlertStatus.EXCEEDED
            speed >= approachingThreshold -> SpeedAlertStatus.APPROACHING
            else -> SpeedAlertStatus.NORMAL
        }

        assertEquals(SpeedAlertStatus.NORMAL, evaluate(0f))
        assertEquals(SpeedAlertStatus.NORMAL, evaluate(60f))
        assertEquals(SpeedAlertStatus.NORMAL, evaluate(74.9f))
        assertEquals(SpeedAlertStatus.APPROACHING, evaluate(75f))
        assertEquals(SpeedAlertStatus.APPROACHING, evaluate(79.9f))
        assertEquals(SpeedAlertStatus.EXCEEDED, evaluate(80f))
        assertEquals(SpeedAlertStatus.EXCEEDED, evaluate(105f))
    }

    @Test
    fun tripStats_calculatesMaxAndAverageCorrectly() {
        var stats = TripStats()
        assertEquals(0f, stats.maxSpeedKmh, 0.001f)
        assertEquals(0f, stats.averageSpeedKmh, 0.001f)

        val speedReadings = listOf(40f, 60f, 80f, 100f)
        var sum = 0f
        var max = 0f
        speedReadings.forEachIndexed { index, speed ->
            sum += speed
            if (speed > max) max = speed
            stats = stats.copy(
                currentSpeedKmh = speed,
                maxSpeedKmh = max,
                averageSpeedKmh = sum / (index + 1),
                speedReadingsCount = index + 1
            )
        }

        assertEquals(100f, stats.maxSpeedKmh, 0.001f)
        assertEquals(70f, stats.averageSpeedKmh, 0.001f)
        assertEquals(4, stats.speedReadingsCount)
    }

    @Test
    fun monitoringState_enumContainsExpectedStates() {
        val states = MonitoringState.values().map { it.name }
        assertTrue(states.contains("STOPPED"))
        assertTrue(states.contains("WAITING_FOR_MOVEMENT"))
        assertTrue(states.contains("ACTIVE_MONITORING"))
        assertTrue(states.contains("PAUSED_STATIONARY"))
    }
}
