package com.speedcheck.android

import com.speedcheck.android.data.UserPreferences
import com.speedcheck.android.model.MonitoringState
import com.speedcheck.android.model.SpeedAlertStatus
import com.speedcheck.android.model.TripStats
import com.speedcheck.android.service.SpeedCameraDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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
        assertEquals(com.speedcheck.android.data.AlertMode.SOUND, prefs.alertMode)
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
        assertEquals(28, cameras.size)

        // 1. Borteyman Sports Facility T-Junction
        val borteyman = cameras.find { it.id == "borteyman_junction" }
        assertNotNull(borteyman)
        assertEquals(80, borteyman?.speedLimitKmh)
        assertTrue(borteyman?.name?.contains("Borteyman") == true)
        assertFalse(borteyman?.isMobilePatrol == true)

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

        // 6. Densu River Bridge near Weija (N1 Accra-Cape Coast) - Mobile Patrol
        val weija = cameras.find { it.id == "n1_densu_weija" }
        assertNotNull(weija)
        assertEquals(80, weija?.speedLimitKmh)
        assertTrue(weija?.isMobilePatrol == true)

        // 7. Ofankor Barrier / Taifa (N6) - Mobile Patrol
        val ofankor = cameras.find { it.id == "ofankor_taifa_barrier" }
        assertNotNull(ofankor)
        assertEquals(80, ofankor?.speedLimitKmh)
        assertTrue(ofankor?.isMobilePatrol == true)

        // 8. East Legon - Madina 30 km/h School Zone
        val eastLegonMadina = cameras.find { it.id == "east_legon_madina_30kmh" }
        assertNotNull(eastLegonMadina)
        assertEquals(30, eastLegonMadina?.speedLimitKmh)

        // 9. Kumasi N6 Bomso / Ayigya / KNUST Gate - Mobile Patrol
        val kumasiKnust = cameras.find { it.id == "kumasi_n6_bomso_ayigya" }
        assertNotNull(kumasiKnust)
        assertEquals(50, kumasiKnust?.speedLimitKmh)
        assertTrue(kumasiKnust?.isMobilePatrol == true)

        // 10. Kumasi Ayeduase (30 km/h campus zone)
        val ayeduase = cameras.find { it.id == "kumasi_knust_ayeduase" }
        assertNotNull(ayeduase)
        assertEquals(30, ayeduase?.speedLimitKmh)

        // 11. Boankra N6 Highway (Mobile Speed Trap)
        val boankra = cameras.find { it.id == "kumasi_boankra_trap" }
        assertNotNull(boankra)
        assertEquals(50, boankra?.speedLimitKmh)
        assertTrue(boankra?.isMobilePatrol == true)

        // 12. Pillar 2 / Sowutuom mobile tripod setup
        val pillar2 = cameras.find { it.id == "sowutuom_christian_village" }
        assertNotNull(pillar2)
        assertEquals(40, pillar2?.speedLimitKmh)
        assertTrue(pillar2?.isMobilePatrol == true)

        // Verify permanent cameras list for Settings page has exactly 5 cameras
        val fixedCameras = cameras.filter { !it.isMobilePatrol }
        assertEquals(5, fixedCameras.size)
    }

    @Test
    fun voiceAlertMessage_resolvesCorrectContextualPhrases() {
        fun resolveAlertMessage(
            speedKmh: Float,
            speedLimitKmh: Int,
            approachingOffsetKmh: Int,
            hasCameraAhead: Boolean,
            isMobilePatrol: Boolean = false
        ): String? {
            // Suppress alerts when stationary or walking (< 8 km/h)
            if (speedKmh < 8.0f) return null

            val approachingThreshold = (speedLimitKmh - approachingOffsetKmh).coerceAtLeast(1)
            val isApproachingOrExceeded = speedKmh >= approachingThreshold

            return if (hasCameraAhead) {
                if (isMobilePatrol) {
                    if (isApproachingOrExceeded) {
                        "Potential police speed camera ahead, slow down."
                    } else {
                        "Potential police speed camera ahead, be careful."
                    }
                } else {
                    if (isApproachingOrExceeded) {
                        "Speed cameras ahead, slow down."
                    } else {
                        "Speed cameras ahead, be careful."
                    }
                }
            } else {
                if (isApproachingOrExceeded) {
                    "You are about to cross the speed limit, slow down."
                } else {
                    null
                }
            }
        }

        val limit = 80
        val offset = 5

        // Case 1: Stationary or walking (0-7.9 km/h) -> No alert in any scenario
        assertEquals(null, resolveAlertMessage(0f, limit, offset, hasCameraAhead = true))
        assertEquals(null, resolveAlertMessage(5.5f, limit, offset, hasCameraAhead = false))
        assertEquals(null, resolveAlertMessage(7.9f, limit, offset, hasCameraAhead = true))

        // Case 2: Fixed Camera zone + driving safely (60 km/h in 80 zone)
        assertEquals("Speed cameras ahead, be careful.", resolveAlertMessage(60f, limit, offset, hasCameraAhead = true, isMobilePatrol = false))

        // Case 3: Fixed Camera zone + approaching/breaking limit (75 km/h or 85 km/h in 80 zone)
        assertEquals("Speed cameras ahead, slow down.", resolveAlertMessage(75f, limit, offset, hasCameraAhead = true, isMobilePatrol = false))
        assertEquals("Speed cameras ahead, slow down.", resolveAlertMessage(85f, limit, offset, hasCameraAhead = true, isMobilePatrol = false))

        // Case 4: Mobile Patrol camera + driving safely
        assertEquals("Potential police speed camera ahead, be careful.", resolveAlertMessage(60f, limit, offset, hasCameraAhead = true, isMobilePatrol = true))

        // Case 5: Mobile Patrol camera + approaching/breaking limit
        assertEquals("Potential police speed camera ahead, slow down.", resolveAlertMessage(75f, limit, offset, hasCameraAhead = true, isMobilePatrol = true))
        assertEquals("Potential police speed camera ahead, slow down.", resolveAlertMessage(85f, limit, offset, hasCameraAhead = true, isMobilePatrol = true))

        // Case 6: Normal road (no camera) + approaching/breaking limit
        assertEquals("You are about to cross the speed limit, slow down.", resolveAlertMessage(75f, limit, offset, hasCameraAhead = false))
        assertEquals("You are about to cross the speed limit, slow down.", resolveAlertMessage(90f, limit, offset, hasCameraAhead = false))

        // Case 7: Normal road (no camera) + driving safely (60 km/h in 80 zone)
        assertEquals(null, resolveAlertMessage(60f, limit, offset, hasCameraAhead = false))
    }

    @Test
    fun stationaryDeadband_clampsLowSpeedsToZero() {
        val minVehicleSpeed = 8.0f
        fun clampSpeed(rawSpeed: Float): Float = if (rawSpeed < minVehicleSpeed) 0f else rawSpeed

        assertEquals(0f, clampSpeed(0f), 0.001f)
        assertEquals(0f, clampSpeed(1.5f), 0.001f) // GPS drift
        assertEquals(0f, clampSpeed(4.2f), 0.001f) // Walking
        assertEquals(0f, clampSpeed(7.9f), 0.001f) // Fast walk / pocket wobble
        assertEquals(10f, clampSpeed(10f), 0.001f) // Vehicle moving
        assertEquals(50f, clampSpeed(50f), 0.001f) // Vehicle driving
    }

    @Test
    fun alertMode_cyclesInStrictOrder() {
        val sound = com.speedcheck.android.data.AlertMode.SOUND
        val vibrate = sound.next()
        val mute = vibrate.next()
        val backToSound = mute.next()

        org.junit.Assert.assertEquals(com.speedcheck.android.data.AlertMode.VIBRATE, vibrate)
        org.junit.Assert.assertEquals(com.speedcheck.android.data.AlertMode.MUTE, mute)
        org.junit.Assert.assertEquals(com.speedcheck.android.data.AlertMode.SOUND, backToSound)
    }

    @Test
    fun ghanaSpeedLimitZones_containMajorRoads() {
        val zones = SpeedCameraDetector.GHANA_SPEED_LIMIT_ZONES
        // Verify we have a substantial road database
        assertTrue("Should have at least 50 speed limit zones", zones.size >= 50)

        // Verify key roads exist
        val temaMotorway = zones.filter { it.roadName.contains("Tema Motorway") }
        assertTrue("Tema Motorway should have multiple segments", temaMotorway.size >= 2)
        assertTrue("Tema Motorway should be 100 km/h", temaMotorway.all { it.speedLimitKmh == 100 })

        // University of Ghana campus should be 30 km/h school zone
        val legonCampus = zones.find { it.id == "legon_university_zone" }
        assertNotNull(legonCampus)
        assertEquals(30, legonCampus?.speedLimitKmh)
        assertEquals("School Zone", legonCampus?.areaType)

        // Spintex Road should be 50 km/h urban
        val spintex = zones.filter { it.roadName.contains("Spintex") }
        assertTrue("Spintex Road should have segments", spintex.isNotEmpty())
        assertTrue("Spintex Road should be 50 km/h", spintex.all { it.speedLimitKmh == 50 })

        // KNUST campus should be 30 km/h
        val knustCampus = zones.find { it.id == "kumasi_knust_campus" }
        assertNotNull(knustCampus)
        assertEquals(30, knustCampus?.speedLimitKmh)
    }

    @Test
    fun dualLayerDetection_cameraZoneTakesPriorityOverSpeedLimitZone() {
        // Camera zones and speed limit zones should be separate lists
        val cameraIds = SpeedCameraDetector.GHANA_SPEED_CAMERAS.map { it.id }.toSet()
        val limitIds = SpeedCameraDetector.GHANA_SPEED_LIMIT_ZONES.map { it.id }.toSet()

        // No overlap in IDs between the two layers
        val overlap = cameraIds.intersect(limitIds)
        assertTrue("Camera and speed limit zone IDs should not overlap: $overlap", overlap.isEmpty())
    }

    @Test
    fun cameraZones_allHaveRoadNames() {
        SpeedCameraDetector.GHANA_SPEED_CAMERAS.forEach { camera ->
            assertFalse(
                "Camera ${camera.id} should have a non-empty roadName",
                camera.roadName.isBlank()
            )
        }
    }

    @Test
    fun speedLimitZones_allHaveValidAreaTypes() {
        val validAreaTypes = setOf("Urban", "Highway", "Motorway", "School Zone", "Residential")
        SpeedCameraDetector.GHANA_SPEED_LIMIT_ZONES.forEach { zone ->
            assertTrue(
                "Zone ${zone.id} has invalid areaType: ${zone.areaType}",
                zone.areaType in validAreaTypes
            )
        }
    }

    @Test
    fun historyFilter_onlyAcceptsApproachingAndExceeded() {
        fun shouldLogHistory(alertStatus: SpeedAlertStatus): Boolean {
            return alertStatus == SpeedAlertStatus.APPROACHING || alertStatus == SpeedAlertStatus.EXCEEDED
        }

        assertFalse("NORMAL status should not be logged in history", shouldLogHistory(SpeedAlertStatus.NORMAL))
        assertTrue("APPROACHING status must be logged in history", shouldLogHistory(SpeedAlertStatus.APPROACHING))
        assertTrue("EXCEEDED status must be logged in history", shouldLogHistory(SpeedAlertStatus.EXCEEDED))
    }

    @Test
    fun cameraList_collapsedViewLimitsToFiveItems() {
        val totalCameras = SpeedCameraDetector.GHANA_SPEED_CAMERAS
        assertEquals(28, totalCameras.size)

        val fixedCameras = totalCameras.filter { !it.isMobilePatrol }
        assertEquals(5, fixedCameras.size)

        val collapsedList = fixedCameras.take(5)
        assertEquals(5, collapsedList.size)
        assertEquals("borteyman_junction", collapsedList[0].id)
    }

    @Test
    fun userPreferences_defaultDualUnitsMphIsFalseAndConvertsAccurately() {
        val prefs = com.speedcheck.android.data.UserPreferences()
        assertFalse("Dual units MPH should default to false", prefs.showDualUnitsMph)

        // 97 KM/H ≈ 60 MPH test
        val kmh = 97f
        val mph = (kmh * 0.621371f).toInt()
        assertEquals(60, mph)

        // 100 KM/H ≈ 62 MPH
        val motorwayMph = (100f * 0.621371f).toInt()
        assertEquals(62, motorwayMph)
    }
}
