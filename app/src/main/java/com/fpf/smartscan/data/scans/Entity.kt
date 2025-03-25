package com.fpf.smartscan.data.scans

import androidx.room.*

// 1. Entity
@Entity(tableName = "scan_data")
data class ScanData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,  // Auto-generated primary key
    val date: Long,
    val result: Int,
)
