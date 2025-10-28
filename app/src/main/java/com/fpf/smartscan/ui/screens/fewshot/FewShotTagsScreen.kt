package com.fpf.smartscan.ui.screens.fewshot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fpf.smartscan.data.fewshot.FewShotPrototypeEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen pro správu Few-Shot Prototypes
 *
 * Zobrazuje seznam všech few-shot tagů (prototypes) s možností:
 * - Vytvoření nového tagu (FAB)
 * - Zobrazení detailu tagu
 * - Smazání tagu
 * - Vyhledávání v tazích
 *
 * Fáze 2 - Základní verze (zobrazení + delete)
 * TODO Fáze 3: Přidat create dialog
 * TODO Fáze 3: Přidat detail view
 * TODO Fáze 3: Přidat edit funkcionalitu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FewShotTagsScreen(
    viewModel: FewShotViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit = {}
) {
    val allPrototypes by viewModel.allPrototypes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // State pro delete dialog
    var prototypeToDelete by remember { mutableStateOf<FewShotPrototypeEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // State pro create dialog flow
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf(false) }

    // Temporary data pro vytváření prototypu
    var pendingPrototypeName by remember { mutableStateOf("") }
    var pendingPrototypeColor by remember { mutableStateOf(0) }
    var pendingPrototypeDescription by remember { mutableStateOf<String?>(null) }
    var pendingPrototypeCategory by remember { mutableStateOf<String?>(null) }
    var selectedImages by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var croppedSamples by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var currentCropIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Few-Shot Tags")
                        if (allPrototypes.isNotEmpty()) {
                            Text(
                                text = "${allPrototypes.size} ${
                                    when {
                                        allPrototypes.size == 1 -> "prototype"
                                        allPrototypes.size < 5 -> "prototypes"
                                        else -> "prototypes"
                                    }
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Přidat few-shot tag")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Chyba: $error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                }
                allPrototypes.isEmpty() -> {
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allPrototypes) { prototype ->
                            FewShotPrototypeCard(
                                prototype = prototype,
                                onViewClick = {
                                    onNavigateToDetail(prototype.id)
                                },
                                onDeleteClick = {
                                    prototypeToDelete = prototype
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && prototypeToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                prototypeToDelete = null
            },
            title = { Text("Smazat prototype?") },
            text = {
                Text("Opravdu chcete smazat prototype '${prototypeToDelete?.name}'? Tato akce je nevratná.")
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        prototypeToDelete = null
                    }
                ) {
                    Text("Zrušit")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prototypeToDelete?.let { prototype ->
                            viewModel.deletePrototype(prototype.id)
                        }
                        showDeleteDialog = false
                        prototypeToDelete = null
                    }
                ) {
                    Text("Smazat", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Create dialog flow
    if (showCreateDialog) {
        CreateFewShotTagDialog(
            onDismiss = {
                showCreateDialog = false
                // Reset pending data
                pendingPrototypeName = ""
                pendingPrototypeColor = 0
                pendingPrototypeDescription = null
                pendingPrototypeCategory = null
            },
            onConfirm = { name, color, description, category ->
                // Uložit metadata a otevřít image picker
                pendingPrototypeName = name
                pendingPrototypeColor = color
                pendingPrototypeDescription = description
                pendingPrototypeCategory = category
                showCreateDialog = false
                showImagePicker = true
            }
        )
    }

    if (showImagePicker) {
        ImagePickerDialog(
            onDismiss = {
                showImagePicker = false
                // Reset all pending data
                pendingPrototypeName = ""
                pendingPrototypeColor = 0
                pendingPrototypeDescription = null
                pendingPrototypeCategory = null
                selectedImages = emptyList()
            },
            onImagesSelected = { images ->
                selectedImages = images
                croppedSamples = emptyList()
                currentCropIndex = 0
                showImagePicker = false
                showCropDialog = true
            }
        )
    }

    if (showCropDialog && selectedImages.isNotEmpty()) {
        ImageCropDialog(
            imageUri = selectedImages[currentCropIndex],
            currentIndex = currentCropIndex,
            totalImages = selectedImages.size,
            onDismiss = {
                showCropDialog = false
                // Reset all
                selectedImages = emptyList()
                croppedSamples = emptyList()
                currentCropIndex = 0
                pendingPrototypeName = ""
                pendingPrototypeColor = 0
                pendingPrototypeDescription = null
                pendingPrototypeCategory = null
            },
            onCropConfirmed = { cropRect ->
                // Přidat crop do seznamu
                val newSample = Pair(
                    selectedImages[currentCropIndex].toString(),
                    cropRect.toJson()
                )
                croppedSamples = croppedSamples + newSample

                // Pokud jsou všechny obrázky oříznuté, vytvořit prototype
                if (currentCropIndex == selectedImages.size - 1) {
                    // Všechny obrázky jsou oříznuté - vytvoř prototype
                    viewModel.createPrototype(
                        name = pendingPrototypeName,
                        color = pendingPrototypeColor,
                        samples = croppedSamples,
                        description = pendingPrototypeDescription,
                        category = pendingPrototypeCategory
                    )

                    // Reset state
                    showCropDialog = false
                    selectedImages = emptyList()
                    croppedSamples = emptyList()
                    currentCropIndex = 0
                    pendingPrototypeName = ""
                    pendingPrototypeColor = 0
                    pendingPrototypeDescription = null
                    pendingPrototypeCategory = null
                } else {
                    // Pokračovat na další obrázek
                    currentCropIndex++
                }
            },
            onSkip = {
                // Skip tento obrázek a pokračovat na další
                if (currentCropIndex == selectedImages.size - 1) {
                    // Poslední obrázek - vytvoř prototype s tím co máme
                    if (croppedSamples.isNotEmpty()) {
                        viewModel.createPrototype(
                            name = pendingPrototypeName,
                            color = pendingPrototypeColor,
                            samples = croppedSamples,
                            description = pendingPrototypeDescription,
                            category = pendingPrototypeCategory
                        )
                    }

                    // Reset state
                    showCropDialog = false
                    selectedImages = emptyList()
                    croppedSamples = emptyList()
                    currentCropIndex = 0
                    pendingPrototypeName = ""
                    pendingPrototypeColor = 0
                    pendingPrototypeDescription = null
                    pendingPrototypeCategory = null
                } else {
                    // Pokračovat na další obrázek
                    currentCropIndex++
                }
            }
        )
    }
}

/**
 * Card pro zobrazení jednotlivého few-shot prototype
 */
@Composable
private fun FewShotPrototypeCard(
    prototype: FewShotPrototypeEntity,
    onViewClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("d.M.yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Color indicator + Name + Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(prototype.color))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = prototype.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sample count
                        Text(
                            text = "${prototype.sampleCount} samples",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Category (if present)
                        prototype.category?.let { category ->
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = category.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "Vytvořeno ${dateFormat.format(Date(prototype.createdAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Right side: Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // View button
                IconButton(onClick = onViewClick) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = "Zobrazit detail",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete button
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Smazat",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Empty state zobrazený když nejsou žádné prototypes
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📸",
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = "Žádné Few-Shot tagy",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Vytvořte si vlastní tagy pro personalizované vyhledávání.\nStačí vybrat 5-10 fotek osoby nebo objektu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = "Klikněte na + tlačítko pro začátek",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}
