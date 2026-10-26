package com.speedcheck.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.speedcheck.android.data.PreferencesRepository
import com.speedcheck.android.data.UserPreferences
import com.speedcheck.android.theme.SpeedCheckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val repository = PreferencesRepository.getInstance(applicationContext)

        setContent {
            val userPreferences by repository.userPreferencesFlow.collectAsState(initial = UserPreferences())

            SpeedCheckTheme(darkTheme = userPreferences.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(userPreferences = userPreferences)
                }
            }
        }
    }
}
