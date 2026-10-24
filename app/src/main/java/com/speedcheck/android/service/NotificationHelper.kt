package com.speedcheck.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.speedcheck.android.MainActivity
import com.speedcheck.android.R
import com.speedcheck.android.model.MonitoringState
import com.speedcheck.android.model.SpeedAlertStatus

/**
 * Creates and updates Foreground Service notifications and alert channels.
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Low priority channel for continuous speed updates without sound/vibrate on each tick
            val trackingChannel = NotificationChannel(
                CHANNEL_ID_TRACKING,
                "Speed Monitoring Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live speed and monitoring status while driving"
                setShowBadge(false)
            }

            // High priority channel for critical over-speed warnings
            val alertsChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                "Speed Limit Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up alert notifications when approaching or exceeding limits"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(trackingChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    fun buildForegroundNotification(
        speedKmh: Float,
        limitKmh: Int,
        monitoringState: MonitoringState,
        alertStatus: SpeedAlertStatus,
        cameraZoneName: String? = null
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, SpeedTrackingService::class.java).apply {
            action = SpeedTrackingService.ACTION_STOP_MONITORING
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val zoneTag = if (!cameraZoneName.isNullOrEmpty()) " • $cameraZoneName" else ""

        val (title, text) = when (monitoringState) {
            MonitoringState.STOPPED -> "SpeedCheck Inactive" to "Tap to start speed monitoring"
            MonitoringState.WAITING_FOR_MOVEMENT -> {
                "SpeedCheck: Waiting for Movement" to "Auto-activates when driving • Limit: $limitKmh km/h$zoneTag"
            }
            MonitoringState.PAUSED_STATIONARY -> {
                "SpeedCheck: Paused (Vehicle Stopped)" to "Conserving battery • Limit: $limitKmh km/h$zoneTag"
            }
            MonitoringState.ACTIVE_MONITORING -> {
                val roundedSpeed = speedKmh.toInt()
                when (alertStatus) {
                    SpeedAlertStatus.EXCEEDED -> {
                        "⚠️ OVER LIMIT: $roundedSpeed km/h!" to "Limit: $limitKmh km/h • Slow down!$zoneTag"
                    }
                    SpeedAlertStatus.APPROACHING -> {
                        "⚠️ Approaching: $roundedSpeed km/h" to "Target limit: $limitKmh km/h$zoneTag"
                    }
                    SpeedAlertStatus.NORMAL -> {
                        "Current Speed: $roundedSpeed km/h" to "Limit: $limitKmh km/h • Safe$zoneTag"
                    }
                }
            }
        }

        return NotificationCompat.Builder(context, CHANNEL_ID_TRACKING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Monitoring",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun updateNotification(
        speedKmh: Float,
        limitKmh: Int,
        monitoringState: MonitoringState,
        alertStatus: SpeedAlertStatus,
        cameraZoneName: String? = null
    ) {
        val notification = buildForegroundNotification(speedKmh, limitKmh, monitoringState, alertStatus, cameraZoneName)
        notificationManager.notify(NOTIFICATION_ID_FOREGROUND, notification)
    }

    companion object {
        const val CHANNEL_ID_TRACKING = "speedcheck_monitoring_channel"
        const val CHANNEL_ID_ALERTS = "speedcheck_alerts_channel"
        const val NOTIFICATION_ID_FOREGROUND = 1001
    }
}
