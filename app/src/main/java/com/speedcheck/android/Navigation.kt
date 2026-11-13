package com.speedcheck.android

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.speedcheck.android.data.PreferencesRepository
import com.speedcheck.android.data.UserPreferences
import com.speedcheck.android.theme.GhanaGold
import com.speedcheck.android.theme.SpeedSafeGreen
import com.speedcheck.android.ui.dashboard.DashboardScreen
import com.speedcheck.android.ui.history.HistoryScreen
import com.speedcheck.android.ui.settings.SettingsScreen
import com.speedcheck.android.util.InAppUpdateManager
import com.speedcheck.android.util.hasUpdateAvailable
import kotlinx.coroutines.launch

/**
 * Primary navigation tabs for the SpeedCheck application:
 * 1. Home - Live dashboard, camera radar, speed limits, speedometer
 * 2. History - Local GPS and speed warning records
 * 3. Settings - Limits, auto-detect, voice parameters, road safety guides
 */
enum class AppTab {
    HOME,
    HISTORY,
    SETTINGS
}

@Composable
fun MainNavigation(userPreferences: UserPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PreferencesRepository.getInstance(context) }
    val updateManager = remember { InAppUpdateManager.getInstance(context) }
    val updateStatus by updateManager.updateStatus.collectAsState()
    val hasUpdateAvailable = updateStatus.hasUpdateAvailable

    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var scrollToUpdatesTrigger by remember { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.HOME,
                    onClick = { selectedTab = AppTab.HOME },
                    icon = { Icon(Icons.Default.Speed, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpeedSafeGreen,
                        selectedTextColor = SpeedSafeGreen,
                        indicatorColor = SpeedSafeGreen.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == AppTab.HISTORY,
                    onClick = { selectedTab = AppTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpeedSafeGreen,
                        selectedTextColor = SpeedSafeGreen,
                        indicatorColor = SpeedSafeGreen.copy(alpha = 0.15f)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == AppTab.SETTINGS,
                    onClick = {
                        selectedTab = AppTab.SETTINGS
                        if (hasUpdateAvailable) {
                            scrollToUpdatesTrigger++
                        }
                    },
                    icon = {
                        SettingsTabIconWithGlow(hasUpdateAvailable = hasUpdateAvailable)
                    },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpeedSafeGreen,
                        selectedTextColor = SpeedSafeGreen,
                        indicatorColor = SpeedSafeGreen.copy(alpha = 0.15f)
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        when (selectedTab) {
            AppTab.HOME -> {
                DashboardScreen(
                    userPreferences = userPreferences,
                    onToggleTheme = {
                        scope.launch {
                            repository.updateDarkMode(!userPreferences.isDarkMode)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            AppTab.HISTORY -> {
                HistoryScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            AppTab.SETTINGS -> {
                SettingsScreen(
                    userPreferences = userPreferences,
                    scrollToUpdateTrigger = scrollToUpdatesTrigger,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

/**
 * Settings tab icon that displays a pulsing glowing indicator badge whenever a new app
 * release is available from Google Play.
 */
@Composable
private fun SettingsTabIconWithGlow(hasUpdateAvailable: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Settings, contentDescription = "Settings")

        if (hasUpdateAvailable) {
            val infiniteTransition = rememberInfiniteTransition(label = "settingsTabGlow")
            val glowScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 2.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowScale"
            )
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.85f,
                targetValue = 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "glowAlpha"
            )

            // Animated pulsing glowing halo
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-3).dp)
                    .size(10.dp)
                    .graphicsLayer {
                        scaleX = glowScale
                        scaleY = glowScale
                        alpha = glowAlpha
                    }
                    .background(GhanaGold, CircleShape)
            )

            // Center glowing dot with crisp contrast border
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-3).dp)
                    .size(9.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .background(GhanaGold, CircleShape)
            )
        }
    }
}
