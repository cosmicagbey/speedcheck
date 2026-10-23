package com.example.speedcheck

import android.app.Application
import android.util.Log
import com.example.speedcheck.service.NotificationHelper

/**
 * Custom Application class for SpeedCheck.
 * Initializes notification channels and application-level singletons.
 */
class SpeedCheckApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("SpeedCheckApp", "Application initializing...")
        // Pre-create notification channels so system knows them early
        NotificationHelper(this)
    }
}
