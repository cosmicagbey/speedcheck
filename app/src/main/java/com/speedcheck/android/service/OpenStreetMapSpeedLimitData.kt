package com.speedcheck.android.service

import android.content.res.AssetManager
import android.util.JsonReader
import com.speedcheck.android.model.RoadCoordinate
import com.speedcheck.android.model.RoadSpeedLimitFeature
import com.speedcheck.android.model.SafetySpeedZone
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object OpenStreetMapSpeedLimitData {
    private const val ASSET_NAME = "ghana_speed_limits.json"
    private const val SUPPORTED_SCHEMA_VERSION = 2
    private const val REQUIRED_LICENSE = "Open Data Commons Open Database License (ODbL) v1.0"

    fun load(assetManager: AssetManager): OpenStreetMapSpeedLimitMatcher {
        var schemaVersion: Int? = null
        var license: String? = null
        var features: List<RoadSpeedLimitFeature>? = null
        var safetyZones: List<SafetySpeedZone>? = null

        JsonReader(InputStreamReader(assetManager.open(ASSET_NAME), StandardCharsets.UTF_8)).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "schemaVersion" -> schemaVersion = reader.nextInt()
                    "license" -> license = reader.nextString()
                    "ways" -> features = readFeatures(reader)
                    "safetyZones" -> safetyZones = readSafetyZones(reader)
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
        val areaZones = safetyZones ?: throw IOException("The OSM speed-limit dataset has no safety-zone data")
        val matcher = OpenStreetMapSpeedLimitMatcher(roadFeatures, areaZones)
        if (matcher.roadCount == 0) {
            throw IOException("The OSM speed-limit dataset contains no supported road segments")
        }
        return matcher
    }

    private fun readSafetyZones(reader: JsonReader): List<SafetySpeedZone> {
            val zones = mutableListOf<SafetySpeedZone>()
            reader.beginArray()
            while (reader.hasNext()) {
                var id: String? = null
                var name = ""
                var category: String? = null
                var latitude: Double? = null
                var longitude: Double? = null
                var radiusMeters: Double? = null

                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "id" -> id = reader.nextString()
                        "name" -> name = reader.nextString()
                        "category" -> category = reader.nextString()
                        "latitude" -> latitude = reader.nextDouble()
                        "longitude" -> longitude = reader.nextDouble()
                        "radiusMeters" -> radiusMeters = reader.nextDouble()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()

                val zoneId = id ?: throw IOException("An OSM safety zone is missing its ID")
                val zoneCategory = category ?: throw IOException("OSM safety zone $zoneId is missing its category")
                val zoneLatitude = latitude ?: throw IOException("OSM safety zone $zoneId is missing its latitude")
                val zoneLongitude = longitude ?: throw IOException("OSM safety zone $zoneId is missing its longitude")
                val zoneRadius = radiusMeters ?: throw IOException("OSM safety zone $zoneId is missing its radius")
                if (zoneCategory !in SUPPORTED_ZONE_CATEGORIES ||
                    !zoneLatitude.isFinite() || zoneLatitude !in -90.0..90.0 ||
                    !zoneLongitude.isFinite() || zoneLongitude !in -180.0..180.0 ||
                    !zoneRadius.isFinite() || zoneRadius !in 1.0..10_000.0
                ) {
                    throw IOException("OSM safety zone $zoneId contains invalid data")
                }
                zones += SafetySpeedZone(
                    id = zoneId,
                    name = name,
                    category = zoneCategory,
                    latitude = zoneLatitude,
                    longitude = zoneLongitude,
                    radiusMeters = zoneRadius
                )
            }
            reader.endArray()
            return zones
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

    private val SUPPORTED_ZONE_CATEGORIES =
        setOf("school", "playground", "health", "worship", "market", "built_up")
}
