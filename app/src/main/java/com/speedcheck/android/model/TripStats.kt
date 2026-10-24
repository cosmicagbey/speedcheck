package com.speedcheck.android.model

/**
 * Statistics for the current driving session.
 */
data class TripStats(
    val currentSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val averageSpeedKmh: Float = 0f,
    val distanceKm: Float = 0f,
    val sessionStartTimeMillis: Long = 0L,
    val speedReadingsCount: Int = 0
)
