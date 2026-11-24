package com.speedcheck.android

import com.speedcheck.android.model.RoadCoordinate
import com.speedcheck.android.model.RoadSpeedLimitFeature
import com.speedcheck.android.model.SafetySpeedZone
import com.speedcheck.android.model.MatchedRoadSpeedLimit
import com.speedcheck.android.service.AutomaticSpeedLimitPolicy
import com.speedcheck.android.service.OpenStreetMapSpeedLimitMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class OpenStreetMapSpeedLimitMatcherTest {
    @Test
    fun matchesRoadGeometryBetweenRecordedPoints() {
        val matcher = OpenStreetMapSpeedLimitMatcher(
            listOf(
                road(
                    id = "101",
                    name = "Test Highway",
                    speedLimitKmh = 80,
                    points = listOf(
                        RoadCoordinate(5.0, -0.02),
                        RoadCoordinate(5.0, 0.02)
                    )
                )
            )
        )

        val match = matcher.findMatch(
            latitude = 5.00001,
            longitude = 0.0,
            accuracyMeters = 4f,
            bearingDegrees = 90f,
            speedKmh = 40f
        )

        assertNotNull(match)
        assertEquals("osm_101", match?.id)
        assertEquals("Test Highway", match?.roadName)
        assertEquals(80, match?.speedLimitKmh)
    }

    @Test
    fun rejectsLocationsWithPoorAccuracyOrOppositeHeading() {
        val matcher = OpenStreetMapSpeedLimitMatcher(
            listOf(
                road(
                    id = "102",
                    name = "East-West Road",
                    speedLimitKmh = 50,
                    points = listOf(
                        RoadCoordinate(5.0, -0.02),
                        RoadCoordinate(5.0, 0.02)
                    )
                )
            )
        )

        assertNull(
            matcher.findMatch(
                latitude = 5.0,
                longitude = 0.0,
                accuracyMeters = 21f,
                bearingDegrees = 90f,
                speedKmh = 40f
            )
        )
        assertNull(
            matcher.findMatch(
                latitude = 5.0,
                longitude = 0.0,
                accuracyMeters = 5f,
                bearingDegrees = 0f,
                speedKmh = 40f
            )
        )
    }

    @Test
    fun returnsNoLimitWhenNearbyRoadsDisagree() {
        val line = listOf(
            RoadCoordinate(5.0, -0.02),
            RoadCoordinate(5.0, 0.02)
        )
        val matcher = OpenStreetMapSpeedLimitMatcher(
            listOf(
                road("103", "Road A", 50, line),
                road("104", "Road B", 80, line)
            )
        )

        assertNull(
            matcher.findMatch(
                latitude = 5.0,
                longitude = 0.0,
                accuracyMeters = 5f,
                bearingDegrees = 90f,
                speedKmh = 40f
            )
        )
    }

    @Test
    fun ignoresResidentialAndServiceRoadsForAutomaticLimits() {
        val line = listOf(
            RoadCoordinate(5.0, -0.02),
            RoadCoordinate(5.0, 0.02)
        )
        val matcher = OpenStreetMapSpeedLimitMatcher(
            listOf(
                road("105", "Side Street", 30, line, highway = "residential"),
                road("106", "Main Road", 80, line, highway = "primary")
            )
        )

        val match = matcher.findMatch(
            latitude = 5.0,
            longitude = 0.0,
            accuracyMeters = 5f,
            bearingDegrees = 90f,
            speedKmh = 40f
        )

        assertEquals(1, matcher.roadCount)
        assertEquals(80, match?.speedLimitKmh)
    }

    @Test
    fun safetyZonesTakePriorityOverBuiltUpZones() {
        val matcher = OpenStreetMapSpeedLimitMatcher(
            emptyList(),
            listOf(
                SafetySpeedZone("town", "Mapped town", "built_up", 5.0, 0.0, 500.0),
                SafetySpeedZone("school", "Mapped school", "school", 5.0, 0.0, 200.0)
            )
        )

        val match = matcher.findSafetyZone(5.0, 0.0, 5f)

        assertEquals("school", match?.id)
    }

    @Test
    fun safetyZoneMatchingRejectsPoorAccuracyAndUnknownAreas() {
        val matcher = OpenStreetMapSpeedLimitMatcher(
            emptyList(),
            listOf(SafetySpeedZone("town", "Mapped town", "built_up", 5.0, 0.0, 500.0))
        )

        assertNull(matcher.findSafetyZone(5.0, 0.0, 21f))
        assertNull(matcher.findSafetyZone(5.02, 0.0, 5f))
    }

    @Test
    fun automaticLimitPolicyUsesConservativeAreaRules() {
        val highwayLimit = MatchedRoadSpeedLimit("road", "Highway", 100, "Highway")
        val urbanLimit = MatchedRoadSpeedLimit("urban", "Urban road", 30, "Urban")
        val school = SafetySpeedZone("school", "School", "school", 5.0, 0.0, 200.0)
        val town = SafetySpeedZone("town", "Town", "built_up", 5.0, 0.0, 500.0)

        assertEquals(30, AutomaticSpeedLimitPolicy.resolve(highwayLimit, school).limitKmh)
        assertEquals(50, AutomaticSpeedLimitPolicy.resolve(highwayLimit, town).limitKmh)
        assertEquals(30, AutomaticSpeedLimitPolicy.resolve(urbanLimit, town).limitKmh)
        assertEquals(30, AutomaticSpeedLimitPolicy.resolve(null, null).limitKmh)
        assertEquals(100, AutomaticSpeedLimitPolicy.resolve(highwayLimit, null).limitKmh)
    }

    @Test
    fun lookaheadPredictsUpcomingHighwayAheadOfTime() {
        val roadA = road(
            id = "urban_road",
            name = "Urban Access Road",
            speedLimitKmh = 50,
            points = listOf(
                RoadCoordinate(5.0, -0.0010),
                RoadCoordinate(5.0, 0.0000)
            ),
            highway = "secondary"
        )
        val roadB = road(
            id = "highway_road",
            name = "George Walker Bush Highway (N1)",
            speedLimitKmh = 80,
            points = listOf(
                RoadCoordinate(5.0, 0.0001),
                RoadCoordinate(5.0, 0.0020)
            ),
            highway = "trunk"
        )
        val matcher = OpenStreetMapSpeedLimitMatcher(listOf(roadA, roadB))

        // Position: near end of Road A (lon = -0.0001, speed = 55 km/h, heading East 90°)
        // Current match is Road A (50 km/h)
        val currentOnly = matcher.findMatch(
            latitude = 5.0,
            longitude = -0.0001,
            accuracyMeters = 5f,
            bearingDegrees = 90f,
            speedKmh = 55f
        )
        assertEquals(50, currentOnly?.speedLimitKmh)

        // Lookahead projected ~38m forward reaches onto Road B (80 km/h)
        val lookahead = matcher.findMatchWithLookahead(
            latitude = 5.0,
            longitude = -0.0001,
            accuracyMeters = 5f,
            bearingDegrees = 90f,
            speedKmh = 55f
        )
        assertNotNull(lookahead)
        assertEquals(80, lookahead?.speedLimitKmh)
        assertEquals("George Walker Bush Highway (N1)", lookahead?.roadName)
    }

    @Test
    fun lookaheadPreservesHighwaySpeedLimitUntilActuallyEnteringLowerSpeedRoad() {
        val highway = road(
            id = "highway_road",
            name = "Highway",
            speedLimitKmh = 80,
            points = listOf(
                RoadCoordinate(5.0, -0.0010),
                RoadCoordinate(5.0, 0.0000)
            ),
            highway = "trunk"
        )
        val exitRamp = road(
            id = "exit_ramp",
            name = "Exit Ramp",
            speedLimitKmh = 50,
            points = listOf(
                RoadCoordinate(5.0, 0.0001),
                RoadCoordinate(5.0, 0.0020)
            ),
            highway = "secondary"
        )
        val matcher = OpenStreetMapSpeedLimitMatcher(listOf(highway, exitRamp))

        // On highway cruising at 75 km/h, approaching 50 km/h ramp
        val match = matcher.findMatchWithLookahead(
            latitude = 5.0,
            longitude = -0.0001,
            accuracyMeters = 5f,
            bearingDegrees = 90f,
            speedKmh = 75f
        )
        // Preserves 80 km/h so driver is not warned while still traveling on highway
        assertEquals(80, match?.speedLimitKmh)
    }

    private fun road(
        id: String,
        name: String,
        speedLimitKmh: Int,
        points: List<RoadCoordinate>,
        highway: String = "primary"
    ) = RoadSpeedLimitFeature(
        id = id,
        roadName = name,
        highway = highway,
        speedLimitKmh = speedLimitKmh,
        points = points
    )
}
