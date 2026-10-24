package com.speedcheck.android.model

/**
 * Status of the driver's current speed relative to the user-set speed limit.
 */
enum class SpeedAlertStatus {
    /** Speed is safely below the warning threshold. */
    NORMAL,

    /**
     * Speed has reached the approaching threshold (e.g., target limit - 5 km/h).
     * Voice prompt: "You are approaching [limit] kilometers per hour."
     */
    APPROACHING,

    /**
     * Speed has exceeded the user's set limit.
     * Voice prompt: "Slow down. Current speed is above your limit."
     */
    EXCEEDED
}
