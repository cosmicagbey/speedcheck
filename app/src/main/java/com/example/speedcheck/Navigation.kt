package com.example.speedcheck

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.speedcheck.data.PreferencesRepository
import com.example.speedcheck.data.UserPreferences
import com.example.speedcheck.ui.dashboard.DashboardScreen
import com.example.speedcheck.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val repository = remember { PreferencesRepository.getInstance(context) }
    val userPreferences by repository.userPreferencesFlow.collectAsState(initial = UserPreferences())

    val backStack = rememberNavBackStack(Dashboard)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Dashboard> {
                DashboardScreen(
                    userPreferences = userPreferences,
                    onNavigateToSettings = { backStack.add(Settings) },
                    modifier = Modifier.fillMaxSize()
                )
            }
            entry<Settings> {
                SettingsScreen(
                    userPreferences = userPreferences,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}
