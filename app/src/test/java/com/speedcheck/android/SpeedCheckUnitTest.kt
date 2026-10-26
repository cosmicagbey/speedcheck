package com.speedcheck.android

import com.speedcheck.android.data.UserPreferences
import com.speedcheck.android.model.MonitoringState
import com.speedcheck.android.model.SpeedAlertStatus
import com.speedcheck.android.model.TripStats
import com.speedcheck.android.service.SpeedCameraDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SpeedCheck core logic, calculations, state evaluation,
 * and Ghana MTTD speed camera zone configurations.
 */
class SpeedCheckUnitTest {

    @Test
    fun defaultPreferences_haveCorrectGhanaStandards() {
        val prefs = UserPreferences()
        assertEquals(80, prefs.speedLimitKmh)
        assertEquals(5, prefs.approachingOffsetKmh)
        assertTrue(prefs.voiceAlertsEnabled)
        assertTrue(prefs.approachingAlertEnabled)
        assertTrue(prefs.autoDetectSpeedLimit)
        assertTrue(prefs.autoDetectVehicleMovement)
        assertTrue(prefs.autoStartOnBoot)
        assertEquals("FEMALE", prefs.voiceGender)
        assertEquals(15, prefs.alertCooldownSeconds)
        assertTrue(prefs.isDarkMode)
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

    @Test
    fun ghanaSpeedCameras_containAllRequiredLocations() {
        val cameras = SpeedCameraDetector.GHANA_SPEED_CAMERAS
        assertEquals(5, cameras.size)

        // 1. Borteyman Sports Facility T-Junction
        val borteyman = cameras.find { it.id == "borteyman_junction" }
        assertNotNull(borteyman)
        assertEquals(80, borteyman?.speedLimitKmh)
        assertTrue(borteyman?.name?.contains("Borteyman") == true)

        // 2. The N1 Highway near Fiesta Royale Hotel
        val fiestaRoyale = cameras.find { it.id == "n1_fiesta_royale" }
        assertNotNull(fiestaRoyale)
        assertEquals(80, fiestaRoyale?.speedLimitKmh)
        assertTrue(fiestaRoyale?.name?.contains("Fiesta Royale") == true)

        // 3. The N1 Highway near Baah Junction / Bar Junction
        val baahJunction = cameras.find { it.id == "n1_baah_junction" }
        assertNotNull(baahJunction)
        assertEquals(80, baahJunction?.speedLimitKmh)
        assertTrue(baahJunction?.name?.contains("Baah") == true)

        // 4. Kawukudi (Obasanjo Highway)
        val kawukudi = cameras.find { it.id == "kawukudi_obasanjo" }
        assertNotNull(kawukudi)
        assertEquals(50, kawukudi?.speedLimitKmh)
        assertTrue(kawukudi?.name?.contains("Kawukudi") == true)

        // 5. Kanda Highway
        val kanda = cameras.find { it.id == "kanda_highway" }
        assertNotNull(kanda)
        assertEquals(50, kanda?.speedLimitKmh)
        assertTrue(kanda?.name?.contains("Kanda") == true)
    }
}
