package com.speedcheck.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.speedcheck.android.data.PreferencesRepository
import com.speedcheck.android.service.SpeedTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered when the device boots or when the app is updated.
 * Automatically restores background speed monitoring if configured by the user.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver onReceive: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            val repository = PreferencesRepository.getInstance(context)

            // Asynchronously read saved preferences
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val preferences = repository.getInitialPreferences()
                    if (preferences.monitoringEnabled && preferences.autoStartOnBoot) {
                        Log.i(TAG, "Auto-starting SpeedTrackingService after device boot.")
                        SpeedTrackingService.startService(context)
                    } else {
                        Log.d(TAG, "Monitoring was not active or autoStartOnBoot disabled; skipping start.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking preferences on boot: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
