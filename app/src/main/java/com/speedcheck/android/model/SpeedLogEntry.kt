package com.speedcheck.android.model

import java.util.UUID

/**
 * Represents a historical driving speed and location snapshot.
 * Serves as verifiable evidence for drivers against false speeding tickets.
 */
data class SpeedLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Float,
    val maxSpeedKmh: Float,
    val speedLimitKmh: Int,
    val alertStatus: SpeedAlertStatus = SpeedAlertStatus.NORMAL,
    val isAutoDetect: Boolean = true,
    val complianceStatus: String = "Compliant",
    val note: String = ""
)
