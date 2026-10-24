package com.example.speedcheck.data

/**
 * Data class representing user configuration and settings.
 */
data class UserPreferences(
    /** The target speed limit set by the user in km/h. Default: 80 km/h. */
    val speedLimitKmh: Int = 80,

    /**
     * Offset in km/h before the speed limit when the approaching alert is triggered.
     * For example, with limit 30 and offset 5, the alert triggers at 25 km/h.
     */
    val approachingOffsetKmh: Int = 5,

    /** Whether voice alerts are enabled. */
    val voiceAlertsEnabled: Boolean = true,

    /** Whether the approaching alert (e.g. at 25 km/h for limit 30) is enabled. */
    val approachingAlertEnabled: Boolean = true,

    /** Preferred voice style: "FEMALE", "MALE", or "DEFAULT". */
    val voiceGender: String = "FEMALE",

    /** TTS speech rate: 0.8f (slower) to 1.3f (faster). */
    val speechRate: Float = 1.0f,

    /** TTS speech pitch: 0.8f (deeper) to 1.3f (higher pitch). */
    val speechPitch: Float = 1.0f,

    /** Cooldown interval between repeated "over limit" alerts in seconds. */
    val alertCooldownSeconds: Int = 15,

    /** Whether background monitoring service is currently toggled on by user. */
    val monitoringEnabled: Boolean = false,

    /**
     * Whether Auto-Detect speed limit mode is active.
     * When enabled, the app automatically detects the driver's location, heading,
     * and approaching speed camera zones (N1, Fiesta Royale, Baah Jct, Kawukudi, Kanda, Borteyman).
     */
    val autoDetectSpeedLimit: Boolean = true,

    /** Whether to automatically detect vehicle motion (~15-20 km/h) and enter active monitoring. */
    val autoDetectVehicleMovement: Boolean = true,

    /** Whether the service should auto-resume when phone restarts. */
    val autoStartOnBoot: Boolean = true,

    /** High contrast display mode for daylight/night driving. */
    val highContrastMode: Boolean = true
)
