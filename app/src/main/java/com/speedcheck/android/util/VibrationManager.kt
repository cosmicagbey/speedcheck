package com.speedcheck.android.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Handles tactile haptic feedback and driving alert vibrations.
 */
class VibrationManager(private val context: Context) {

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        Log.w(TAG, "Could not initialize Vibrator service: ${e.message}")
        null
    }

    /**
     * Triggers short vibration bursts each second for 4 seconds in place of sound alerts:
     * - Second 1: 250ms buzz + 750ms silence
     * - Second 2: 250ms buzz + 750ms silence
     * - Second 3: 250ms buzz + 750ms silence
     * - Second 4: 250ms buzz + 750ms silence
     */
    fun triggerAlertVibration() {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        // Pattern: [delay, buzz, silence, buzz, silence, buzz, silence, buzz]
        val timings = longArrayOf(0, 250, 750, 250, 750, 250, 750, 250)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vib.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing alert vibration: ${e.message}")
        }
    }

    /**
     * Subtle 50ms haptic tick when toggling the alert mode on the home screen.
     */
    fun triggerModeSwitchTick() {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(50L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error executing mode switch tick: ${e.message}")
        }
    }

    fun cancel() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling vibration: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "VibrationManager"
    }
}
