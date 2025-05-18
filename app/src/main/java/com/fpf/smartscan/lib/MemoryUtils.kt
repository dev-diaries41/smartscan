package com.fpf.smartscan.lib

import android.app.ActivityManager
import android.content.Context

class MemoryUtils(
    private val context: Context,
    private val lowMemoryThreshold: Long = 800L * 1024 * 1024,
    private val highMemoryThreshold: Long = 1_600L * 1024 * 1024,
    private val minConcurrency: Int = 1,
    private val maxConcurrency: Int = 4
) {

    fun getFreeMemory(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem
    }

    fun calculateConcurrencyLevel(): Int {
        val freeMemory = getFreeMemory()
        return when {
            freeMemory < lowMemoryThreshold -> minConcurrency
            freeMemory >= highMemoryThreshold -> maxConcurrency
            else -> {
                val proportion = (freeMemory - lowMemoryThreshold).toDouble() /
                        (highMemoryThreshold - lowMemoryThreshold)
                (minConcurrency + proportion * (maxConcurrency - minConcurrency)).toInt().coerceAtLeast(minConcurrency)
            }
        }
    }
}
