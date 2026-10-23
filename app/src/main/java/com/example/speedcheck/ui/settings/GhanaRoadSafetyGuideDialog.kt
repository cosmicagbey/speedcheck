package com.example.speedcheck.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.speedcheck.theme.GhanaGold
import com.example.speedcheck.theme.GhanaGreen
import com.example.speedcheck.theme.GhanaRed

/**
 * Explanatory guide detailing Ghana MTTD Automated Speed Camera limits and regulations.
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
                tint = GhanaGold
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Ghana Speed Camera Guide",
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
                    text = "The Ghana Police MTTD and NRSA operate automated speed camera traps along major highways. Fines are issued automatically for violations.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Official Ghana Speed Limits (NRSA):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                SpeedLimitRow(
                    limit = "30 km/h",
                    desc = "School zones, markets, hospitals & residential roads"
                )
                SpeedLimitRow(
                    limit = "50 km/h",
                    desc = "Standard urban roads & town centers (Accra, Kumasi, etc.)"
                )
                SpeedLimitRow(
                    limit = "80 km/h",
                    desc = "Inter-urban highways & single-lane regional roads"
                )
                SpeedLimitRow(
                    limit = "90-100 km/h",
                    desc = "Dual carriageway highways (e.g. N1 George Walker Bush Hwy)"
                )
                SpeedLimitRow(
                    limit = "100-120 km/h",
                    desc = "Expressways & Motorways (Accra-Tema Motorway)"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = GhanaGold,
                                modifier = Modifier.height(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Camera Hotspots in Ghana",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• N1 Highway (Mallam - Dimples - Dzorwulu)\n• Accra - Tema Motorway\n• Pokuase Interchange - Amasaman corridor\n• Accra - Cape Coast Highway",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It")
            }
        }
    )
}

@Composable
private fun SpeedLimitRow(limit: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = limit,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
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
