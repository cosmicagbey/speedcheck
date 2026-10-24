package com.speedcheck.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedcheck.android.service.SpeedCameraDetector
import com.speedcheck.android.theme.GhanaGold
import com.speedcheck.android.theme.GhanaGreen
import com.speedcheck.android.theme.GhanaRed
import com.speedcheck.android.theme.SpeedSafeGreen

/**
 * Explanatory guide detailing Ghana MTTD Automated Speed Camera locations and regulations.
 */
@Composable
fun GhanaRoadSafetyGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = GhanaGold,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Ghana Speed Cameras & Limits",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                // Ghana Flag color bar
                Row(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .height(4.dp)
                        .width(100.dp)
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    Box(modifier = Modifier.weight(1f).background(GhanaRed))
                    Box(modifier = Modifier.weight(1f).background(GhanaGold))
                    Box(modifier = Modifier.weight(1f).background(GhanaGreen))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "The Ghana Police MTTD has deployed automated speed cameras across critical corridors in Accra. With Auto-Detect enabled, SpeedCheck automatically sets your limit as you drive towards these areas.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Active MTTD Camera Locations:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GhanaGold
                )

                Spacer(modifier = Modifier.height(8.dp))

                SpeedCameraDetector.GHANA_SPEED_CAMERAS.forEach { camera ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.GpsFixed,
                                    contentDescription = null,
                                    tint = SpeedSafeGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = camera.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${camera.speedLimitKmh} km/h",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = camera.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Standard Ghana NRSA Speed Limits:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                SpeedLimitRow("30 km/h", "School zones, markets, hospitals & residential roads")
                SpeedLimitRow("50 km/h", "Standard urban roads & town centers (Accra, Kumasi)")
                SpeedLimitRow("80 km/h", "Inter-urban highways & single-lane regional roads")
                SpeedLimitRow("90-100 km/h", "Dual carriageway highways (e.g. N1 George Walker Bush)")
                SpeedLimitRow("100-120 km/h", "Motorways & expressways (Accra-Tema Motorway)")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Understood")
            }
        }
    )
}

@Composable
private fun SpeedLimitRow(limit: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = limit,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = desc,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f)
        )
    }
}
