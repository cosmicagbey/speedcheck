package com.speedcheck.android.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.speedcheck.android.R
import kotlinx.coroutines.delay

/**
 * Branded Splash Screen displayed upon app launch before navigating to the main dashboard.
 * Shows the official "Speed Check - Built for Ghana Roads" graphic.
 * Dismisses automatically after 2.2 seconds or immediately on screen tap.
 */
@Composable
fun SplashScreen(
    onDismiss: () -> Unit,
    displayDurationMs: Long = 2200L
) {
    LaunchedEffect(Unit) {
        delay(displayDurationMs)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06152D))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_screen_banner),
            contentDescription = "Speed Check - Drive Safe, Save Money",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
