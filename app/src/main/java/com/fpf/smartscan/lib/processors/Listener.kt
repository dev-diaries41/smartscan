package com.fpf.smartscan.lib.processors

import android.content.Context

interface IIndexListener {
    fun onProgress(processedCount: Int, total: Int)
    fun onComplete(context: Context, totalProcessed: Int, processingTime: Long)
}
