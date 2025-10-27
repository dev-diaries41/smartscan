# 🔍 Diagnostika: Proč se nepřiřazují tagy?

## ❓ Možné příčiny

### 1. Žádné aktivní tagy (isActive = 0)
**Kontrola:**
```sql
-- V Room Inspector nebo přes adb shell:
SELECT name, isActive, threshold FROM user_tags;
```

**Očekávaný výsledek:**
```
name              | isActive | threshold
------------------|----------|----------
Porn - Explicit   | 1        | 0.30
Selfie           | 1        | 0.25
Screenshots      | 1        | 0.35
...
```

**Pokud všechny isActive = 0:**
- ❌ Tagy jsou deaktivované!
- ✅ **Fix:** Aktivovat tagy v TagEditScreen

---

### 2. Similarity je POD threshold

**Problém:** Cosine similarity je moc nízká.

**Kontrola - přidat dočasný log:**

```kotlin
// TaggingService.kt, řádek 65
activeTags.forEach { userTag ->
    val similarity = cosineSimilarity(imageEmbedding, userTag.embedding)

    // ⭐ PŘIDAT DEBUG LOG
    Log.d(TAG, "Tag '${userTag.name}': similarity=$similarity, threshold=${userTag.threshold}")

    if (similarity >= userTag.threshold) {
        // ... přiřazení
    }
}
```

**Očekávaný výstup v logcat:**
```
TaggingService: Tag 'Porn - Explicit': similarity=0.45, threshold=0.30  ✅ PASS
TaggingService: Tag 'Selfie': similarity=0.15, threshold=0.25           ❌ FAIL
TaggingService: Tag 'Screenshots': similarity=0.38, threshold=0.35      ✅ PASS
```

**Pokud similarity je vždy <0.1:**
- ❌ Embeddingy nejsou kompatibilní!
- ❌ Tag embedding je špatný
- ❌ Image embedding je špatný

---

### 3. Tag embedding je prázdný nebo null

**Kontrola:**

```kotlin
// TaggingService.kt, řádek 48
val activeTags = repository.getActiveTagsSync()

// ⭐ PŘIDAT DEBUG LOG
Log.d(TAG, "Active tags count: ${activeTags.size}")
activeTags.forEach { tag ->
    Log.d(TAG, "Tag '${tag.name}': embedding size=${tag.embedding.size}, first=${tag.embedding.firstOrNull()}")
}
```

**Očekávaný výstup:**
```
TaggingService: Active tags count: 15
TaggingService: Tag 'Porn - Explicit': embedding size=512, first=0.0234
TaggingService: Tag 'Selfie': embedding size=512, first=-0.0156
...
```

**Pokud embedding size = 0 nebo first = null:**
- ❌ Tag embedding nebyl vytvořen!
- ✅ **Fix:** Re-generovat embeddings v TagEditScreen

---

### 4. Image embedding není v FileEmbeddingStore

**Kontrola:**

```kotlin
// RetaggingWorker.kt, řádek 59-72
val imageStore = FileEmbeddingStore(...)

if (!imageStore.exists) {
    Log.w(TAG, "No image index found")
}

val embeddings = imageStore.get()
Log.d(TAG, "Loaded ${embeddings.size} embeddings from store")

// ⭐ ZKONTROLOVAT PRVNÍ EMBEDDING
if (embeddings.isNotEmpty()) {
    val first = embeddings.first()
    Log.d(TAG, "First embedding: id=${first.id}, size=${first.embeddings.size}")
}
```

**Očekávaný výstup:**
```
RetaggingWorker: Loaded 5432 embeddings from store
RetaggingWorker: First embedding: id=12345, size=512
```

**Pokud embeddings.size = 0:**
- ❌ Index není vytvořen!
- ✅ **Fix:** Spustit indexování v Settings

---

### 5. Cosine similarity vrací NaN nebo špatné hodnoty

**Kontrola:**

```kotlin
// TaggingService.kt, cosineSimilarity funkce
private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    require(a.size == b.size) {
        "Vectors must have same length (a=${a.size}, b=${b.size})"
    }

    var dotProduct = 0f
    var normA = 0f
    var normB = 0f

    for (i in a.indices) {
        dotProduct += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }

    val denominator = sqrt(normA) * sqrt(normB)

    // ⭐ PŘIDAT DEBUG LOG
    val result = if (denominator > 0f) {
        dotProduct / denominator
    } else {
        Log.w(TAG, "Zero denominator in cosine similarity! normA=$normA, normB=$normB")
        0f
    }

    // ⭐ DETEKCE NaN
    if (result.isNaN()) {
        Log.e(TAG, "NaN result! dotProduct=$dotProduct, normA=$normA, normB=$normB")
        return 0f
    }

    return result
}
```

**Pokud vidíš "Zero denominator":**
- ❌ Embedding vector je nulový (všechny hodnoty = 0)
- ❌ Model vrátil prázdný výstup

**Pokud vidíš "NaN result":**
- ❌ Floating point overflow/underflow
- ❌ Neplatné hodnoty v embeddingu

---

## 🛠️ Debugging Workflow

### Krok 1: Zjisti kolik máš aktivních tagů

```kotlin
// Přidej do RetaggingWorker.doWork(), před volání retagAllImages
val database = TagDatabase.getDatabase(applicationContext)
val allTags = database.userTagDao().getActiveTagsSync()
Log.d(TAG, "===== TAGGING DEBUG =====")
Log.d(TAG, "Active tags: ${allTags.size}")
allTags.forEach { tag ->
    Log.d(TAG, "  - ${tag.name}: isActive=${tag.isActive}, threshold=${tag.threshold}, embeddingSize=${tag.embedding.size}")
}
```

### Krok 2: Zkontroluj image embeddings

```kotlin
// Přidej do RetaggingWorker.doWork(), po načtení embeddings
Log.d(TAG, "Image embeddings: ${embeddings.size}")
if (embeddings.isNotEmpty()) {
    val sample = embeddings.take(3)
    sample.forEach { emb ->
        Log.d(TAG, "  - Image ${emb.id}: size=${emb.embeddings.size}, first=${emb.embeddings.firstOrNull()}")
    }
}
```

### Krok 3: Sleduj similarity scores

```kotlin
// Přidej do TaggingService.assignTags(), před if (similarity >= threshold)
Log.d(TAG, "Image $imageId vs '${userTag.name}': similarity=${"%.4f".format(similarity)} (threshold=${userTag.threshold})")
```

### Krok 4: Zkontroluj jestli se volá assignTags

```kotlin
// Přidej na začátek TaggingService.assignTags()
Log.d(TAG, "assignTags() called for image $imageId")
```

---

## 📊 Očekávané výsledky (když funguje správně)

```
RetaggingWorker: Starting image re-tagging
RetaggingWorker: ===== TAGGING DEBUG =====
RetaggingWorker: Active tags: 15
RetaggingWorker:   - Porn - Explicit: isActive=true, threshold=0.3, embeddingSize=512
RetaggingWorker:   - Selfie: isActive=true, threshold=0.25, embeddingSize=512
RetaggingWorker:   - Screenshots: isActive=true, threshold=0.35, embeddingSize=512
...
RetaggingWorker: Image embeddings: 5432
RetaggingWorker:   - Image 12345: size=512, first=0.0234
RetaggingWorker:   - Image 12346: size=512, first=-0.0156
RetaggingWorker:   - Image 12347: size=512, first=0.0891

TaggingService: assignTags() called for image 12345
TaggingService: Image 12345 vs 'Porn - Explicit': similarity=0.4523 (threshold=0.3)     ✅
TaggingService: Image 12345 vs 'Selfie': similarity=0.1234 (threshold=0.25)            ❌
TaggingService: Image 12345 vs 'Screenshots': similarity=0.3891 (threshold=0.35)        ✅
TaggingService: Assigned 2 tags to image 12345

TaggingService: assignTags() called for image 12346
...
```

---

## 🔧 Rychlé Fix-y

### Fix 1: Žádné aktivní tagy
```kotlin
// V TagManagerScreen, po importu preset tagů:
// Ujistit se že isActive = true při vytváření
val newTag = UserTagEntity(
    name = name,
    description = description,
    embedding = embedding,
    threshold = threshold,
    isActive = true,  // ⭐ DŮLEŽITÉ!
    color = color,
    createdAt = System.currentTimeMillis()
)
```

### Fix 2: Threshold je příliš vysoký
```kotlin
// Snížit threshold v TagEditScreen
// Doporučené hodnoty:
// - Explicitní obsah: 0.25-0.35
// - Selfie: 0.20-0.30
// - Screenshots: 0.30-0.40
// - Obecné kategorie: 0.15-0.25
```

### Fix 3: Embeddings chybí
```kotlin
// Spustit indexování v Settings
// Nebo manuálně:
val intent = Intent(context, MediaIndexForegroundService::class.java)
intent.putExtra(MediaIndexForegroundService.EXTRA_MEDIA_TYPE, "image")
context.startForegroundService(intent)
```

---

## 🎯 Diagnostický Checklist

- [ ] **Aktivní tagy existují** (isActive = 1)
- [ ] **Tag embeddings jsou vyplněné** (size = 512, not null)
- [ ] **Image embeddings existují** (FileEmbeddingStore není prázdný)
- [ ] **Image embedding size = 512** (ne 0, ne null)
- [ ] **Threshold je rozumný** (0.15 - 0.40)
- [ ] **Cosine similarity se počítá** (ne NaN, ne 0.0 vždy)
- [ ] **assignTags() se volá** (log viditelný)
- [ ] **Similarity >= threshold** alespoň pro nějaké tagy

---

## 💡 Nejpravděpodobnější příčiny (podle zkušenosti)

1. **80% pravděpodobnost:** Tagy mají `isActive = 0` (deaktivované)
2. **15% pravděpodobnost:** Threshold je příliš vysoký (similarity pod threshold)
3. **4% pravděpodobnost:** Tag embeddings nejsou vytvořené (null nebo prázdné)
4. **1% pravděpodobnost:** Bug v cosine similarity (NaN, division by zero)

---

## 🚀 Akční plán

1. **Přidat debug logy** podle Krok 1-4 výše
2. **Spustit re-tagging** a sledovat logcat
3. **Identifikovat problém** podle výstupu
4. **Aplikovat fix** podle zjištění
5. **Odstranit debug logy** po vyřešení
