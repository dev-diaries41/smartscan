# Few-Shot Learning pro SmartScan

## 📋 Přehled konceptu

Few-shot learning umožňuje uživatelům vytvářet vlastní "koncepty" (např. osoby, objekty) pomocí několika příkladů (5-10 obrázků). Systém vytvoří průměrný embedding z těchto příkladů a uloží jej jako "prototype", který pak lze použít pro vyhledávání.

### Příklad Use Case:
```
1. Uživatel vybere 10 fotek dcery Barunky
2. U každé ořízne obličej pomocí crop nástroje
3. Systém vytvoří embeddingy všech cropů
4. Spočítá průměrný embedding → "Barunka prototype"
5. Uloží jako Few-Shot Tag "Barunka"

Poté může vyhledávat:
- "Barunka" (jen prototype)
- "Barunka v lese" (prototype + text)
- "Barunka a Jonasek" (2 prototypes + text)
```

## 🎯 Teoretický základ

### Jak funguje Few-Shot Learning:

```
Few-Shot Tag "Barunka":
├── Sample 1: [0.23, 0.45, 0.12, ...] (768 dim embedding)
├── Sample 2: [0.21, 0.47, 0.10, ...]
├── Sample 3: [0.24, 0.43, 0.14, ...]
├── Sample 4: [0.22, 0.46, 0.11, ...]
├── Sample 5: [0.25, 0.44, 0.13, ...]
└── Prototype: [0.23, 0.45, 0.12, ...] → Průměr všech samplu

Search query: "Barunka a Jonasek v lese"
├── Text embedding: "v lese" → [0.8, 0.3, 0.5, ...]
├── Prototype 1: "Barunka" → [0.23, 0.45, 0.12, ...]
├── Prototype 2: "Jonasek" → [0.15, 0.52, 0.08, ...]
└── Combined embedding: Average nebo weighted sum → Final search vector
```

### Proč to funguje:

1. **Semantic averaging** - průměr podobných embeddingů zachovává sdílené rysy
2. **Noise reduction** - průměrování potlačuje náhodné variace
3. **Generalizace** - prototype reprezentuje "esenci" konceptu
4. **Composition** - embeddingy lze kombinovat (Barunka + les + Jonasek)

## 🗄️ Databázová struktura

### Nové tabulky:

```kotlin
/**
 * Tabulka pro few-shot prototypes
 *
 * Obsahuje průměrné embeddingy vytvořené z multiple samplu.
 * Každý prototype reprezentuje jeden "koncept" (osobu, objekt, styl).
 */
@Entity(tableName = "few_shot_prototypes")
data class FewShotPrototypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,                    // "Barunka", "Jonasek", "Moje auto"
    val embedding: FloatArray,           // Průměrný embedding (768 dim pro CLIP)
    val color: Int,                      // Barva pro UI (hex)
    val sampleCount: Int,                // Počet samplu použitých pro průměr

    val createdAt: Long,                 // Timestamp vytvoření
    val updatedAt: Long,                 // Timestamp poslední úpravy

    // Metadata
    val description: String? = null,     // Volitelný popis
    val category: String? = null         // "person", "object", "scene", "style"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FewShotPrototypeEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * Tabulka pro jednotlivé sample embeddingy
 *
 * Ukládá jednotlivé příklady použité pro vytvoření prototype.
 * Umožňuje re-compute průměru při přidání/odebrání samplu.
 */
@Entity(
    tableName = "few_shot_samples",
    foreignKeys = [
        ForeignKey(
            entity = FewShotPrototypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["prototypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["prototypeId"])]
)
data class FewShotSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val prototypeId: Long,               // FK to FewShotPrototypeEntity

    val imageUri: String,                // URI původního obrázku
    val cropRect: String,                // JSON: {"left": 100, "top": 200, "width": 300, "height": 400}
    val embedding: FloatArray,           // Embedding tohoto konkrétního cropu

    val addedAt: Long,                   // Timestamp přidání

    // Metadata
    val thumbnailPath: String? = null    // Path k thumbnail cropu (pro UI)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FewShotSampleEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
```

### Room DAO:

```kotlin
@Dao
interface FewShotPrototypeDao {
    @Query("SELECT * FROM few_shot_prototypes ORDER BY updatedAt DESC")
    fun getAllPrototypes(): Flow<List<FewShotPrototypeEntity>>

    @Query("SELECT * FROM few_shot_prototypes WHERE id = :id")
    suspend fun getPrototypeById(id: Long): FewShotPrototypeEntity?

    @Query("SELECT * FROM few_shot_prototypes WHERE name LIKE '%' || :query || '%'")
    suspend fun searchPrototypes(query: String): List<FewShotPrototypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrototype(prototype: FewShotPrototypeEntity): Long

    @Update
    suspend fun updatePrototype(prototype: FewShotPrototypeEntity)

    @Delete
    suspend fun deletePrototype(prototype: FewShotPrototypeEntity)

    @Query("DELETE FROM few_shot_prototypes WHERE id = :id")
    suspend fun deletePrototypeById(id: Long)
}

@Dao
interface FewShotSampleDao {
    @Query("SELECT * FROM few_shot_samples WHERE prototypeId = :prototypeId ORDER BY addedAt ASC")
    fun getSamplesForPrototype(prototypeId: Long): Flow<List<FewShotSampleEntity>>

    @Query("SELECT COUNT(*) FROM few_shot_samples WHERE prototypeId = :prototypeId")
    suspend fun getSampleCount(prototypeId: Long): Int

    @Insert
    suspend fun insertSample(sample: FewShotSampleEntity): Long

    @Delete
    suspend fun deleteSample(sample: FewShotSampleEntity)

    @Query("DELETE FROM few_shot_samples WHERE id = :id")
    suspend fun deleteSampleById(id: Long)

    @Query("DELETE FROM few_shot_samples WHERE prototypeId = :prototypeId")
    suspend fun deleteAllSamplesForPrototype(prototypeId: Long)
}
```

### Repository:

```kotlin
class FewShotRepository(
    private val prototypeDao: FewShotPrototypeDao,
    private val sampleDao: FewShotSampleDao,
    private val imageEmbedder: ClipImageEmbedder
) {
    val allPrototypes: Flow<List<FewShotPrototypeEntity>> = prototypeDao.getAllPrototypes()

    suspend fun createPrototype(
        name: String,
        color: Int,
        samples: List<Pair<String, String>>, // (imageUri, cropRect)
        description: String? = null,
        category: String? = null
    ): Long {
        // 1. Extract embeddings from all samples
        val embeddings = samples.map { (uri, cropRect) ->
            extractEmbeddingFromCrop(uri, cropRect)
        }

        // 2. Compute average embedding
        val avgEmbedding = computeAverageEmbedding(embeddings)

        // 3. Create prototype
        val prototype = FewShotPrototypeEntity(
            name = name,
            embedding = avgEmbedding,
            color = color,
            sampleCount = samples.size,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            description = description,
            category = category
        )
        val prototypeId = prototypeDao.insertPrototype(prototype)

        // 4. Save individual samples
        samples.forEachIndexed { index, (uri, cropRect) ->
            val sample = FewShotSampleEntity(
                prototypeId = prototypeId,
                imageUri = uri,
                cropRect = cropRect,
                embedding = embeddings[index],
                addedAt = System.currentTimeMillis()
            )
            sampleDao.insertSample(sample)
        }

        return prototypeId
    }

    suspend fun addSampleToPrototype(
        prototypeId: Long,
        imageUri: String,
        cropRect: String
    ) {
        // 1. Extract embedding from new sample
        val embedding = extractEmbeddingFromCrop(imageUri, cropRect)

        // 2. Add sample to DB
        val sample = FewShotSampleEntity(
            prototypeId = prototypeId,
            imageUri = imageUri,
            cropRect = cropRect,
            embedding = embedding,
            addedAt = System.currentTimeMillis()
        )
        sampleDao.insertSample(sample)

        // 3. Re-compute prototype average
        recomputePrototype(prototypeId)
    }

    suspend fun removeSampleFromPrototype(sampleId: Long, prototypeId: Long) {
        sampleDao.deleteSampleById(sampleId)
        recomputePrototype(prototypeId)
    }

    private suspend fun recomputePrototype(prototypeId: Long) {
        val prototype = prototypeDao.getPrototypeById(prototypeId) ?: return
        val samples = sampleDao.getSamplesForPrototype(prototypeId).first()

        if (samples.isEmpty()) {
            // Pokud nejsou žádné samples, smazat prototype
            prototypeDao.deletePrototypeById(prototypeId)
            return
        }

        val avgEmbedding = computeAverageEmbedding(samples.map { it.embedding })
        val updatedPrototype = prototype.copy(
            embedding = avgEmbedding,
            sampleCount = samples.size,
            updatedAt = System.currentTimeMillis()
        )
        prototypeDao.updatePrototype(updatedPrototype)
    }

    private suspend fun extractEmbeddingFromCrop(
        imageUri: String,
        cropRectJson: String
    ): FloatArray {
        // Parse crop rect
        val cropRect = parseCropRect(cropRectJson)

        // Load and crop image
        val bitmap = loadAndCropImage(imageUri, cropRect)

        // Extract embedding
        return imageEmbedder.embed(bitmap)
    }

    private fun computeAverageEmbedding(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) throw IllegalArgumentException("No embeddings to average")

        val dimension = embeddings[0].size
        val sum = FloatArray(dimension) { 0f }

        embeddings.forEach { embedding ->
            for (i in embedding.indices) {
                sum[i] += embedding[i]
            }
        }

        return sum.map { it / embeddings.size }.toFloatArray()
    }

    suspend fun searchPrototypes(query: String): List<FewShotPrototypeEntity> {
        return prototypeDao.searchPrototypes(query)
    }

    suspend fun deletePrototype(prototypeId: Long) {
        prototypeDao.deletePrototypeById(prototypeId)
        // Samples se smažou automaticky (CASCADE)
    }
}
```

## 🎨 UI Implementace

### 1. Nová Tab "Few-Shot Tags"

```
┌─────────────────────────────────────────────────┐
│  Few-Shot Tags                    [+ New Tag]   │
├─────────────────────────────────────────────────┤
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │ 🟣 Barunka                     (12 samples)│ │
│  │    Osoba • Vytvořeno 15.1.2025            │ │
│  │    [👁️ View] [✏️ Edit] [🔍 Search] [🗑️]  │ │
│  └───────────────────────────────────────────┘ │
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │ 🔵 Jonasek                      (8 samples)│ │
│  │    Osoba • Vytvořeno 14.1.2025            │ │
│  │    [👁️ View] [✏️ Edit] [🔍 Search] [🗑️]  │ │
│  └───────────────────────────────────────────┘ │
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │ 🟢 Amily                        (5 samples)│ │
│  │    Osoba • Vytvořeno 13.1.2025            │ │
│  │    [👁️ View] [✏️ Edit] [🔍 Search] [🗑️]  │ │
│  └───────────────────────────────────────────┘ │
│                                                 │
│  ┌───────────────────────────────────────────┐ │
│  │ 🟠 Moje auto                    (7 samples)│ │
│  │    Objekt • Vytvořeno 10.1.2025           │ │
│  │    [👁️ View] [✏️ Edit] [🔍 Search] [🗑️]  │ │
│  └───────────────────────────────────────────┘ │
│                                                 │
└─────────────────────────────────────────────────┘
```

### 2. Create/Edit Few-Shot Tag Dialog

```
┌─────────────────────────────────────────────────┐
│  Nový Few-Shot Tag                    [✕]       │
├─────────────────────────────────────────────────┤
│                                                 │
│  Název:                                         │
│  [Barunka_____________________________]         │
│                                                 │
│  Kategorie:                                     │
│  [Person ▼] (Person, Object, Scene, Style)     │
│                                                 │
│  Barva:                                         │
│  🔴 🟠 🟡 🟢 🔵 🟣 🟤 ⚫ ⚪                       │
│                                                 │
│  Popis (volitelné):                            │
│  [Moje dcera Barunka_________________]         │
│                                                 │
├─────────────────────────────────────────────────┤
│  Samples (3/10):                               │
│  Doporučeno 5-10 příkladů pro nejlepší výsledky│
│                                                 │
│  ┌─────────┬─────────┬─────────┬─────────┐    │
│  │         │         │         │         │    │
│  │  IMG 1  │  IMG 2  │  IMG 3  │   [+]   │    │
│  │         │         │         │  Add    │    │
│  │  [🗑️]   │  [🗑️]   │  [🗑️]   │  More   │    │
│  └─────────┴─────────┴─────────┴─────────┘    │
│                                                 │
│  [📷 Take Photo]  [🖼️ Pick from Gallery]       │
│                                                 │
├─────────────────────────────────────────────────┤
│                       [Cancel]  [Create Tag]    │
└─────────────────────────────────────────────────┘
```

### 3. Add Sample Flow

```
User Journey:
1. Klikne na "Pick from Gallery"
2. Vybere obrázek
3. Zobrazí se CropImageDialog (existující komponent!)
4. Uživatel vybere oblast (obličej, objekt)
5. Klikne "Crop and Add"
6. Thumbnail cropu se zobrazí v sample gridu
7. Embedding se extrahuje na pozadí
8. Opakuje pro další samples (5-10×)
9. Klikne "Create Tag"
10. Systém spočítá průměrný embedding a uloží prototype
```

### 4. View Prototype Detail

```
┌─────────────────────────────────────────────────┐
│  ← Barunka                             [✏️ Edit]│
├─────────────────────────────────────────────────┤
│                                                 │
│  🟣 Barunka                                     │
│  Person • 12 samples                            │
│  Created: 15.1.2025 14:30                      │
│  Updated: 20.1.2025 10:15                      │
│                                                 │
│  Description:                                   │
│  Moje dcera Barunka                            │
│                                                 │
├─────────────────────────────────────────────────┤
│  Samples:                                       │
│                                                 │
│  ┌───┬───┬───┬───┐                             │
│  │ 1 │ 2 │ 3 │ 4 │                             │
│  └───┴───┴───┴───┘                             │
│  ┌───┬───┬───┬───┐                             │
│  │ 5 │ 6 │ 7 │ 8 │                             │
│  └───┴───┴───┴───┘                             │
│  ┌───┬───┬───┬───┐                             │
│  │ 9 │10 │11 │12 │                             │
│  └───┴───┴───┴───┘                             │
│                                                 │
│  [+ Add More Samples]                          │
│                                                 │
├─────────────────────────────────────────────────┤
│  Actions:                                       │
│  [🔍 Search with this tag]                     │
│  [📤 Export tag]                               │
│  [🗑️ Delete tag]                               │
└─────────────────────────────────────────────────┘
```

### 5. Search UI s Few-Shot Integration

```
┌─────────────────────────────────────────────────┐
│  Search                                         │
├─────────────────────────────────────────────────┤
│                                                 │
│  Few-Shot Tags:                                │
│  [🟣 Barunka] [🔵 Jonasek] [🟢 Amily] [+ More] │
│       ✓             ✓                          │
│   (selected)    (selected)                     │
│                                                 │
│  Text Query:                                    │
│  [v lese_____________________________] 🔍       │
│                                                 │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│  Combined Search:                              │
│  🟣 Barunka + 🔵 Jonasek + "v lese"            │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                 │
│  [Similarity: 70% ▬▬▬▬▬▬▬▬▬▬▬▬▬▬░░░░░░]        │
│                                                 │
│  Results (47):                                 │
│  ┌───────┬───────┬───────┐                    │
│  │       │       │       │                    │
│  │ IMG 1 │ IMG 2 │ IMG 3 │                    │
│  │  95%  │  92%  │  88%  │                    │
│  └───────┴───────┴───────┘                    │
│                                                 │
└─────────────────────────────────────────────────┘
```

### 6. Autocomplete/Suggestions

```
┌─────────────────────────────────────────────────┐
│  Search                                         │
├─────────────────────────────────────────────────┤
│                                                 │
│  [Bar_____________________________] 🔍          │
│   ↓                                            │
│  ┌─────────────────────────────────┐           │
│  │ 🟣 Barunka (Few-Shot Tag)       │           │
│  │ 🏖️ beach (Text)                 │           │
│  │ 🚗 car (Text)                   │           │
│  └─────────────────────────────────┘           │
│                                                 │
└─────────────────────────────────────────────────┘
```

## 🔬 Embedding Combination Strategies

### Strategie A: Simple Average (DOPORUČENO PRO START)

```kotlin
/**
 * Simple averaging - nejjednodušší a překvapivě efektivní přístup
 *
 * Výhody:
 * - Jednoduchá implementace
 * - Funguje dobře v praxi
 * - Rychlé
 *
 * Nevýhody:
 * - Všechny komponenty mají stejnou váhu
 * - Může "rozmělnit" důležité rysy
 */
fun combineEmbeddingsSimple(
    prototypes: List<FloatArray>,
    textEmbedding: FloatArray?
): FloatArray {
    val all = prototypes + listOfNotNull(textEmbedding)
    if (all.isEmpty()) throw IllegalArgumentException("No embeddings to combine")

    val dimension = all[0].size
    val sum = FloatArray(dimension) { 0f }

    all.forEach { embedding ->
        for (i in embedding.indices) {
            sum[i] += embedding[i]
        }
    }

    return sum.map { it / all.size }.toFloatArray()
}

// Použití:
val combined = combineEmbeddingsSimple(
    prototypes = listOf(barunkaEmbedding, jonasekEmbedding),
    textEmbedding = textEmbedder.embed("v lese")
)
```

### Strategie B: Weighted Average

```kotlin
/**
 * Weighted averaging - různé váhy pro různé komponenty
 *
 * Výhody:
 * - Kontrola důležitosti každé komponenty
 * - Můžeme zvýhodnit prototype nebo text
 *
 * Nevýhody:
 * - Potřeba tuning vah
 * - Složitější
 *
 * Doporučené váhy:
 * - Prototypes: 0.6-0.7 celkem (rozděleno mezi všechny prototypes)
 * - Text: 0.3-0.4
 */
fun combineEmbeddingsWeighted(
    prototypes: List<FloatArray>,
    textEmbedding: FloatArray?,
    prototypeWeight: Float = 0.7f,
    textWeight: Float = 0.3f
): FloatArray {
    require(prototypeWeight + textWeight == 1.0f) { "Weights must sum to 1.0" }

    val dimension = prototypes[0].size
    val result = FloatArray(dimension) { 0f }

    // Weighted sum of prototypes
    val prototypeWeightEach = if (prototypes.isNotEmpty()) prototypeWeight / prototypes.size else 0f
    prototypes.forEach { prototype ->
        for (i in prototype.indices) {
            result[i] += prototype[i] * prototypeWeightEach
        }
    }

    // Weighted sum of text
    textEmbedding?.let { text ->
        for (i in text.indices) {
            result[i] += text[i] * textWeight
        }
    }

    return result
}

// Použití:
val combined = combineEmbeddingsWeighted(
    prototypes = listOf(barunkaEmbedding, jonasekEmbedding),
    textEmbedding = textEmbedder.embed("v lese"),
    prototypeWeight = 0.7f,  // 70% váha pro prototypes
    textWeight = 0.3f        // 30% váha pro text
)
```

### Strategie C: Multi-Vector Search (Nejvíce accurate, ale pomalejší)

```kotlin
/**
 * Multi-vector search - každý prototype hledá samostatně
 *
 * Výhody:
 * - Nejpřesnější výsledky
 * - Najde obrázky které obsahují VŠECHNY prototypes
 *
 * Nevýhody:
 * - Pomalejší (N × více výpočtů)
 * - Složitější ranking
 *
 * Princip:
 * 1. Pro každý prototype spočítej similarity se všemi obrázky
 * 2. Kombinuj scores pomocí AND nebo OR logiky
 * 3. Filter podle textu pokud je zadán
 */
fun multiVectorSearch(
    imageStore: ImageStore,
    prototypes: List<FloatArray>,
    textEmbedding: FloatArray?,
    threshold: Float = 0.2f,
    combineMode: CombineMode = CombineMode.AND
): List<Pair<Uri, Float>> {
    // 1. Pro každý prototype najdi matches
    val prototypeMatches = prototypes.map { prototype ->
        imageStore.search(prototype, threshold)
    }

    // 2. Kombinuj výsledky
    val combinedMatches = when (combineMode) {
        CombineMode.AND -> {
            // Průnik - obrázek musí matchovat VŠECHNY prototypes
            prototypeMatches.reduce { acc, matches ->
                acc.filter { (uri, _) ->
                    matches.any { it.first == uri }
                }.map { (uri, score) ->
                    // Average score z všech matches
                    val otherScore = matches.first { it.first == uri }.second
                    uri to (score + otherScore) / 2
                }
            }
        }
        CombineMode.OR -> {
            // Sjednocení - obrázek stačí matchovat JAKÝKOLIV prototype
            prototypeMatches.flatten()
                .groupBy { it.first }
                .map { (uri, scores) ->
                    // Max score
                    uri to scores.maxOf { it.second }
                }
        }
        CombineMode.WEIGHTED_OR -> {
            // Weighted sum - obrázky matchující více prototypes mají vyšší score
            prototypeMatches.flatten()
                .groupBy { it.first }
                .map { (uri, scores) ->
                    // Average of all matches
                    uri to scores.map { it.second }.average().toFloat()
                }
        }
    }

    // 3. Filter podle textu pokud je zadán
    val finalMatches = if (textEmbedding != null) {
        combinedMatches.filter { (uri, _) ->
            val imgEmbedding = imageStore.getEmbedding(uri)
            cosineSimilarity(imgEmbedding, textEmbedding) >= threshold
        }
    } else {
        combinedMatches
    }

    return finalMatches.sortedByDescending { it.second }
}

enum class CombineMode {
    AND,           // Obrázek musí matchovat VŠECHNY prototypes
    OR,            // Obrázek stačí matchovat JAKÝKOLIV prototype
    WEIGHTED_OR    // Obrázky matchující více prototypes mají vyšší prioritu
}
```

### Strategie D: Adaptive Weighting (Advanced)

```kotlin
/**
 * Adaptive weighting - váhy se mění podle kontextu
 *
 * Výhody:
 * - Inteligentní balancing
 * - Přizpůsobí se specifičnosti query
 *
 * Nevýhody:
 * - Komplexní
 * - Potřeba experimentování
 *
 * Princip:
 * - Pokud text je velmi specifický ("v lese na jaře za deště"),
 *   zvýšit jeho váhu
 * - Pokud je jen jeden prototype, zvýšit jeho váhu
 * - Pokud jsou prototypes velmi podobné, snížit jejich váhu
 */
fun combineEmbeddingsAdaptive(
    prototypes: List<FloatArray>,
    textEmbedding: FloatArray?,
    textQuery: String?
): FloatArray {
    // Analyze text specificity
    val textSpecificity = textQuery?.split(" ")?.size ?: 0
    val textWeight = when {
        textSpecificity == 0 -> 0f
        textSpecificity <= 2 -> 0.2f
        textSpecificity <= 4 -> 0.3f
        else -> 0.4f
    }

    // Analyze prototype diversity
    val prototypeDiversity = if (prototypes.size > 1) {
        computePrototypeDiversity(prototypes)
    } else {
        1.0f
    }

    // Adjust prototype weight based on diversity
    val prototypeWeight = (1.0f - textWeight) * prototypeDiversity

    return combineEmbeddingsWeighted(
        prototypes = prototypes,
        textEmbedding = textEmbedding,
        prototypeWeight = prototypeWeight,
        textWeight = textWeight
    )
}

fun computePrototypeDiversity(prototypes: List<FloatArray>): Float {
    if (prototypes.size <= 1) return 1.0f

    // Compute pairwise similarities
    val similarities = mutableListOf<Float>()
    for (i in prototypes.indices) {
        for (j in i + 1 until prototypes.size) {
            similarities.add(cosineSimilarity(prototypes[i], prototypes[j]))
        }
    }

    // Average similarity
    val avgSimilarity = similarities.average().toFloat()

    // Diversity = 1 - similarity
    // If prototypes are very similar (avg sim = 0.9), diversity = 0.1
    // If prototypes are very different (avg sim = 0.1), diversity = 0.9
    return 1.0f - avgSimilarity
}
```

## 📊 Implementační fáze

### Fáze 1: Core Infrastructure (Týden 1)

**Cíl:** Funkční databáze a základní logika

```
Úkoly:
☐ Vytvořit FewShotPrototypeEntity a FewShotSampleEntity
☐ Vytvořit FewShotPrototypeDao a FewShotSampleDao
☐ Vytvořit FewShotRepository s metodami:
  - createPrototype()
  - addSampleToPrototype()
  - removeSampleFromPrototype()
  - recomputePrototype()
  - deletePrototype()
☐ Přidat Room migration (DATABASE_VERSION++)
☐ Unit testy pro repository
☐ Integration testy pro DB operace

Testovací kritéria:
✓ Lze vytvořit prototype z 5 samplu
✓ Průměrný embedding se správně spočítá
✓ Přidání/odebrání samplu re-compute prototype
✓ Cascade delete funguje (sample → prototype)
```

### Fáze 2: UI Components (Týden 2)

**Cíl:** Funkční UI pro správu few-shot tagů

```
Úkoly:
☐ Vytvořit FewShotTagsScreen (nová tab)
  - Seznam prototypes s preview
  - Search bar pro filtering
  - Sort options (date, name, sample count)
☐ Vytvořit CreateFewShotTagDialog
  - Input fields (name, category, color, description)
  - Sample grid
  - Add/remove sample buttons
☐ Vytvořit FewShotSamplePicker
  - Integrace s ImagePicker
  - Reuse CropImageDialog
  - Progress indicator při extrakci embeddings
☐ Vytvořit FewShotPrototypeDetailScreen
  - View samples
  - Edit metadata
  - Add more samples
  - Delete prototype
☐ ViewModel pro FewShotTags
  - State management
  - Loading states
  - Error handling

Testovací kritéria:
✓ Lze vytvořit nový tag s 5 samples
✓ Lze zobrazit detail tagu
✓ Lze přidat další samples do existujícího tagu
✓ Lze smazat tag
✓ Loading indicators zobrazeny během zpracování
```

### Fáze 3: Search Integration (Týden 3)

**Cíl:** Few-shot tagy použitelné ve vyhledávání

```
Úkoly:
☐ Upravit SearchViewModel:
  - Přidat selectedFewShotPrototypes: StateFlow<Set<Long>>
  - Přidat toggleFewShotPrototype(id: Long)
  - Upravit textSearch() a imageSearch() pro kombinaci embeddingů
☐ Vytvořit EmbeddingCombiner
  - Implementovat simple average strategii
  - Přidat weighted average (s konfigurovatelnou váhou)
  - Unit testy pro kombinace
☐ Upravit SearchScreen:
  - FewShotChips horizontal scroll
  - Indicator vybraných tagů
  - "Combined search" preview
☐ Implementovat autocomplete:
  - Suggestions z few-shot tagů
  - Rank podle shody s query
  - Mix s text suggestions

Testovací kritéria:
✓ Lze vybrat few-shot tag v search UI
✓ Search s 1 prototype funguje
✓ Search s 2+ prototypes funguje
✓ Kombinace prototype + text funguje
✓ Autocomplete navrhuje few-shot tagy
```

### Fáze 4: Advanced Features (Týden 4)

**Cíl:** Vylepšení UX a pokročilé funkce

```
Úkoly:
☐ Multi-vector search (volitelná strategie)
☐ Export/Import few-shot tagů
  - JSON format
  - Share přes Intent
  - Import z JSON
☐ Auto-clustering podobných obličejů
  - Batch process všech obrázků
  - Cluster embeddingy pomocí K-means
  - Nabídnout vytvoření few-shot tagu pro cluster
☐ Confidence scoring
  - Zobrazit similarity score pro každý result
  - Color-code podle confidence (zelená = high, žlutá = medium, červená = low)
☐ Negative prototypes (advanced)
  - "Ne Barunka" - exclude podobné obrázky
  - Implementace: invertovat similarity nebo odečíst od výsledku
☐ Analytics
  - Track použití few-shot tagů
  - Nejpoužívanější tagy
  - Success rate (kolik resultů vrací)

Testovací kritéria:
✓ Multi-vector search vrací přesnější výsledky
✓ Lze exportovat tag jako JSON
✓ Lze importovat tag z JSON
✓ Auto-clustering navrhuje relevantní clustery
✓ Confidence scores správně zobrazeny
```

### Fáze 5: Polish & Optimization (Týden 5)

**Cíl:** Performance a UX vylepšení

```
Úkoly:
☐ Performance optimalizace:
  - Cache computed embeddings
  - Batch processing při vytváření tagů
  - Lazy loading thumbnailů
  - Background processing pro re-compute
☐ UX vylepšení:
  - Empty states
  - Error states s recovery akcemi
  - Onboarding tutorial
  - Tooltips a nápověda
☐ Accessibility:
  - Content descriptions
  - Keyboard navigation
  - Screen reader support
☐ Testing:
  - UI tests pro všechny screens
  - Integration tests pro complete flow
  - Performance tests (time to create prototype)
☐ Documentation:
  - User guide
  - Developer docs
  - API documentation

Testovací kritéria:
✓ Vytvoření tagu s 10 samples < 5 sekund
✓ Search s few-shot tagem < 500ms
✓ UI reaguje plynule (60 FPS)
✓ Všechny screens mají empty/error states
✓ Accessibility score > 90%
```

## 🧪 Testing Strategy

### Unit Tests

```kotlin
@Test
fun `computeAverageEmbedding správně průměruje embeddingy`() {
    val emb1 = floatArrayOf(1f, 2f, 3f)
    val emb2 = floatArrayOf(3f, 4f, 5f)
    val emb3 = floatArrayOf(2f, 3f, 4f)

    val avg = computeAverageEmbedding(listOf(emb1, emb2, emb3))

    assertArrayEquals(floatArrayOf(2f, 3f, 4f), avg, 0.001f)
}

@Test
fun `combineEmbeddingsSimple kombinuje prototypes a text`() {
    val prototype1 = floatArrayOf(1f, 0f, 0f)
    val prototype2 = floatArrayOf(0f, 1f, 0f)
    val text = floatArrayOf(0f, 0f, 1f)

    val combined = combineEmbeddingsSimple(
        listOf(prototype1, prototype2),
        text
    )

    // (1+0+0)/3 = 0.33, (0+1+0)/3 = 0.33, (0+0+1)/3 = 0.33
    assertArrayEquals(floatArrayOf(0.33f, 0.33f, 0.33f), combined, 0.01f)
}

@Test
fun `recomputePrototype aktualizuje průměr při přidání samplu`() = runTest {
    // Setup
    val prototypeId = repository.createPrototype(
        name = "Test",
        color = 0xFF0000,
        samples = listOf(
            "uri1" to "crop1",
            "uri2" to "crop2"
        )
    )

    val originalPrototype = repository.getPrototypeById(prototypeId)!!

    // Add sample
    repository.addSampleToPrototype(prototypeId, "uri3", "crop3")

    // Verify
    val updatedPrototype = repository.getPrototypeById(prototypeId)!!
    assertEquals(3, updatedPrototype.sampleCount)
    assertNotEquals(originalPrototype.embedding, updatedPrototype.embedding)
}
```

### Integration Tests

```kotlin
@Test
fun `complete few-shot tag creation flow`() = runTest {
    // 1. Create prototype with 3 samples
    val prototypeId = repository.createPrototype(
        name = "Test Person",
        color = 0xFF0000,
        samples = listOf(
            "content://media/1" to """{"left":0,"top":0,"width":100,"height":100}""",
            "content://media/2" to """{"left":0,"top":0,"width":100,"height":100}""",
            "content://media/3" to """{"left":0,"top":0,"width":100,"height":100}"""
        )
    )

    // 2. Verify prototype created
    val prototype = repository.getPrototypeById(prototypeId)
    assertNotNull(prototype)
    assertEquals("Test Person", prototype?.name)
    assertEquals(3, prototype?.sampleCount)

    // 3. Verify samples stored
    val samples = repository.getSamplesForPrototype(prototypeId).first()
    assertEquals(3, samples.size)

    // 4. Add another sample
    repository.addSampleToPrototype(
        prototypeId,
        "content://media/4",
        """{"left":0,"top":0,"width":100,"height":100}"""
    )

    // 5. Verify re-computed
    val updatedPrototype = repository.getPrototypeById(prototypeId)
    assertEquals(4, updatedPrototype?.sampleCount)

    // 6. Delete prototype
    repository.deletePrototype(prototypeId)

    // 7. Verify cascade delete
    val deletedPrototype = repository.getPrototypeById(prototypeId)
    assertNull(deletedPrototype)
    val deletedSamples = repository.getSamplesForPrototype(prototypeId).first()
    assertTrue(deletedSamples.isEmpty())
}

@Test
fun `search with few-shot prototype returns relevant results`() = runTest {
    // Setup: Create prototype for "dog"
    val dogPrototypeId = repository.createPrototype(
        name = "My Dog",
        color = 0xFF0000,
        samples = listOf(
            // URIs k obrázkům psa
        )
    )

    // Execute search
    val dogPrototype = repository.getPrototypeById(dogPrototypeId)!!
    val results = imageStore.search(dogPrototype.embedding, threshold = 0.2f)

    // Verify
    assertTrue(results.isNotEmpty())
    // Měly by být obrázky psa
}
```

### UI Tests

```kotlin
@Test
fun `user can create few-shot tag`() {
    // Launch app
    composeTestRule.setContent { App() }

    // Navigate to Few-Shot Tags tab
    composeTestRule.onNodeWithText("Few-Shot Tags").performClick()

    // Click "New Tag"
    composeTestRule.onNodeWithText("+ New Tag").performClick()

    // Fill name
    composeTestRule.onNodeWithTag("nameInput").performTextInput("Test Person")

    // Select category
    composeTestRule.onNodeWithTag("categorySelector").performClick()
    composeTestRule.onNodeWithText("Person").performClick()

    // Add 3 samples (mock)
    repeat(3) {
        composeTestRule.onNodeWithText("Pick from Gallery").performClick()
        // Mock image picker result
        composeTestRule.waitForIdle()
    }

    // Create tag
    composeTestRule.onNodeWithText("Create Tag").performClick()

    // Verify tag appears in list
    composeTestRule.onNodeWithText("Test Person").assertExists()
}
```

## 🎓 Best Practices & Tips

### 1. Počet Samplu

```
✅ Doporučeno: 5-10 samplu
- 5 samplu: Minimum pro solidní prototype
- 7-8 samplu: Sweet spot (diversity vs. noise)
- 10+ samplu: Diminishing returns, ale pořád OK

❌ Nedoporučeno:
- 1-2 samples: Příliš málo, prototype bude overfitted
- 20+ samples: Zbytečné, nepřidává přesnost
```

### 2. Kvalita Samplu

```
✅ Dobré samples:
- Různé úhly (frontal, profil, 3/4)
- Různé osvětlení (denní, umělé, backlit)
- Různé výrazy (úsměv, neutrální, vážný)
- Podobná vzdálenost od kamery
- Clear, sharp images

❌ Špatné samples:
- Všechny stejný úhel
- Rozmazané/neostrá
- Částečně zakryté (ruka před obličejem)
- Příliš malé (< 64x64 px)
- Příliš různorodé vzdálenosti (closeup + daleko)
```

### 3. Naming Convention

```
✅ Dobré názvy:
- Konkrétní: "Barunka", "Jonasek", "Moje auto"
- Krátké: < 15 znaků
- Bez speciálních znaků
- CamelCase nebo prostý text

❌ Špatné názvy:
- Příliš obecné: "Person1", "Face"
- Příliš dlouhé: "Moje dcera Barunka která má ráda psy"
- Se speciálními znaky: "Barunka#2023"
```

### 4. Category Guidelines

```
Person:
- Obličeje lidí
- Celé postavy konkrétních osob

Object:
- Auta, kola, hračky
- Specifické objekty které vlastníte

Scene:
- Místa (kuchyně, obývák, zahrada)
- Krajiny (hory, les, pláž)

Style:
- Fotografický styl (černobílé, vintage)
- Kompozice (makro, panorama)
```

### 5. Performance Tips

```kotlin
// ✅ Dobrá praxe: Batch processing
suspend fun createPrototypeBatch(samples: List<Sample>) {
    withContext(Dispatchers.IO) {
        val embeddings = samples.map { sample ->
            async { extractEmbedding(sample) }
        }.awaitAll()

        savePrototype(embeddings)
    }
}

// ❌ Špatná praxe: Sequential processing
suspend fun createPrototypeSequential(samples: List<Sample>) {
    val embeddings = samples.map { sample ->
        extractEmbedding(sample) // Čeká na každý embedding
    }
    savePrototype(embeddings)
}
```

### 6. User Feedback

```
Při vytváření prototypu:
✅ "Extracting embeddings... (2/5)"
✅ Progress bar
✅ "Computing average..."
✅ "Done! Created prototype 'Barunka'"

Při vyhledávání:
✅ "Searching with: Barunka + v lese"
✅ Show combined chips
✅ "Found 47 results (avg similarity: 82%)"
```

## 🔮 Budoucí rozšíření

### 1. Transfer Learning

```
Místo průměrování embeddingů natrénovat malý adaptér:

Input: CLIP embedding (768 dim)
   ↓
Dense(512, ReLU)
   ↓
Dense(256, ReLU)
   ↓
Output: Refined embedding (768 dim)

Trénování:
- Positive pairs: (sample, prototype)
- Negative pairs: (sample, other_prototype)
- Contrastive loss

Benefit: Lepší generalizace než simple averaging
Náročnost: Potřeba 100+ samplu, GPU
```

### 2. Online Learning

```
Průběžné vylepšování prototypu:

User vyhledá "Barunka" → 50 results
User vybere 5 obrázků jako "correct"
   ↓
System:
1. Extract embeddings z correct results
2. Blend do existujícího prototype (weighted average)
3. Re-compute prototype
4. Improve future searches

Benefit: Prototype se učí z user feedback
```

### 3. Hierarchické Prototypes

```
Person
├── Barunka
│   ├── Barunka_closeup
│   ├── Barunka_fullbody
│   └── Barunka_profile
├── Jonasek
└── Amily

Object
├── Cars
│   ├── My_Car
│   └── Red_Cars
└── Bikes

Benefit: Organizace, inheritance embeddingů
```

### 4. Multi-Modal Prototypes

```
Místo jen image embeddings kombinovat více modalit:

Prototype "Barunka":
├── Image embeddings (obličej)
├── Text description: "blond hair, blue eyes, 5 years old"
├── Location: GPS coordinates domova
├── Time: typicky fotky 15:00-18:00
└── Co-occurrence: často s "Jonasek", "zahrada"

Search: "Barunka" → kombinuje všechny modality
Benefit: Mnohem přesnější výsledky
```

### 5. Social Features

```
- Share prototypes mezi uživateli
- Public prototype marketplace ("Celebrity Faces", "Car Brands")
- Collaborative prototypes (rodina přidává samples)
- Rating system pro prototypes
```

## 📚 References & Resources

### Academic Papers

1. **Prototypical Networks** (Snell et al., 2017)
   - https://arxiv.org/abs/1703.05175
   - Základ few-shot learning s prototypes

2. **CLIP** (Radford et al., 2021)
   - https://arxiv.org/abs/2103.00020
   - Multi-modal embeddings

3. **Matching Networks** (Vinyals et al., 2016)
   - https://arxiv.org/abs/1606.04080
   - Few-shot learning přístup

### Code Examples

```kotlin
// Příklad použití v aplikaci:

// 1. Create few-shot tag
val barunkaId = fewShotRepository.createPrototype(
    name = "Barunka",
    color = 0xFF9C27B0.toInt(),
    samples = listOf(
        "content://media/1" to """{"left":100,"top":200,"width":300,"height":300}""",
        "content://media/5" to """{"left":150,"top":250,"width":280,"height":280}""",
        "content://media/12" to """{"left":120,"top":180,"width":320,"height":320}""",
        // ... 5-10 samples total
    ),
    category = "person"
)

// 2. Search using prototype
val barunkaPrototype = fewShotRepository.getPrototypeById(barunkaId)!!
val textEmbedding = textEmbedder.embed("v lese")

val combinedEmbedding = combineEmbeddingsSimple(
    prototypes = listOf(barunkaPrototype.embedding),
    textEmbedding = textEmbedding
)

val results = imageStore.search(combinedEmbedding, threshold = 0.2f)

// 3. Multi-prototype search
val jonasekPrototype = fewShotRepository.getPrototypeById(jonasekId)!!

val multiPrototypeResults = multiVectorSearch(
    imageStore = imageStore,
    prototypes = listOf(barunkaPrototype.embedding, jonasekPrototype.embedding),
    textEmbedding = textEmbedder.embed("na dovolené"),
    threshold = 0.2f,
    combineMode = CombineMode.AND // Must contain BOTH Barunka and Jonasek
)
```

## 🚀 Implementační checklist

### Databáze
- [ ] FewShotPrototypeEntity
- [ ] FewShotSampleEntity
- [ ] FewShotPrototypeDao
- [ ] FewShotSampleDao
- [ ] FewShotRepository
- [ ] Room migration
- [ ] Unit testy

### Core Logic
- [ ] computeAverageEmbedding()
- [ ] combineEmbeddingsSimple()
- [ ] combineEmbeddingsWeighted()
- [ ] multiVectorSearch() (optional)
- [ ] extractEmbeddingFromCrop()
- [ ] Integration testy

### UI - Few-Shot Tags Screen
- [ ] FewShotTagsScreen
- [ ] CreateFewShotTagDialog
- [ ] FewShotPrototypeDetailScreen
- [ ] FewShotSampleGrid
- [ ] FewShotTagsViewModel
- [ ] UI testy

### UI - Search Integration
- [ ] FewShotChips component
- [ ] Autocomplete pro few-shot tags
- [ ] Combined search preview
- [ ] Update SearchViewModel
- [ ] Update SearchScreen
- [ ] Integration testy

### Advanced Features (Optional)
- [ ] Export/Import prototypes
- [ ] Auto-clustering
- [ ] Confidence scoring
- [ ] Negative prototypes
- [ ] Analytics

### Polish
- [ ] Empty states
- [ ] Error states
- [ ] Loading indicators
- [ ] Onboarding tutorial
- [ ] Performance optimization
- [ ] Accessibility
- [ ] Documentation

---

**Autor:** Jaroslav + Claude Code
**Datum:** 2025-01-28
**Verze:** 1.0
**Status:** Design Document - Připraveno k implementaci
