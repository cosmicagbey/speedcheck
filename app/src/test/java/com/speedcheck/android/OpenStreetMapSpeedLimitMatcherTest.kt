package com.speedcheck.android

import com.speedcheck.android.model.RoadCoordinate
import com.speedcheck.android.model.RoadSpeedLimitFeature
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
                accuracyMeters = 16f,
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
