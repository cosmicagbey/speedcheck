package com.speedcheck.android.service

import com.speedcheck.android.model.MatchedRoadSpeedLimit
import com.speedcheck.android.model.SafetySpeedZone
import com.speedcheck.android.model.SpeedLimitInfo
import com.speedcheck.android.model.SpeedLimitZone

internal data class AutomaticSpeedLimitResult(
    val limitKmh: Int,
    val zone: SpeedLimitInfo?,
    val areaType: String
)

internal object AutomaticSpeedLimitPolicy {
    fun resolve(
        mappedRoadLimit: MatchedRoadSpeedLimit?,
        safetyZone: SafetySpeedZone?
    ): AutomaticSpeedLimitResult {
        if (safetyZone != null && safetyZone.category != "built_up") {
            return safetyZone.toLimitZone(30, safetyZone.category.toAreaName())
        }

        if (safetyZone?.category == "built_up") {
            val limit = minOf(mappedRoadLimit?.speedLimitKmh ?: 50, 50)
            if (mappedRoadLimit != null && mappedRoadLimit.speedLimitKmh <= 50) {
                return AutomaticSpeedLimitResult(limit, mappedRoadLimit, mappedRoadLimit.areaType)
            }
            return safetyZone.toLimitZone(limit, "Built-up area")
        }

        if (mappedRoadLimit != null) {
            return AutomaticSpeedLimitResult(
                mappedRoadLimit.speedLimitKmh,
                mappedRoadLimit,
                mappedRoadLimit.areaType
            )
        }

        return AutomaticSpeedLimitResult(30, null, "Unmapped")
    }

    private fun SafetySpeedZone.toLimitZone(limitKmh: Int, area: String): AutomaticSpeedLimitResult {
        val zone = SpeedLimitZone(
            id = "osm_zone_$id",
            roadName = name.ifBlank { area },
            latitude = latitude,
            longitude = longitude,
            speedLimitKmh = limitKmh,
            radiusMeters = radiusMeters.toFloat(),
            areaType = area
        )
        return AutomaticSpeedLimitResult(limitKmh, zone, area)
    }

    private fun String.toAreaName(): String =
        replaceFirstChar { character -> character.uppercase() }
}
