package com.fpf.smartscan.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun SearchBar(
    query: String,
    enabled: Boolean,
    onQueryChange: (query: String) -> Unit,
    threshold: Float,
    nSimilarResult: Int,
    onSearch: (n: Int, threshold: Float) -> Unit,
    onClearQuery: () -> Unit,
    label: String,
    ){
    OutlinedTextField(
        enabled = enabled,
        value = query,
        onValueChange = onQueryChange,
        label = { Text(label)},
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        keyboardActions = KeyboardActions (
            onSearch = {onSearch(nSimilarResult, threshold)}
        ),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Search
        ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        },
        trailingIcon = if (query.isNotBlank()) { {
            IconButton(
                enabled = enabled && query.isNotEmpty(),
                onClick = {onClearQuery()},
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear query",
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.onBackground, shape = CircleShape)
                        .padding(2.dp)
                )
            }
        } }else null,
    )
}