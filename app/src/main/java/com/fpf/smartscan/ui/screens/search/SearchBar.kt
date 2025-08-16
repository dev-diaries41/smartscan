package com.fpf.smartscan.ui.screens.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
        trailingIcon = {
            IconButton(
                enabled = enabled && query.isNotEmpty(),
                onClick = {onSearch(nSimilarResult, threshold)}
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFFFC5C7D)
                )
            }
        }
    )
}