package com.fpf.smartscan.ui.screens.retagging

/**
 * Data class pro real-time statistiky re-tagging procesu
 *
 * Obsahuje veškeré metriky pro zobrazení v RetaggingScreen:
 * - Progress (current/total)
 * - Přiřazené tagy
 * - Top 5 tagů s počty
 * - Performance metriky
 * - Error state
 */
data class RetaggingStats(
    val current: Int = 0,
    val total: Int = 0,
    val tagsAssigned: Int = 0,
    val activeTagsCount: Int = 0,
    val topTags: List<TagStats> = emptyList(),
    val avgTimePerImage: Float = 0f, // v milisekundách
    val imagesPerMinute: Float = 0f,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    /**
     * Progress v procentech (0.0 - 1.0)
     */
    val progress: Float
        get() = if (total > 0) current.toFloat() / total.toFloat() else 0f

    /**
     * Progress jako procento pro UI (0 - 100)
     */
    val progressPercent: Float
        get() = progress * 100f

    /**
     * Elapsed time v sekundách (odvozeno z avgTimePerImage)
     */
    val elapsedSeconds: Float
        get() = if (current > 0) (avgTimePerImage * current) / 1000f else 0f
}

/**
 * Statistiky pro jeden tag během re-taggingu
 *
 * @property name Název tagu
 * @property count Počet obrázků s tímto tagem
 * @property color Barva tagu (Android color int)
 */
data class TagStats(
    val name: String,
    val count: Int,
    val color: Int
)
