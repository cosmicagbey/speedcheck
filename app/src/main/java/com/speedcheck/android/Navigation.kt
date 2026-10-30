package com.speedcheck.android

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.speedcheck.android.data.PreferencesRepository
import com.speedcheck.android.data.UserPreferences
import com.speedcheck.android.theme.SpeedSafeGreen
import com.speedcheck.android.ui.dashboard.DashboardScreen
import com.speedcheck.android.ui.history.HistoryScreen
import com.speedcheck.android.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * Primary navigation tabs for the SpeedCheck application:
 * 1. Home - Live dashboard, camera radar, speed limits, speedometer
 * 2. History - Verifiable GPS and speed evidence records
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

    var selectedTab by remember { mutableStateOf(AppTab.HOME) }

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
                    onClick = { selectedTab = AppTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}
