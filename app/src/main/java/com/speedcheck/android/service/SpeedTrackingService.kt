package com.speedcheck.android.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.speedcheck.android.data.PreferencesRepository
import com.speedcheck.android.data.SpeedLogRepository
import com.speedcheck.android.data.UserPreferences
import com.speedcheck.android.model.CameraDetectionResult
import com.speedcheck.android.model.MonitoringState
import com.speedcheck.android.model.SpeedAlertStatus
import com.speedcheck.android.model.SpeedLogEntry
import com.speedcheck.android.model.TripStats
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground Service responsible for continuous GPS speed monitoring,
 * automatic location-based speed camera detection (Ghana MTTD corridors),
 * vehicle movement detection, and Text-to-Speech overspeed alerts.
 */
class SpeedTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var preferencesJob: Job? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var vibrationManager: com.speedcheck.android.util.VibrationManager
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var speedLogRepository: SpeedLogRepository

    private var currentPreferences = UserPreferences()

    // Vehicle movement detection and idle state counters
    private var consecutiveMovingReadings = 0
    private var consecutiveStationarySeconds = 0
    private var lastLocation: Location? = null
    private var smoothedSpeedKmh = 0f
    private var currentMonitoringState = MonitoringState.STOPPED

    // GPS calibration: suppress voice alerts for the first GPS_CALIBRATION_WINDOW_MS milliseconds
    // after monitoring starts, so the speed limit for the user's current location is confirmed
    // before any alert fires. After calibration, a summary is announced.
    private var gpsCalibrationStartMs = 0L
    private var calibrationAnnouncementFired = false

    // Speed camera zone announcement & log tracker
    private var lastAnnouncedCameraZoneId: String? = null
    private var lastLogSnapshotTimeMs = 0L
    private var lastLoggedCameraZoneId: String? = null

    // Trip Statistics
    private var tripStats = TripStats()
    private var totalSpeedSum = 0.0
    private var speedReadingsCount = 0

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            handleNewLocation(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SpeedTrackingService onCreate")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationHelper = NotificationHelper(this)
        ttsManager = TextToSpeechManager(this)
        vibrationManager = com.speedcheck.android.util.VibrationManager(this)
        preferencesRepository = PreferencesRepository.getInstance(this)
        speedLogRepository = SpeedLogRepository.getInstance(this)

        observePreferences()
    }

    private fun observePreferences() {
        preferencesJob = serviceScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                currentPreferences = prefs
                ttsManager.updateVoiceConfig(
                    gender = prefs.voiceGender,
                    rate = prefs.speechRate,
                    pitch = prefs.speechPitch
                )

                val effectiveLimit = resolveEffectiveLimit()
                notificationHelper.updateNotification(
                    speedKmh = _currentSpeedFlow.value,
                    limitKmh = effectiveLimit,
                    monitoringState = currentMonitoringState,
                    alertStatus = _alertStatusFlow.value,
                    cameraZoneName = _cameraDetectionFlow.value.activeZone?.name
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_MONITORING
        Log.d(TAG, "onStartCommand action: $action")

        when (action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
        }

        return START_STICKY
    }

    private fun startMonitoring() {
        if (currentMonitoringState != MonitoringState.STOPPED) {
            return
        }

        // Initialize state
        currentMonitoringState = if (currentPreferences.autoDetectVehicleMovement) {
            MonitoringState.WAITING_FOR_MOVEMENT
        } else {
            MonitoringState.ACTIVE_MONITORING
        }
        _monitoringStateFlow.value = currentMonitoringState

        tripStats = TripStats(sessionStartTimeMillis = System.currentTimeMillis())
        _tripStatsFlow.value = tripStats
        totalSpeedSum = 0.0
        speedReadingsCount = 0
        smoothedSpeedKmh = 0f
        lastAnnouncedCameraZoneId = null

        // Start GPS calibration window: suppress alerts for the first 12 seconds so the
        // detector can confirm the actual speed limit for this location before speaking.
        gpsCalibrationStartMs = System.currentTimeMillis()
        calibrationAnnouncementFired = false

        val effectiveLimit = resolveEffectiveLimit()
        val notification = notificationHelper.buildForegroundNotification(
            speedKmh = 0f,
            limitKmh = effectiveLimit,
            monitoringState = currentMonitoringState,
            alertStatus = SpeedAlertStatus.NORMAL
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID_FOREGROUND,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID_FOREGROUND, notification)
        }

        serviceScope.launch {
            preferencesRepository.updateMonitoringEnabled(true)
        }

        requestLocationUpdates(highFrequency = true)
        Log.i(TAG, "Monitoring started successfully.")
    }

    private fun stopMonitoring() {
        Log.i(TAG, "Stopping monitoring.")
        fusedLocationClient.removeLocationUpdates(locationCallback)

        currentMonitoringState = MonitoringState.STOPPED
        _monitoringStateFlow.value = MonitoringState.STOPPED
        _currentSpeedFlow.value = 0f
        smoothedSpeedKmh = 0f
        _alertStatusFlow.value = SpeedAlertStatus.NORMAL
        _cameraDetectionFlow.value = CameraDetectionResult()
        lastAnnouncedCameraZoneId = null
        gpsCalibrationStartMs = 0L
        calibrationAnnouncementFired = false

        serviceScope.launch {
            preferencesRepository.updateMonitoringEnabled(false)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates(highFrequency: Boolean) {
        val intervalMs = if (highFrequency) 1000L else 5000L
        val minIntervalMs = if (highFrequency) 500L else 2000L

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs
        )
            .setMinUpdateIntervalMillis(minIntervalMs)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException requesting location updates: ${e.message}")
        }
    }

    private fun handleNewLocation(location: Location) {
        // Calculate raw GPS speed in km/h
        val rawSpeedKmh = calculateSpeedKmh(location)

        // Apply speedometer offset calibration: aligns GPS reading with the vehicle's instrument
        // cluster. The offset is set by the user in Settings (range: -15 to +15 km/h).
        val speedKmh = if (rawSpeedKmh > 0f) {
            (rawSpeedKmh + currentPreferences.speedometerOffsetKmh).coerceAtLeast(0f)
        } else {
            0f
        }
        _currentSpeedFlow.value = speedKmh

        // Update Trip Statistics
        updateTripStats(speedKmh, location)

        // Evaluate Vehicle Motion State Machine (Movement vs Stationary)
        evaluateMotionState(speedKmh)

        // Auto-Detect Speed Camera Zone & Trajectory
        val detectionResult = SpeedCameraDetector.evaluateLocation(
            currentLocation = location,
            currentSpeedKmh = speedKmh,
            autoDetectEnabled = currentPreferences.autoDetectSpeedLimit,
            manualLimitKmh = currentPreferences.speedLimitKmh
        )
        _cameraDetectionFlow.value = detectionResult

        val effectiveLimit = if (currentPreferences.autoDetectSpeedLimit) {
            detectionResult.recommendedLimitKmh
        } else {
            currentPreferences.speedLimitKmh
        }
        _effectiveLimitFlow.value = effectiveLimit

        // Evaluate Speed Alert Status (Normal / Approaching / Exceeded)
        val alertStatus = evaluateSpeedAlert(speedKmh, effectiveLimit)
        _alertStatusFlow.value = alertStatus

        // Unified Contextual Voice & Vibration Prompts:
        // - On a known road without camera & approaching/breaking limit: "You are about to cross the speed limit, slow down"
        // - In fixed camera zone & safe speed: "Speed cameras ahead, be careful"
        // - In fixed camera zone & approaching/breaking limit: "Speed cameras ahead, slow down"
        // - In mobile patrol hotspot & safe speed: "Potential police speed camera ahead, be careful"
        // - In mobile patrol hotspot & approaching/breaking limit: "Potential police speed camera ahead, slow down"
        // - In VIBRATE mode: short burst each second for 4 seconds in place of sound
        if (currentPreferences.alertMode != com.speedcheck.android.data.AlertMode.MUTE &&
            currentMonitoringState == MonitoringState.ACTIVE_MONITORING) {

            val nowMs = System.currentTimeMillis()
            val elapsedSinceStart = nowMs - gpsCalibrationStartMs
            val isCalibrating = elapsedSinceStart < GPS_CALIBRATION_WINDOW_MS

            if (isCalibrating) {
                // GPS calibration window: location detector is still locking onto the
                // correct speed limit for this area. Suppress alerts until calibrated.
                Log.d(TAG, "GPS calibration window active (${elapsedSinceStart}ms / ${GPS_CALIBRATION_WINDOW_MS}ms). Alerts suppressed.")
            } else {
                // Fire one-time calibration-complete announcement then resume normal alerts
                if (!calibrationAnnouncementFired &&
                    currentPreferences.alertMode == com.speedcheck.android.data.AlertMode.SOUND) {
                    val zoneContext = when {
                        detectionResult.hasCameraAhead && detectionResult.isMobilePatrol ->
                            "Potential police speed camera corridor. Limit $effectiveLimit km/h."
                        detectionResult.hasCameraAhead ->
                            "Speed camera zone confirmed. Limit $effectiveLimit km/h."
                        detectionResult.detectedRoadName != null ->
                            "SpeedCheck ready. Speed limit $effectiveLimit km/h."
                        else ->
                            "SpeedCheck ready. Speed limit $effectiveLimit km/h."
                    }
                    ttsManager.speak(zoneContext, android.speech.tts.TextToSpeech.QUEUE_FLUSH)
                    calibrationAnnouncementFired = true
                    Log.i(TAG, "GPS calibration complete. Announced: $zoneContext")
                }

                val hasCameraAhead = detectionResult.hasCameraAhead
                val cameraZoneId = if (hasCameraAhead) detectionResult.activeZone?.id else null
                val isMobilePatrol = detectionResult.isMobilePatrol

                ttsManager.processSpeedAlert(
                    currentSpeedKmh = speedKmh,
                    speedLimitKmh = effectiveLimit,
                    approachingOffsetKmh = currentPreferences.approachingOffsetKmh,
                    cooldownSeconds = currentPreferences.alertCooldownSeconds,
                    hasCameraAhead = hasCameraAhead,
                    cameraZoneId = cameraZoneId,
                    isMobilePatrol = isMobilePatrol,
                    alertMode = currentPreferences.alertMode,
                    onVibrateAlert = {
                        vibrationManager.triggerAlertVibration()
                    }
                )
            }
        }

        // Update persistent notification
        notificationHelper.updateNotification(
            speedKmh = speedKmh,
            limitKmh = effectiveLimit,
            monitoringState = currentMonitoringState,
            alertStatus = alertStatus,
            cameraZoneName = detectionResult.activeZone?.name
        )

        // Capture verifiable driving log snapshot for history & evidence
        evaluateLogSnapshot(location, speedKmh, effectiveLimit, alertStatus, detectionResult)

        lastLocation = location
    }

    private fun evaluateLogSnapshot(
        location: Location,
        speedKmh: Float,
        effectiveLimit: Int,
        alertStatus: SpeedAlertStatus,
        detectionResult: CameraDetectionResult
    ) {
        val now = System.currentTimeMillis()
        val zone = detectionResult.activeZone

        // STRICT FILTER: Only log history when driver hits the warning buffer (APPROACHING)
        // or breaks the speed limit (EXCEEDED)
        val isWarningBufferOrExceeded = alertStatus == SpeedAlertStatus.APPROACHING || alertStatus == SpeedAlertStatus.EXCEEDED
        if (!isWarningBufferOrExceeded) return

        // Throttle snapshots: at most once every 12 seconds to prevent log flooding
        if (now - lastLogSnapshotTimeMs < 12000L) return

        val place = if (zone != null) {
            "${zone.name} (${zone.roadName})"
        } else if (detectionResult.detectedRoadName != null) {
            detectionResult.detectedRoadName
        } else if (speedKmh >= 65f) {
            "Ghana National Highway Corridor"
        } else {
            "Urban Road / Built-up Area"
        }

        val compliance = if (alertStatus == SpeedAlertStatus.EXCEEDED) {
            "EXCEEDED (+${(speedKmh - effectiveLimit).toInt()} km/h)"
        } else {
            "WARNING BUFFER (${speedKmh.toInt()} / $effectiveLimit km/h)"
        }

        val note = if (alertStatus == SpeedAlertStatus.EXCEEDED) {
            if (zone != null) "Exceeded speed limit in MTTD camera corridor ($effectiveLimit km/h)."
            else "Driver speed exceeded limit ($effectiveLimit km/h). Immediate alert triggered."
        } else {
            if (zone != null) "Approaching speed limit in MTTD camera corridor ($effectiveLimit km/h)."
            else "Driver reached early warning buffer ($effectiveLimit km/h). Warning prompt triggered."
        }

        serviceScope.launch(Dispatchers.IO) {
            speedLogRepository.logSnapshot(
                SpeedLogEntry(
                    timestamp = now,
                    placeName = place,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speedKmh = speedKmh,
                    maxSpeedKmh = tripStats.maxSpeedKmh,
                    speedLimitKmh = effectiveLimit,
                    alertStatus = alertStatus,
                    isAutoDetect = currentPreferences.autoDetectSpeedLimit,
                    complianceStatus = compliance,
                    note = note
                )
            )
        }

        lastLogSnapshotTimeMs = now
        if (zone != null) {
            lastLoggedCameraZoneId = zone.id
        } else {
            lastLoggedCameraZoneId = null
        }
    }

    private fun calculateSpeedKmh(location: Location): Float {
        // Discard fallback calculation if GPS accuracy is severely degraded (> 35m)
        val hasGoodAccuracy = !location.hasAccuracy() || location.accuracy <= 35f

        val rawSpeedKmh = if (location.hasSpeed()) {
            (location.speed * 3.6f).coerceAtLeast(0f)
        } else if (hasGoodAccuracy) {
            val prev = lastLocation
            if (prev != null && location.time > prev.time) {
                val distanceMeters = location.distanceTo(prev)
                val timeSeconds = (location.time - prev.time) / 1000.0f

                // Ignore micro-drift under 3.5m and require at least 0.8s between updates
                if (distanceMeters >= 3.5f && timeSeconds >= 0.8f) {
                    val calculatedSpeedMs = distanceMeters / timeSeconds
                    val calculatedSpeedKmh = (calculatedSpeedMs * 3.6f).coerceAtLeast(0f)
                    // Physical acceleration sanity cap (max 25 km/h per second acceleration)
                    val maxPermitted = smoothedSpeedKmh + (25f * timeSeconds)
                    calculatedSpeedKmh.coerceIn(0f, maxPermitted)
                } else 0f
            } else 0f
        } else {
            0f
        }

        // Deadband filter: Stationary / Walking Cutoff
        // Speeds under MIN_VEHICLE_SPEED_KMH (8.0 km/h) are clamped to 0 km/h.
        // This stops speedometer jitter when stationary, sitting at traffic lights, or walking.
        return if (rawSpeedKmh < MIN_VEHICLE_SPEED_KMH) {
            smoothedSpeedKmh = 0f
            0f
        } else {
            // Exponential moving average smoothing for realistic vehicle gauge response
            smoothedSpeedKmh = if (smoothedSpeedKmh == 0f) {
                rawSpeedKmh
            } else {
                (0.60f * rawSpeedKmh) + (0.40f * smoothedSpeedKmh)
            }
            smoothedSpeedKmh
        }
    }

    private fun updateTripStats(speedKmh: Float, location: Location) {
        lastLocation?.let { prev ->
            // Only accumulate distance if the vehicle is actively moving (not stationary drift)
            if (speedKmh > 0f) {
                val deltaDistanceKm = prev.distanceTo(location) / 1000f
                tripStats = tripStats.copy(distanceKm = tripStats.distanceKm + deltaDistanceKm)
            }
        }

        val maxSpeed = maxOf(tripStats.maxSpeedKmh, speedKmh)
        if (speedKmh > 0f) {
            totalSpeedSum += speedKmh
            speedReadingsCount++
        }
        val avgSpeed = if (speedReadingsCount > 0) (totalSpeedSum / speedReadingsCount).toFloat() else 0f

        tripStats = tripStats.copy(
            currentSpeedKmh = speedKmh,
            maxSpeedKmh = maxSpeed,
            averageSpeedKmh = avgSpeed,
            speedReadingsCount = speedReadingsCount
        )
        _tripStatsFlow.value = tripStats
    }

    private fun evaluateMotionState(speedKmh: Float) {
        if (!currentPreferences.autoDetectVehicleMovement) {
            if (currentMonitoringState != MonitoringState.ACTIVE_MONITORING) {
                currentMonitoringState = MonitoringState.ACTIVE_MONITORING
                _monitoringStateFlow.value = currentMonitoringState
            }
            return
        }

        // Automatic Vehicle Detection: Speed consistently above ~18 km/h
        if (speedKmh >= 18f) {
            consecutiveMovingReadings++
            consecutiveStationarySeconds = 0

            if (consecutiveMovingReadings >= 2 && currentMonitoringState != MonitoringState.ACTIVE_MONITORING) {
                currentMonitoringState = MonitoringState.ACTIVE_MONITORING
                _monitoringStateFlow.value = currentMonitoringState
                requestLocationUpdates(highFrequency = true)
                Log.d(TAG, "Vehicle movement detected! Activated high-frequency monitoring.")
            }
        } else if (speedKmh < 5f) {
            consecutiveMovingReadings = 0
            consecutiveStationarySeconds++

            if (consecutiveStationarySeconds >= 120 && currentMonitoringState == MonitoringState.ACTIVE_MONITORING) {
                currentMonitoringState = MonitoringState.PAUSED_STATIONARY
                _monitoringStateFlow.value = currentMonitoringState
                requestLocationUpdates(highFrequency = false)
                Log.d(TAG, "Vehicle stationary for > 2 min. Paused to save battery.")
            }
        } else {
            consecutiveStationarySeconds = 0
        }
    }

    private fun evaluateSpeedAlert(speedKmh: Float, effectiveLimit: Int): SpeedAlertStatus {
        val approachingThreshold = (effectiveLimit - currentPreferences.approachingOffsetKmh).coerceAtLeast(1)

        return when {
            speedKmh >= effectiveLimit -> SpeedAlertStatus.EXCEEDED
            speedKmh >= approachingThreshold -> SpeedAlertStatus.APPROACHING
            else -> SpeedAlertStatus.NORMAL
        }
    }

    private fun resolveEffectiveLimit(): Int {
        return if (currentPreferences.autoDetectSpeedLimit) {
            _cameraDetectionFlow.value.recommendedLimitKmh
        } else {
            currentPreferences.speedLimitKmh
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SpeedTrackingService onDestroy")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        ttsManager.shutdown()
        vibrationManager.cancel()
        preferencesJob?.cancel()
        serviceScope.cancel()
        _monitoringStateFlow.value = MonitoringState.STOPPED
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "SpeedTrackingService"
        const val MIN_VEHICLE_SPEED_KMH = 8.0f

        /**
         * GPS calibration window (milliseconds). Voice alerts are suppressed for this duration
         * after monitoring starts, giving the location detector time to confirm the speed limit
         * for the user's current area before any alert fires. 3 seconds is enough for modern
         * GPS chips to deliver an accurate first fix and zone classification.
         */
        const val GPS_CALIBRATION_WINDOW_MS = 3_000L

        const val ACTION_START_MONITORING = "com.speedcheck.android.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.speedcheck.android.action.STOP_MONITORING"

        // Observable state flows for UI
        private val _currentSpeedFlow = MutableStateFlow(0f)
        val currentSpeedFlow: StateFlow<Float> = _currentSpeedFlow.asStateFlow()

        private val _monitoringStateFlow = MutableStateFlow(MonitoringState.STOPPED)
        val monitoringStateFlow: StateFlow<MonitoringState> = _monitoringStateFlow.asStateFlow()

        private val _alertStatusFlow = MutableStateFlow(SpeedAlertStatus.NORMAL)
        val alertStatusFlow: StateFlow<SpeedAlertStatus> = _alertStatusFlow.asStateFlow()

        private val _tripStatsFlow = MutableStateFlow(TripStats())
        val tripStatsFlow: StateFlow<TripStats> = _tripStatsFlow.asStateFlow()

        private val _cameraDetectionFlow = MutableStateFlow(CameraDetectionResult())
        val cameraDetectionFlow: StateFlow<CameraDetectionResult> = _cameraDetectionFlow.asStateFlow()

        private val _effectiveLimitFlow = MutableStateFlow(80)
        val effectiveLimitFlow: StateFlow<Int> = _effectiveLimitFlow.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, SpeedTrackingService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SpeedTrackingService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }
    }
}
