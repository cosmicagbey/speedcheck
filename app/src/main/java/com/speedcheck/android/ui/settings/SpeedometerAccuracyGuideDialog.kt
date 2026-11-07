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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedcheck.android.theme.GhanaGold
import com.speedcheck.android.theme.GhanaGreen
import com.speedcheck.android.theme.GhanaRed
import com.speedcheck.android.theme.SpeedApproachingAmber
import com.speedcheck.android.theme.SpeedExceededRed
import com.speedcheck.android.theme.SpeedSafeGreen

/**
 * Educational guide detailing vehicle speedometer accuracy, tyre diameter variances,
 * American-import MPH vs. KM/H conversions, and Traffitech automated camera enforcement.
 */
@Composable
fun SpeedometerAccuracyGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = GhanaGold,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Speedometer vs GPS Ground Truth",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
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
                    text = "Did you know that the speed your car dashboard shows is almost never your actual ground speed? Here is what you need to know about your car and speed enforcement in Ghana.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Card 1: Tyre & Rim Changes (Abossey Okai Effect)
                GuideCard(
                    title = "1. Tyre & Rim Sizes (Abossey Okai)",
                    icon = Icons.Default.DirectionsCar,
                    accentColor = SpeedSafeGreen
                ) {
                    Text(
                        text = "Your car calculates speed purely by counting wheel revolutions. If you change your rims or tyre profiles at Abossey Okai or a local fitter, your speedometer reading will be thrown off:\n\n" +
                                "• Larger overall diameter: Your car travels farther per rotation than the computer expects. You are moving FASTER than your dashboard shows.\n" +
                                "• Smaller diameter: You are moving SLOWER than displayed.\n" +
                                "• Worn tread: Even tyre wear gradually shifts readings over time.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card 2: American Imports (MPH vs KM/H)
                GuideCard(
                    title = "2. American Imports: MPH vs KM/H",
                    icon = Icons.Default.WarningAmber,
                    accentColor = SpeedApproachingAmber
                ) {
                    Text(
                        text = "Many vehicles in Ghana are North American imports with speedometers primarily displaying MPH (Miles Per Hour). Confusing MPH for KM/H is a major cause of speeding fines:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ConversionRow("31 MPH", "50 KM/H", "Urban limit (Accra & Kumasi)")
                    ConversionRow("50 MPH", "80 KM/H", "Standard highway limit")
                    ConversionRow("56 MPH", "90 KM/H", "N1 highway upper limit")
                    ConversionRow("62 MPH", "100 KM/H", "Motorway maximum")
                    ConversionRow("80 MPH", "129 KM/H", "⚠️ Severe violation (30+ over!)")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💡 Tip: Turn on 'US Import Mode' in Settings to view real-time MPH alongside KM/H.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GhanaGold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card 3: GPS Ground Speed vs Dashboard
                GuideCard(
                    title = "3. GPS Speed: Satellite Ground Truth",
                    icon = Icons.Default.Info,
                    accentColor = GhanaGold
                ) {
                    Text(
                        text = "SpeedCheck estimates your speed from GPS location data. GPS accuracy varies with device, weather, buildings, and satellite reception; the app's speed may differ from your vehicle's speedometer or an enforcement device. Calibration adjusts the speed used by the app's alerts as well as its display. Always follow posted speed limits.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card 4: Traffitech Automated Enforcement
                GuideCard(
                    title = "4. Traffitech Automated Cameras",
                    icon = Icons.Default.Speed,
                    accentColor = SpeedExceededRed
                ) {
                    Text(
                        text = "Camera and road-zone locations in SpeedCheck are approximate and may be incomplete or out of date. GPS-based speed and location records are for personal reference, not official or legal proof. Always follow posted signs and applicable regulations:\n\n" +
                                "• Keep SpeedCheck active in the background for continuous voice alerts.\n" +
                                "• Set an early warning buffer to give yourself time to slow down.\n" +
                                "• Review the local History tab for your own reference.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 17.sp
                    )
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
private fun GuideCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun ConversionRow(mph: String, kmh: String, note: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$mph = $kmh",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(135.dp)
        )
        Text(
            text = "($note)",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
    }
}
