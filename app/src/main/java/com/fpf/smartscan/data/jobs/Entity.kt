package com.fpf.smartscan.data.jobs

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "job_results")
data class JobResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobName: String,
    val startTime: Long,
    val processedCount: Int,
    val finishTime: Long,
    val isSuccess: Boolean
)
