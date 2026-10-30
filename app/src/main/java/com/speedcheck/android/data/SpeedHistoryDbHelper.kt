package com.speedcheck.android.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.speedcheck.android.model.SpeedAlertStatus
import com.speedcheck.android.model.SpeedLogEntry

/**
 * SQLite database helper managing permanent storage of vehicle speed evidence logs.
 */
class SpeedHistoryDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSql = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_TIMESTAMP INTEGER,
                $COLUMN_PLACE_NAME TEXT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_SPEED REAL,
                $COLUMN_MAX_SPEED REAL,
                $COLUMN_SPEED_LIMIT INTEGER,
                $COLUMN_ALERT_STATUS TEXT,
                $COLUMN_IS_AUTO_DETECT INTEGER,
                $COLUMN_COMPLIANCE TEXT,
                $COLUMN_NOTE TEXT
            )
        """.trimIndent()
        db.execSQL(createTableSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertLog(entry: SpeedLogEntry): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, entry.id)
            put(COLUMN_TIMESTAMP, entry.timestamp)
            put(COLUMN_PLACE_NAME, entry.placeName)
            put(COLUMN_LATITUDE, entry.latitude)
            put(COLUMN_LONGITUDE, entry.longitude)
            put(COLUMN_SPEED, entry.speedKmh)
            put(COLUMN_MAX_SPEED, entry.maxSpeedKmh)
            put(COLUMN_SPEED_LIMIT, entry.speedLimitKmh)
            put(COLUMN_ALERT_STATUS, entry.alertStatus.name)
            put(COLUMN_IS_AUTO_DETECT, if (entry.isAutoDetect) 1 else 0)
            put(COLUMN_COMPLIANCE, entry.complianceStatus)
            put(COLUMN_NOTE, entry.note)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun getAllLogs(): List<SpeedLogEntry> {
        val list = mutableListOf<SpeedLogEntry>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP DESC",
            "500" // retain last 500 records
        )

        cursor.use { c ->
            val idIndex = c.getColumnIndexOrThrow(COLUMN_ID)
            val timeIndex = c.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            val placeIndex = c.getColumnIndexOrThrow(COLUMN_PLACE_NAME)
            val latIndex = c.getColumnIndexOrThrow(COLUMN_LATITUDE)
            val lonIndex = c.getColumnIndexOrThrow(COLUMN_LONGITUDE)
            val speedIndex = c.getColumnIndexOrThrow(COLUMN_SPEED)
            val maxSpeedIndex = c.getColumnIndexOrThrow(COLUMN_MAX_SPEED)
            val limitIndex = c.getColumnIndexOrThrow(COLUMN_SPEED_LIMIT)
            val alertIndex = c.getColumnIndexOrThrow(COLUMN_ALERT_STATUS)
            val autoIndex = c.getColumnIndexOrThrow(COLUMN_IS_AUTO_DETECT)
            val compIndex = c.getColumnIndexOrThrow(COLUMN_COMPLIANCE)
            val noteIndex = c.getColumnIndexOrThrow(COLUMN_NOTE)

            while (c.moveToNext()) {
                val statusStr = c.getString(alertIndex)
                val status = try {
                    SpeedAlertStatus.valueOf(statusStr)
                } catch (_: Exception) {
                    SpeedAlertStatus.NORMAL
                }

                list.add(
                    SpeedLogEntry(
                        id = c.getString(idIndex),
                        timestamp = c.getLong(timeIndex),
                        placeName = c.getString(placeIndex),
                        latitude = c.getDouble(latIndex),
                        longitude = c.getDouble(lonIndex),
                        speedKmh = c.getFloat(speedIndex),
                        maxSpeedKmh = c.getFloat(maxSpeedIndex),
                        speedLimitKmh = c.getInt(limitIndex),
                        alertStatus = status,
                        isAutoDetect = c.getInt(autoIndex) == 1,
                        complianceStatus = c.getString(compIndex),
                        note = c.getString(noteIndex)
                    )
                )
            }
        }
        return list
    }

    fun clearAllLogs(): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, null, null)
    }

    companion object {
        private const val DATABASE_NAME = "speedcheck_history.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "speed_history_logs"
        const val COLUMN_ID = "id"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_PLACE_NAME = "place_name"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_SPEED = "speed_kmh"
        const val COLUMN_MAX_SPEED = "max_speed_kmh"
        const val COLUMN_SPEED_LIMIT = "speed_limit_kmh"
        const val COLUMN_ALERT_STATUS = "alert_status"
        const val COLUMN_IS_AUTO_DETECT = "is_auto_detect"
        const val COLUMN_COMPLIANCE = "compliance_status"
        const val COLUMN_NOTE = "note"
    }
}
