# Best Practices Analýza - Hybrid Auto-Tagging

## 📚 Zdroje

**Analyzované knihovny:**
- ✅ ONNX Runtime Android (Microsoft)
- ✅ AndroidX WorkManager
- ✅ Room Database
- ⚠️ SmartScan SDK (custom, není v Context7)

---

## ✅ CO JE SPRÁVNĚ V SOUČASNÉM NÁVRHU

### 1. Architektura - VÝBORNÁ

**Hybrid přístup:**
```
Indexování → Embedding extraction → FileEmbeddingStore
              ↓ (pokud tagy exist)
              Auto-tagging (batch 50)
```

✅ **Proč je to dobré:**
- Separace concerns (indexing vs tagging)
- FileEmbeddingStore místo DB pro embeddings = rychlejší
- Batch processing místo "all at once"
- Progress reporting možný

### 2. Batch Size: 50 obrázků

✅ **Proč 50:**
- Memory footprint: ~20KB (50 × 10 tagů × ImageTagEntity)
- Umožňuje granulární progress updates
- Není příliš malý (overhead) ani velký (memory)

### 3. Room pro Tag Metadata

✅ **Správná separace:**
- Tags/metadata → Room Database (relační data)
- Embeddings → FileEmbeddingStore (velké binární bloby)

---

## 🚨 DOPORUČENÉ OPTIMALIZACE

### 1. ONNX Runtime - Session Reuse ⭐ KRITICKÉ

**❌ SOUČASNÝ PROBLÉM:**

```kotlin
// MediaIndexForegroundService.kt - řádek 52
override fun onCreate() {
    super.onCreate()
    embeddingHandler = ClipImageEmbedder(resources, ResourceId(R.raw.image_encoder_quant_int8))
    // Session se vytváří při každém onCreate
}

// TaggingService.kt - PROBLÉM NENÍ (service je singleton)
```

**✅ DOPORUČENÍ:**

**Pro MediaIndexForegroundService:**
```kotlin
class MediaIndexForegroundService : Service() {
    // ✅ Lazy initialization - session se vytvoří JEDNOU
    private val embeddingHandler by lazy {
        ClipImageEmbedder(
            resources,
            ResourceId(R.raw.image_encoder_quant_int8)
        ).apply {
            // Optimalizace session při prvním použití
            initialize()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceNotification()
        // DON'T initialize embeddingHandler here - wait for first use
    }

    override fun onStartCommand(...) {
        serviceScope.launch {
            // Session se vytvoří při prvním volání (lazy)
            embeddingHandler.initialize() // Pokud už není initialized

            // ... indexing ...
        } finally {
            // ⚠️ DŮLEŽITÉ: Close session až na konci
            embeddingHandler.closeSession()
        }
    }
}
```

**Pro optimální ONNX performance:**
```kotlin
// Pokud máš přístup k ONNX SessionOptions (SmartScan SDK):
class ClipImageEmbedder {
    private fun createSession(): InferenceSession {
        val options = SessionOptions().apply {
            // Threading
            setIntraOpNumThreads(4)  // Parallel ops within operator
            setInterOpNumThreads(2)  // Parallel execution of operators

            // Memory optimization
            setMemoryPatternOptimization(true)
            setCpuMemArena(true)

            // Graph optimization
            setOptimizationLevel(OptLevel.ALL_OPT)

            // Execution mode
            setExecutionMode(ExecutionMode.SEQUENTIAL) // or PARALLEL for batch
        }

        return InferenceSession(modelBytes, options)
    }
}
```

---

### 2. WorkManager - Foreground Service ⭐ KRITICKÉ

**❌ SOUČASNÝ PROBLÉM:**

RetaggingWorker NE volá `setForeground()` HNED na začátku.

**✅ SOUČASNÝ KÓD (RetaggingWorker.kt:50-56):**
```kotlin
override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    try {
        Log.d(TAG, "Starting image re-tagging")
        startTime = System.currentTimeMillis()

        // Nastavení foreground notifikace
        setForeground(createForegroundInfo(0, 0))  // ✅ DOBŘE - hned na začátku!
```

**✅ TOTO JE SPRÁVNĚ!** Worker už volá `setForeground()` na začátku.

**Doporučení pro budoucí ClassificationBatchWorker:**
```kotlin
class ClassificationBatchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // ⭐ KRITICKÉ - HNED jako první věc!
        setForeground(createForegroundInfo(0, 0))

        return withContext(Dispatchers.IO) {
            try {
                // ... batch processing ...
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }

    private fun createForegroundInfo(current: Int, total: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Auto-tagging images")
            .setContentText(if (total > 0) "Processing $current/$total" else "Preparing...")
            .setSmallIcon(R.drawable.smartscan_logo)
            .setProgress(total, current, total == 0)
            .setOngoing(true)
            .build()

        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            // ⚠️ DŮLEŽITÉ: Specifikovat service type (Android 14+)
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }
}
```

---

### 3. Room Batch Insert - Optimalizace

**✅ SOUČASNÝ KÓD (TaggingService.kt:79-80):**
```kotlin
// Batch insert do databáze
if (assignedTags.isNotEmpty()) {
    repository.insertImageTags(assignedTags)  // ✅ Používá batch insert
```

**Ověření Repository pattern:**

```kotlin
// TagRepository.kt - zkontrolovat implementaci
class TagRepository(
    private val imageTagDao: ImageTagDao
) {
    suspend fun insertImageTags(tags: List<ImageTagEntity>) {
        // ✅ DOBRÁ implementace - Room automaticky použije transaction
        imageTagDao.insertAll(tags)
    }
}

// ImageTagDao.kt
@Dao
interface ImageTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<ImageTagEntity>)
    // ✅ Room creates transaction automatically
}
```

**⚠️ DOPORUČENÍ pro VELKÉ batche (1000+ tagů):**

Pokud by se stalo že máš velký batch, chunk to:

```kotlin
suspend fun insertImageTags(tags: List<ImageTagEntity>) {
    // Pro velké množství - chunk po 500
    if (tags.size > 500) {
        tags.chunked(500).forEach { chunk ->
            imageTagDao.insertAll(chunk)
        }
    } else {
        imageTagDao.insertAll(tags)
    }
}
```

**Důvod:** Room transaction má limit, velmi velké batche mohou způsobit:
- TransactionTooLargeException
- SQLiteDatabaseLockedException
- OutOfMemoryError

---

### 4. Hybrid Auto-Tagging - Konkrétní Implementace

**Upravit MediaIndexForegroundService.kt:**

```kotlin
class MediaIndexForegroundService : Service() {
    private val taggingBatch = mutableListOf<Pair<Long, FloatArray>>()
    private val TAGGING_BATCH_SIZE = 50

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mediaType = intent?.getStringExtra(EXTRA_MEDIA_TYPE) ?: TYPE_BOTH

        serviceScope.launch {
            try {
                val appSettings = loadSettings(sharedPrefs)
                embeddingHandler.initialize()

                // ⭐ NOVÉ: Zjisti jestli auto-tagovat
                val shouldAutoTag = shouldAutoTag()
                val taggingService = if (shouldAutoTag) {
                    TaggingService(applicationContext)
                } else null

                if (mediaType == TYPE_IMAGE || mediaType == TYPE_BOTH) {
                    val imageStore = FileEmbeddingStore(...)
                    val imageIndexer = ImageIndexer(embeddingHandler, application, ImageIndexListener, store = imageStore)
                    val ids = queryImageIds(application, appSettings.searchableImageDirectories.map{it.toUri()})
                    val existingIds = if(imageStore.exists) imageStore.get().map{it.id}.toSet() else emptySet()
                    val filteredIds = ids.filterNot { existingIds.contains(it) }

                    // ⭐ CUSTOM RUN s auto-tagging
                    if (shouldAutoTag && taggingService != null) {
                        imageIndexer.runWithAutoTag(filteredIds, taggingService, ::addToTaggingBatch)
                    } else {
                        imageIndexer.run(filteredIds)
                    }

                    // ⭐ Flush zbylé tagy
                    if (taggingBatch.isNotEmpty() && taggingService != null) {
                        flushTaggingBatch(taggingService)
                    }
                }

                // ... video indexing ...

            } catch (e: Exception) {
                Log.e(TAG, "Indexing failed:", e)
            } finally {
                sharedPrefs.edit { putString("lastIndexed", System.currentTimeMillis().toString()) }
                embeddingHandler.closeSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun shouldAutoTag(): Boolean {
        val database = TagDatabase.getDatabase(applicationContext)
        val activeTags = database.userTagDao().getActiveTagsSync()
        return activeTags.isNotEmpty()
    }

    private fun addToTaggingBatch(imageId: Long, embedding: FloatArray) {
        taggingBatch.add(imageId to embedding)
    }

    private suspend fun flushTaggingBatch(taggingService: TaggingService) {
        if (taggingBatch.isEmpty()) return

        taggingService.assignTagsBatch(
            images = taggingBatch,
            onProgress = null // Progress tracking pro indexing, ne pro tagging
        )

        taggingBatch.clear()
    }
}
```

**⚠️ PROBLÉM:** ImageIndexer je z SmartScan SDK, nemáme kontrolu nad jeho API.

**✅ ŘEŠENÍ: Wrapper pattern**

```kotlin
class AutoTaggingImageIndexer(
    private val baseIndexer: ImageIndexer,
    private val taggingService: TaggingService?
) {
    private val taggingBatch = mutableListOf<Pair<Long, FloatArray>>()
    private val BATCH_SIZE = 50

    suspend fun run(ids: List<Long>) {
        // Hook do ImageIndexer není možný, takže:
        // 1. Indexuj normálně
        baseIndexer.run(ids)

        // 2. Pokud auto-tagging enabled, načti embeddings a taguj
        if (taggingService != null) {
            val embeddings = baseIndexer.store.get()
            val newEmbeddings = embeddings.filter { it.id in ids }

            newEmbeddings.chunked(BATCH_SIZE).forEach { batch ->
                val imagePairs = batch.map { it.id to it.embeddings }
                taggingService.assignTagsBatch(imagePairs, onProgress = null)
            }
        }
    }
}
```

**⚠️ NEVÝHODA:** Načítáme embeddings ZNOVU z file store.

**✅ ALTERNATIVA: Listener pattern (pokud SDK podporuje)**

```kotlin
// Pokud ImageIndexer má callback API:
imageIndexer.setOnImageProcessed { imageId, embedding ->
    if (taggingService != null) {
        taggingBatch.add(imageId to embedding)

        if (taggingBatch.size >= BATCH_SIZE) {
            runBlocking { // nebo launch v scope
                taggingService.assignTagsBatch(taggingBatch, null)
                taggingBatch.clear()
            }
        }
    }
}
```

---

### 5. TaggingService - Optimalizace Batch Processing

**✅ SOUČASNÁ IMPLEMENTACE je DOBRÁ:**

```kotlin
suspend fun assignTagsBatch(
    images: List<Pair<Long, FloatArray>>,
    onProgress: ((Int, Int) -> Unit)? = null
): Int = withContext(Dispatchers.IO) {
    try {
        var totalAssigned = 0
        val total = images.size

        images.forEachIndexed { index, (imageId, embedding) ->
            val assigned = assignTags(imageId, embedding)
            totalAssigned += assigned.size

            onProgress?.invoke(index + 1, total)
        }

        totalAssigned
    } catch (e: Exception) {
        0
    }
}
```

**⚠️ PROBLÉM:** Loop volá `assignTags()` pro každý obrázek = N × DB queries.

**✅ OPTIMALIZOVANÁ VERZE:**

```kotlin
suspend fun assignTagsBatch(
    images: List<Pair<Long, FloatArray>>,
    onProgress: ((Int, Int) -> Unit)? = null
): Int = withContext(Dispatchers.IO) {
    try {
        // 1. Načti aktivní tagy JEDNOU (místo N-krát)
        val activeTags = repository.getActiveTagsSync()
        if (activeTags.isEmpty()) return@withContext 0

        // 2. Najdi všechny matching tagy pro všechny obrázky
        val allImageTags = mutableListOf<ImageTagEntity>()

        images.forEachIndexed { index, (imageId, imageEmbedding) ->
            // Nejdřív smaž existující auto-assigned tagy pro tento obrázek
            // (pro re-tagging scenario)
            val existingTags = repository.getTagsForImage(imageId)
            val autoTags = existingTags.filter { !it.isUserAssigned }
            autoTags.forEach { repository.deleteImageTag(it) }

            // Najdi matching tagy
            activeTags.forEach { userTag ->
                val similarity = cosineSimilarity(imageEmbedding, userTag.embedding)

                if (similarity >= userTag.threshold) {
                    allImageTags.add(
                        ImageTagEntity(
                            imageId = imageId,
                            tagName = userTag.name,
                            confidence = similarity,
                            isUserAssigned = false
                        )
                    )
                }
            }

            onProgress?.invoke(index + 1, images.size)
        }

        // 3. JEDEN batch insert místo N insertů
        if (allImageTags.isNotEmpty()) {
            repository.insertImageTags(allImageTags)
        }

        allImageTags.size
    } catch (e: Exception) {
        Log.e(TAG, "Error in batch tagging", e)
        0
    }
}
```

**⚠️ PROBLÉM S DELETE:** Delete v loopu je stále N queries.

**✅ JEŠTĚ LEPŠÍ VERZE - Batch Delete:**

```kotlin
// ImageTagDao.kt - přidat batch delete
@Dao
interface ImageTagDao {
    @Query("DELETE FROM image_tags WHERE imageId IN (:imageIds) AND isUserAssigned = 0")
    suspend fun deleteAutoTagsForImages(imageIds: List<Long>)
}

// TaggingService.kt - použít batch delete
suspend fun assignTagsBatch(
    images: List<Pair<Long, FloatArray>>,
    onProgress: ((Int, Int) -> Unit)? = null
): Int = withContext(Dispatchers.IO) {
    // 1. Batch delete auto-assigned tagů
    val imageIds = images.map { it.first }
    repository.deleteAutoTagsForImages(imageIds)

    // 2. Načti aktivní tagy
    val activeTags = repository.getActiveTagsSync()
    if (activeTags.isEmpty()) return@withContext 0

    // 3. Najdi matching tagy
    val allImageTags = mutableListOf<ImageTagEntity>()

    images.forEachIndexed { index, (imageId, imageEmbedding) ->
        activeTags.forEach { userTag ->
            val similarity = cosineSimilarity(imageEmbedding, userTag.embedding)

            if (similarity >= userTag.threshold) {
                allImageTags.add(
                    ImageTagEntity(
                        imageId = imageId,
                        tagName = userTag.name,
                        confidence = similarity,
                        isUserAssigned = false
                    )
                )
            }
        }

        onProgress?.invoke(index + 1, images.size)
    }

    // 4. Batch insert
    if (allImageTags.isNotEmpty()) {
        repository.insertImageTags(allImageTags)
    }

    allImageTags.size
}
```

**Performance improvement:**
- Před: N × (1 read + M deletes + K inserts) = **1000+ queries** (pro 100 obrázků)
- Po: 1 batch delete + 1 read + 1 batch insert = **3 queries** ✅

---

## 📊 PERFORMANCE SROVNÁNÍ

### Současná implementace (RetaggingWorker):
```
10,000 obrázků × 10 tagů:
- Load embeddings: ~2s
- Tagging loop: 10,000 × assignTags()
  - Each: 1 getActiveTagsSync + M cosine + N inserts
  - Total: ~10,000 × 150ms = 25 minut
```

### Optimalizovaná implementace:
```
10,000 obrázků × 10 tagů (batch 50):
- Load embeddings: ~2s (stejné)
- Batch delete: 1 query = ~0.5s
- Load active tags: 1 query = ~0.1s
- Cosine similarity: 10,000 × 10 × 5ms = 8.3 minut
- Batch insert (200 batches): 200 × 50ms = 10s
CELKEM: ~9 minut ✅ (3× rychlejší!)
```

---

## 🎯 AKČNÍ PLÁN - PRIORITIZOVANÉ ÚPRAVY

### ⭐ VYSOKÁ PRIORITA (před implementací hybrid tagging)

1. **Optimalizovat TaggingService.assignTagsBatch()**
   - [ ] Přidat `deleteAutoTagsForImages()` do ImageTagDao
   - [ ] Refaktorovat assignTagsBatch na batch delete + batch insert
   - [ ] Odstranit loop s assignTags()
   - **Očekávaný zisk:** 3× rychlejší re-tagging

2. **ONNX Session Options** (pokud máš přístup v SmartScan SDK)
   - [ ] Zkontrolovat jestli ClipImageEmbedder používá optimální SessionOptions
   - [ ] Přidat threading configuration (intra/inter threads)
   - [ ] Enable memory optimizations
   - **Očekávaný zisk:** 10-20% rychlejší inference

### 🔵 STŘEDNÍ PRIORITA (nice to have)

3. **Room Chunking pro velké batche**
   - [ ] Přidat chunking po 500 items v insertImageTags()
   - **Očekávaný zisk:** Prevence crashes při velkých datech

4. **Foreground Service Type** (Android 14+)
   - [ ] Přidat SERVICE_TYPE_DATA_SYNC do ForegroundInfo
   - **Očekávaný zisk:** Lepší compliance s Android 14+

### 🟢 NÍZKÁ PRIORITA (budoucí optimalizace)

5. **Parallel Batch Processing**
   - [ ] Zpracovat více batchů paralelně (s memory limitem)
   - **Očekávaný zisk:** Využití multi-core CPU

6. **Progress Granularity**
   - [ ] Per-image progress místo per-batch
   - **Očekávaný zisk:** Smooth progress bar

---

## 📝 ZÁVĚR

**✅ Tvůj návrh je architektonicky SPRÁVNÝ!**

Hlavní doporučené změny:
1. **TaggingService batch optimization** - největší performance win
2. **ONNX session options** - pokud máš přístup k SDK
3. **Room chunking** - safety pro velká data

**Hybrid auto-tagging lze implementovat BEZ ZMĚN** současné architektury - pouze přidání volání do MediaIndexForegroundService.

**Největší risk:** SmartScan SDK nemá dokumentaci v Context7, takže nevím:
- Jestli ImageIndexer podporuje callbacks
- Jestli máš přístup k ONNX SessionOptions
- Jaké jsou best practices pro tento konkrétní SDK

**Doporučuji:**
1. Zkontrolovat SmartScan SDK dokumentaci/source code
2. Implementovat batch optimization v TaggingService (vysoká priorita)
3. Pak teprve implementovat hybrid auto-tagging
