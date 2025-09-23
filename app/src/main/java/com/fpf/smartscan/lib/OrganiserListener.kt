package com.fpf.smartscan.lib

import android.app.Application
import android.content.Context
import android.net.Uri
import com.fpf.smartscan.data.movehistory.MoveHistory
import com.fpf.smartscan.data.movehistory.MoveHistoryDatabase
import com.fpf.smartscan.data.movehistory.MoveHistoryRepository
import com.fpf.smartscansdk.core.processors.IProcessorListener
import com.fpf.smartscansdk.core.processors.Metrics
import com.fpf.smartscansdk.extensions.organisers.OrganiserResult

object OrganiserListener : IProcessorListener<Uri, OrganiserResult> {

    var failedMoveCount = 0
    private var moveHistoryRepository: MoveHistoryRepository? = null
    const val TAG = "OrganiserListener"

    override suspend fun onBatchComplete(context: Context, batch: List<OrganiserResult>) {
        batch.forEach { result ->
            if(result.destination == null) {
                ++failedMoveCount
                return@forEach
            }
            val newFileUri = moveFile(context, result.source, result.destination!!)
            val isMoved = newFileUri != null
            if(isMoved){
                getRepo(context.applicationContext).insert(
                    MoveHistory(
                        scanId = result.scanId,
                        sourceUri = result.source.toString(),
                        destinationUri = newFileUri.toString(),
                        date = System.currentTimeMillis(),
                    )
                )
            }else{
                ++failedMoveCount
            }
        }
    }

    override suspend fun onActive(context: Context) {
        failedMoveCount = 0
    }
    private fun getRepo(context: Context): MoveHistoryRepository {
        return moveHistoryRepository ?: MoveHistoryRepository(MoveHistoryDatabase.getDatabase(context.applicationContext as Application).moveHistoryDao())
            .also { moveHistoryRepository = it }
    }
}