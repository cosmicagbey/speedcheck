package com.example.speedcheck.service

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
import com.example.speedcheck.data.PreferencesRepository
import com.example.speedcheck.data.UserPreferences
import com.example.speedcheck.model.MonitoringState
import com.example.speedcheck.model.SpeedAlertStatus
import com.example.speedcheck.model.TripStats
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
 * automatic vehicle movement detection, battery-conserving idle pause,
 * and Text-to-Speech overspeed alerts.
 */
class SpeedTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var preferencesJob: Job? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var preferencesRepository: PreferencesRepository

    private var currentPreferences = UserPreferences()

    // Vehicle movement detection and idle state counters
    private var consecutiveMovingReadings = 0
    private var consecutiveStationarySeconds = 0
    private var lastLocation: Location? = null
    private var currentMonitoringState = MonitoringState.STOPPED

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
        preferencesRepository = PreferencesRepository.getInstance(this)

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

                // Update notification with new limit
                notificationHelper.updateNotification(
                    speedKmh = _currentSpeedFlow.value,
                    limitKmh = prefs.speedLimitKmh,
                    monitoringState = currentMonitoringState,
                    alertStatus = _alertStatusFlow.value
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

        // Start Foreground Service with permanent notification
        val notification = notificationHelper.buildForegroundNotification(
            speedKmh = 0f,
            limitKmh = currentPreferences.speedLimitKmh,
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
        _alertStatusFlow.value = SpeedAlertStatus.NORMAL

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
        // Calculate speed in km/h
        val speedKmh = calculateSpeedKmh(location)
        _currentSpeedFlow.value = speedKmh

        // Update Trip Statistics
        updateTripStats(speedKmh, location)

        // Evaluate Vehicle Motion State Machine
        evaluateMotionState(speedKmh)

        // Evaluate Speed Alert Status (Normal / Approaching / Exceeded)
        val alertStatus = evaluateSpeedAlert(speedKmh)
        _alertStatusFlow.value = alertStatus

        // Trigger Voice Prompt if enabled
        if (currentPreferences.voiceAlertsEnabled && currentMonitoringState == MonitoringState.ACTIVE_MONITORING) {
            ttsManager.processSpeedAlert(
                currentSpeedKmh = speedKmh,
                speedLimitKmh = currentPreferences.speedLimitKmh,
                approachingOffsetKmh = currentPreferences.approachingOffsetKmh,
                cooldownSeconds = currentPreferences.alertCooldownSeconds
            )
        }

        // Update persistent notification
        notificationHelper.updateNotification(
            speedKmh = speedKmh,
            limitKmh = currentPreferences.speedLimitKmh,
            monitoringState = currentMonitoringState,
            alertStatus = alertStatus
        )

        lastLocation = location
    }

    private fun calculateSpeedKmh(location: Location): Float {
        // Android Location.getSpeed() returns speed in meters/second
        return if (location.hasSpeed()) {
            (location.speed * 3.6f).coerceAtLeast(0f)
        } else {
            // Fallback: calculate delta distance / delta time if lastLocation exists
            val prev = lastLocation
            if (prev != null && location.time > prev.time) {
                val distanceMeters = location.distanceTo(prev)
                val timeSeconds = (location.time - prev.time) / 1000.0f
                if (timeSeconds > 0) {
                    val calculatedSpeedMs = distanceMeters / timeSeconds
                    (calculatedSpeedMs * 3.6f).coerceAtLeast(0f)
                } else 0f
            } else 0f
        }
    }

    private fun updateTripStats(speedKmh: Float, location: Location) {
        lastLocation?.let { prev ->
            val deltaDistanceKm = prev.distanceTo(location) / 1000f
            tripStats = tripStats.copy(distanceKm = tripStats.distanceKm + deltaDistanceKm)
        }

        val maxSpeed = maxOf(tripStats.maxSpeedKmh, speedKmh)
        if (speedKmh > 3f) {
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

            // If stopped for > 120 seconds, pause/throttle to save battery
            if (consecutiveStationarySeconds >= 120 && currentMonitoringState == MonitoringState.ACTIVE_MONITORING) {
                currentMonitoringState = MonitoringState.PAUSED_STATIONARY
                _monitoringStateFlow.value = currentMonitoringState
                requestLocationUpdates(highFrequency = false)
                Log.d(TAG, "Vehicle stationary for > 2 min. Paused to save battery.")
            }
        } else {
            // Speed between 5 and 18 km/h (slow crawling or walking)
            consecutiveStationarySeconds = 0
        }
    }

    private fun evaluateSpeedAlert(speedKmh: Float): SpeedAlertStatus {
        val limit = currentPreferences.speedLimitKmh
        val approachingThreshold = (limit - currentPreferences.approachingOffsetKmh).coerceAtLeast(1)

        return when {
            speedKmh >= limit -> SpeedAlertStatus.EXCEEDED
            speedKmh >= approachingThreshold -> SpeedAlertStatus.APPROACHING
            else -> SpeedAlertStatus.NORMAL
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SpeedTrackingService onDestroy")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        ttsManager.shutdown()
        preferencesJob?.cancel()
        serviceScope.cancel()
        _monitoringStateFlow.value = MonitoringState.STOPPED
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "SpeedTrackingService"

        const val ACTION_START_MONITORING = "com.example.speedcheck.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.speedcheck.action.STOP_MONITORING"

        // Observable state flows for UI
        private val _currentSpeedFlow = MutableStateFlow(0f)
        val currentSpeedFlow: StateFlow<Float> = _currentSpeedFlow.asStateFlow()

        private val _monitoringStateFlow = MutableStateFlow(MonitoringState.STOPPED)
        val monitoringStateFlow: StateFlow<MonitoringState> = _monitoringStateFlow.asStateFlow()

        private val _alertStatusFlow = MutableStateFlow(SpeedAlertStatus.NORMAL)
        val alertStatusFlow: StateFlow<SpeedAlertStatus> = _alertStatusFlow.asStateFlow()

        private val _tripStatsFlow = MutableStateFlow(TripStats())
        val tripStatsFlow: StateFlow<TripStats> = _tripStatsFlow.asStateFlow()

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
