# 🐛 Fix: Problémy se zobrazením tagů v GUI

## 🔍 Identifikované problémy

### 1. **loadAvailableTagsWithCounts() zobrazuje VŠECHNY tagy**

**Problém:**
```kotlin
// SearchViewModel.kt řádek 520
tagRepository.allTags.collect { tags ->  // ❌ ŠPATNĚ - všechny tagy včetně neaktivních
```

**Důsledek:**
- V GUI se zobrazují i deaktivované tagy (isActive = 0)
- Uživatel vidí tagy které nemají být aktivní

**Řešení:**
```kotlin
tagRepository.activeTags.collect { tags ->  // ✅ SPRÁVNĚ - pouze aktivní tagy
```

---

### 2. **getImageIdsForTags() používá AND logiku místo správné**

**Problém:**
```sql
-- ImageTagDao.kt řádek 17-18
SELECT DISTINCT imageId
FROM image_tags
WHERE tagName IN (:tagNames)
GROUP BY imageId
HAVING COUNT(DISTINCT tagName) = :tagCount
```

**Co to dělá:**
- Vrací pouze obrázky které mají **PŘESNĚ všechny vybrané tagy**
- Pokud vybereš `["Selfie", "Porn"]` ale obrázek má jen `["Selfie", "Screenshots"]`
  → COUNT = 1 (pouze Selfie se matchne)
  → HAVING COUNT = 2 FAIL ❌

**Scénáře:**

| Vybrané filtry        | Tagy obrázku           | Současný výsledek | Očekávaný výsledek |
|-----------------------|------------------------|-------------------|---------------------|
| `["Selfie"]`          | `["Selfie", "Screenshots"]` | ✅ ZOBRAZÍ     | ✅ ZOBRAZÍ        |
| `["Selfie", "Screenshots"]` | `["Selfie", "Screenshots"]` | ✅ ZOBRAZÍ | ✅ ZOBRAZÍ        |
| `["Selfie", "Porn"]`  | `["Selfie", "Screenshots"]` | ❌ NEZOBRAZÍ  | ✅ ZOBRAZÍ (má Selfie) |

**Problém je v logice:**
- Současná implementace: Obrázek musí mít **VŠECHNY vybrané tagy** (AND)
- Správná logika: Obrázek musí mít **ALESPOŇ JEDEN z vybraných tagů** (OR)

**Řešení - Varianta A (OR logika - DOPORUČENO):**

```kotlin
// ImageTagDao.kt - nová metoda
@Query("""
    SELECT DISTINCT imageId
    FROM image_tags
    WHERE tagName IN (:tagNames)
""")
suspend fun getImageIdsWithAnyTag(tagNames: List<String>): List<Long>
```

**Použití:**
```kotlin
// SearchViewModel.kt
val filteredImageIds = tagRepository.getImageIdsWithAnyTag(
    _selectedTagFilters.value.toList()
).toSet()
```

**Výsledek:**
- Vybereš `["Selfie"]` → zobrazí všechny obrázky s tagem Selfie ✅
- Vybereš `["Selfie", "Porn"]` → zobrazí obrázky s Selfie **NEBO** Porn ✅
- Více intuitivní pro uživatele

---

**Řešení - Varianta B (AND logika zůstane, ale opravená):**

Pokud chceš zachovat AND logiku (zobrazit jen obrázky se VŠEMI vybranými tagy):

```kotlin
// Oprava není potřeba - současný query je správný pro AND
// ALE potřeba přidat OR variantu pro alternativní UX
```

**Doporučuji přidat OBOJÍ a dát uživateli volbu:**

```kotlin
enum class TagFilterMode {
    OR,   // Zobraz obrázky s JAKÝMKOLI vybranými tagem
    AND   // Zobraz obrázky se VŠEMI vybranými tagy
}
```

---

### 3. **Tag counts nezohledňují isActive**

**Problém:**
```kotlin
// SearchViewModel.kt loadAvailableTagsWithCounts()
tagRepository.allTags.collect { tags ->  // Všechny tagy
    val withCounts = tags.map { tag ->
        val count = tagRepository.getImageCountForTag(tag.name)
        tag to count
    }
```

Načítá count i pro neaktivní tagy.

**Řešení:**
```kotlin
tagRepository.activeTags.collect { tags ->  // Pouze aktivní
```

---

## 🛠️ Implementace fixů

### Fix 1: Zobrazovat pouze aktivní tagy

```kotlin
// SearchViewModel.kt řádek 517-532
fun loadAvailableTagsWithCounts() {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            // ✅ OPRAVA: použít activeTags místo allTags
            tagRepository.activeTags.collect { tags ->
                val withCounts = tags.map { tag ->
                    val count = tagRepository.getImageCountForTag(tag.name)
                    tag to count
                }.filter { it.second > 0 }  // Pouze tagy s obrázky

                _availableTagsWithCounts.value = withCounts
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading tags with counts", e)
        }
    }
}
```

### Fix 2: Přidat OR logiku pro tag filtering

```kotlin
// ImageTagDao.kt - přidat novou metodu
@Query("""
    SELECT DISTINCT imageId
    FROM image_tags
    WHERE tagName IN (:tagNames)
""")
suspend fun getImageIdsWithAnyTag(tagNames: List<String>): List<Long>
```

```kotlin
// TagRepository.kt - přidat wrapper
suspend fun getImageIdsWithAnyTag(tagNames: List<String>): List<Long> =
    imageTagDao.getImageIdsWithAnyTag(tagNames)
```

```kotlin
// SearchViewModel.kt - použít novou metodu (řádek 618, 632)
// PŮVODNÍ (AND):
val filteredImageIds = tagRepository.getImageIdsForTags(
    _selectedTagFilters.value.toList()
).toSet()

// NOVÝ (OR):
val filteredImageIds = tagRepository.getImageIdsWithAnyTag(
    _selectedTagFilters.value.toList()
).toSet()
```

### Fix 3: UI toggle pro OR/AND mode (optional - budoucnost)

```kotlin
// SearchViewModel.kt - přidat state
private val _tagFilterMode = MutableStateFlow(TagFilterMode.OR)
val tagFilterMode: StateFlow<TagFilterMode> = _tagFilterMode

fun setTagFilterMode(mode: TagFilterMode) {
    _tagFilterMode.value = mode
    // Re-apply filters
    viewModelScope.launch(Dispatchers.IO) {
        applyTagFilters()
    }
}

// V applyAllFilters() použít podle módu:
val filteredImageIds = when (_tagFilterMode.value) {
    TagFilterMode.OR -> tagRepository.getImageIdsWithAnyTag(
        _selectedTagFilters.value.toList()
    )
    TagFilterMode.AND -> tagRepository.getImageIdsForTags(
        _selectedTagFilters.value.toList()
    )
}.toSet()
```

---

## 📊 Testovací scénáře

### Testovací data:
```
Obrázek 1: ["Selfie", "Screenshots"]
Obrázek 2: ["Porn", "Selfie"]
Obrázek 3: ["Screenshots"]
Obrázek 4: [] (žádné tagy)
```

### Očekávané výsledky (OR logika):

| Vybrané filtry | Zobrazené obrázky | Počet |
|----------------|-------------------|-------|
| `[]` (žádné)   | Všechny search results | N    |
| `["Selfie"]`   | Obrázek 1, 2      | 2     |
| `["Screenshots"]` | Obrázek 1, 3   | 2     |
| `["Selfie", "Porn"]` | Obrázek 1, 2 | 2     |
| `["Selfie", "Screenshots"]` | Obrázek 1, 2, 3 | 3 |

### Očekávané výsledky (AND logika):

| Vybrané filtry | Zobrazené obrázky | Počet |
|----------------|-------------------|-------|
| `[]` (žádné)   | Všechny search results | N    |
| `["Selfie"]`   | Obrázek 1, 2      | 2     |
| `["Screenshots"]` | Obrázek 1, 3   | 2     |
| `["Selfie", "Porn"]` | Obrázek 2   | 1     |
| `["Selfie", "Screenshots"]` | Obrázek 1 | 1 |

---

## 🎯 Doporučená implementace

**PRIORITA 1 - Vysoká:**
1. ✅ Fix 1: `activeTags` místo `allTags` (1 řádek změna)
2. ✅ Fix 2: Přidat OR logiku (default chování)

**PRIORITA 2 - Střední:**
3. 🔵 Přidat toggle OR/AND mode v UI (optional)

**PRIORITA 3 - Nízká:**
4. 🟢 Přidat tooltip/help text vysvětlující rozdíl

---

## 🚀 Akční plán

1. [ ] Opravit `loadAvailableTagsWithCounts()` - activeTags
2. [ ] Přidat `getImageIdsWithAnyTag()` do ImageTagDao
3. [ ] Přidat wrapper do TagRepository
4. [ ] Změnit SearchViewModel na OR logiku
5. [ ] Otestovat s testovacími daty
6. [ ] Build + deploy

---

## 💡 Další poznámky

**Proč OR logika?**
- Intuitivnější pro běžné použití
- "Zobraz mi všechny selfie NEBO pornografii"
- Většina uživatelů očekává OR

**Kdy AND logika?**
- Power user feature
- "Zobraz mi selfies které JSOU ZÁROVEŇ screenshots"
- Narrow down results

**Best UX:**
- Default: OR mode
- Toggle v Search UI: "Match ANY tag" vs "Match ALL tags"
- Tooltips vysvětlující rozdíl
