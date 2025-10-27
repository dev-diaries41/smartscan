package com.fpf.smartscan.ui.screens.retagging

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel pro RetaggingScreen
 *
 * Sleduje WorkManager progress a poskytuje real-time statistiky
 * přes StateFlow. Podporuje polling každých 500ms pro smooth updates.
 */
class RetaggingViewModel : ViewModel() {

    private val _stats = MutableStateFlow(RetaggingStats())
    val stats: StateFlow<RetaggingStats> = _stats.asStateFlow()

    private var workId: UUID? = null
    private var isMonitoring = false

    /**
     * Spustí monitoring WorkManager job pro daný work ID
     *
     * Polling každých 500ms pro získání aktuálního progress z WorkManager.
     * Progress data obsahují:
     * - current: aktuální počet zpracovaných obrázků
     * - total: celkový počet obrázků
     * - tagsAssigned: celkový počet přiřazených tagů
     * - activeTagsCount: počet aktivních tagů
     * - topTags: JSON seznam top 5 tagů
     * - avgTimePerImage: průměrný čas na obrázek (ms)
     * - imagesPerMinute: rychlost zpracování
     */
    fun startMonitoring(context: Context, workId: UUID) {
        this.workId = workId
        isMonitoring = true

        viewModelScope.launch {
            while (isMonitoring) {
                try {
                    val workInfo = WorkManager.getInstance(context)
                        .getWorkInfoById(workId)
                        .get()

                    when (workInfo?.state) {
                        WorkInfo.State.RUNNING -> {
                            // Načtení progress dat z WorkManager
                            val current = workInfo.progress.getInt("current", 0)
                            val total = workInfo.progress.getInt("total", 0)
                            val tagsAssigned = workInfo.progress.getInt("tagsAssigned", 0)
                            val activeTagsCount = workInfo.progress.getInt("activeTagsCount", 0)
                            val avgTime = workInfo.progress.getFloat("avgTimePerImage", 0f)
                            val imagesPerMin = workInfo.progress.getFloat("imagesPerMinute", 0f)
                            val topTagsJson = workInfo.progress.getString("topTags")

                            // Parse top tags z JSON
                            val topTags = parseTopTags(topTagsJson)

                            // Update state
                            _stats.value = RetaggingStats(
                                current = current,
                                total = total,
                                tagsAssigned = tagsAssigned,
                                activeTagsCount = activeTagsCount,
                                topTags = topTags,
                                avgTimePerImage = avgTime,
                                imagesPerMinute = imagesPerMin,
                                isComplete = false,
                                error = null
                            )
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            // Finální stav - 100% dokončeno
                            _stats.value = _stats.value.copy(
                                current = _stats.value.total,
                                isComplete = true,
                                error = null
                            )
                            stopMonitoring()
                        }
                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            // Error state
                            val errorMsg = if (workInfo.state == WorkInfo.State.FAILED) {
                                workInfo.outputData.getString("error") ?: "Re-tagging selhal"
                            } else {
                                "Re-tagging byl zrušen"
                            }
                            _stats.value = _stats.value.copy(
                                error = errorMsg,
                                isComplete = false
                            )
                            stopMonitoring()
                        }
                        else -> {
                            // ENQUEUED, BLOCKED - čekání
                        }
                    }
                } catch (e: Exception) {
                    // WorkInfo nedostupné (pravděpodobně job ještě nezačal)
                    _stats.value = _stats.value.copy(
                        error = "Chyba při načítání statistik: ${e.message}"
                    )
                }

                // Polling interval: 500ms
                delay(500)
            }
        }
    }

    /**
     * Zastaví monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
    }

    /**
     * Reset stats na výchozí hodnoty
     */
    fun reset() {
        _stats.value = RetaggingStats()
        stopMonitoring()
    }

    /**
     * Parse JSON string top tagů na List<TagStats>
     *
     * Očekávaný formát:
     * [{"name":"Porn - Explicit","count":234,"color":-65536},...]
     */
    private fun parseTopTags(json: String?): List<TagStats> {
        if (json.isNullOrBlank()) return emptyList()

        return try {
            // Simple JSON parsing (bez Gson/Moshi závislosti)
            // Formát: [{"name":"X","count":N,"color":C},...]
            val items = json
                .trim()
                .removeSurrounding("[", "]")
                .split("},")
                .mapNotNull { item ->
                    val cleaned = item.trim().removeSurrounding("{", "}")
                    val parts = cleaned.split(",")
                    if (parts.size >= 3) {
                        val name = parts[0].substringAfter("\"name\":\"").substringBefore("\"")
                        val count = parts[1].substringAfter("\"count\":").toIntOrNull() ?: 0
                        val color = parts[2].substringAfter("\"color\":").toIntOrNull() ?: 0
                        TagStats(name, count, color)
                    } else null
                }
            items
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
