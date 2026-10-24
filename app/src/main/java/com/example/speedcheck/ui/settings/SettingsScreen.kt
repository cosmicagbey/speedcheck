package com.example.speedcheck.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.speedcheck.data.PreferencesRepository
import com.example.speedcheck.data.UserPreferences
import com.example.speedcheck.service.SpeedCameraDetector
import com.example.speedcheck.service.TextToSpeechManager
import com.example.speedcheck.theme.AccentCyan
import com.example.speedcheck.theme.GhanaGold
import com.example.speedcheck.theme.SpeedSafeGreen
import com.example.speedcheck.ui.components.BatteryOptimizationCard
import kotlinx.coroutines.launch

/**
 * Settings Screen allows customizing speed limits, Auto-Detect camera modes,
 * voice alerts, vehicle motion detection, and reviewing Ghana road safety speed guides.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PreferencesRepository.getInstance(context) }
    val ttsManager = remember { TextToSpeechManager(context) }

    var customLimitInput by remember(userPreferences.speedLimitKmh) {
        mutableStateOf(userPreferences.speedLimitKmh.toString())
    }
    var showGhanaGuideDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    val presetLimits = listOf(30, 50, 80, 100, 120)
    val cooldownOptions = listOf(10, 15, 20, 30)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        Text(
                            text = "Enforced Ghana Speed Camera Corridors:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GhanaGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SpeedCameraDetector.GHANA_SPEED_CAMERAS.forEach { camera ->
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

            // SECTION 4: Voice Alerts & Text-to-Speech
            SettingsSectionHeader(title = "VOICE ALERTS & TEXT-TO-SPEECH", icon = Icons.Default.RecordVoiceOver)

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
                                text = "Enable Voice Alerts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Spoken camera announcements and speed warnings",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = userPreferences.voiceAlertsEnabled,
                            onCheckedChange = { scope.launch { repository.updateVoiceAlertsEnabled(it) } }
                        )
                    }

                    if (userPreferences.voiceAlertsEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
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

                        // Test Voice Button
                        OutlinedButton(
                            onClick = {
                                ttsManager.updateVoiceConfig(
                                    gender = userPreferences.voiceGender,
                                    rate = userPreferences.speechRate,
                                    pitch = userPreferences.speechPitch
                                )
                                ttsManager.speak("Speed camera zone ahead on N1 Highway Fiesta Royale. Speed limit is 80 kilometers per hour. You are approaching 80 kilometers per hour. Slow down, current speed is above your limit.")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Voice Alerts Now")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 5: Battery Optimization Status
            SettingsSectionHeader(title = "BATTERY MANAGEMENT", icon = Icons.Default.Info)
            BatteryOptimizationCard()

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 6: Ghana Road Safety & Speed Camera Regulations
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

            // SECTION 7: Privacy Policy
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

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicyDialog = false })
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
