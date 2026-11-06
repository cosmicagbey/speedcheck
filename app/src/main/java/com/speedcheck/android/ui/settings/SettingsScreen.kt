package com.speedcheck.android.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedcheck.android.data.AlertMode
import com.speedcheck.android.data.PreferencesRepository
import com.speedcheck.android.data.UserPreferences
import com.speedcheck.android.service.SpeedCameraDetector
import com.speedcheck.android.service.TextToSpeechManager
import com.speedcheck.android.theme.AccentCyan
import com.speedcheck.android.theme.GhanaGold
import com.speedcheck.android.theme.SpeedExceededRed
import com.speedcheck.android.theme.SpeedSafeGreen
import com.speedcheck.android.ui.components.BatteryOptimizationCard
import com.speedcheck.android.util.InAppUpdateManager
import com.speedcheck.android.util.InAppUpdateStatus
import com.speedcheck.android.util.VibrationManager
import kotlinx.coroutines.launch

/**
 * Settings Screen allows customizing speed limits, Auto-Detect camera modes,
 * voice alerts, vehicle motion detection, and reviewing Ghana road safety speed guides.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PreferencesRepository.getInstance(context) }
    val ttsManager = remember { TextToSpeechManager(context) }
    val vibrationManager = remember { VibrationManager(context) }
    val updateManager = remember { InAppUpdateManager.getInstance(context) }
    val updateStatus by updateManager.updateStatus.collectAsState()
    val activity = context as? android.app.Activity

    var customLimitInput by remember(userPreferences.speedLimitKmh) {
        mutableStateOf(userPreferences.speedLimitKmh.toString())
    }
    var showGhanaGuideDialog by remember { mutableStateOf(false) }
    var showSpeedometerGuideDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var isCameraListExpanded by remember { mutableStateOf(false) }

    val presetLimits = listOf(30, 50, 80, 100, 120)
    val cooldownOptions = listOf(10, 15, 20, 30)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // SECTION 1: Auto-Detect Speed Limits & Camera Zones
            SettingsSectionHeader(title = "LOCATION & CAMERA AUTO-DETECT", icon = Icons.Default.Radar)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (userPreferences.autoDetectSpeedLimit)
                        AccentCyan.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.surface
                ),
                border = if (userPreferences.autoDetectSpeedLimit)
                    androidx.compose.foundation.BorderStroke(1.dp, AccentCyan.copy(alpha = 0.4f))
                else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Detect Speed Limit",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Picks your GPS location & travel heading to find the speed limit for your area and approaching cameras.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = userPreferences.autoDetectSpeedLimit,
                            onCheckedChange = { scope.launch { repository.updateAutoDetectSpeedLimit(it) } },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AccentCyan
                            )
                        )
                    }

                    if (userPreferences.autoDetectSpeedLimit) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val fixedCameras = remember { SpeedCameraDetector.GHANA_SPEED_CAMERAS.filter { !it.isMobilePatrol } }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enforced Speed Cameras (${fixedCameras.size}):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GhanaGold
                            )
                            if (fixedCameras.size > 5) {
                                Row(
                                    modifier = Modifier
                                        .clickable { isCameraListExpanded = !isCameraListExpanded }
                                        .padding(vertical = 4.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isCameraListExpanded) "Show Less" else "Show All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentCyan
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = if (isCameraListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isCameraListExpanded) "Show Less" else "Show All",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        val displayedCameras = if (isCameraListExpanded) {
                            fixedCameras
                        } else {
                            fixedCameras.take(5)
                        }

                        displayedCameras.forEach { camera ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${camera.name}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${camera.speedLimitKmh} km/h",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpeedSafeGreen
                                )
                            }
                        }

                        if (!isCameraListExpanded && fixedCameras.size > 5) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isCameraListExpanded = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+${fixedCameras.size - 5} more speed camera zones",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand list",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 2: Manual Speed Limit Settings
            SettingsSectionHeader(title = "MANUAL SPEED LIMIT SETTING", icon = Icons.Default.Speed)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fallback / Manual Limit: ${userPreferences.speedLimitKmh} km/h",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (userPreferences.autoDetectSpeedLimit)
                            "Used when outside camera zones or if Auto-Detect is turned off."
                        else
                            "Currently used for all speed alerts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

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
                                label = { Text("$limit km/h") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SpeedSafeGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customLimitInput,
                            onValueChange = { input ->
                                if (input.length <= 3 && input.all { it.isDigit() }) {
                                    customLimitInput = input
                                    val parsed = input.toIntOrNull()
                                    if (parsed != null && parsed in 10..200) {
                                        scope.launch { repository.updateSpeedLimit(parsed) }
                                    }
                                }
                            },
                            label = { Text("Custom Limit (km/h)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 3: Approaching Warning Configuration
            SettingsSectionHeader(title = "EARLY APPROACHING ALERT", icon = Icons.Default.NotificationsActive)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Approaching Alert",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Voice warning before reaching target speed limit",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = userPreferences.approachingAlertEnabled,
                            onCheckedChange = { scope.launch { repository.updateApproachingAlertEnabled(it) } }
                        )
                    }

                    if (userPreferences.approachingAlertEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Warning Buffer: ${userPreferences.approachingOffsetKmh} km/h before limit",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = userPreferences.approachingOffsetKmh.toFloat(),
                            onValueChange = { offset ->
                                scope.launch { repository.updateApproachingOffset(offset.toInt()) }
                            },
                            valueRange = 3f..10f,
                            steps = 6
                        )
                        Text(
                            text = "Example: For 30 km/h limit, notifies at 25 km/h: \"You are approaching 30 kilometers per hour.\"",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 4: Voice Alerts & Haptic Vibration Feedback
            SettingsSectionHeader(title = "ALERT DELIVERY & VOICE SETTINGS", icon = Icons.Default.RecordVoiceOver)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Alert Delivery Mode:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Sound Mode: Speaker icon + (Voice)
                        FilterChip(
                            selected = userPreferences.alertMode == AlertMode.SOUND,
                            onClick = {
                                scope.launch { repository.updateAlertMode(AlertMode.SOUND) }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("(Voice)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpeedSafeGreen,
                                selectedLabelColor = Color.Black
                            )
                        )

                        // 2. Vibrate Mode: Vibration icon only
                        FilterChip(
                            selected = userPreferences.alertMode == AlertMode.VIBRATE,
                            onClick = {
                                scope.launch { repository.updateAlertMode(AlertMode.VIBRATE) }
                                vibrationManager.triggerModeSwitchTick()
                            },
                            label = {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = "Vibrate Alert Mode",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GhanaGold,
                                selectedLabelColor = Color.Black
                            )
                        )

                        // 3. Mute Mode: Volume off icon only
                        FilterChip(
                            selected = userPreferences.alertMode == AlertMode.MUTE,
                            onClick = {
                                scope.launch { repository.updateAlertMode(AlertMode.MUTE) }
                            },
                            label = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Mute Alert Mode",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpeedExceededRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (userPreferences.alertMode == AlertMode.SOUND) {
                        Text(
                            text = "Voice Style:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("FEMALE" to "Female", "MALE" to "Male", "DEFAULT" to "System").forEach { (code, label) ->
                                FilterChip(
                                    selected = userPreferences.voiceGender == code,
                                    onClick = {
                                        scope.launch { repository.updateVoiceGender(code) }
                                    },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SpeedSafeGreen,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = "Alert Repeat Cooldown (Prevents Spam):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        cooldownOptions.forEach { seconds ->
                            FilterChip(
                                selected = userPreferences.alertCooldownSeconds == seconds,
                                onClick = {
                                    scope.launch { repository.updateAlertCooldown(seconds) }
                                },
                                label = { Text("${seconds}s") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SpeedSafeGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test Alert Button (Voice or Vibration based on mode)
                    OutlinedButton(
                        onClick = {
                            if (userPreferences.alertMode == AlertMode.VIBRATE) {
                                vibrationManager.triggerAlertVibration()
                            } else {
                                ttsManager.updateVoiceConfig(
                                    gender = userPreferences.voiceGender,
                                    rate = userPreferences.speechRate,
                                    pitch = userPreferences.speechPitch
                                )
                                ttsManager.speak("Speed cameras ahead, slow down. You are about to cross the speed limit, slow down.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (userPreferences.alertMode == AlertMode.VIBRATE) Icons.Default.Vibration else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (userPreferences.alertMode == AlertMode.VIBRATE) "Test 4s Vibration Alert Now" else "Test Voice Alerts Now")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 5: Vehicle & Display Units (US Imports)
            SettingsSectionHeader(title = "VEHICLE & DISPLAY UNITS", icon = Icons.Default.DirectionsCar)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "US Import Vehicle Mode (Show MPH)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Displays secondary MPH under KM/H for American-spec dashboard clusters (60 MPH ≈ 97 KM/H).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = userPreferences.showDualUnitsMph,
                            onCheckedChange = { scope.launch { repository.updateShowDualUnitsMph(it) } }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Speedometer Offset Calibration Slider
                    val offset = userPreferences.speedometerOffsetKmh
                    val offsetDisplay = when {
                        offset > 0 -> "+$offset km/h"
                        offset < 0 -> "$offset km/h"
                        else -> "0 km/h (Exact GPS)"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Speedometer Offset Calibration",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Align app speed with your vehicle's instrument cluster. If car speedometer shows 80 while app shows 75, set +5.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Text(
                            text = offsetDisplay,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (offset != 0) GhanaGold else SpeedSafeGreen,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = offset.toFloat(),
                        onValueChange = { newVal ->
                            scope.launch { repository.updateSpeedometerOffset(kotlin.math.round(newVal).toInt()) }
                        },
                        valueRange = -15f..15f,
                        steps = 29,
                        colors = SliderDefaults.colors(
                            thumbColor = if (offset != 0) GhanaGold else SpeedSafeGreen,
                            activeTrackColor = if (offset != 0) GhanaGold else SpeedSafeGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 6: In-App Updates
            SettingsSectionHeader(title = "IN-APP UPDATES", icon = Icons.Default.SystemUpdate)
            InAppUpdateCard(
                updateStatus = updateStatus,
                onCheckForUpdates = { updateManager.checkForUpdates() },
                onStartUpdate = { activity?.let { updateManager.startUpdate(it, immediate = false) } },
                onCompleteUpdate = { updateManager.completeUpdate() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 7: Battery Optimization Status
            SettingsSectionHeader(title = "BATTERY MANAGEMENT", icon = Icons.Default.Info)
            BatteryOptimizationCard()

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 8: Ghana Road Safety & Speed Camera Regulations
            Button(
                onClick = { showGhanaGuideDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GhanaGold,
                    contentColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ghana MTTD Speed Camera Guide", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Speedometer Accuracy & Calibration Advisory
            Button(
                onClick = { showSpeedometerGuideDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Why Speedometers Lie & GPS Accuracy", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // SECTION 9: Privacy Policy
            OutlinedButton(
                onClick = { showPrivacyPolicyDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Privacy Policy")
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showGhanaGuideDialog) {
        GhanaRoadSafetyGuideDialog(onDismiss = { showGhanaGuideDialog = false })
    }

    if (showSpeedometerGuideDialog) {
        SpeedometerAccuracyGuideDialog(onDismiss = { showSpeedometerGuideDialog = false })
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicyDialog = false })
    }
}



@Composable
private fun InAppUpdateCard(
    updateStatus: InAppUpdateStatus,
    onCheckForUpdates: () -> Unit,
    onStartUpdate: () -> Unit,
    onCompleteUpdate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = GhanaGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "In-App Updates",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Receive & install app updates directly without opening Play Store",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (updateStatus) {
                is InAppUpdateStatus.Checking -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = GhanaGold
                        )
                        Text(
                            text = "Checking for new releases...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                is InAppUpdateStatus.UpdateAvailable -> {
                    Text(
                        text = "🎉 New Update Available! (Build ${updateStatus.availableVersionCode})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpeedSafeGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onStartUpdate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpeedSafeGreen,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download & Install In-App", fontWeight = FontWeight.Bold)
                    }
                }
                is InAppUpdateStatus.Downloading -> {
                    val percent = if (updateStatus.totalBytes > 0) {
                        ((updateStatus.bytesDownloaded * 100) / updateStatus.totalBytes).toInt()
                    } else 0
                    Column {
                        Text(
                            text = "Downloading in-app ($percent%)...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { if (updateStatus.totalBytes > 0) updateStatus.bytesDownloaded.toFloat() / updateStatus.totalBytes else 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = GhanaGold
                        )
                    }
                }
                is InAppUpdateStatus.Downloaded -> {
                    Column {
                        Text(
                            text = "✅ Update downloaded and ready to apply!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpeedSafeGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onCompleteUpdate,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GhanaGold,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Restart App to Complete Update", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is InAppUpdateStatus.UpToDate -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Installed: v${updateStatus.currentVersion} (Build ${updateStatus.currentVersionCode})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "You are using the latest version",
                                fontSize = 12.sp,
                                color = SpeedSafeGreen
                            )
                        }
                        OutlinedButton(
                            onClick = onCheckForUpdates,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Check Now", fontSize = 12.sp)
                        }
                    }
                }
                is InAppUpdateStatus.Error -> {
                    Column {
                        Text(
                            text = updateStatus.message,
                            fontSize = 12.sp,
                            color = SpeedExceededRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onCheckForUpdates) {
                            Text("Try Again")
                        }
                    }
                }
                is InAppUpdateStatus.Idle -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SpeedCheck v1.0.6",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = onCheckForUpdates,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GhanaGold,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Check for Updates")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SpeedSafeGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
    }
}


