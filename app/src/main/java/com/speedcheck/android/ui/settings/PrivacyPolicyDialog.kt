package com.speedcheck.android.ui.settings

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
import com.speedcheck.android.theme.SpeedSafeGreen

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
                    content = "GPS coordinates, speed calculations, and camera proximity checks are processed on your device. SpeedCheck does not upload location data."
                )

                PolicySection(
                    title = "2. No Data Collection or Tracking",
                    content = "SpeedCheck does not require an account, phone number, name, or email. We do not track you across other apps, and we do not sell or share any information with third parties."
                )

                PolicySection(
                    title = "3. Background Location Use",
                    content = "Background location is used to alert you of speed limits and cameras when the screen is off. Warning and violation history stores the event's GPS coordinates on your device (up to 500 records) until you clear it. You can export records using the Android share sheet."
                )

                PolicySection(
                    title = "4. Local Preferences Storage",
                    content = "Preferences are stored with Android DataStore. History and coordinates are stored in the app's local database. App data is excluded from Android cloud backup and device transfer; uninstalling the app or clearing its storage removes this data."
                )

                PolicySection(
                    title = "5. Compliance",
                    content = "SpeedCheck aims to follow applicable privacy requirements and minimize data use. This is not a certification or legal determination of compliance."
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
