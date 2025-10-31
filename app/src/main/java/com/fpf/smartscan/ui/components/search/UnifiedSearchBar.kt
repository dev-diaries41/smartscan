package com.fpf.smartscan.ui.components.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R
import com.fpf.smartscan.data.MediaType
import com.fpf.smartscan.data.QueryType

/**
 * Unified Search Bar podle UI mockupu
 *
 * Kombinuje:
 * - Query Type Toggle (leading icon - TEXT/IMAGE)
 * - Search Input
 * - Clear Query button (X - podmíněně viditelný)
 * - Clear Results button (⊗ - podmíněně viditelný)
 * - Overflow Menu (⋮ - optional trailing content)
 *
 * Vše v jednom kompaktním baru s Material 3 designem.
 * Clear Results button se zobrazí pouze když existují výsledky.
 * Overflow menu se zobrazí pokud je poskytnut overflowMenuContent.
 */
@Composable
fun UnifiedSearchBar(
    // Query state
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,

    // Query type
    queryType: QueryType,
    onQueryTypeToggle: () -> Unit,

    // Media type (pro placeholder)
    mediaType: MediaType,

    // Search action
    onSearch: () -> Unit,
    enabled: Boolean,

    // Optional: translated query display
    translatedQuery: String? = null,

    // Optional: Clear results action
    hasResults: Boolean = false,
    onClearResults: (() -> Unit)? = null,

    // Optional: Overflow menu (pro trailing actions)
    overflowMenuContent: (@Composable () -> Unit)? = null,

    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Query Type Toggle Icon (leading)
            IconButton(
                onClick = onQueryTypeToggle,
                enabled = enabled,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = when (queryType) {
                        QueryType.TEXT -> Icons.Filled.TextFields
                        QueryType.IMAGE -> Icons.Filled.Image
                    },
                    contentDescription = when (queryType) {
                        QueryType.TEXT -> stringResource(R.string.switch_to_image_search)
                        QueryType.IMAGE -> stringResource(R.string.switch_to_text_search)
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Vertical divider
            HorizontalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )

            // Search icon
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )

            // Input field
            TextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled && queryType == QueryType.TEXT,
                placeholder = {
                    Text(
                        text = when {
                            queryType == QueryType.IMAGE -> stringResource(R.string.select_image_to_search)
                            mediaType == MediaType.IMAGE -> stringResource(R.string.search_images_placeholder)
                            else -> stringResource(R.string.search_videos_placeholder)
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (query.isNotBlank() && queryType == QueryType.TEXT) {
                            onSearch()
                        }
                    }
                )
            )

            // Clear query button (conditionally visible)
            if (query.isNotBlank() && queryType == QueryType.TEXT) {
                IconButton(
                    onClick = onClearQuery,
                    enabled = enabled,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.clear_query),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Clear results button (conditionally visible)
            if (hasResults && onClearResults != null) {
                IconButton(
                    onClick = onClearResults,
                    enabled = enabled,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.menu_clear_results),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Overflow menu (conditionally visible)
            overflowMenuContent?.invoke()
        }
    }

    // Translated query hint (pod search barem, pokud existuje)
    if (translatedQuery != null && queryType == QueryType.TEXT) {
        Text(
            text = "→ $translatedQuery",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
}
