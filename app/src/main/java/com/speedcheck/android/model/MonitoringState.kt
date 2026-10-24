package com.speedcheck.android.model

/**
 * Represents the operational state of the speed monitoring service.
 */
enum class MonitoringState {
    /** Monitoring service is stopped by the user. */
    STOPPED,

    /** Service is active, GPS is connected, waiting for vehicle movement (>15-20 km/h). */
    WAITING_FOR_MOVEMENT,

    /** Vehicle is moving; high-frequency speed monitoring and voice alerts are active. */
    ACTIVE_MONITORING,

    /** Vehicle has stopped for an extended period; polling is throttled to conserve battery. */
    PAUSED_STATIONARY
}
