package com.example.speedcheck.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedcheck.data.PreferencesRepository
import com.example.speedcheck.data.UserPreferences
import com.example.speedcheck.model.MonitoringState
import com.example.speedcheck.model.SpeedAlertStatus
import com.example.speedcheck.model.TripStats
import com.example.speedcheck.service.SpeedTrackingService
import com.example.speedcheck.theme.AccentCyan
import com.example.speedcheck.theme.GhanaGold
import com.example.speedcheck.theme.GhanaGreen
import com.example.speedcheck.theme.GhanaRed
import com.example.speedcheck.theme.SpeedApproachingAmber
import com.example.speedcheck.theme.SpeedExceededRed
import com.example.speedcheck.theme.SpeedSafeGreen
import com.example.speedcheck.ui.components.PermissionHandler
import kotlinx.coroutines.launch

/**
 * Dashboard Screen: High-contrast digital speedometer, limit selectors,
 * live trip statistics, and one-touch monitoring controls.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    userPreferences: UserPreferences,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PreferencesRepository.getInstance(context) }

    // Live state collected from the foreground service
    val currentSpeed by SpeedTrackingService.currentSpeedFlow.collectAsState()
    val monitoringState by SpeedTrackingService.monitoringStateFlow.collectAsState()
    val alertStatus by SpeedTrackingService.alertStatusFlow.collectAsState()
    val tripStats by SpeedTrackingService.tripStatsFlow.collectAsState()

    var showCustomLimitDialog by remember { mutableStateOf(false) }
    var customLimitText by remember { mutableStateOf("") }
    var hasLocationPermissions by remember { mutableStateOf(false) }

    val presetLimits = listOf(30, 50, 80, 100, 120)

    // Permission Handler
    PermissionHandler(
        onPermissionsGranted = { hasLocationPermissions = true }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            DashboardTopBar(
                isVoiceMuted = !userPreferences.voiceAlertsEnabled,
                onToggleMute = {
                    scope.launch {
                        repository.updateVoiceAlertsEnabled(!userPreferences.voiceAlertsEnabled)
                    }
                },
                onOpenSettings = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status Badge (Active / Waiting for Movement / Stopped / Paused)
            StatusBadge(monitoringState = monitoringState, alertStatus = alertStatus)

            Spacer(modifier = Modifier.height(16.dp))

            // Main Digital Speedometer Gauge
            SpeedometerGauge(
                currentSpeedKmh = currentSpeed,
                speedLimitKmh = userPreferences.speedLimitKmh,
                alertStatus = alertStatus
            )

            // Early Warning Notification Info
            val approachingThreshold = userPreferences.speedLimitKmh - userPreferences.approachingOffsetKmh
            Text(
                text = "Voice warning at $approachingThreshold km/h • Limit ${userPreferences.speedLimitKmh} km/h",
                fontSize = 13.sp,
                color = when (alertStatus) {
                    SpeedAlertStatus.EXCEEDED -> SpeedExceededRed
                    SpeedAlertStatus.APPROACHING -> SpeedApproachingAmber
                    SpeedAlertStatus.NORMAL -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                },
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Speed Limit Quick Switcher Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SPEED LIMIT (KM/H)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Active: ${userPreferences.speedLimitKmh} km/h",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpeedSafeGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetLimits.forEach { limit ->
                            FilterChip(
                                selected = userPreferences.speedLimitKmh == limit,
                                onClick = {
                                    scope.launch { repository.updateSpeedLimit(limit) }
                                },
                                label = { Text("$limit") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SpeedSafeGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }

                        // Custom Limit Chip
                        FilterChip(
                            selected = userPreferences.speedLimitKmh !in presetLimits,
                            onClick = {
                                customLimitText = userPreferences.speedLimitKmh.toString()
                                showCustomLimitDialog = true
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (userPreferences.speedLimitKmh !in presetLimits) "${userPreferences.speedLimitKmh}" else "Custom")
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GhanaGold,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Big Start/Stop Monitoring Button
            val isMonitoring = monitoringState != MonitoringState.STOPPED
            Button(
                onClick = {
                    if (isMonitoring) {
                        SpeedTrackingService.stopService(context)
                    } else {
                        SpeedTrackingService.startService(context)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) SpeedExceededRed else SpeedSafeGreen,
                    contentColor = if (isMonitoring) Color.White else Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = if (isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isMonitoring) "STOP MONITORING" else "START MONITORING",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Trip Statistics Card
            TripStatsCard(tripStats = tripStats)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Custom Speed Limit Dialog
    if (showCustomLimitDialog) {
        AlertDialog(
            onDismissRequest = { showCustomLimitDialog = false },
            title = { Text("Set Custom Speed Limit") },
            text = {
                Column {
                    Text(
                        text = "Enter any target speed limit in km/h (e.g., 40, 60, 70):",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customLimitText,
                        onValueChange = { customLimitText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Speed Limit (km/h)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = customLimitText.toIntOrNull()
                        if (parsed != null && parsed in 10..220) {
                            scope.launch { repository.updateSpeedLimit(parsed) }
                            showCustomLimitDialog = false
                        }
                    }
                ) {
                    Text("Save Limit")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomLimitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DashboardTopBar(
    isVoiceMuted: Boolean,
    onToggleMute: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(SpeedSafeGreen, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SpeedCheck",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // Small Ghana flag color badge
                    Row(
                        modifier = Modifier
                            .height(12.dp)
                            .width(22.dp)
                            .clip(RoundedCornerShape(2.dp))
                    ) {
                        Box(modifier = Modifier.weight(1f).background(GhanaRed))
                        Box(modifier = Modifier.weight(1f).background(GhanaGold))
                        Box(modifier = Modifier.weight(1f).background(GhanaGreen))
                    }
                }
                Text(
                    text = "Ghana Speed Camera Assistant",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Row {
            IconButton(onClick = onToggleMute) {
                Icon(
                    imageVector = if (isVoiceMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isVoiceMuted) "Unmute Voice" else "Mute Voice",
                    tint = if (isVoiceMuted) SpeedExceededRed else SpeedSafeGreen
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    monitoringState: MonitoringState,
    alertStatus: SpeedAlertStatus
) {
    val (statusText, badgeColor) = when {
        monitoringState == MonitoringState.STOPPED -> "MONITORING STOPPED" to Color.Gray
        monitoringState == MonitoringState.WAITING_FOR_MOVEMENT -> "WAITING FOR VEHICLE MOVEMENT" to AccentCyan
        monitoringState == MonitoringState.PAUSED_STATIONARY -> "PAUSED (VEHICLE STOPPED)" to SpeedApproachingAmber
        alertStatus == SpeedAlertStatus.EXCEEDED -> "OVER SPEED LIMIT! SLOW DOWN" to SpeedExceededRed
        alertStatus == SpeedAlertStatus.APPROACHING -> "APPROACHING SPEED LIMIT" to SpeedApproachingAmber
        else -> "MONITORING ACTIVE" to SpeedSafeGreen
    }

    Surface(
        color = badgeColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(badgeColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = badgeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun TripStatsCard(tripStats: TripStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "SESSION TRIP STATS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(label = "MAX SPEED", value = "${tripStats.maxSpeedKmh.toInt()}", unit = "km/h")
                StatItem(label = "AVG SPEED", value = "${tripStats.averageSpeedKmh.toInt()}", unit = "km/h")
                StatItem(label = "DISTANCE", value = String.format("%.1f", tripStats.distanceKm), unit = "km")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = unit,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
