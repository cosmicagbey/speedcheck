package com.speedcheck.android.service

import com.speedcheck.android.model.MatchedRoadSpeedLimit
import com.speedcheck.android.model.RoadCoordinate
import com.speedcheck.android.model.RoadSpeedLimitFeature
import com.speedcheck.android.model.SafetySpeedZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class OpenStreetMapSpeedLimitMatcher(
    features: List<RoadSpeedLimitFeature>,
    safetyZones: List<SafetySpeedZone> = emptyList()
) {
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
    private val safetyGrid = HashMap<Long, MutableList<SafetySpeedZone>>()

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

        safetyZones.forEach { zone ->
            if (!zone.latitude.isFinite() || zone.latitude !in -90.0..90.0 ||
                !zone.longitude.isFinite() || zone.longitude !in -180.0..180.0 ||
                !zone.radiusMeters.isFinite() || zone.radiusMeters <= 0.0 ||
                zone.category !in SUPPORTED_ZONE_CATEGORIES
            ) {
                return@forEach
            }

            val latitudeRadius = zone.radiusMeters / METERS_PER_DEGREE
            val longitudeRadius = latitudeRadius / cos(zone.latitude * DEGREES_TO_RADIANS)
            val minLatitudeCell = cellFor(zone.latitude - latitudeRadius)
            val maxLatitudeCell = cellFor(zone.latitude + latitudeRadius)
            val minLongitudeCell = cellFor(zone.longitude - longitudeRadius)
            val maxLongitudeCell = cellFor(zone.longitude + longitudeRadius)
            for (latitudeCell in minLatitudeCell..maxLatitudeCell) {
                for (longitudeCell in minLongitudeCell..maxLongitudeCell) {
                    safetyGrid.getOrPut(cellKey(latitudeCell, longitudeCell)) { mutableListOf() } += zone
                }
            }
        }
    }

    /**
     * Finds the applicable speed limit matching the current position or an upcoming
     * road along the driver's forward trajectory (predictive lookahead matching).
     *
     * When a driver is heading toward a highway or road with a different (especially higher)
     * speed limit, projecting forward by 2.0 to 3.5 seconds detects the upcoming road ahead of time.
     * This avoids latency and eliminates false overspeed warnings as the driver accelerates to merge.
     */
    fun findMatchWithLookahead(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?,
        bearingDegrees: Float?,
        speedKmh: Float,
        lookaheadSeconds: Double = 2.5
    ): MatchedRoadSpeedLimit? {
        val currentMatch = findMatch(latitude, longitude, accuracyMeters, bearingDegrees, speedKmh)

        if (bearingDegrees == null || !bearingDegrees.isFinite() || speedKmh < MIN_HEADING_SPEED_KMH) {
            return currentMatch
        }

        // Project forward along heading vector by 2.5 seconds of travel (clamped between 25m and 70m)
        val speedMps = speedKmh * (1000.0 / 3600.0)
        val lookaheadMeters = (speedMps * lookaheadSeconds).coerceIn(25.0, 70.0)
        val (projLat, projLon) = projectForward(latitude, longitude, bearingDegrees.toDouble(), lookaheadMeters)

        val lookaheadMatch = findMatch(projLat, projLon, accuracyMeters, bearingDegrees, speedKmh)

        return when {
            // Case 1: Approaching a higher speed limit road ahead (e.g. accelerating onto a highway).
            // Adopt early so driver accelerating onto the highway does not get false overspeed alerts!
            lookaheadMatch != null && (currentMatch == null || lookaheadMatch.speedLimitKmh > currentMatch.speedLimitKmh) -> {
                lookaheadMatch
            }
            // Case 2: Entering a mapped road from an unmapped area ahead
            currentMatch == null && lookaheadMatch != null -> {
                lookaheadMatch
            }
            // Case 3: Retain current match (including when upcoming road has lower limit until driver reaches it).
            else -> currentMatch
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

                    val isHighway = segment.feature.highway in HIGHWAY_TYPES
                    val baseDistance = if (isHighway) HIGHWAY_MATCH_DISTANCE_METERS else MIN_MATCH_DISTANCE_METERS
                    val maximumDistance = maxOf(baseDistance, accuracy.toDouble())

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

    fun findSafetyZone(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float?
    ): SafetySpeedZone? {
        if (!latitude.isFinite() || !longitude.isFinite() ||
            latitude !in -90.0..90.0 || longitude !in -180.0..180.0
        ) {
            return null
        }
        val accuracy = accuracyMeters?.takeIf { it.isFinite() && it >= 0f } ?: return null
        if (accuracy > MAX_LOCATION_ACCURACY_METERS) return null

        val zones = safetyGrid[cellKey(cellFor(latitude), cellFor(longitude))] ?: return null
        val checkedZones = HashSet<String>()
        var closestSafety: SafetySpeedZone? = null
        var closestSafetyDistance = Double.MAX_VALUE
        var closestBuiltUp: SafetySpeedZone? = null
        var closestBuiltUpDistance = Double.MAX_VALUE

        for (zone in zones) {
            if (!checkedZones.add(zone.id)) continue
            val distance = distanceToZoneMeters(latitude, longitude, zone)
            if (distance > zone.radiusMeters + accuracy) continue
            if (zone.category == BUILT_UP_CATEGORY) {
                if (distance < closestBuiltUpDistance) {
                    closestBuiltUp = zone
                    closestBuiltUpDistance = distance
                }
            } else if (distance < closestSafetyDistance) {
                closestSafety = zone
                closestSafetyDistance = distance
            }
        }
        return closestSafety ?: closestBuiltUp
    }

    private fun distanceToZoneMeters(
        latitude: Double,
        longitude: Double,
        zone: SafetySpeedZone
    ): Double {
        val longitudeScale = METERS_PER_DEGREE * cos(latitude * DEGREES_TO_RADIANS)
        return hypot(
            (zone.latitude - latitude) * METERS_PER_DEGREE,
            (zone.longitude - longitude) * longitudeScale
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
        private const val MIN_MATCH_DISTANCE_METERS = 18.0
        private const val HIGHWAY_MATCH_DISTANCE_METERS = 26.0
        private const val MIN_AMBIGUITY_DISTANCE_METERS = 5.0
        private const val AMBIGUITY_ACCURACY_FACTOR = 0.5
        private const val MAX_LOCATION_ACCURACY_METERS = 20f
        private const val MIN_HEADING_SPEED_KMH = 10f
        private const val MAX_HEADING_DIFFERENCE_DEGREES = 55.0
        private const val BUILT_UP_CATEGORY = "built_up"
        private val SUPPORTED_ZONE_CATEGORIES =
            setOf("school", "playground", "health", "worship", "market", BUILT_UP_CATEGORY)
        private val HIGHWAY_TYPES = setOf(
            "motorway", "motorway_link", "trunk", "trunk_link", "primary", "primary_link"
        )
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

        fun projectForward(
            latitude: Double,
            longitude: Double,
            bearingDegrees: Double,
            distanceMeters: Double
        ): Pair<Double, Double> {
            val angularDistance = distanceMeters / 6_371_000.0
            val latRad = latitude * DEGREES_TO_RADIANS
            val lonRad = longitude * DEGREES_TO_RADIANS
            val bearingRad = bearingDegrees * DEGREES_TO_RADIANS

            val targetLatRad = kotlin.math.asin(
                sin(latRad) * cos(angularDistance) +
                    cos(latRad) * sin(angularDistance) * cos(bearingRad)
            )
            val targetLonRad = lonRad + atan2(
                sin(bearingRad) * sin(angularDistance) * cos(latRad),
                cos(angularDistance) - sin(latRad) * sin(targetLatRad)
            )
            return Pair(targetLatRad * RADIANS_TO_DEGREES, targetLonRad * RADIANS_TO_DEGREES)
        }
    }
}
