package com.fpf.smartscan.ui.screens.retagging

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.R
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Fullscreen stránka pro re-tagging obrázků s real-time statistikami
 *
 * Features:
 * - Real-time progress tracking
 * - Live statistiky (zpracované obrázky, přiřazené tagy, rychlost)
 * - Top 5 tagů s live počítadly
 * - Animované přírůstky
 * - Success/Error states
 * - Auto-návrat po dokončení (3s countdown)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetaggingScreen(
    workId: java.util.UUID,
    viewModel: RetaggingViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Auto-návrat countdown při dokončení
    var countdown by remember { mutableIntStateOf(3) }

    // Spuštění monitoring při vytvoření screen
    LaunchedEffect(workId) {
        viewModel.startMonitoring(context, workId)
    }

    // Auto-návrat po dokončení (3s countdown)
    LaunchedEffect(stats.isComplete) {
        if (stats.isComplete && stats.error == null) {
            countdown = 3
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            onNavigateBack()
        }
    }

    // Disable system back button během procesu
    BackHandler(enabled = !stats.isComplete && stats.error == null) {
        // Block back navigation během běhu
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            when {
                // Error state
                stats.error != null -> {
                    ErrorState(
                        errorMessage = stats.error!!,
                        onBack = onNavigateBack
                    )
                }
                // Success state
                stats.isComplete -> {
                    SuccessState(
                        stats = stats,
                        countdown = countdown,
                        onBack = onNavigateBack
                    )
                }
                // Running state
                else -> {
                    RunningState(stats = stats)
                }
            }
        }
    }
}

/**
 * Running state - zobrazení progress a statistik během běhu
 */
@Composable
private fun RunningState(stats: RetaggingStats) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header
        Text(
            text = "🏷️ " + stringResource(R.string.retagging_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Main progress
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress text
                if (stats.total > 0) {
                    Text(
                        text = stringResource(R.string.retagging_processed_images),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Animated number
                    AnimatedNumber(
                        number = stats.current,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "/ ${stats.total}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { stats.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = getProgressColor(stats.progress),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    // Percentage
                    Text(
                        text = String.format(Locale.US, "%.1f%%", stats.progressPercent),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    // Příprava
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.retagging_preparing),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Statistiky
        if (stats.total > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📊 " + stringResource(R.string.retagging_stats_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    StatRow(
                        icon = "✅",
                        label = stringResource(R.string.retagging_stats_tags_assigned),
                        value = stats.tagsAssigned.toString()
                    )

                    StatRow(
                        icon = "🏷️",
                        label = stringResource(R.string.retagging_stats_active_tags),
                        value = stats.activeTagsCount.toString()
                    )

                    StatRow(
                        icon = "⏱️",
                        label = stringResource(R.string.retagging_stats_avg_time),
                        value = String.format(Locale.US, "%.2fs", stats.avgTimePerImage / 1000f)
                    )

                    StatRow(
                        icon = "🚀",
                        label = stringResource(R.string.retagging_stats_speed),
                        value = String.format(Locale.US, "~%.0f obr/min", stats.imagesPerMinute)
                    )
                }
            }
        }

        // Top tagy
        if (stats.topTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "📋 " + stringResource(R.string.retagging_top_tags_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(stats.topTags.take(5)) { tagStat ->
                            TopTagRow(tagStat)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Success state - zobrazení po dokončení s countdown
 */
@Composable
private fun SuccessState(
    stats: RetaggingStats,
    countdown: Int,
    onBack: () -> Unit
) {
    // Success animation
    val infiniteTransition = rememberInfiniteTransition(label = "success_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success icon
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Success title
        Text(
            text = "🎉 " + stringResource(R.string.retagging_complete_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Summary
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryRow(
                    icon = "✅",
                    label = stringResource(R.string.retagging_summary_processed),
                    value = "${stats.total} ${stringResource(R.string.images)}"
                )

                SummaryRow(
                    icon = "🏷️",
                    label = stringResource(R.string.retagging_summary_assigned),
                    value = "${stats.tagsAssigned} ${stringResource(R.string.tags)}"
                )

                SummaryRow(
                    icon = "⏱️",
                    label = stringResource(R.string.retagging_summary_duration),
                    value = formatDuration(stats.elapsedSeconds)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Auto-návrat countdown
        Text(
            text = stringResource(R.string.retagging_auto_return, countdown),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Manual back button
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.retagging_back_button))
        }
    }
}

/**
 * Error state - zobrazení při chybě
 */
@Composable
private fun ErrorState(
    errorMessage: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "❌ " + stringResource(R.string.retagging_error_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.retagging_back_button))
        }
    }
}

/**
 * Řádek pro jednu statistiku
 */
@Composable
private fun StatRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Řádek pro summary při dokončení
 */
@Composable
private fun SummaryRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Řádek pro jeden tag v top 5
 */
@Composable
private fun TopTagRow(tagStat: TagStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(tagStat.color))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )

        // Tag name
        Text(
            text = tagStat.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Count
        Text(
            text = "${tagStat.count} obr",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Animované číslo
 */
@Composable
private fun AnimatedNumber(
    number: Int,
    style: androidx.compose.ui.text.TextStyle,
    fontWeight: FontWeight = FontWeight.Normal
) {
    var displayedNumber by remember { mutableIntStateOf(0) }

    LaunchedEffect(number) {
        val start = displayedNumber
        val diff = number - start
        val steps = 10
        val increment = diff / steps

        repeat(steps) {
            displayedNumber += increment
            delay(30)
        }
        displayedNumber = number
    }

    Text(
        text = displayedNumber.toString(),
        style = style,
        fontWeight = fontWeight
    )
}

/**
 * Barva progress bar podle pokroku
 * - 0-50%: Primary
 * - 50-80%: Tertiary
 * - 80-100%: Gradient -> Green
 */
@Composable
private fun getProgressColor(progress: Float): Color {
    return when {
        progress < 0.5f -> MaterialTheme.colorScheme.primary
        progress < 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> Color(0xFF4CAF50) // Green
    }
}

/**
 * Formátování času
 */
private fun formatDuration(seconds: Float): String {
    val totalSec = seconds.toInt()
    val minutes = totalSec / 60
    val secs = totalSec % 60
    return if (minutes > 0) {
        "${minutes}m ${secs}s"
    } else {
        "${secs}s"
    }
}

/**
 * BackHandler pro disable system back button
 */
@Composable
private fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}
