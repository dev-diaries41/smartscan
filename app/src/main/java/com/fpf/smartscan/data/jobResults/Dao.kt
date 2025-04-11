package com.fpf.smartscan.data.jobResults

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface JobResultDao {

    @Insert
    suspend fun insert(jobResult: JobResult)

    @Query("SELECT * FROM job_results WHERE jobName = :jobName ORDER BY finishTime DESC")
    suspend fun getAllResults(jobName: String): List<JobResult>

    @Query("DELETE FROM job_results")
    suspend fun clearAll()

    @Query("DELETE FROM job_results WHERE jobName = :jobName")
    suspend fun clearByJobName(jobName: String)

    @Query("SELECT SUM(processedCount) FROM job_results WHERE jobName = :jobName")
    suspend fun getTotalProcessedCount(jobName: String): Int?
}
