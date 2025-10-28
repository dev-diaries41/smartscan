package com.fpf.smartscan.ui.screens.fewshot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Dialog pro vytvoření nového Few-Shot tagu
 *
 * Umožňuje uživateli:
 * - Zadat název tagu
 * - Vybrat barvu
 * - Volitelně přidat popis
 * - Volitelně přidat kategorii
 *
 * Po potvrzení otevře ImagePickerDialog pro výběr samplu
 */
@Composable
fun CreateFewShotTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Int, description: String?, category: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(predefinedColors[0]) }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "Vytvořit Few-Shot Tag",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Vytvořte si vlastní tag pro personalizované vyhledávání. V dalším kroku vyberete 3-10 ukázkových obrázků.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text("Název tagu") },
                    placeholder = { Text("např. Barunka, Auto, Kočka") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true
                )

                // Color picker
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Barva",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(predefinedColors) { color ->
                            ColorPickerItem(
                                color = color,
                                isSelected = color == selectedColor,
                                onClick = { selectedColor = color }
                            )
                        }
                    }
                }

                // Optional: Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Popis (volitelné)") },
                    placeholder = { Text("např. Rodinné fotky Barunky") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // Optional: Category
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Kategorie (volitelné)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            CategoryChip(
                                category = category,
                                isSelected = category == selectedCategory,
                                onClick = {
                                    selectedCategory = if (category == selectedCategory) null else category
                                }
                            )
                        }
                    }
                }

                // Info box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Pro nejlepší výsledky vyberte 5-10 obrázků s podobnými vlastnostmi (např. stejná osoba, stejný objekt).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Zrušit")
                    }
                    Button(
                        onClick = {
                            when {
                                name.isBlank() -> {
                                    showError = true
                                    errorMessage = "Název nesmí být prázdný"
                                }
                                name.length < 2 -> {
                                    showError = true
                                    errorMessage = "Název musí mít alespoň 2 znaky"
                                }
                                else -> {
                                    onConfirm(
                                        name.trim(),
                                        selectedColor.toArgb(),
                                        description.takeIf { it.isNotBlank() },
                                        selectedCategory
                                    )
                                }
                            }
                        }
                    ) {
                        Text("Pokračovat →")
                    }
                }
            }
        }
    }
}

/**
 * Komponenta pro výběr barvy
 */
@Composable
private fun ColorPickerItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Text(
                text = "✓",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Komponenta pro výběr kategorie
 */
@Composable
private fun CategoryChip(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(category.replaceFirstChar { it.uppercase() }) },
        leadingIcon = if (isSelected) {
            { Text("✓") }
        } else null
    )
}

/**
 * Předdefinované barvy pro tagy
 */
private val predefinedColors = listOf(
    Color(0xFFE57373), // Red
    Color(0xFFBA68C8), // Purple
    Color(0xFF64B5F6), // Blue
    Color(0xFF4DD0E1), // Cyan
    Color(0xFF81C784), // Green
    Color(0xFFFFD54F), // Yellow
    Color(0xFFFF8A65), // Orange
    Color(0xFFA1887F), // Brown
    Color(0xFF90A4AE), // Blue Grey
    Color(0xFFF06292), // Pink
)

/**
 * Předdefinované kategorie
 */
private val categories = listOf(
    "person",
    "object",
    "scene",
    "style"
)

/**
 * Extension function pro konverzi Color na Int (ARGB)
 */
private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
