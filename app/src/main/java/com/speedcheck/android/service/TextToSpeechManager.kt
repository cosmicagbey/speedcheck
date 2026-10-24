package com.speedcheck.android.service

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Manages Android Text-to-Speech (TTS) alerts with spam prevention,
 * voice selection (male/female/custom pitch), and loud navigation audio attributes.
 */
class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    // Voice configuration
    private var voiceGender = "FEMALE"
    private var speechRate = 1.0f
    private var speechPitch = 1.0f

    // Alert spam prevention timers
    private var lastApproachingAlertTimeMs = 0L
    private var lastExceededAlertTimeMs = 0L
    private var hasApproachingAlertFiredInCurrentZone = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.UK)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }

            // Configure audio attributes for Navigation Guidance (loud and clear over car noise)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)

            isInitialized = true
            applyVoiceSettings()
            Log.d(TAG, "TextToSpeech successfully initialized.")
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech engine. Status: $status")
        }
    }

    fun updateVoiceConfig(gender: String, rate: Float, pitch: Float) {
        voiceGender = gender
        speechRate = rate
        speechPitch = pitch
        if (isInitialized) {
            applyVoiceSettings()
        }
    }

    private fun applyVoiceSettings() {
        tts?.setSpeechRate(speechRate)

        // Adjust pitch & choose voice based on gender preference
        when (voiceGender.uppercase(Locale.ROOT)) {
            "MALE" -> {
                // Try finding an installed male voice
                var selected = false
                try {
                    tts?.voices?.forEach { voice ->
                        val name = voice.name.lowercase(Locale.ROOT)
                        if ((name.contains("male") || name.contains("en-gb-x-rjs") || name.contains("en-us-x-sfg"))
                            && !name.contains("female")) {
                            tts?.voice = voice
                            selected = true
                            return@forEach
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error looking up male voice: ${e.message}")
                }
                // Fallback to deeper pitch if specific male voice is not installed
                tts?.setPitch(if (selected) speechPitch else speechPitch * 0.85f)
            }
            "FEMALE" -> {
                // Try finding an installed female voice
                var selected = false
                try {
                    tts?.voices?.forEach { voice ->
                        val name = voice.name.lowercase(Locale.ROOT)
                        if (name.contains("female") || name.contains("en-gb-x-fis") || name.contains("en-us-x-sfg")) {
                            tts?.voice = voice
                            selected = true
                            return@forEach
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error looking up female voice: ${e.message}")
                }
                tts?.setPitch(if (selected) speechPitch else speechPitch * 1.15f)
            }
            else -> {
                tts?.setPitch(speechPitch)
            }
        }
    }

    /**
     * Checks speed relative to set limit and triggers appropriate voice alert with cooldown.
     *
     * @param currentSpeedKmh current vehicle speed in km/h
     * @param speedLimitKmh user-set speed limit (e.g., 30, 50, 80, 100, 120 km/h)
     * @param approachingOffsetKmh offset for early warning (default 5 km/h)
     * @param cooldownSeconds interval between repeated over-limit alerts (e.g. 15-20s)
     */
    fun processSpeedAlert(
        currentSpeedKmh: Float,
        speedLimitKmh: Int,
        approachingOffsetKmh: Int,
        cooldownSeconds: Int
    ) {
        val currentTime = System.currentTimeMillis()
        val approachingThreshold = (speedLimitKmh - approachingOffsetKmh).coerceAtLeast(1)

        when {
            // Speed exceeds limit: "Slow down. Current speed is above your limit."
            currentSpeedKmh >= speedLimitKmh -> {
                val cooldownMs = cooldownSeconds * 1000L
                if (currentTime - lastExceededAlertTimeMs >= cooldownMs) {
                    val message = "Slow down. Current speed is above your limit of $speedLimitKmh kilometers per hour."
                    speak(message, TextToSpeech.QUEUE_FLUSH)
                    lastExceededAlertTimeMs = currentTime
                }
            }

            // Approaching speed limit: "You are approaching X kilometers per hour."
            currentSpeedKmh >= approachingThreshold && currentSpeedKmh < speedLimitKmh -> {
                if (!hasApproachingAlertFiredInCurrentZone) {
                    val message = "You are approaching $speedLimitKmh kilometers per hour."
                    speak(message, TextToSpeech.QUEUE_FLUSH)
                    lastApproachingAlertTimeMs = currentTime
                    hasApproachingAlertFiredInCurrentZone = true
                }
            }

            // Safely below approaching threshold: reset approaching zone trigger
            currentSpeedKmh < (approachingThreshold - 2) -> {
                hasApproachingAlertFiredInCurrentZone = false
            }
        }
    }

    /**
     * Speaks the given message using the current TTS settings.
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized) {
            Log.w(TAG, "Cannot speak: TextToSpeech is not yet initialized.")
            return
        }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val utteranceId = "SpeedCheck_${System.currentTimeMillis()}"
        tts?.speak(text, queueMode, params, utteranceId)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}
