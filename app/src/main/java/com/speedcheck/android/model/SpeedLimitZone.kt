package com.speedcheck.android.model

/** Common display information for a location-based speed limit. */
interface SpeedLimitInfo {
    val id: String
    val roadName: String
    val speedLimitKmh: Int
    val areaType: String
}

/** A mapped safety or built-up-area speed limit. */
data class SpeedLimitZone(
    override val id: String,
    override val roadName: String,
    val latitude: Double,
    val longitude: Double,
    override val speedLimitKmh: Int,
    val radiusMeters: Float = 1200f,
    override val areaType: String = "Urban"
) : SpeedLimitInfo

/** An explicit OpenStreetMap speed limit matched to road geometry. */
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

data class SafetySpeedZone(
    val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
)
