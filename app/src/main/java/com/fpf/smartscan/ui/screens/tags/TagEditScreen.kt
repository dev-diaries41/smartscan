package com.fpf.smartscan.ui.screens.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.data.tags.UserTagEntity
import kotlinx.coroutines.launch

/**
 * Screen pro vytvoření nebo editaci tagu
 *
 * @param tagName Název tagu pro editaci (null = nový tag)
 * @param onNavigateBack Callback pro návrat zpět
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditScreen(
    tagName: String? = null,
    tagViewModel: TagViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEditMode = tagName != null

    // State pro formulář
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var threshold by remember { mutableFloatStateOf(0.30f) }
    var selectedColor by remember { mutableIntStateOf(0xFF2196F3.toInt()) }
    var isActive by remember { mutableStateOf(true) }

    // Loading a error state
    val isLoading by tagViewModel.isLoading.collectAsState()
    val error by tagViewModel.error.collectAsState()

    // Validace
    val isNameValid = name.isNotBlank()
    val isDescriptionValid = description.length >= 10
    val isFormValid = isNameValid && isDescriptionValid

    // Načtení existujícího tagu při editaci
    LaunchedEffect(tagName) {
        if (tagName != null) {
            val tagWithCount = tagViewModel.getTagWithImageCount(tagName)
            tagWithCount?.let { (tag, _) ->
                name = tag.name
                description = tag.description
                threshold = tag.threshold
                selectedColor = tag.color
                isActive = tag.isActive
            }
        }
    }

    // Přednastavené barvy
    val availableColors = listOf(
        0xFF2196F3.toInt(), // Modrá
        0xFFF44336.toInt(), // Červená
        0xFF4CAF50.toInt(), // Zelená
        0xFFFFEB3B.toInt(), // Žlutá
        0xFF9C27B0.toInt(), // Fialová
        0xFFFF9800.toInt(), // Oranžová
        0xFF607D8B.toInt(), // Šedá
        0xFF795548.toInt()  // Hnědá
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Upravit tag" else "Přidat tag") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Název tagu
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Název tagu *") },
                    placeholder = { Text("např. Rekonstrukce domu") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isNotBlank() && !isNameValid,
                    supportingText = {
                        if (name.isNotBlank() && !isNameValid) {
                            Text("Název nesmí být prázdný")
                        }
                    },
                    enabled = !isLoading
                )

                // Popis pro AI
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Popis pro AI *") },
                    placeholder = { Text("např. fotografie renovace a rekonstrukce interiéru domu...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    minLines = 5,
                    maxLines = 8,
                    isError = description.isNotBlank() && !isDescriptionValid,
                    supportingText = {
                        if (description.isNotBlank() && !isDescriptionValid) {
                            Text("Popis musí mít minimálně 10 znaků")
                        } else {
                            Text("${description.length} znaků")
                        }
                    },
                    enabled = !isLoading
                )

                // Tip card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Column {
                            Text(
                                text = "Tip:",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Popište, co je vidět na fotkách s tímto tagem. Buďte konkrétní a použijte vizuální detaily.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Threshold slider
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Práh citlivosti: ${String.format("%.2f", threshold)}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = threshold,
                        onValueChange = { threshold = it },
                        valueRange = 0.0f..1.0f,
                        steps = 19, // 0.05 kroky
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Nižší (více výsledků)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Vyšší (přesnější)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Color picker
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Barva:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        availableColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .border(
                                        width = if (color == selectedColor) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable(enabled = !isLoading) {
                                        selectedColor = color
                                    }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Active switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading) { isActive = !isActive }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Aktivní",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Automaticky přiřazovat tento tag při indexování",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        enabled = !isLoading
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error message
                error?.let { errorMessage ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Zrušit")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val result = if (isEditMode && tagName != null) {
                                    tagViewModel.updateTagWithEmbedding(
                                        originalName = tagName,
                                        name = name,
                                        description = description,
                                        threshold = threshold,
                                        color = selectedColor,
                                        isActive = isActive
                                    )
                                } else {
                                    tagViewModel.createTag(
                                        name = name,
                                        description = description,
                                        threshold = threshold,
                                        color = selectedColor,
                                        isActive = isActive
                                    )
                                }

                                if (result.isSuccess) {
                                    onNavigateBack()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isFormValid && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (isEditMode) "Uložit" else "Vytvořit tag")
                        }
                    }
                }

                // Spacer pro scroll
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Cleanup error při unmount
    DisposableEffect(Unit) {
        onDispose {
            tagViewModel.clearError()
        }
    }
}
