# TODO - Tagging Systém pro SmartScan

## 📋 Přehled projektu

Implementace user-defined tagging systému pro automatickou kategorizaci obrázků pomocí CLIP ML modelu.

**Princip fungování:**
1. Uživatel nadefinuje tagy s názvem + popisem (např. "Rekonstrukce domu" + "fotografie renovace...")
2. CLIP text encoder převede popis na 512-dimenzionální embedding vektor
3. Při indexování obrázků porovná CLIP image embedding s tag embeddingy (cosine similarity)
4. Automaticky přiřadí tagy, které překročí threshold (např. 0.30)

---

## ✅ HOTOVO (4 commity)

### 1. Multi-select + Batch operace (commit: 0aad9c7)
- ✅ Selection mode s long-press
- ✅ Multi-select ve výsledcích vyhledávání
- ✅ Batch přesun souborů (directory picker)
- ✅ Batch smazání souborů (s confirmation)
- ✅ SelectionActionBar komponenta
- ✅ Visual feedback (checkbox, borders, alpha)

**Soubory:**
- `SearchViewModel.kt`: selection state + batch operace
- `SearchResults.kt`: multi-select support
- `SelectionActionBar.kt`: action bar komponenta
- `FileUtils.kt`: `deleteFiles()`, `moveFiles()`

### 2. Swipe navigace (commit: 511e564)
- ✅ HorizontalPager pro swipe mezi výsledky
- ✅ Pozice indikátor "X / Y"
- ✅ SwipeableMediaViewer komponenta
- ✅ Zachování všech akcí (share, copy, open in gallery)

**Soubory:**
- `SwipeableMediaViewer.kt`: nová komponenta
- `SearchViewModel.kt`: viewerIndex state
- `SearchResults.kt`: předává index místo URI

### 3. Database struktura pro tagy (commit: 364afd3)
- ✅ `UserTagEntity`: User-defined tagy
  - `name`: String - zobrazované jméno ("Rekonstrukce domu")
  - `description`: String - popis pro CLIP ("fotografie renovace domu...")
  - `embedding`: FloatArray - 512-d vektor z CLIP text encoderu
  - `threshold`: Float - práh pro auto-assignment (0.0-1.0, default 0.30)
  - `color`: Int - barva pro UI (Material Blue default)
  - `isActive`: Boolean - zapnuto/vypnuto pro auto-tagging

- ✅ `ImageTagEntity`: Přiřazené tagy k obrázkům
  - `imageId`: Long - MediaStore ID
  - `tagName`: String - reference na UserTagEntity
  - `confidence`: Float - cosine similarity (0.0-1.0)
  - `isUserAssigned`: Boolean - auto vs manual

- ✅ `TagDatabase`: Room database s verzí 1
- ✅ `UserTagDao`: CRUD operace pro user tagy
  - `getAllTags()`, `getActiveTags()`, `getTagByName()`
  - `insertTag()`, `updateTag()`, `deleteTag()`

- ✅ `ImageTagDao`: Operace pro image tagy
  - `getTagsForImage()`, `getImageIdsForTag()`
  - `getImageIdsForTags()` - multi-tag filtering
  - `getTagCounts()`, `getImageCountForTag()`
  - `insertTags()`, `deleteTags()`

- ✅ `TagRepository`: Abstrakce nad DAO
- ✅ `TagConverters`: FloatArray ↔ ByteArray conversion

**Soubory:**
- `data/tags/UserTagEntity.kt`
- `data/tags/ImageTagEntity.kt`
- `data/tags/UserTagDao.kt`
- `data/tags/ImageTagDao.kt`
- `data/tags/TagDatabase.kt`
- `data/tags/TagRepository.kt`
- `data/tags/Converters.kt`

---

## 🚧 CO SE MÁ UDĚLAT

### **PHASE 2: Tag Manager UI** (Settings screen) ⭐ PRIORITA 1

**Cíl:** UI pro vytváření, editaci a mazání tagů

**Komponenty k vytvoření:**

#### 2.1 TagManagerScreen.kt
**Cesta:** `ui/screens/tags/TagManagerScreen.kt`

**UI struktura:**
```kotlin
@Composable
fun TagManagerScreen(
    tagViewModel: TagViewModel = viewModel(),
    onNavigateBack: () -> Unit
)
```

**Layout:**
```
┌─────────────────────────────────┐
│ ← Tag Manager                   │
├─────────────────────────────────┤
│ My Tags (5)            [+ Add]  │
│                                  │
│ ┌─────────────────────────────┐ │
│ │ 🏠 Rekonstrukce domu  [Edit]│ │
│ │    124 obrázků              │ │
│ │    Threshold: 0.35          │ │
│ │    [Delete]                 │ │
│ └─────────────────────────────┘ │
│                                  │
│ ┌─────────────────────────────┐ │
│ │ 👶 Děti               [Edit]│ │
│ │    89 obrázků               │ │
│ │    Threshold: 0.40          │ │
│ │    [Delete]                 │ │
│ └─────────────────────────────┘ │
│                                  │
│ (scrollable list...)             │
└─────────────────────────────────┘
```

**Funkce:**
- LazyColumn s UserTagEntity items
- FAB tlačítko pro Add
- Edit button → navigace na TagEditScreen
- Delete button → confirmation dialog
- Zobrazení počtu obrázků (z ImageTagDao)
- Swipe to delete (optional)

#### 2.2 TagEditScreen.kt
**Cesta:** `ui/screens/tags/TagEditScreen.kt`

**UI struktura:**
```
┌─────────────────────────────────┐
│ ← Add Tag / Edit Tag            │
├─────────────────────────────────┤
│ Tag Name: *                     │
│ ┌─────────────────────────────┐ │
│ │ Rekonstrukce domu           │ │
│ └─────────────────────────────┘ │
│                                  │
│ Description (for AI): *          │
│ ┌─────────────────────────────┐ │
│ │ fotografie renovace a       │ │
│ │ rekonstrukce interiéru domu │ │
│ │ s viditelnými stavebními    │ │
│ │ pracemi, nářadím a materiály│ │
│ └─────────────────────────────┘ │
│                                  │
│ 💡 Tip: Popište, co je vidět    │
│    na fotkách s tímto tagem     │
│                                  │
│ Threshold: 0.35                  │
│ ├─────────●────────────┤         │
│ 0.0  (Lower)  0.5  (Higher) 1.0 │
│                                  │
│ Color:                           │
│ 🔵 🔴 🟢 🟡 🟣 🟠 ⚫         │
│                                  │
│ Active: [✓]                      │
│                                  │
│ [ Cancel ]          [ Save Tag ] │
└─────────────────────────────────┘
```

**Funkce:**
- TextField pro name (required)
- TextField multiline pro description (required)
- Slider pro threshold (0.0-1.0, default 0.30)
- Color picker (Material colors)
- Switch pro isActive
- Validace: name nesmí být prázdné, description minimálně 10 znaků
- Save: vytvoří CLIP embedding z description
- Info tooltip o tom, jak psát dobré popisy

#### 2.3 TagViewModel.kt
**Cesta:** `ui/screens/tags/TagViewModel.kt`

**State management:**
```kotlin
class TagViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TagRepository
    private val textEmbedder: ClipTextEmbedder

    val allTags: StateFlow<List<UserTagEntity>>
    val isLoading: StateFlow<Boolean>
    val error: StateFlow<String?>

    // CRUD operace
    suspend fun createTag(name: String, description: String, threshold: Float, color: Int)
    suspend fun updateTag(tag: UserTagEntity)
    suspend fun deleteTag(tag: UserTagEntity)
    suspend fun getTagWithImageCount(tagName: String): Pair<UserTagEntity, Int>

    // Pomocné
    private fun generateEmbedding(description: String): FloatArray
}
```

**Klíčové:**
- Inicializuje `ClipTextEmbedder` s `R.raw.text_encoder_quant_int8`
- `createTag()`:
  1. Validuje input
  2. Volá `textEmbedder.embed(description)` → FloatArray
  3. Vytvoří UserTagEntity
  4. Uloží do DB přes repository

#### 2.4 Přednastavené tagy (optional)
**Soubor:** `data/tags/PresetTags.kt`

```kotlin
object PresetTags {
    val RECOMMENDED = listOf(
        TagPreset(
            name = "Rekonstrukce domu",
            description = "fotografie stavebních prací, renovace a rekonstrukce interiéru nebo exteriéru domu, viditelné nástroje, materiály, rozbitá místa"
        ),
        TagPreset(
            name = "Děti",
            description = "fotografie dětí, batolat, školáků při hře, učení nebo jiných aktivitách, dětské portréty a rodinné fotky s dětmi"
        ),
        TagPreset(
            name = "Screenshoty",
            description = "screenshot mobilní aplikace, snímek obrazovky telefonu nebo počítače s viditelným uživatelským rozhraním, tlačítky, menu, textem a notifikacemi"
        ),
        TagPreset(
            name = "Selfie",
            description = "selfie fotografie pořízená z ruky s natočenou kamerou k sobě, autoportrét, zrcadlové selfie, typický selfie úhel"
        ),
        TagPreset(
            name = "Explicitní obsah",
            description = "fotografie nahého těla, intimních částí, erotického nebo pornografického obsahu, sexuálně explicitní materiál"
        ),
        TagPreset(
            name = "Dokumenty",
            description = "naskenovaný dokument, text na papíře, účtenka, faktura, smlouva, formulář, oficiální dokument"
        ),
        TagPreset(
            name = "Jídlo",
            description = "fotografie jídla na talíři, v restauraci nebo při vaření, snídaně, oběd, večeře, dezert, nápoje"
        ),
        TagPreset(
            name = "Příroda",
            description = "krajina s přírodou, stromy, lesy, hory, řeky, jezera, západ slunce, obloha, příroda bez lidí"
        )
    )
}

data class TagPreset(
    val name: String,
    val description: String,
    val threshold: Float = 0.30f,
    val color: Int = 0xFF2196F3.toInt()
)
```

**UI pro import:**
- Dialog s přednastavenými tagy
- Checkbox selection
- Import všech najednou

---

### **PHASE 3: Auto-tagging Engine** ⭐ PRIORITA 2

**Cíl:** Automaticky přiřadit tagy při indexování obrázků

#### 3.1 TaggingService.kt
**Cesta:** `services/TaggingService.kt`

```kotlin
class TaggingService(private val context: Context) {
    private val tagRepository: TagRepository
    private val textEmbedder: ClipTextEmbedder  // Pro lazy load tag embeddingů

    /**
     * Přiřadí tagy jednomu obrázku
     * @param imageId MediaStore ID
     * @param imageEmbedding CLIP embedding obrázku (už máme z indexování)
     * @return List přiřazených tagů
     */
    suspend fun assignTags(
        imageId: Long,
        imageEmbedding: FloatArray
    ): List<ImageTagEntity> {
        val activeTags = tagRepository.getActiveTagsSync()
        val assignedTags = mutableListOf<ImageTagEntity>()

        activeTags.forEach { userTag ->
            val similarity = cosineSimilarity(imageEmbedding, userTag.embedding)

            if (similarity >= userTag.threshold) {
                val imageTag = ImageTagEntity(
                    imageId = imageId,
                    tagName = userTag.name,
                    confidence = similarity,
                    isUserAssigned = false
                )
                assignedTags.add(imageTag)
            }
        }

        if (assignedTags.isNotEmpty()) {
            tagRepository.insertImageTags(assignedTags)
        }

        return assignedTags
    }

    /**
     * Batch přiřazení tagů (pro re-tagging celé knihovny)
     */
    suspend fun assignTagsBatch(
        images: List<Pair<Long, FloatArray>>, // (imageId, embedding)
        onProgress: (Int, Int) -> Unit
    ) {
        // Implementace batch processingu
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Vectors must have same length" }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        return dotProduct / (sqrt(normA) * sqrt(normB))
    }
}
```

#### 3.2 Integrace do MediaIndexForegroundService
**Soubor:** `services/MediaIndexForegroundService.kt`

**Kde přidat:**
Po vytvoření image embeddingu (cca řádek 80-100), zavolat:

```kotlin
// V MediaIndexForegroundService po embed operaci:
val imageEmbedding = imageEmbedder.embed(bitmap)

// NOVÉ: Přidat tagging
val taggingService = TaggingService(applicationContext)
taggingService.assignTags(imageId, imageEmbedding.embeddings)

// Pokračovat s ukládáním do index
imageStore.add(listOf(imageEmbedding))
```

**Alternativa:** Samostatný worker pro tagging (pokud nechceš zpomalit indexing)

#### 3.3 Re-tagging Worker (optional)
**Soubor:** `workers/RetaggingWorker.kt`

Pro re-tagging již indexovaných obrázků:
```kotlin
class RetaggingWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 1. Načíst všechny embeddingy z FileEmbeddingStore
        // 2. Pro každý embedding zavolat TaggingService.assignTags()
        // 3. Progress notification
    }
}
```

---

### **PHASE 4: Search Filters** ⭐ PRIORITA 3

**Cíl:** Filtrovat výsledky vyhledávání podle tagů

#### 4.1 Tag Filter UI v SearchScreen
**Soubor:** `ui/screens/search/Search.kt`

**Přidat nad SearchResults:**
```kotlin
// Po SearchBar, před SearchResults
TagFilterChips(
    availableTags = availableTags,  // List<Pair<UserTagEntity, Int>> (tag, count)
    selectedTags = selectedTags,     // Set<String> (tag names)
    onTagToggle = { tagName -> searchViewModel.toggleTagFilter(tagName) }
)
```

**UI Layout:**
```
┌───────────────────────────────────┐
│ Search: "fotka"         [Search]  │
├───────────────────────────────────┤
│ Filters:                          │
│ [✓ Děti (89)]  [✗ Selfie (45)]   │
│ [✗ Rekonstrukce (124)]            │
└───────────────────────────────────┘
│ Results (34)  ← jen obrázky s     │
│               vybranými tagy      │
```

#### 4.2 Komponenta TagFilterChips
**Soubor:** `ui/components/search/TagFilterChips.kt`

```kotlin
@Composable
fun TagFilterChips(
    availableTags: List<Pair<UserTagEntity, Int>>,
    selectedTags: Set<String>,
    onTagToggle: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(availableTags) { (tag, count) ->
            FilterChip(
                selected = selectedTags.contains(tag.name),
                onClick = { onTagToggle(tag.name) },
                label = { Text("${tag.name} ($count)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(tag.color)
                )
            )
        }
    }
}
```

#### 4.3 SearchViewModel update
**Soubor:** `ui/screens/search/SearchViewModel.kt`

**Přidat state:**
```kotlin
private val _selectedTagFilters = MutableStateFlow<Set<String>>(emptySet())
val selectedTagFilters: StateFlow<Set<String>> = _selectedTagFilters

private val _availableTagsWithCounts = MutableStateFlow<List<Pair<UserTagEntity, Int>>>(emptyList())
val availableTagsWithCounts: StateFlow<List<Pair<UserTagEntity, Int>>> = _availableTagsWithCounts

fun toggleTagFilter(tagName: String) {
    val current = _selectedTagFilters.value.toMutableSet()
    if (current.contains(tagName)) {
        current.remove(tagName)
    } else {
        current.add(tagName)
    }
    _selectedTagFilters.value = current

    // Re-filter results
    applyTagFilters()
}

private suspend fun applyTagFilters() {
    if (_selectedTagFilters.value.isEmpty()) {
        // Zobrazit všechny výsledky
        return
    }

    // Získat imageIds které mají VŠECHNY vybrané tagy (AND logic)
    val filteredImageIds = tagRepository.getImageIdsForTags(
        _selectedTagFilters.value.toList()
    )

    // Filtrovat _searchResults podle těchto IDs
    val filtered = _searchResults.value.filter { uri ->
        val imageId = getImageIdFromUri(uri)
        imageId in filteredImageIds
    }

    _searchResults.value = filtered
}

// Helper
private fun getImageIdFromUri(uri: Uri): Long {
    return ContentUris.parseId(uri)
}
```

**Načíst available tags:**
```kotlin
init {
    viewModelScope.launch {
        tagRepository.allTags.collect { tags ->
            val withCounts = tags.map { tag ->
                val count = tagRepository.getImageCountForTag(tag.name)
                tag to count
            }.filter { it.second > 0 }  // Pouze tagy s obrázky

            _availableTagsWithCounts.value = withCounts
        }
    }
}
```

---

### **PHASE 5: Media Viewer Tags** ⭐ PRIORITA 4

**Cíl:** Zobrazit tagy v Media Viewer + manual add/remove

#### 5.1 Update SwipeableMediaViewer
**Soubor:** `ui/components/media/SwipeableMediaViewer.kt`

**Přidat pod action bar:**
```kotlin
// Pod SwipeableActionRow
TagChipsRow(
    imageId = getImageIdFromUri(currentUri),
    onAddTag = { imageId -> /* show tag picker dialog */ },
    onRemoveTag = { imageId, tagName -> /* remove tag */ }
)
```

#### 5.2 Komponenta TagChipsRow
**Soubor:** `ui/components/media/TagChipsRow.kt`

```kotlin
@Composable
fun TagChipsRow(
    imageId: Long,
    tagViewModel: TagViewModel = viewModel(),
    onAddTag: (Long) -> Unit,
    onRemoveTag: (Long, String) -> Unit
) {
    val imageTags by tagViewModel.getTagsForImage(imageId).collectAsState(initial = emptyList())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        imageTags.forEach { imageTag ->
            AssistChip(
                onClick = { onRemoveTag(imageId, imageTag.tagName) },
                label = {
                    Text("${imageTag.tagName} (${(imageTag.confidence * 100).toInt()}%)")
                },
                trailingIcon = {
                    Icon(Icons.Default.Close, contentDescription = "Remove")
                }
            )
        }

        // Add button
        AssistChip(
            onClick = { onAddTag(imageId) },
            label = { Text("+ Přidat tag") }
        )
    }
}
```

#### 5.3 Tag Picker Dialog
**Soubor:** `ui/components/media/TagPickerDialog.kt`

```kotlin
@Composable
fun TagPickerDialog(
    imageId: Long,
    availableTags: List<UserTagEntity>,
    currentTags: List<ImageTagEntity>,
    onDismiss: () -> Unit,
    onTagSelected: (UserTagEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Přidat tag") },
        text = {
            LazyColumn {
                items(availableTags.filter { tag ->
                    currentTags.none { it.tagName == tag.name }
                }) { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTagSelected(tag) }
                            .padding(16.dp)
                    ) {
                        Text(tag.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}
```

---

## 🎯 TESTING CHECKLIST

### Database Tests
- [ ] Vytvoření tagu s embeddingem
- [ ] Update tagu (změna threshold)
- [ ] Smazání tagu (cascade delete image_tags)
- [ ] Query: getImageIdsForTags (multi-tag filter)
- [ ] Query: getTagCounts
- [ ] Foreign key constraints

### Tag Manager UI Tests
- [ ] Vytvoření tagu s name + description
- [ ] Validace: prázdný name
- [ ] Validace: krátký description
- [ ] Edit existujícího tagu
- [ ] Delete tagu s confirmation
- [ ] Zobrazení počtu obrázků

### Auto-tagging Tests
- [ ] Tag assignment při indexování
- [ ] Cosine similarity calculation
- [ ] Threshold filtering (0.30)
- [ ] Multiple tags na jeden obrázek
- [ ] Performance: 1000+ obrázků

### Search Filter Tests
- [ ] Filter po jednom tagu
- [ ] Filter po více tazích (AND logic)
- [ ] Clear filters
- [ ] Update counts při změně tagů

### Media Viewer Tests
- [ ] Zobrazení tagů pro obrázek
- [ ] Manual add tag
- [ ] Manual remove tag
- [ ] Confidence display

---

## 📚 DŮLEŽITÉ POZNÁMKY

### CLIP Embeddings
- **Velikost:** 512 floatů (2048 bytes)
- **Model:** text_encoder_quant_int8.onnx (62 MB)
- **API:** `ClipTextEmbedder.embed(text: String): FloatArray`
- **Import:** `com.fpf.smartscansdk.core.ml.embeddings.clip.ClipTextEmbedder`

### Cosine Similarity
```kotlin
fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    val dotProduct = a.zip(b) { x, y -> x * y }.sum()
    val normA = sqrt(a.map { it * it }.sum())
    val normB = sqrt(b.map { it * it }.sum())
    return dotProduct / (normA * normB)
}
```
- **Range:** -1.0 až 1.0 (v praxi 0.0-1.0 pro CLIP)
- **Threshold doporučení:**
  - 0.25-0.30 = široké matching (více false positives)
  - 0.35-0.40 = balanced (doporučeno)
  - 0.45-0.50 = strict (méně false positives)

### Doporučené popisy tagů
- Používat věty, ne jednotlivá slova
- Přidat vizuální detaily ("s viditelným...")
- Kontext prostředí ("v restauraci", "při hře")
- Můžeš použít synonyma
- Anglické termíny fungují lépe pro některé koncepty
- České popisy fungují dobře pro běžné objekty

### Performance tipy
- Batch insert tagů: `insertTags(List<ImageTagEntity>)` místo loop
- Index na `imageId` a `tagName` už existuje
- Při re-taggingu: použít WorkManager s constraints
- Cache active tags v memory (mění se zřídka)

### Navigation
- Tag Manager: Add do Settings screen jako nový item
- Settings → Tag Manager → Tag Edit Screen
- Media Viewer → Tag Picker Dialog (bottom sheet)

---

## 🚀 NEXT STEPS

1. **Immediate (Phase 2):** Tag Manager UI
   - Start: `TagManagerScreen.kt`
   - Priority: High
   - Time: ~2-3 hodiny

2. **Core (Phase 3):** Auto-tagging Engine
   - Start: `TaggingService.kt`
   - Priority: High
   - Time: ~1-2 hodiny

3. **UX (Phase 4):** Search Filters
   - Start: Tag filter chips
   - Priority: Medium
   - Time: ~1 hodina

4. **Polish (Phase 5):** Media Viewer Tags
   - Start: `TagChipsRow.kt`
   - Priority: Low
   - Time: ~1 hodina

**Celkový odhad:** 5-7 hodin čistého kódování

---

## 📝 COMMITS PŘIPRAVENÉ

```bash
# Po Phase 2
git commit -m "✨ feat: Tag Manager UI pro CRUD operace s tagy"

# Po Phase 3
git commit -m "🤖 feat: Auto-tagging engine s CLIP embeddings"

# Po Phase 4
git commit -m "🔍 feat: Search filters podle tagů"

# Po Phase 5
git commit -m "🏷️ feat: Tag chips v Media Viewer s manual edit"
```

---

**Autor:** Claude Code
**Datum:** 2025-10-27
**Projekt:** SmartScan Android App
**Verze:** 1.1.6+
