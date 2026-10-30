package com.speedcheck.android.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

/**
 * Manages high-quality, realistic Android Text-to-Speech (TTS) alerts.
 *
 * Enhancements for realistic voice synthesis:
 * - Prioritizes Google Speech Services ("com.google.android.tts") for Neural/WaveNet voices.
 * - Searches for high-definition offline voices (QUALITY_HIGH/VERY_HIGH) instead of artificial pitch shifting.
 * - Requests transient audio focus with ducking (USAGE_ASSISTANCE_NAVIGATION_GUIDANCE) so background audio
 *   (e.g., Spotify, radio) fades down smoothly during alerts and restores afterward.
 * - Uses natural prosody, breathing pauses, and concise driving alerts.
 */
class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // Voice configuration
    private var voiceGender = "FEMALE"
    private var speechRate = 1.0f
    private var speechPitch = 1.0f

    // Alert spam prevention timers
    private var lastApproachingAlertTimeMs = 0L
    private var lastExceededAlertTimeMs = 0L
    private var hasApproachingAlertFiredInCurrentZone = false

    init {
        initializeEngine()
    }

    private fun initializeEngine() {
        val googleTtsPackage = "com.google.android.tts"
        val hasGoogleTts = try {
            context.packageManager.getPackageInfo(googleTtsPackage, 0)
            true
        } catch (e: Exception) {
            false
        }

        // Prefer Google's realistic speech engine if installed on the device
        tts = if (hasGoogleTts) {
            Log.d(TAG, "Initializing with Google Speech Services engine ($googleTtsPackage)")
            TextToSpeech(context.applicationContext, this, googleTtsPackage)
        } else {
            Log.d(TAG, "Initializing with default system TTS engine")
            TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Prefer UK English (widely used in Ghana) or US English
            val ukResult = tts?.setLanguage(Locale.UK)
            if (ukResult == TextToSpeech.LANG_MISSING_DATA || ukResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }

            // Configure audio attributes for Navigation Guidance (loud and clear over car noise)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)

            // Listen for speech completion to release audio ducking
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    abandonAudioFocus()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    abandonAudioFocus()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    abandonAudioFocus()
                }
            })

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

    /**
     * Selects the highest quality natural neural voice installed on the device
     * matching the preferred gender, without pitch distortion.
     */
    private fun applyVoiceSettings() {
        val engine = tts ?: return

        // Set speech speed (natural cadence: 1.0f)
        engine.setSpeechRate(speechRate)

        val targetGender = voiceGender.uppercase(Locale.ROOT)
        if (targetGender == "DEFAULT") {
            engine.setPitch(speechPitch)
            return
        }

        try {
            val availableVoices = engine.voices ?: emptySet()
            val englishVoices = availableVoices.filter { voice ->
                val lang = voice.locale.language.lowercase(Locale.ROOT)
                lang == "en"
            }

            // Find best voice candidates matching gender and high quality
            val matchedVoice = findBestRealisticVoice(englishVoices, targetGender)

            if (matchedVoice != null) {
                engine.voice = matchedVoice
                // When using a genuine neural voice, keep natural pitch to avoid robotic distortion
                engine.setPitch(speechPitch)
                Log.d(TAG, "Selected realistic voice: ${matchedVoice.name} (quality=${matchedVoice.quality})")
            } else {
                // Fallback: If no distinct voice package is found, use system voice with mild pitch nuance
                val fallbackPitch = when (targetGender) {
                    "MALE" -> speechPitch * 0.90f
                    "FEMALE" -> speechPitch * 1.10f
                    else -> speechPitch
                }
                engine.setPitch(fallbackPitch)
                Log.w(TAG, "No specific $targetGender voice found, using fallback pitch: $fallbackPitch")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error configuring realistic voice: ${e.message}")
            engine.setPitch(speechPitch)
        }
    }

    /**
     * Discovers the best quality, offline-capable realistic voice matching the requested gender.
     */
    private fun findBestRealisticVoice(voices: Collection<Voice>, gender: String): Voice? {
        val isMale = gender == "MALE"

        // Known natural/neural voice identifiers in Google TTS
        val maleKeywords = listOf("male", "en-gb-x-rjs", "en-gb-x-gbb", "en-us-x-iom", "en-us-x-iol", "en-au-x-aud")
        val femaleKeywords = listOf("female", "en-gb-x-fis", "en-gb-x-gba", "en-us-x-sfg", "en-us-x-tpc", "en-us-x-tpd", "en-au-x-aub")

        val targetKeywords = if (isMale) maleKeywords else femaleKeywords
        val opposingKeywords = if (isMale) femaleKeywords else maleKeywords

        val candidates = voices.filter { voice ->
            val name = voice.name.lowercase(Locale.ROOT)
            val matchesTarget = targetKeywords.any { kw -> name.contains(kw) }
            val matchesOpposing = opposingKeywords.any { kw -> name.contains(kw) && !kw.contains("male") }
            matchesTarget && !matchesOpposing
        }

        if (candidates.isEmpty()) return null

        // Priority 1: High quality offline voices (no network required, zero delay while driving)
        // Priority 2: Highest Voice.quality (e.g. QUALITY_VERY_HIGH = 400, QUALITY_HIGH = 300)
        return candidates.sortedWith(
            compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                .thenByDescending { it.quality }
        ).firstOrNull()
    }

    /**
     * Checks vehicle speed and triggers proactive voice alerts with anti-spam cooldown.
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
            // Speed exceeds limit: Clear, short and precise prompt
            currentSpeedKmh >= speedLimitKmh -> {
                val cooldownMs = cooldownSeconds * 1000L
                if (currentTime - lastExceededAlertTimeMs >= cooldownMs) {
                    val message = "Slow down. Speed limit exceeded."
                    speak(message, TextToSpeech.QUEUE_FLUSH)
                    lastExceededAlertTimeMs = currentTime
                }
            }

            // Approaching speed limit: Early short warning
            currentSpeedKmh >= approachingThreshold && currentSpeedKmh < speedLimitKmh -> {
                if (!hasApproachingAlertFiredInCurrentZone) {
                    val message = "You are about to cross the speed limit."
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
     * Speaks the given message using the current TTS settings and requests transient audio focus.
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized) {
            Log.w(TAG, "Cannot speak: TextToSpeech is not yet initialized.")
            return
        }

        requestAudioDucking()

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val utteranceId = "SpeedCheck_${System.currentTimeMillis()}"
        tts?.speak(text, queueMode, params, utteranceId)
    }

    private fun requestAudioDucking() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .build()

            audioFocusRequest = focusRequest
            audioManager?.requestAudioFocus(focusRequest)
        } catch (e: Exception) {
            Log.w(TAG, "Could not request audio focus: ${e.message}")
        }
    }

    private fun abandonAudioFocus() {
        try {
            audioFocusRequest?.let { req ->
                audioManager?.abandonAudioFocusRequest(req)
                audioFocusRequest = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error abandoning audio focus: ${e.message}")
        }
    }

    fun shutdown() {
        abandonAudioFocus()
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}
