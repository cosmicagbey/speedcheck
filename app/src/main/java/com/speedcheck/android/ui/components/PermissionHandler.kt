package com.speedcheck.android.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Checks and manages Location and Notification permissions.
 * Background driving tracking is powered by a Foreground Service with an ongoing notification,
 * so ACCESS_BACKGROUND_LOCATION is not required.
 */
@Composable
fun PermissionHandler(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current

    var showGpsDisabledDialog by remember { mutableStateOf(false) }
    var showSettingsRationaleDialog by remember { mutableStateOf(false) }

    // Launcher for Location and Notifications
    val foregroundPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            onPermissionsGranted()
        } else {
            showSettingsRationaleDialog = true
        }
    }

    LaunchedEffect(Unit) {
        checkAndRequestPermissions(
            context = context,
            onGpsDisabled = { showGpsDisabledDialog = true },
            onRequestPermissions = { permissions ->
                foregroundPermissionsLauncher.launch(permissions)
            },
            onAllGranted = onPermissionsGranted
        )
    }

    // Dialog: GPS Hardware is turned off
    if (showGpsDisabledDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDisabledDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("GPS is Disabled") },
            text = {
                Text("SpeedCheck requires GPS to measure your vehicle's speed accurately. Please turn on Location in your device settings.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGpsDisabledDialog = false
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGpsDisabledDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Permissions Denied Rationale
    if (showSettingsRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsRationaleDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("Permissions Needed") },
            text = {
                Text("Location permissions are required to detect your vehicle speed and protect you from speed camera tickets.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsRationaleDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open App Settings")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSettingsRationaleDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun checkAndRequestPermissions(
    context: Context,
    onGpsDisabled: () -> Unit,
    onRequestPermissions: (Array<String>) -> Unit,
    onAllGranted: () -> Unit
) {
    // 1. Check if GPS provider is enabled
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    if (!isGpsEnabled) {
        onGpsDisabled()
    }

    // 2. Check Location Permissions
    val hasFine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val permissionsToRequest = mutableListOf<String>()

    if (!hasFine) permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (!hasCoarse) permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hasNotification = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasNotification) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (permissionsToRequest.isNotEmpty()) {
        onRequestPermissions(permissionsToRequest.toTypedArray())
        return
    }

    onAllGranted()
}
