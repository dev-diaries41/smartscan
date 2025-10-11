package com.fpf.smartscan.lib

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.fpf.smartscan.data.AppSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun loadSettings(sharedPrefs: SharedPreferences): AppSettings {
    val jsonSettings = sharedPrefs.getString("app_settings", null)
    return if (jsonSettings != null) {
        try {
            Json.decodeFromString<AppSettings>(jsonSettings)
        } catch (e: Exception) {
            Log.e("loadSettings", "Failed to decode settings", e)
            AppSettings()
        }
    } else {
        AppSettings()
    }
}

fun saveSettings(sharedPrefs: SharedPreferences, settings: AppSettings) {
    sharedPrefs.edit {putString("app_settings", Json.encodeToString(settings))  }
}