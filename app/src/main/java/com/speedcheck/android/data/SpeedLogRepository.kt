package com.speedcheck.android.data

import android.content.Context
import android.content.Intent
import com.speedcheck.android.model.SpeedLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for managing driving speed logs, providing observable state,
 * and generating verifiable evidence exports.
 */
class SpeedLogRepository private constructor(private val context: Context) {

    private val dbHelper = SpeedHistoryDbHelper(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _logsFlow = MutableStateFlow<List<SpeedLogEntry>>(emptyList())
    val logsFlow: StateFlow<List<SpeedLogEntry>> = _logsFlow.asStateFlow()

    init {
        refreshLogs()
    }

    fun refreshLogs() {
        scope.launch {
            val list = dbHelper.getAllLogs()
            _logsFlow.value = list
        }
    }

    suspend fun logSnapshot(entry: SpeedLogEntry) = withContext(Dispatchers.IO) {
        dbHelper.insertLog(entry)
        val updated = dbHelper.getAllLogs()
        _logsFlow.value = updated
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dbHelper.clearAllLogs()
        _logsFlow.value = emptyList()
    }

    /**
     * Generates a CSV format evidence report and opens Android Share Sheet.
     */
    fun exportLogs(context: Context) {
        val logs = _logsFlow.value
        if (logs.isEmpty()) return

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val csvBuilder = StringBuilder()

        // CSV Header
        csvBuilder.append("Timestamp,Date & Time,Location / Area,Latitude,Longitude,Current Speed (km/h),Max Speed (km/h),Speed Limit (km/h),Compliance Status,Alert Status,Detection Mode\n")

        for (log in logs) {
            val dateStr = dateFormat.format(Date(log.timestamp))
            val cleanPlace = log.placeName.replace(",", ";").replace("\"", "")
            csvBuilder.append("${log.timestamp},\"$dateStr\",\"$cleanPlace\",${log.latitude},${log.longitude},${log.speedKmh},${log.maxSpeedKmh},${log.speedLimitKmh},\"${log.complianceStatus}\",\"${log.alertStatus.name}\",\"${if (log.isAutoDetect) "Auto-Detect" else "Manual"}\"\n")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "SpeedCheck Driving Evidence Log - ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}")
            putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val chooser = Intent.createChooser(shareIntent, "Export Driving Evidence Log").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    companion object {
        @Volatile
        private var INSTANCE: SpeedLogRepository? = null

        fun getInstance(context: Context): SpeedLogRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SpeedLogRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
