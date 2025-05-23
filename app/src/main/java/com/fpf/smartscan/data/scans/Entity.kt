package com.fpf.smartscan.data.scans

import androidx.room.*

// 1. Entity
@Entity(tableName = "scan_data")
data class ScanData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,  // Auto-generated primary key
    val date: Long,
    val result: Int,
){
    companion object {
        const val ERROR_RESULT = -1
        const val IN_PROGRESS_RESULT = -2 // workaround to allow undo fucntionality
    }
}
