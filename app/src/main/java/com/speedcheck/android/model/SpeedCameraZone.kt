package com.speedcheck.android.model

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
 * Live detection result for the driver's current position relative to
 * speed camera zones AND named road speed limit zones.
 *
 * Detection priority: Camera zone > Speed limit zone > Ghana default.
 */
data class CameraDetectionResult(
    val isAutoDetectActive: Boolean = false,

    /** Non-null when the driver is inside a known speed camera zone. */
    val activeZone: SpeedCameraZone? = null,

    /** Non-null when the driver is on a known road segment (without camera). */
    val activeSpeedLimitZone: SpeedLimitZone? = null,

    val distanceToCameraMeters: Float = Float.MAX_VALUE,
    val isMovingTowardsCamera: Boolean = false,

    /** True when the driver is inside a speed camera zone. */
    val hasCameraAhead: Boolean = false,

    /** The detected road name (from camera zone or speed limit zone). */
    val detectedRoadName: String? = null,

    /** The area type: "Urban", "Highway", "Motorway", "School Zone", etc. */
    val areaType: String = "Urban",

    val recommendedLimitKmh: Int = 80,
    val statusDescription: String = "Manual Speed Limit"
)
