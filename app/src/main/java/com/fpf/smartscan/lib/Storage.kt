package com.fpf.smartscan.lib

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A Storage class similar to React Native's AsyncStorage.
 *
 * Provides asynchronous APIs for setItem, getItem, deleteItem, clear, and getAllKeys.
 */
class Storage private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "AsyncStorage"

        @Volatile
        private var instance: Storage? = null

        fun getInstance(context: Context): Storage {
            return instance ?: synchronized(this) {
                instance ?: Storage(context.applicationContext).also { instance = it }
            }
        }
    }


    suspend fun setItem(key: String, value: String) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit() { putString(key, value) }
        }
    }

    suspend fun getItem(key: String): String? {
        return withContext(Dispatchers.IO) {
            sharedPreferences.getString(key, null)
        }
    }

    suspend fun deleteItem(key: String) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit() { remove(key) }
        }
    }

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit() { clear() }
        }
    }

    suspend fun getAllKeys(): Set<String> {
        return withContext(Dispatchers.IO) {
            sharedPreferences.all.keys
        }
    }
}