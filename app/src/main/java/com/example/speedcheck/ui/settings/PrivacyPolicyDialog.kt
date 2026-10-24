package com.example.speedcheck.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedcheck.theme.SpeedSafeGreen

/**
 * In-app dialog rendering the complete SpeedCheck Privacy Policy.
 */
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = SpeedSafeGreen
            )
        },
        title = {
            Text(
                text = "Privacy Policy",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "SpeedCheck is dedicated to protecting your privacy. Below is an overview of how the app handles your information:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                PolicySection(
                    title = "1. 100% On-Device Processing",
                    content = "All GPS coordinates, vehicle speed calculations, and camera proximity checks run strictly on your device in real-time. No GPS logs or routes are ever transmitted or saved to external servers."
                )

                PolicySection(
                    title = "2. No Data Collection or Tracking",
                    content = "SpeedCheck does not require an account, phone number, name, or email. We do not track you across other apps, and we do not sell or share any information with third parties."
                )

                PolicySection(
                    title = "3. Background Location Use",
                    content = "Background location is used solely to alert you of speed limits and cameras when your screen is off or when using navigation apps like Google Maps. Location data is never stored permanently."
                )

                PolicySection(
                    title = "4. Local Preferences Storage",
                    content = "Your speed limit selections, voice preferences, and trip statistics are stored only on your physical device using Android DataStore and are cleared upon uninstalling the app."
                )

                PolicySection(
                    title = "5. Compliance",
                    content = "Built in compliance with the Ghana Data Protection Act, 2012 (Act 843), GDPR, and Google Play Developer Program Policies."
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PolicySection(title: String, content: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = content,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
