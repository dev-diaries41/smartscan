package com.fpf.smartscan.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ListCreator(
    categories: List<String>,
    onCategoriesChanged: (List<String>) -> Unit,
    description: String? = null
) {
    var newCategory by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.8f).padding(bottom = 16.dp),
            )
        }

        OutlinedTextField(
            value = newCategory,
            onValueChange = { newCategory = it },
            label = { Text("New Category") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (newCategory.isNotBlank() && !categories.contains(newCategory)) {
                            onCategoriesChanged(categories + newCategory)
                            newCategory = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add category", tint = Color(0xFF4CAF50))
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (categories.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors= CardDefaults.cardColors(Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Label, contentDescription = "Category", tint = Color(0xFF3F51B5))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = category, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onCategoriesChanged(categories - category) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove category", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}


