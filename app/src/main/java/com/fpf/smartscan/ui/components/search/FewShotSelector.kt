package com.fpf.smartscan.ui.components.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
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
 * Rozbalovací seznam prototypů (podobně jako TagFilterChips).
 * Vybraný prototype se kombinuje s text/image query.
 *
 * @param prototypes List dostupných few-shot prototypes
 * @param selectedPrototype Aktuálně vybraný prototype
 * @param onPrototypeSelected Callback při výběru prototypu (nebo null pro deselect)
 * @param onSearchTriggered Callback pro spuštění vyhledávání s vybraným prototypem
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FewShotSelector(
    prototypes: List<FewShotPrototypeEntity>,
    selectedPrototype: FewShotPrototypeEntity?,
    onPrototypeSelected: (FewShotPrototypeEntity?) -> Unit,
    onSearchTriggered: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (prototypes.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Klikací header pro rozbalení/sbalení
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Few-Shot:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (selectedPrototype != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "1",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "Sbalit" else "Rozbalit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Rozbalovací seznam prototypů s animací
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                prototypes.forEach { prototype ->
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
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        modifier = modifier,
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
