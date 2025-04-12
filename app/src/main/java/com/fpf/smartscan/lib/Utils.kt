package com.fpf.smartscan.lib

import com.fpf.smartscan.data.scans.ScanData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

fun toDateString(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getTimeInMinutesAndSeconds(milliseconds: Long): Pair<Long, Long> {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    return Pair(minutes, seconds % 60)
}

fun generatePlaceholderScanHistory(n: Int): List<ScanData> {
    val generatedItems = mutableListOf<ScanData>()

    for (i in 0 until n) {
        val date = System.currentTimeMillis() - (i * 1000 * 60 * 60 * 24)
        val item = ScanData(
            date = date,
            result = i +  Random.nextInt(1, 20)
        )
        generatedItems.add(item)
    }
    return generatedItems
}