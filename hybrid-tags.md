# Hybrid Auto-Tagging System

## 🎯 Koncept

Inteligentní kombinace **auto-tagging během indexování** + **manuální re-tagging** pro optimální UX a performance.

## 📊 Use Cases

### Scénář 1: První použití aplikace
```
1. Uživatel nainstaluje SmartScan
2. Spustí první indexování (10,000 obrázků)
   → Žádné tagy = rychlé indexování (~30 min)
3. Vytvoří 15 tagů přes TagManagerScreen
4. Klikne "Re-taggovat všechny obrázky"
   → RetaggingScreen zobrazí progress
   → Všech 10,000 obrázků dostane tagy (~15 min)
```

**Výsledek:** Uživatel má kontrolu, indexování je rychlé, re-tagging jednorázový.

---

### Scénář 2: Pravidelné používání
```
1. Uživatel má již vytvořené tagy
2. Přidá 50 nových fotek do galerie
3. Spustí indexování (automatické nebo manuální)
   → Detekce: "Existují aktivní tagy? ANO"
   → Auto-tagging během indexování
   → 50 nových fotek OKAMŽITĚ tagováno
```

**Výsledek:** Nové obrázky jsou automaticky tagované bez manuálního zásahu.

---

### Scénář 3: Změna tagů
```
1. Uživatel upraví threshold u tagu "Selfie" (0.35 → 0.25)
2. Klikne "Re-taggovat všechny obrázky"
   → Smaže staré auto-assigned tagy
   → Přiřadí nové tagy s novým thresholdem
```

**Výsledek:** Flexibilní přeznačkování při změně pravidel.

---

## 🏗️ Architektura

### 1. Detekce režimu při indexování

```kotlin
// MediaIndexForegroundService.kt
private suspend fun shouldAutoTag(): Boolean {
    val database = TagDatabase.getDatabase(applicationContext)
    val activeTags = database.userTagDao().getActiveTagsSync()
    return activeTags.isNotEmpty()
}
```

### 2. Conditional auto-tagging

```kotlin
// V MediaIndexForegroundService po vytvoření embeddingu
if (shouldAutoTag()) {
    val taggingService = TaggingService(applicationContext)
    taggingService.assignTags(imageId, embedding.embeddings)
}

// Vždy uložit embedding (nezávisle na taggingu)
imageStore.add(listOf(embedding))
```

### 3. Optimalizace: Batch tagging

**Problém:** Volání `assignTags()` pro každý obrázek = N * DB queries

**Řešení:** Batch processing po X obrázcích

```kotlin
// MediaIndexForegroundService.kt
private val taggingBatch = mutableListOf<Pair<Long, FloatArray>>()
private val BATCH_SIZE = 50

private suspend fun processImageWithTagging(imageId: Long, embedding: Embedding) {
    // Přidat do batche
    taggingBatch.add(imageId to embedding.embeddings)

    // Flush batch když dosáhne limitu
    if (taggingBatch.size >= BATCH_SIZE) {
        flushTaggingBatch()
    }
}

private suspend fun flushTaggingBatch() {
    if (taggingBatch.isEmpty()) return

    val taggingService = TaggingService(applicationContext)
    taggingService.assignTagsBatch(
        images = taggingBatch,
        onProgress = null // Nepotřebujeme progress pro batch
    )

    taggingBatch.clear()
}
```

---

## 📝 Implementační kroky

### Krok 1: Upravit MediaIndexForegroundService.kt

**Soubor:** `app/src/main/java/com/fpf/smartscan/services/MediaIndexForegroundService.kt`

**Změny:**
1. Přidat dependency na TaggingService
2. Přidat `shouldAutoTag()` funkci
3. Přidat batch processing pro tagging
4. Volat auto-tagging v image indexing loop

**Pseudokód:**
```kotlin
class MediaIndexForegroundService : Service() {
    private val taggingBatch = mutableListOf<Pair<Long, FloatArray>>()
    private val TAGGING_BATCH_SIZE = 50

    override fun onStartCommand(...) {
        serviceScope.launch {
            val shouldTag = shouldAutoTag()
            val taggingService = if (shouldTag) TaggingService(applicationContext) else null

            // ... existující indexing kód ...

            // PŘI KAŽDÉM ZPRACOVANÉM OBRÁZKU:
            val embedding = embeddingHandler.embed(bitmap)

            // Auto-tagging pokud jsou aktivní tagy
            if (taggingService != null) {
                taggingBatch.add(imageId to embedding.embeddings)

                if (taggingBatch.size >= TAGGING_BATCH_SIZE) {
                    taggingService.assignTagsBatch(taggingBatch, onProgress = null)
                    taggingBatch.clear()
                }
            }

            // Uložit embedding (vždy)
            imageStore.add(listOf(embedding))

            // ... pokračování indexování ...

            // NA KONCI: Flush zbylé tagy
            if (taggingBatch.isNotEmpty()) {
                taggingService?.assignTagsBatch(taggingBatch, onProgress = null)
                taggingBatch.clear()
            }
        }
    }

    private suspend fun shouldAutoTag(): Boolean {
        val database = TagDatabase.getDatabase(applicationContext)
        val activeTags = database.userTagDao().getActiveTagsSync()
        return activeTags.isNotEmpty()
    }
}
```

---

### Krok 2: Optimalizace TaggingService

**Současný problém:**
`assignTagsBatch()` volá `assignTags()` v loop, což znamená N * DB queries.

**Řešení:** Skutečný batch insert

```kotlin
// TaggingService.kt
suspend fun assignTagsBatch(
    images: List<Pair<Long, FloatArray>>,
    onProgress: ((Int, Int) -> Unit)? = null
): Int = withContext(Dispatchers.IO) {
    try {
        // Načíst aktivní tagy JEDNOU
        val activeTags = repository.getActiveTagsSync()
        if (activeTags.isEmpty()) return@withContext 0

        // Batch zpracování
        val allImageTags = mutableListOf<ImageTagEntity>()

        images.forEachIndexed { index, (imageId, embedding) ->
            // Najít matching tagy pro tento obrázek
            activeTags.forEach { userTag ->
                val similarity = cosineSimilarity(embedding, userTag.embedding)

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

        // JEDEN batch insert místo N insertů
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

**Zlepšení performance:**
- Před: N obrázků * M tagů * DB queries = **1000 queries** (pro 100 obrázků, 10 tagů)
- Po: 1 read + 1 batch insert = **2 queries** ✅

---

### Krok 3: Notification Update

**Upravit notifikaci během indexování:**

```kotlin
// MediaIndexForegroundService.kt
private fun updateNotification(current: Int, total: Int, isTagging: Boolean) {
    val text = if (isTagging) {
        "Indexing and tagging: $current/$total"
    } else {
        "Indexing: $current/$total"
    }

    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("SmartScan")
        .setContentText(text)
        .setProgress(total, current, false)
        .build()

    notificationManager.notify(NOTIFICATION_ID, notification)
}
```

---

### Krok 4: Settings (Optional - Budoucí rozšíření)

**Přidat nastavení pro kontrolu chování:**

```kotlin
// SettingsScreen.kt - nová sekce
val autoTagDuringIndexing by settingsViewModel.autoTagDuringIndexing.collectAsState()

SwitchPreference(
    title = "Auto-tag během indexování",
    description = "Automaticky přiřadit tagy novým obrázkům při indexování",
    checked = autoTagDuringIndexing,
    onCheckedChange = { enabled ->
        settingsViewModel.setAutoTagDuringIndexing(enabled)
    }
)
```

**Uložení do SharedPreferences:**
```kotlin
// SharedPreferences key
const val KEY_AUTO_TAG_DURING_INDEXING = "auto_tag_during_indexing"

// Default: true (pokud existují tagy)
```

---

## ⚡ Performance analýza

### Současný stav (Manuální re-tagging)
```
Indexování 1000 obrázků:
- CLIP embedding: ~1.5s/obrázek = 25 minut
- Tagging: 0s (skip)
CELKEM: 25 minut

Re-tagging (později):
- Load embeddings: ~0.1s
- Tagging 1000 obrázků: ~15 minut (10 tagů, cosine similarity)
CELKEM: 40 minut (index + retag)
```

### Hybrid přístup
```
První indexování (bez tagů):
- CLIP embedding: ~1.5s/obrázek = 25 minut
- Tagging: 0s (skip - žádné tagy)
CELKEM: 25 minut

Další indexování (100 nových obrázků):
- CLIP embedding: ~1.5s/obrázek = 2.5 minuty
- Tagging (batch 50): ~0.5s/batch = 1 minuta
CELKEM: 3.5 minuty ✅

Změna tagů (re-tagging):
- Stejné jako současný stav: 15 minut
```

**Výsledek:**
- ✅ První indexování: **stejně rychlé**
- ✅ Další indexování: **okamžitě tagováno**
- ✅ Re-tagging: **pouze při změně pravidel**

---

## 🎨 UX Flow

### První spuštění

```
┌─────────────────────────────────────┐
│  SmartScan - První spuštění         │
├─────────────────────────────────────┤
│                                     │
│  Krok 1: Indexování obrázků         │
│  ▓▓▓▓▓▓▓▓▓▓░░░░░ 2,345 / 5,000     │
│  47%                                │
│                                     │
│  ℹ️  Tip: Po indexování můžete      │
│     vytvořit tagy pro automatickou  │
│     kategorizaci.                   │
└─────────────────────────────────────┘

      ↓ Indexování dokončeno

┌─────────────────────────────────────┐
│  ✅ Indexování dokončeno!            │
├─────────────────────────────────────┤
│                                     │
│  Zpracováno: 5,000 obrázků          │
│  Trvání: 1h 25m                     │
│                                     │
│  💡 Chcete vytvořit tagy?           │
│                                     │
│  [Vytvořit tagy]  [Později]         │
└─────────────────────────────────────┘

      ↓ Klikne "Vytvořit tagy"

┌─────────────────────────────────────┐
│  Správa tagů                        │
├─────────────────────────────────────┤
│  📦 Importovat doporučené tagy      │
│     (Explicitní, Selfie, Jídlo...)  │
└─────────────────────────────────────┘

      ↓ Import tagů

┌─────────────────────────────────────┐
│  Správa tagů - 19 tagů              │
├─────────────────────────────────────┤
│  🔄 Re-taggovat všechny obrázky     │
│                                     │
│  💡 Přiřadí tagy všem existujícím   │
│     obrázkům podle pravidel.        │
└─────────────────────────────────────┘

      ↓ Spustí re-tagging

┌─────────────────────────────────────┐
│  🏷️  Přeznačkování obrázků          │
│  (fullscreen RetaggingScreen)       │
└─────────────────────────────────────┘
```

---

### Pravidelné používání

```
Automatické indexování (1 Week interval)
      ↓
┌─────────────────────────────────────┐
│  Notifikace                         │
├─────────────────────────────────────┤
│  SmartScan                          │
│  Indexing and tagging: 47 / 120    │
│  ▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░         │
└─────────────────────────────────────┘
      ↓ Dokončeno
┌─────────────────────────────────────┐
│  Notifikace                         │
├─────────────────────────────────────┤
│  ✅ Indexování dokončeno             │
│  120 nových obrázků automaticky     │
│  přiřazeno do tagů.                 │
└─────────────────────────────────────┘
```

**Uživatel nemusí dělat NIC** - nové obrázky jsou okamžitě tagované! ✅

---

## 🔒 Edge Cases

### 1. Přerušení indexování
**Problém:** Indexování přerušeno (kill app, crash) uprostřed batche

**Řešení:**
- Embeddingy jsou uloženy průběžně ✅
- Tagging batch se ztratí (max 50 obrázků)
- Re-indexování skipne již zpracované embeddingy
- **Workaround:** Re-tagging najde obrázky bez tagů

### 2. Změna tagů během indexování
**Problém:** Uživatel edituje tag BĚHEM běžícího indexování

**Řešení:**
- Indexování načte tagy na ZAČÁTKU
- Nové změny se projeví až při dalším indexování
- **Alternativa:** Re-tagging po změně

### 3. Deaktivace tagu
**Problém:** Uživatel deaktivuje tag, ale obrázky ho stále mají přiřazený

**Řešení:**
- UI filtruje pouze `isActive = true` tagy
- Re-tagging smaže auto-assigned tagy neaktivních tagů
- **Neměnit:** Ponechat user-assigned tagy i když tag deaktivován

### 4. Memory při velkém batchi
**Problém:** 10,000 obrázků v indexování = velký tagging batch

**Řešení:**
- Batch size = 50 (kontrolované memory footprint)
- Flush každých 50 obrázků
- Max memory: 50 obrázků * 10 tagů * ImageTagEntity = ~20KB ✅

---

## 📊 Metriky pro monitoring

### Co trackovat:

```kotlin
data class IndexingMetrics(
    val totalImages: Int,
    val imagesTagged: Int,
    val tagsAssigned: Int,
    val indexingTime: Long,    // ms
    val taggingTime: Long,      // ms
    val autoTagEnabled: Boolean
)
```

### Log při dokončení:

```
✅ Indexing complete
   - Total images: 1,245
   - Auto-tagged: 1,245
   - Tags assigned: 3,456
   - Indexing time: 31m 15s
   - Tagging time: 2m 08s
   - Total: 33m 23s
```

---

## 🚀 Migration Strategy

### Fáze 1: Implementace (bez breaking changes)
- Přidat auto-tagging do MediaIndexForegroundService
- Default: **ENABLED** (pokud existují tagy)
- Současný re-tagging workflow **zůstává beze změn**

### Fáze 2: Testing
- Otestovat na malé sadě (100 obrázků)
- Otestovat batch processing (1000 obrázků)
- Otestovat performance impact
- Otestovat edge cases (přerušení, změna tagů)

### Fáze 3: Rollout
- Beta verze s hybrid auto-taggingem
- Feedback od uživatelů
- Monitoring performance metrics
- Optimalizace podle dat

### Fáze 4: Optional Settings (budoucnost)
- Přidat nastavení "Auto-tag během indexování"
- Přidat nastavení "Batch size" (advanced)
- Přidat statistiky do Settings screen

---

## ✅ Závěr

**Hybrid přístup = Best of both worlds:**

✅ **Rychlé první indexování** (bez tagů)
✅ **Automatické tagování nových obrázků** (pokud tagy existují)
✅ **Flexibilní re-tagging** (při změně pravidel)
✅ **Žádný breaking change** (současný workflow funguje)
✅ **Optimalizovaný performance** (batch processing)
✅ **Intuitivní UX** ("prostě to funguje")

**Doporučení:** Implementovat v další verzi (v1.2.0) jako klíčovou vylepšení UX.

---

## 📝 TODO: Implementační checklist

Když se rozhodneme implementovat:

- [ ] Upravit `MediaIndexForegroundService.kt`
  - [ ] Přidat `shouldAutoTag()` funkce
  - [ ] Přidat batch processing proměnné
  - [ ] Integrovat auto-tagging do image processing loop
  - [ ] Přidat flush batche na konci
  - [ ] Update notifikace s "and tagging" textem

- [ ] Optimalizovat `TaggingService.kt`
  - [ ] Upravit `assignTagsBatch()` na skutečný batch insert
  - [ ] Odstranit loop s `assignTags()` uvnitř
  - [ ] Přidat batch delete pro existující auto-assigned tagy

- [ ] Přidat string resources (EN + CS)
  - [ ] "Indexing and tagging: X/Y"
  - [ ] "X new images automatically tagged"
  - [ ] Tip texty pro první použití

- [ ] Testing
  - [ ] Unit test pro batch tagging
  - [ ] Integration test pro MediaIndexForegroundService
  - [ ] Performance test (1000+ obrázků)
  - [ ] Edge case testy (přerušení, změna tagů)

- [ ] Documentation
  - [ ] Update README.md
  - [ ] Update TODO.md (mark Phase 3 complete)
  - [ ] Changelog entry

- [ ] Optional (budoucnost)
  - [ ] Settings toggle pro auto-tag
  - [ ] Metrics tracking
  - [ ] Statistics screen
