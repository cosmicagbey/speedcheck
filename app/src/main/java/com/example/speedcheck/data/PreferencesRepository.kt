package com.example.speedcheck.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "speedcheck_settings")

/**
 * Repository to persist and retrieve user speed preferences via Jetpack DataStore.
 */
class PreferencesRepository private constructor(private val context: Context) {

    private object PreferencesKeys {
        val SPEED_LIMIT_KMH = intPreferencesKey("speed_limit_kmh")
        val APPROACHING_OFFSET_KMH = intPreferencesKey("approaching_offset_kmh")
        val VOICE_ALERTS_ENABLED = booleanPreferencesKey("voice_alerts_enabled")
        val APPROACHING_ALERT_ENABLED = booleanPreferencesKey("approaching_alert_enabled")
        val VOICE_GENDER = stringPreferencesKey("voice_gender")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val ALERT_COOLDOWN_SECONDS = intPreferencesKey("alert_cooldown_seconds")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val AUTO_DETECT_MOVEMENT = booleanPreferencesKey("auto_detect_movement")
        val AUTO_DETECT_SPEED_LIMIT = booleanPreferencesKey("auto_detect_speed_limit")
        val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")
        val HIGH_CONTRAST_MODE = booleanPreferencesKey("high_contrast_mode")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                speedLimitKmh = preferences[PreferencesKeys.SPEED_LIMIT_KMH] ?: 80,
                approachingOffsetKmh = preferences[PreferencesKeys.APPROACHING_OFFSET_KMH] ?: 5,
                voiceAlertsEnabled = preferences[PreferencesKeys.VOICE_ALERTS_ENABLED] ?: true,
                approachingAlertEnabled = preferences[PreferencesKeys.APPROACHING_ALERT_ENABLED] ?: true,
                voiceGender = preferences[PreferencesKeys.VOICE_GENDER] ?: "FEMALE",
                speechRate = preferences[PreferencesKeys.SPEECH_RATE] ?: 1.0f,
                speechPitch = preferences[PreferencesKeys.SPEECH_PITCH] ?: 1.0f,
                alertCooldownSeconds = preferences[PreferencesKeys.ALERT_COOLDOWN_SECONDS] ?: 15,
                monitoringEnabled = preferences[PreferencesKeys.MONITORING_ENABLED] ?: false,
                autoDetectSpeedLimit = preferences[PreferencesKeys.AUTO_DETECT_SPEED_LIMIT] ?: true,
                autoDetectVehicleMovement = preferences[PreferencesKeys.AUTO_DETECT_MOVEMENT] ?: true,
                autoStartOnBoot = preferences[PreferencesKeys.AUTO_START_ON_BOOT] ?: true,
                highContrastMode = preferences[PreferencesKeys.HIGH_CONTRAST_MODE] ?: true
            )
        }

    suspend fun getInitialPreferences(): UserPreferences {
        return userPreferencesFlow.first()
    }

    suspend fun updateSpeedLimit(limitKmh: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SPEED_LIMIT_KMH] = limitKmh
        }
    }

    suspend fun updateApproachingOffset(offsetKmh: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APPROACHING_OFFSET_KMH] = offsetKmh
        }
    }

    suspend fun updateVoiceAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOICE_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun updateApproachingAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APPROACHING_ALERT_ENABLED] = enabled
        }
    }

    suspend fun updateVoiceGender(gender: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOICE_GENDER] = gender
        }
    }

    suspend fun updateSpeechRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SPEECH_RATE] = rate
        }
    }

    suspend fun updateSpeechPitch(pitch: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SPEECH_PITCH] = pitch
        }
    }

    suspend fun updateAlertCooldown(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALERT_COOLDOWN_SECONDS] = seconds
        }
    }

    suspend fun updateMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MONITORING_ENABLED] = enabled
        }
    }

    suspend fun updateAutoDetectSpeedLimit(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_DETECT_SPEED_LIMIT] = enabled
        }
    }

    suspend fun updateAutoDetectMovement(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_DETECT_MOVEMENT] = enabled
        }
    }

    suspend fun updateAutoStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_START_ON_BOOT] = enabled
        }
    }

    suspend fun updateHighContrastMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIGH_CONTRAST_MODE] = enabled
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PreferencesRepository? = null

        fun getInstance(context: Context): PreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
