package com.speedcheck.android.service

import android.content.res.AssetManager
import android.util.JsonReader
import com.speedcheck.android.model.RoadCoordinate
import com.speedcheck.android.model.RoadSpeedLimitFeature
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object OpenStreetMapSpeedLimitData {
    private const val ASSET_NAME = "ghana_speed_limits.json"
    private const val SUPPORTED_SCHEMA_VERSION = 1
    private const val REQUIRED_LICENSE = "Open Data Commons Open Database License (ODbL) v1.0"

    fun load(assetManager: AssetManager): OpenStreetMapSpeedLimitMatcher {
        var schemaVersion: Int? = null
        var license: String? = null
        var features: List<RoadSpeedLimitFeature>? = null

        JsonReader(InputStreamReader(assetManager.open(ASSET_NAME), StandardCharsets.UTF_8)).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "schemaVersion" -> schemaVersion = reader.nextInt()
                    "license" -> license = reader.nextString()
                    "ways" -> features = readFeatures(reader)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }

        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw IOException("Unsupported OSM speed-limit dataset schema: $schemaVersion")
        }
        if (license != REQUIRED_LICENSE) {
            throw IOException("The OSM speed-limit dataset is missing its expected ODbL license metadata")
        }

        val roadFeatures = features ?: throw IOException("The OSM speed-limit dataset has no road data")
        val matcher = OpenStreetMapSpeedLimitMatcher(roadFeatures)
        if (matcher.roadCount == 0) {
            throw IOException("The OSM speed-limit dataset contains no supported road segments")
        }
        return matcher
    }

    private fun readFeatures(reader: JsonReader): List<RoadSpeedLimitFeature> {
        val features = mutableListOf<RoadSpeedLimitFeature>()
        reader.beginArray()
        while (reader.hasNext()) {
            var id: String? = null
            var name = ""
            var highway: String? = null
            var speedLimitKmh: Int? = null
            var points: List<RoadCoordinate>? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> id = reader.nextString()
                    "name" -> name = reader.nextString()
                    "highway" -> highway = reader.nextString()
                    "speedLimitKmh" -> speedLimitKmh = reader.nextInt()
                    "points" -> points = readPoints(reader)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            val featureId = id ?: throw IOException("An OSM speed-limit way is missing its ID")
            val roadType = highway ?: throw IOException("OSM speed-limit way $featureId is missing its road type")
            val speedLimit = speedLimitKmh
                ?: throw IOException("OSM speed-limit way $featureId is missing its speed limit")
            val geometry = points
                ?: throw IOException("OSM speed-limit way $featureId is missing its geometry")

            if (speedLimit !in 5..160 || geometry.size < 2 ||
                geometry.any { !it.latitude.isFinite() || !it.longitude.isFinite() }
            ) {
                throw IOException("OSM speed-limit way $featureId contains invalid road data")
            }

            features += RoadSpeedLimitFeature(
                id = featureId,
                roadName = name,
                highway = roadType,
                speedLimitKmh = speedLimit,
                points = geometry
            )
        }
        reader.endArray()
        return features
    }

    private fun readPoints(reader: JsonReader): List<RoadCoordinate> {
        val points = mutableListOf<RoadCoordinate>()
        reader.beginArray()
        while (reader.hasNext()) {
            reader.beginArray()
            if (!reader.hasNext()) {
                throw IOException("An OSM speed-limit coordinate is empty")
            }
            val latitude = reader.nextDouble()
            if (!reader.hasNext()) {
                throw IOException("An OSM speed-limit coordinate has no longitude")
            }
            val longitude = reader.nextDouble()
            if (reader.hasNext()) {
                throw IOException("An OSM speed-limit coordinate has extra values")
            }
            reader.endArray()

            if (!latitude.isFinite() || latitude !in -90.0..90.0 ||
                !longitude.isFinite() || longitude !in -180.0..180.0
            ) {
                throw IOException("An OSM speed-limit coordinate is out of range")
            }
            points += RoadCoordinate(latitude, longitude)
        }
        reader.endArray()
        return points
    }
}
