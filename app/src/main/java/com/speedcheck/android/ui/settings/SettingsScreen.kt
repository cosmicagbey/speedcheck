package com.speedcheck.android.ui.settings

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
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
import com.speedcheck.android.util.hasUpdateAvailable
import kotlinx.coroutines.delay
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
    scrollToUpdateTrigger: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val repository = remember { PreferencesRepository.getInstance(context) }
    val ttsManager = remember { TextToSpeechManager(context) }
    val beepManager = remember { com.speedcheck.android.util.BeepManager(context) }
    val updateManager = remember { InAppUpdateManager.getInstance(context) }
    val updateStatus by updateManager.updateStatus.collectAsState()
    val activity = context as? android.app.Activity

    val scrollState = rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var updatesSectionY by remember { mutableIntStateOf(-1) }
    var columnTopY by remember { mutableIntStateOf(0) }
    var highlightUpdatesGlow by remember { mutableStateOf(false) }

    val hasUpdateAvailable = updateStatus.hasUpdateAvailable
    val shouldGlow = hasUpdateAvailable || highlightUpdatesGlow

    val infiniteTransition = rememberInfiniteTransition(label = "inAppUpdatesGlow")
    val cardGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cardGlowAlpha"
    )
    val cardGlowBorderWidth = (2.0f + (cardGlowAlpha - 0.45f) * 2.5f).dp

    LaunchedEffect(scrollToUpdateTrigger) {
        if (scrollToUpdateTrigger > 0) {
            highlightUpdatesGlow = true
            delay(150)
            if (updatesSectionY > 0) {
                val targetScroll = (updatesSectionY - 40).coerceAtLeast(0).coerceAtMost(scrollState.maxValue)
                scrollState.animateScrollTo(targetScroll)
            } else {
                try {
                    bringIntoViewRequester.bringIntoView()
                } catch (e: Exception) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
        }
    }

    var showGhanaGuideDialog by remember { mutableStateOf(false) }
    var showSpeedometerGuideDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var isCameraListExpanded by remember { mutableStateOf(false) }

    val cooldownOptions = listOf(10, 15, 20, 30, 60)

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
                .verticalScroll(scrollState)
                .onGloballyPositioned { coords ->
                    columnTopY = coords.positionInRoot().y.toInt()
                }
        ) {
            // SECTION 1: Location & known camera zones
            SettingsSectionHeader(title = "LOCATION", icon = Icons.Default.Radar)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Known Speed Camera Locations (${SpeedCameraDetector.GHANA_SPEED_CAMERAS.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GhanaGold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Approximate locations. Always follow posted signs.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mapped road limits are incomplete; always follow posted signs.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "© OpenStreetMap contributors · ODbL 1.0",
                        fontSize = 11.sp,
                        color = AccentCyan,
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://www.openstreetmap.org/copyright")
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Speed Camera Voice Prompts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Hear camera warnings within 75 m. Speed-limit alerts follow your alert mode.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = userPreferences.cameraVoiceAlertsEnabled,
                            onCheckedChange = {
                                scope.launch { repository.updateCameraVoiceAlertsEnabled(it) }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val allCameras = remember { SpeedCameraDetector.GHANA_SPEED_CAMERAS }

                    val displayedCameras = if (isCameraListExpanded) {
                        allCameras
                    } else {
                        allCameras.take(4)
                    }

                    displayedCameras.forEach { camera ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "• ${camera.name}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (camera.isMobilePatrol) "⚠️ Mobile Patrol Corridor • ${camera.roadName}" else "📷 Fixed Radar • ${camera.roadName}",
                                    fontSize = 10.sp,
                                    color = if (camera.isMobilePatrol) GhanaGold.copy(alpha = 0.85f) else AccentCyan.copy(alpha = 0.85f)
                                )
                            }
                            Text(
                                text = "${camera.speedLimitKmh} km/h",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpeedSafeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCameraListExpanded = !isCameraListExpanded }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCameraListExpanded) "Show Less ▴" else "+${allCameras.size - 4} more speed camera locations ▾",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isCameraListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isCameraListExpanded) "Show Less" else "Expand",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 2: Approaching Warning Configuration
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
                        // 1. Voice Mode
                        FilterChip(
                            selected = userPreferences.alertMode == AlertMode.VOICE,
                            onClick = {
                                scope.launch { repository.updateAlertMode(AlertMode.VOICE) }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Voice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SpeedSafeGreen,
                                selectedLabelColor = Color.Black
                            )
                        )

                        // 2. Beep Mode
                        FilterChip(
                            selected = userPreferences.alertMode == AlertMode.BEEP,
                            onClick = {
                                scope.launch { repository.updateAlertMode(AlertMode.BEEP) }
                                beepManager.triggerApproachingBeep()
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Beep", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GhanaGold,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (userPreferences.alertMode == AlertMode.VOICE) {
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
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

                    // Test Alert Button (Voice or Beep based on mode)
                    OutlinedButton(
                        onClick = {
                            if (userPreferences.alertMode == AlertMode.BEEP) {
                                beepManager.triggerExceededBeep()
                            } else {
                                ttsManager.updateVoiceConfig(
                                    gender = userPreferences.voiceGender,
                                    rate = userPreferences.speechRate,
                                    pitch = userPreferences.speechPitch
                                )
                                ttsManager.speak("Speed cameras ahead, slow down. You have crossed the speed limit, slow down.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (userPreferences.alertMode == AlertMode.BEEP) Icons.Default.NotificationsActive else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (userPreferences.alertMode == AlertMode.BEEP) "Test Beep Alerts Now" else "Test Voice Alerts Now")
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

                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 6: In-App Updates
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onGloballyPositioned { coords ->
                        updatesSectionY = (coords.positionInRoot().y - columnTopY).toInt() + scrollState.value
                    }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsSectionHeader(title = "IN-APP UPDATES", icon = Icons.Default.SystemUpdate)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (shouldGlow) {
                                    Modifier
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    GhanaGold.copy(alpha = 0.35f * cardGlowAlpha),
                                                    SpeedSafeGreen.copy(alpha = 0.2f * cardGlowAlpha),
                                                    Color.Transparent
                                                ),
                                                radius = 700f
                                            ),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .padding(3.dp)
                                } else Modifier
                            )
                    ) {
                        InAppUpdateCard(
                            updateStatus = updateStatus,
                            isGlowing = shouldGlow,
                            glowAlpha = cardGlowAlpha,
                            glowBorderWidth = cardGlowBorderWidth,
                            onCheckForUpdates = { updateManager.checkForUpdates() },
                            onStartUpdate = { activity?.let { updateManager.startUpdate(it, immediate = false) } },
                            onCompleteUpdate = { updateManager.completeUpdate() }
                        )
                    }
                }
            }

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
    isGlowing: Boolean = false,
    glowAlpha: Float = 1.0f,
    glowBorderWidth: Dp = 2.dp,
    onCheckForUpdates: () -> Unit,
    onStartUpdate: () -> Unit,
    onCompleteUpdate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isGlowing) {
                    Modifier.border(
                        width = glowBorderWidth,
                        brush = Brush.linearGradient(
                            listOf(
                                GhanaGold.copy(alpha = glowAlpha),
                                SpeedSafeGreen.copy(alpha = glowAlpha),
                                GhanaGold.copy(alpha = glowAlpha)
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
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
                            text = "SpeedCheck v1.0.0",
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
