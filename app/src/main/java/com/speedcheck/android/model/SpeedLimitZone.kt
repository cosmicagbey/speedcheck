package com.speedcheck.android.model

/**
 * Represents a named road segment in Ghana with its legal speed limit.
 * These zones do NOT have speed cameras — they provide accurate speed limits
 * based on the driver's GPS position and the road they're on.
 *
 * Detection priority: SpeedCameraZone > SpeedLimitZone > Ghana default.
 */
data class SpeedLimitZone(
    val id: String,
    val roadName: String,
    val latitude: Double,
    val longitude: Double,
    val speedLimitKmh: Int,
    val radiusMeters: Float = 1200f,
    val areaType: String = "Urban" // "Urban", "Highway", "Motorway", "School Zone", "Residential"
)
