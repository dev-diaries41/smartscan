package com.fpf.smartscan

import android.app.Application
import android.util.Log
import com.fpf.smartscan.services.TagInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class pro SmartScan
 *
 * Inicializuje globální závislosti a background services:
 * - Preset tagy pro auto-tagging (při prvním spuštění)
 */
class SmartScanApplication : Application() {

    // Application scope pro background operace
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val TAG = "SmartScanApplication"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "SmartScan Application starting...")

        // Inicializace preset tagů v pozadí
        initializePresetTagsIfNeeded()
    }

    /**
     * Inicializuje preset tagy při prvním spuštění aplikace
     *
     * Tato operace:
     * - Běží v background coroutine (neblokuje UI)
     * - Kontroluje, zda již byly tagy inicializovány
     * - Generuje embeddingy a vkládá tagy do databáze
     * - Je thread-safe a idempotentní
     */
    private fun initializePresetTagsIfNeeded() {
        applicationScope.launch {
            try {
                // Kontrola, zda již byly inicializovány
                if (TagInitializer.isInitialized(this@SmartScanApplication)) {
                    Log.d(TAG, "Preset tags already initialized")
                    return@launch
                }

                Log.d(TAG, "Starting preset tags initialization in background...")

                val initializer = TagInitializer(
                    context = this@SmartScanApplication,
                    resources = resources
                )

                val result = initializer.initializePresetTagsAsync()

                result.onSuccess { count ->
                    Log.d(TAG, "Successfully initialized $count preset tags")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to initialize preset tags", error)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during preset tags initialization", e)
            }
        }
    }
}
