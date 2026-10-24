package com.example.speedcheck.service

import android.location.Location
import com.example.speedcheck.model.CameraDetectionResult
import com.example.speedcheck.model.SpeedCameraZone
import kotlin.math.abs

/**
 * Intelligent detector for Ghana MTTD automated speed cameras.
 * Tracks driver heading, proximity, and automatically resolves the correct speed limit.
 */
object SpeedCameraDetector {

    /**
     * Active automated speed camera locations in Ghana specified by the MTTD:
     * 1. Borteyman Sports Facility T-Junction
     * 2. The N1 Highway (specifically near Fiesta Royale Hotel and Baah Junction / Bar Junction)
     * 3. Kawukudi (Obasanjo Highway)
     * 4. Kanda Highway
     */
    val GHANA_SPEED_CAMERAS = listOf(
        SpeedCameraZone(
            id = "borteyman_junction",
            name = "Borteyman Sports Facility T-Junction",
            roadName = "Borteyman / Motorway Corridor",
            latitude = 5.6675,
            longitude = -0.1030,
            speedLimitKmh = 80,
            radiusMeters = 1400f,
            description = "Automated camera enforcing speed along the Borteyman Sports Complex arterial link."
        ),
        SpeedCameraZone(
            id = "n1_fiesta_royale",
            name = "N1 Highway (Fiesta Royale Hotel)",
            roadName = "George Walker Bush Highway (N1)",
            latitude = 5.6062,
            longitude = -0.1985,
            speedLimitKmh = 80,
            radiusMeters = 1500f,
            description = "High-speed radar camera mounted near Fiesta Royale Hotel / Dzorwulu footbridge."
        ),
        SpeedCameraZone(
            id = "n1_baah_junction",
            name = "N1 Highway (Baah / Bar Junction)",
            roadName = "George Walker Bush Highway (N1)",
            latitude = 5.5925,
            longitude = -0.2580,
            speedLimitKmh = 80,
            radiusMeters = 1500f,
            description = "Automated camera monitoring the Lapaz to Kwashieman / Baah Junction corridor."
        ),
        SpeedCameraZone(
            id = "kawukudi_obasanjo",
            name = "Kawukudi (Obasanjo Highway)",
            roadName = "Olusegun Obasanjo Highway",
            latitude = 5.5892,
            longitude = -0.1932,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            description = "Automated camera enforcing the 50 km/h urban limit at Kawukudi Junction."
        ),
        SpeedCameraZone(
            id = "kanda_highway",
            name = "Kanda Highway",
            roadName = "Kanda Highway",
            latitude = 5.5780,
            longitude = -0.1970,
            speedLimitKmh = 50,
            radiusMeters = 1200f,
            description = "Automated camera monitoring the Kanda Highway arterial corridor."
        )
    )

    /**
     * Evaluates current GPS location and vehicle heading against known Ghana speed camera zones.
     */
    fun evaluateLocation(
        currentLocation: Location,
        currentSpeedKmh: Float,
        autoDetectEnabled: Boolean,
        manualLimitKmh: Int
    ): CameraDetectionResult {
        if (!autoDetectEnabled) {
            return CameraDetectionResult(
                isAutoDetectActive = false,
                activeZone = null,
                distanceToCameraMeters = Float.MAX_VALUE,
                isMovingTowardsCamera = false,
                recommendedLimitKmh = manualLimitKmh,
                statusDescription = "Manual Limit ($manualLimitKmh km/h)"
            )
        }

        var closestApproachingZone: SpeedCameraZone? = null
        var minDistanceToApproaching = Float.MAX_VALUE
        var isMovingTowards = false

        val cameraLocation = Location("camera_provider")

        for (zone in GHANA_SPEED_CAMERAS) {
            cameraLocation.latitude = zone.latitude
            cameraLocation.longitude = zone.longitude

            val distanceMeters = currentLocation.distanceTo(cameraLocation)

            if (distanceMeters <= zone.radiusMeters) {
                // Check whether vehicle is moving in the direction of the camera
                val bearingToCamera = currentLocation.bearingTo(cameraLocation)
                val movingTowards = if (currentLocation.hasBearing() && currentSpeedKmh >= 10f) {
                    val angleDiff = abs((currentLocation.bearing - bearingToCamera + 540) % 360 - 180)
                    angleDiff <= 85 || distanceMeters <= 350f
                } else {
                    // When stopped or low speed, any vehicle within zone radius is covered
                    true
                }

                if (movingTowards && distanceMeters < minDistanceToApproaching) {
                    minDistanceToApproaching = distanceMeters
                    closestApproachingZone = zone
                    isMovingTowards = true
                }
            }
        }

        return if (closestApproachingZone != null) {
            val dist = minDistanceToApproaching.toInt()
            CameraDetectionResult(
                isAutoDetectActive = true,
                activeZone = closestApproachingZone,
                distanceToCameraMeters = minDistanceToApproaching,
                isMovingTowardsCamera = isMovingTowards,
                recommendedLimitKmh = closestApproachingZone.speedLimitKmh,
                statusDescription = "📍 ${closestApproachingZone.name} (${dist}m) • ${closestApproachingZone.speedLimitKmh} km/h"
            )
        } else {
            // Outside specific camera zones, intelligently choose Ghana standard limit based on speed
            val standardLimit = if (currentSpeedKmh >= 65f) 80 else 50
            val areaType = if (standardLimit == 80) "Ghana Highway" else "Urban Built-up Area"

            CameraDetectionResult(
                isAutoDetectActive = true,
                activeZone = null,
                distanceToCameraMeters = Float.MAX_VALUE,
                isMovingTowardsCamera = false,
                recommendedLimitKmh = standardLimit,
                statusDescription = "Auto-Detect: $areaType ($standardLimit km/h)"
            )
        }
    }
}
