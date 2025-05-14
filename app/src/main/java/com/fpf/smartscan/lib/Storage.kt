package com.fpf.smartscan.lib

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * A Storage class similar to React Native's AsyncStorage.
 *
 * Provides APIs for setItem, getItem, deleteItem, clear, and getAllKeys.
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

    fun setItem(key: String, value: String) {
        sharedPreferences.edit() {
            putString(key, value)
        }
    }

    fun getItem(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    fun deleteItem(key: String) {
        sharedPreferences.edit() {
            remove(key)
        }
    }

    fun clear() {
        sharedPreferences.edit() {
            clear()
        }
    }

    fun getAllKeys(): Set<String> {
        return sharedPreferences.all.keys
    }
}
