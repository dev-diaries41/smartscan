package com.fpf.smartscan.lib

import android.app.ActivityManager
import android.content.Context

class MemoryUtils(private val context: Context) {

    companion object {
        private const val LOW_MEMORY_THRESHOLD = 800L * 1024 * 1024
        private const val HIGH_MEMORY_THRESHOLD = 1_600L * 1024 * 1024
        private const val MIN_CONCURRENCY = 1
        private const val MAX_CONCURRENCY = 4
    }

    fun getFreeMemory(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem
    }

    fun calculateConcurrencyLevel(): Int {
        val freeMemory = getFreeMemory()
        return when {
            freeMemory < LOW_MEMORY_THRESHOLD -> MIN_CONCURRENCY
            freeMemory >= HIGH_MEMORY_THRESHOLD -> MAX_CONCURRENCY
            else -> {
                val proportion = (freeMemory - LOW_MEMORY_THRESHOLD).toDouble() /
                        (HIGH_MEMORY_THRESHOLD - LOW_MEMORY_THRESHOLD)
                (MIN_CONCURRENCY + proportion * (MAX_CONCURRENCY - MIN_CONCURRENCY)).toInt().coerceAtLeast(MIN_CONCURRENCY)
            }
        }
    }
}
