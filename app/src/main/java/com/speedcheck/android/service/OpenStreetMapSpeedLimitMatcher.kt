package com.speedcheck.android.service

import com.speedcheck.android.model.MatchedRoadSpeedLimit
import com.speedcheck.android.model.RoadCoordinate
import com.speedcheck.android.model.RoadSpeedLimitFeature
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class OpenStreetMapSpeedLimitMatcher(features: List<RoadSpeedLimitFeature>) {
    private data class IndexedSegment(
        val index: Int,
        val feature: RoadSpeedLimitFeature,
        val start: RoadCoordinate,
        val end: RoadCoordinate,
        val bearingDegrees: Double
    )

    private data class Candidate(
        val feature: RoadSpeedLimitFeature,
        val distanceMeters: Double
    )

    private val segments = mutableListOf<IndexedSegment>()
    private val grid = HashMap<Long, MutableList<IndexedSegment>>()

    val roadCount: Int
    val segmentCount: Int
        get() = segments.size

    init {
        val eligibleFeatures = features.filter {
            it.highway in SUPPORTED_HIGHWAYS &&
                it.speedLimitKmh in MIN_SPEED_LIMIT_KMH..MAX_SPEED_LIMIT_KMH &&
                it.points.size >= 2
        }
        roadCount = eligibleFeatures.size

        eligibleFeatures.forEach { feature ->
            feature.points.zipWithNext().forEach { (start, end) ->
                if (start.isValidCoordinate() && end.isValidCoordinate()) {
                    val segment = IndexedSegment(
                        index = segments.size,
                        feature = feature,
                        start = start,
                        end = end,
                        bearingDegrees = calculateBearing(start, end)
                    )
                    segments += segment

                    val minLatitudeCell = cellFor(min(start.latitude, end.latitude))
                    val maxLatitudeCell = cellFor(maxOf(start.latitude, end.latitude))
                    val minLongitudeCell = cellFor(min(start.longitude, end.longitude))
                    val maxLongitudeCell = cellFor(maxOf(start.longitude, end.longitude))
                    for (latitudeCell in minLatitudeCell..maxLatitudeCell) {
                        for (longitudeCell in minLongitudeCell..maxLongitudeCell) {
                            grid.getOrPut(cellKey(latitudeCell, longitudeCell)) { mutableListOf() } += segment
                        }
                    }
                }
            }
        }
    }

    fun findMatch(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
        bearingDegrees: Float?,
        speedKmh: Float
    ): MatchedRoadSpeedLimit? {
        if (!latitude.isFinite() || !longitude.isFinite() ||
            latitude !in -90.0..90.0 || longitude !in -180.0..180.0
        ) {
            return null
        }

        val accuracy = accuracyMeters?.takeIf { it.isFinite() && it >= 0f } ?: return null
        if (accuracy > MAX_LOCATION_ACCURACY_METERS) return null

        val maximumDistance = maxOf(MIN_MATCH_DISTANCE_METERS, accuracy.toDouble())
        val latitudeCell = cellFor(latitude)
        val longitudeCell = cellFor(longitude)
        val checkedSegments = HashSet<Int>()
        val bestByWay = HashMap<String, Candidate>()
        val hasReliableBearing = bearingDegrees?.isFinite() == true && speedKmh >= MIN_HEADING_SPEED_KMH

        for (latCell in latitudeCell - 1..latitudeCell + 1) {
            for (lonCell in longitudeCell - 1..longitudeCell + 1) {
                val cellSegments = grid[cellKey(latCell, lonCell)] ?: continue
                for (segment in cellSegments) {
                    if (!checkedSegments.add(segment.index)) continue
                    if (hasReliableBearing &&
                        axisAngleDifference(bearingDegrees, segment.bearingDegrees) > MAX_HEADING_DIFFERENCE_DEGREES
                    ) {
                        continue
                    }

                    val distance = distanceToSegmentMeters(latitude, longitude, segment)
                    if (distance > maximumDistance) continue

                    val previous = bestByWay[segment.feature.id]
                    if (previous == null || distance < previous.distanceMeters) {
                        bestByWay[segment.feature.id] = Candidate(segment.feature, distance)
                    }
                }
            }
        }

        val candidates = bestByWay.values.sortedBy { it.distanceMeters }
        val closest = candidates.firstOrNull() ?: return null
        val ambiguityBand = maxOf(
            MIN_AMBIGUITY_DISTANCE_METERS,
            accuracy.toDouble() * AMBIGUITY_ACCURACY_FACTOR
        )
        val conflictingNearRoad = candidates.drop(1).any {
            it.feature.speedLimitKmh != closest.feature.speedLimitKmh &&
                it.distanceMeters - closest.distanceMeters <= ambiguityBand
        }
        if (conflictingNearRoad) return null

        return MatchedRoadSpeedLimit(
            id = "osm_${closest.feature.id}",
            roadName = closest.feature.roadName.ifBlank { "Mapped road" },
            speedLimitKmh = closest.feature.speedLimitKmh,
            areaType = closest.feature.highway.toAreaType()
        )
    }

    private fun distanceToSegmentMeters(
        latitude: Double,
        longitude: Double,
        segment: IndexedSegment
    ): Double {
        val latitudeScale = METERS_PER_DEGREE
        val longitudeScale = latitudeScale * cos(latitude * DEGREES_TO_RADIANS)
        val startX = (segment.start.longitude - longitude) * longitudeScale
        val startY = (segment.start.latitude - latitude) * latitudeScale
        val endX = (segment.end.longitude - longitude) * longitudeScale
        val endY = (segment.end.latitude - latitude) * latitudeScale
        val deltaX = endX - startX
        val deltaY = endY - startY
        val lengthSquared = deltaX * deltaX + deltaY * deltaY
        if (lengthSquared == 0.0) return hypot(startX, startY)

        val projection = (-(startX * deltaX + startY * deltaY) / lengthSquared).coerceIn(0.0, 1.0)
        return hypot(startX + projection * deltaX, startY + projection * deltaY)
    }

    private fun calculateBearing(start: RoadCoordinate, end: RoadCoordinate): Double {
        val latitudeRadians = start.latitude * DEGREES_TO_RADIANS
        val deltaLongitude = (end.longitude - start.longitude) * DEGREES_TO_RADIANS
        val y = sin(deltaLongitude) * cos(end.latitude * DEGREES_TO_RADIANS)
        val x = cos(latitudeRadians) * sin(end.latitude * DEGREES_TO_RADIANS) -
            sin(latitudeRadians) * cos(end.latitude * DEGREES_TO_RADIANS) * cos(deltaLongitude)
        return (atan2(y, x) * RADIANS_TO_DEGREES + 360.0) % 360.0
    }

    private fun axisAngleDifference(heading: Float, roadBearing: Double): Double {
        val directedDifference = abs((heading - roadBearing + 540.0) % 360.0 - 180.0)
        return min(directedDifference, 180.0 - directedDifference)
    }

    private fun RoadCoordinate.isValidCoordinate(): Boolean {
        return latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun String.toAreaType(): String = when (this) {
        "motorway", "motorway_link" -> "Motorway"
        "trunk", "trunk_link", "primary", "primary_link", "secondary", "secondary_link" -> "Highway"
        else -> "Urban"
    }

    private fun cellFor(coordinate: Double): Int = floor(coordinate / GRID_CELL_DEGREES).toInt()

    private fun cellKey(latitudeCell: Int, longitudeCell: Int): Long {
        return (latitudeCell.toLong() shl 32) xor (longitudeCell.toLong() and 0xffffffffL)
    }

    companion object {
        private const val GRID_CELL_DEGREES = 0.01
        private const val METERS_PER_DEGREE = 111_320.0
        private const val DEGREES_TO_RADIANS = PI / 180.0
        private const val RADIANS_TO_DEGREES = 180.0 / PI
        private const val MIN_SPEED_LIMIT_KMH = 5
        private const val MAX_SPEED_LIMIT_KMH = 160
        private const val MIN_MATCH_DISTANCE_METERS = 8.0
        private const val MIN_AMBIGUITY_DISTANCE_METERS = 5.0
        private const val AMBIGUITY_ACCURACY_FACTOR = 0.5
        private const val MAX_LOCATION_ACCURACY_METERS = 15f
        private const val MIN_HEADING_SPEED_KMH = 10f
        private const val MAX_HEADING_DIFFERENCE_DEGREES = 55.0
        private val SUPPORTED_HIGHWAYS = setOf(
            "motorway",
            "motorway_link",
            "trunk",
            "trunk_link",
            "primary",
            "primary_link",
            "secondary",
            "secondary_link",
            "tertiary",
            "tertiary_link"
        )
    }
}
