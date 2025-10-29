package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.data.fewshot.FewShotPrototypeEntity

/**
 * Komponenta pro výběr Few-Shot prototype pro vyhledávání
 *
 * Zobrazuje horizontální scroll list prototypů s možností výběru.
 * Vybraný prototype se kombinuje s text/image query.
 *
 * @param prototypes List dostupných few-shot prototypes
 * @param selectedPrototype Aktuálně vybraný prototype
 * @param onPrototypeSelected Callback při výběru prototypu (nebo null pro deselect)
 * @param onSearchTriggered Callback pro spuštění vyhledávání s vybraným prototypem
 */
@Composable
fun FewShotSelector(
    prototypes: List<FewShotPrototypeEntity>,
    selectedPrototype: FewShotPrototypeEntity?,
    onPrototypeSelected: (FewShotPrototypeEntity?) -> Unit,
    onSearchTriggered: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (prototypes.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Few-Shot Filter",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            if (selectedPrototype != null) {
                TextButton(
                    onClick = { onPrototypeSelected(null) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Prototype chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(prototypes) { prototype ->
                FewShotPrototypeChip(
                    prototype = prototype,
                    isSelected = prototype.id == selectedPrototype?.id,
                    onClick = {
                        // Logika výběru:
                        // 1. Pokud kliknu na již vybraný tag → deselect
                        // 2. Pokud kliknu na nový tag → select + trigger search
                        if (prototype.id == selectedPrototype?.id) {
                            onPrototypeSelected(null)
                        } else {
                            onPrototypeSelected(prototype)
                            onSearchTriggered() // Automaticky spustit vyhledávání
                        }
                    }
                )
            }
        }
    }
}

/**
 * Chip pro jednotlivý few-shot prototype
 *
 * Používá Material3 FilterChip design stejně jako TagFilterChip pro konzistenci UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FewShotPrototypeChip(
    prototype: FewShotPrototypeEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color indicator (stejná velikost 8.dp jako u TagFilterChip)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(prototype.color))
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )

                // Name
                Text(prototype.name)

                // Sample count badge
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = prototype.sampleCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(prototype.color).copy(alpha = 0.8f),
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

/**
 * Indikátor aktivního few-shot vyhledávání
 * Zobrazuje se v search baru když je vybraný prototype
 */
@Composable
fun FewShotActiveIndicator(
    prototype: FewShotPrototypeEntity,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(prototype.color))
        )

        Text(
            text = "Few-Shot: ${prototype.name}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
