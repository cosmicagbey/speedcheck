package com.example.speedcheck.model

/**
 * Represents an automated speed camera enforcement zone in Ghana.
 */
data class SpeedCameraZone(
    val id: String,
    val name: String,
    val roadName: String,
    val latitude: Double,
    val longitude: Double,
    val speedLimitKmh: Int,
    val radiusMeters: Float = 1200f,
    val description: String = ""
)

/**
 * Live detection result for the driver's current position relative to speed cameras.
 */
data class CameraDetectionResult(
    val isAutoDetectActive: Boolean = false,
    val activeZone: SpeedCameraZone? = null,
    val distanceToCameraMeters: Float = Float.MAX_VALUE,
    val isMovingTowardsCamera: Boolean = false,
    val recommendedLimitKmh: Int = 80,
    val statusDescription: String = "Manual Speed Limit"
)
