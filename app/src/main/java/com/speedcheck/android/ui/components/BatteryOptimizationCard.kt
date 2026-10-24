package com.speedcheck.android.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedcheck.android.theme.SpeedApproachingAmber
import com.speedcheck.android.theme.SpeedSafeGreen

/**
 * Card explaining battery optimization and providing a 1-tap setup button.
 */
@SuppressLint("BatteryLife")
@Composable
fun BatteryOptimizationCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(checkBatteryOptimization(context))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isIgnoringBatteryOptimizations)
                MaterialTheme.colorScheme.surfaceVariant
            else
                SpeedApproachingAmber.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isIgnoringBatteryOptimizations) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = if (isIgnoringBatteryOptimizations) SpeedSafeGreen else SpeedApproachingAmber,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Battery Optimization",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (isIgnoringBatteryOptimizations) "Unrestricted (Optimal)" else "Restricted by Android",
                        fontSize = 13.sp,
                        color = if (isIgnoringBatteryOptimizations) SpeedSafeGreen else SpeedApproachingAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isIgnoringBatteryOptimizations) {
                    "SpeedCheck has unrestricted background execution. GPS monitoring and voice alerts will function accurately even when your screen is locked."
                } else {
                    "Android may kill background GPS speed tracking while driving if battery optimization is enabled. Please allow SpeedCheck to run unrestricted."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            if (!isIgnoringBatteryOptimizations) {
                Spacer(modifier = Modifier.height(12.dp))
                ElevatedButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                        isIgnoringBatteryOptimizations = checkBatteryOptimization(context)
                    },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = SpeedApproachingAmber,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Disable Optimization")
                }
            }
        }
    }
}

private fun checkBatteryOptimization(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
}
