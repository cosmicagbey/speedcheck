package com.speedcheck.android.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages audio beeps for driver speed alerts:
 * - Warning buffer approaching limit: 4 beeps per second for 4 seconds (16 beeps total).
 * - Exceeded speed limit: 6 fast beeps in 3 seconds (6 beeps total).
 */
class BeepManager(context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentBeepJob: Job? = null

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    } catch (e: Exception) {
        try {
            ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
        } catch (e2: Exception) {
            Log.e(TAG, "Failed to initialize ToneGenerator", e2)
            null
        }
    }

    /**
     * Warning buffer approaching limit alert:
     * 4 beeps per second for 4 seconds = 16 beeps over 4 seconds.
     * Interval: 250ms (tone 90ms, pause 160ms).
     */
    fun triggerApproachingBeep() {
        currentBeepJob?.cancel()
        currentBeepJob = scope.launch {
            try {
                repeat(16) {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
                    delay(250)
                }
            } catch (e: CancellationException) {
                toneGenerator?.stopTone()
            }
        }
    }

    /**
     * Exceeded speed limit alert:
     * 6 fast beeps in 3 seconds = 6 beeps over 3 seconds.
     * Interval: 500ms (tone 140ms, pause 360ms).
     */
    fun triggerExceededBeep() {
        currentBeepJob?.cancel()
        currentBeepJob = scope.launch {
            try {
                repeat(6) {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 140)
                    delay(500)
                }
            } catch (e: CancellationException) {
                toneGenerator?.stopTone()
            }
        }
    }

    fun stop() {
        currentBeepJob?.cancel()
        toneGenerator?.stopTone()
    }

    fun release() {
        stop()
        toneGenerator?.release()
        toneGenerator = null
    }

    companion object {
        private const val TAG = "BeepManager"
    }
}
