package com.speedcheck.android.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State representing in-app update progress.
 */
sealed class InAppUpdateStatus {
    object Idle : InAppUpdateStatus()
    object Checking : InAppUpdateStatus()
    data class UpdateAvailable(
        val availableVersionCode: Int,
        val appUpdateInfo: AppUpdateInfo
    ) : InAppUpdateStatus()
    data class UpToDate(val currentVersion: String, val currentVersionCode: Long) : InAppUpdateStatus()
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : InAppUpdateStatus()
    object Downloaded : InAppUpdateStatus()
    data class Error(val message: String) : InAppUpdateStatus()
}

/**
 * Manages official Google Play In-App Updates, allowing users to receive, download,
 * and install new releases seamlessly inside the app without opening the Play Store.
 */
class InAppUpdateManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(appContext)

    private val _updateStatus = MutableStateFlow<InAppUpdateStatus>(InAppUpdateStatus.Idle)
    val updateStatus: StateFlow<InAppUpdateStatus> = _updateStatus.asStateFlow()

    private var latestAppUpdateInfo: AppUpdateInfo? = null

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytes = state.totalBytesToDownload()
                _updateStatus.value = InAppUpdateStatus.Downloading(bytesDownloaded, totalBytes)
                Log.d(TAG, "In-app update downloading: $bytesDownloaded / $totalBytes")
            }
            InstallStatus.DOWNLOADED -> {
                _updateStatus.value = InAppUpdateStatus.Downloaded
                Log.i(TAG, "In-app update downloaded and ready to install.")
            }
            InstallStatus.FAILED -> {
                _updateStatus.value = InAppUpdateStatus.Error("Download failed. Please try again.")
                Log.e(TAG, "In-app update install failed with error: ${state.installErrorCode()}")
            }
            InstallStatus.CANCELED -> {
                _updateStatus.value = InAppUpdateStatus.Idle
                Log.d(TAG, "In-app update cancelled.")
            }
            else -> {}
        }
    }

    init {
        try {
            appUpdateManager.registerListener(installStateUpdatedListener)
        } catch (e: Exception) {
            Log.w(TAG, "Could not register install listener: ${e.message}")
        }
    }

    /**
     * Checks Google Play for new app releases in-app.
     */
    fun checkForUpdates(coroutineScope: CoroutineScope? = null, onComplete: ((Boolean) -> Unit)? = null) {
        _updateStatus.value = InAppUpdateStatus.Checking

        val packageInfo = getPackageInfo()
        val currentVersion = packageInfo?.versionName ?: "1.0.6"
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode ?: 7L
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toLong() ?: 7L
        }

        try {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                latestAppUpdateInfo = info
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) || info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE))
                ) {
                    _updateStatus.value = InAppUpdateStatus.UpdateAvailable(
                        availableVersionCode = info.availableVersionCode(),
                        appUpdateInfo = info
                    )
                    onComplete?.invoke(true)
                } else if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _updateStatus.value = InAppUpdateStatus.Downloaded
                    onComplete?.invoke(true)
                } else {
                    _updateStatus.value = InAppUpdateStatus.UpToDate(currentVersion, currentVersionCode)
                    onComplete?.invoke(false)
                }
            }.addOnFailureListener { exception ->
                Log.w(TAG, "Google Play In-App Update check returned: ${exception.message}")
                // If Play Store API is unavailable (e.g. testing build outside store), report clean up-to-date status
                _updateStatus.value = InAppUpdateStatus.UpToDate(currentVersion, currentVersionCode)
                onComplete?.invoke(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during in-app update check: ${e.message}")
            _updateStatus.value = InAppUpdateStatus.UpToDate(currentVersion, currentVersionCode)
            onComplete?.invoke(false)
        }
    }

    /**
     * Starts the in-app update flow using the user's preferred type (Flexible or Immediate).
     */
    fun startUpdate(activity: Activity, immediate: Boolean = false, requestCode: Int = REQUEST_CODE_IN_APP_UPDATE): Boolean {
        val info = latestAppUpdateInfo ?: return false
        val updateType = if (immediate && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            AppUpdateType.IMMEDIATE
        } else if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            AppUpdateType.FLEXIBLE
        } else if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            AppUpdateType.IMMEDIATE
        } else {
            return false
        }

        return try {
            val options = AppUpdateOptions.newBuilder(updateType).build()
            appUpdateManager.startUpdateFlowForResult(info, activity, options, requestCode)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start in-app update flow: ${e.message}")
            _updateStatus.value = InAppUpdateStatus.Error("Could not start update: ${e.message}")
            false
        }
    }

    /**
     * Completes a flexible update by restarting the app and installing the new release.
     */
    fun completeUpdate() {
        try {
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete update: ${e.message}")
        }
    }

    fun unregisterListener() {
        try {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
        } catch (e: Exception) {
            Log.w(TAG, "Could not unregister listener: ${e.message}")
        }
    }

    private fun getPackageInfo(): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.getPackageInfo(appContext.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "InAppUpdateManager"
        const val REQUEST_CODE_IN_APP_UPDATE = 8801

        @Volatile
        private var instance: InAppUpdateManager? = null

        fun getInstance(context: Context): InAppUpdateManager {
            return instance ?: synchronized(this) {
                instance ?: InAppUpdateManager(context).also { instance = it }
            }
        }
    }
}
