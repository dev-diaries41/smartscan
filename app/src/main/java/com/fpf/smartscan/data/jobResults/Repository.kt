package com.fpf.smartscan.data.jobResults

class JobResultRepository(private val jobResultDao: JobResultDao) {

    suspend fun insertJobResult(jobResult: JobResult) {
        jobResultDao.insert(jobResult)
    }

    suspend fun getAllResultsByJobName(jobName: String): List<JobResult> {
        return jobResultDao.getAllResults(jobName)
    }

    suspend fun clearResults() {
        jobResultDao.clearAll()
    }

    suspend fun clearResultsByJobName(jobName: String) {
        jobResultDao.clearByJobName(jobName)
    }

    suspend fun getTotalProcessedCount(jobName: String): Int {
        return jobResultDao.getTotalProcessedCount(jobName) ?: 0
    }

}
