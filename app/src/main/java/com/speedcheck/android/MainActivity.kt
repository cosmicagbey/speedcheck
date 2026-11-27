package com.speedcheck.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.speedcheck.android.data.PreferencesRepository
import com.speedcheck.android.data.UserPreferences
import com.speedcheck.android.theme.DarkDashboardBg
import com.speedcheck.android.theme.SpeedCheckTheme
import com.speedcheck.android.ui.splash.SplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock the system status bar and navigation bar to dark mode with white icons
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        val repository = PreferencesRepository.getInstance(applicationContext)

        // Silently check for in-app updates from Google Play
        val updateManager = com.speedcheck.android.util.InAppUpdateManager.getInstance(applicationContext)
        updateManager.checkForUpdates()

        setContent {
            val userPreferences by repository.userPreferencesFlow.collectAsState(initial = UserPreferences())
            var showSplashScreen by rememberSaveable { mutableStateOf(true) }

            SpeedCheckTheme(darkTheme = userPreferences.isDarkMode) {
                Crossfade(
                    targetState = showSplashScreen,
                    animationSpec = tween(durationMillis = 350),
                    label = "SplashCrossfade"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(onDismiss = { showSplashScreen = false })
                    } else {
                        // Root layout: The system status bar area is protected by a solid dark container
                        // so the phone's notification bar (clock, battery, wifi, notifications) is ALWAYS dark and legible.
                        // Light theme only applies to the app content below the status bar!
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkDashboardBg)
                        ) {
                            // Dedicated top status bar area (Phone's Notification Bar):
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsTopHeight(WindowInsets.statusBars)
                                    .background(DarkDashboardBg)
                            )

                            // The App itself:
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                MainNavigation(userPreferences = userPreferences)
                            }

                            // Dedicated bottom navigation bar area:
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                                    .background(DarkDashboardBg)
                            )
                        }
                    }
                }
            }
        }
    }
}
