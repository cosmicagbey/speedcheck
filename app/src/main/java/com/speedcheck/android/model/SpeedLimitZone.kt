package com.speedcheck.android.model

/**
 * Represents a named road segment in Ghana with its legal speed limit.
 * These legacy point zones provide approximate limits when no confident
 * OpenStreetMap road match is available.
 *
 * OpenStreetMap road matches and camera-zone limits may take precedence.
 */
interface SpeedLimitInfo {
    val id: String
    val roadName: String
    val speedLimitKmh: Int
    val areaType: String
}

data class SpeedLimitZone(
    override val id: String,
    override val roadName: String,
    val latitude: Double,
    val longitude: Double,
    override val speedLimitKmh: Int,
    val radiusMeters: Float = 1200f,
    override val areaType: String = "Urban"
) : SpeedLimitInfo

data class MatchedRoadSpeedLimit(
    override val id: String,
    override val roadName: String,
    override val speedLimitKmh: Int,
    override val areaType: String
) : SpeedLimitInfo

data class RoadCoordinate(
    val latitude: Double,
    val longitude: Double
)

data class RoadSpeedLimitFeature(
    val id: String,
    val roadName: String,
    val highway: String,
    val speedLimitKmh: Int,
    val points: List<RoadCoordinate>
)
