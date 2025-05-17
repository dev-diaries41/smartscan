package com.fpf.smartscan.data.moveHistory

import androidx.room.*

@Entity(tableName = "move_history")
data class MoveHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,  // Auto-generated primary key
    val scanId: Long,
    val sourceUri: String,
    val destinationUri: String,
    val date: Long,
)
