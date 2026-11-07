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
    val description: String = "",
    val isMobilePatrol: Boolean = false
)

/**
 * Live detection result for the driver's current position relative to
 * speed camera zones and named or OpenStreetMap road speed limits.
 *
 * Camera alerts remain independent from the selected speed-limit source.
 */
data class CameraDetectionResult(
    val isAutoDetectActive: Boolean = false,

    /** Non-null when the driver is inside a known speed camera zone. */
    val activeZone: SpeedCameraZone? = null,

    /** Non-null when a named or mapped road limit matches; may coexist with a camera zone. */
    val activeSpeedLimitZone: SpeedLimitInfo? = null,

    val distanceToCameraMeters: Float = Float.MAX_VALUE,
    val isMovingTowardsCamera: Boolean = false,

    /** True when the driver is inside a speed camera zone. */
    val hasCameraAhead: Boolean = false,

    /** True when the camera zone is a mobile/rotating police speedcam hotspot. */
    val isMobilePatrol: Boolean = false,

    /** The detected road name (from camera zone or speed limit zone). */
    val detectedRoadName: String? = null,

    /** The area type: "Urban", "Highway", "Motorway", "School Zone", etc. */
    val areaType: String = "Urban",

    val recommendedLimitKmh: Int = 30,
    val statusDescription: String = "Manual Speed Limit"
)
