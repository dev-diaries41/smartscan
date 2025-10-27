# Vylepšení UI pro přeznačkování obrázků

## 🎯 Cíl
Vytvořit fullscreen stránku pro přeznačkování obrázků s real-time statistikami a vizualizací procesu.

## 📋 Současný stav
- ✅ Overlay s progress card v TagManagerScreen
- ✅ Zobrazení current/total + procenta
- ✅ WorkManager progress tracking

## 🚀 Požadované vylepšení

### 1. Fullscreen samostatná stránka (místo overlay)
**Navigace:**
- User klikne "Re-taggovat všechny obrázky" → navigace na novou screen `/retagging`
- Po dokončení → automatický návrat zpět na TagManagerScreen

**Design:**
- Celá obrazovka věnovaná procesu
- Gradient pozadí nebo animovaný pattern
- Centrální progress area

### 2. Real-time statistiky

**Zobrazované metriky:**

```
┌─────────────────────────────────────┐
│   🏷️  Přeznačkování obrázků          │
├─────────────────────────────────────┤
│                                     │
│   Zpracováno obrázků:               │
│   ▓▓▓▓▓▓▓▓▓▓░░░░░ 1,234 / 2,500   │
│   49.4%                             │
│                                     │
│   ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                     │
│   📊 Statistiky:                    │
│   ┌─────────────────────────────┐   │
│   │ ✅ Přiřazeno tagů: 3,456    │   │
│   │ 🏷️  Aktivních tagů: 19      │   │
│   │ ⏱️  Průměrný čas: 0.8s      │   │
│   │ 🚀 Rychlost: ~75 obr/min    │   │
│   └─────────────────────────────┘   │
│                                     │
│   📋 Přiřazené tagy (top 5):        │
│   • Porn - Explicit: 234 obr       │
│   • Screenshots: 156 obr            │
│   • Jídlo: 89 obr                   │
│   • Příroda: 67 obr                 │
│   • Art: 45 obr                     │
│                                     │
└─────────────────────────────────────┘
```

### 3. Datová struktura pro statistiky

**RetaggingWorker by měl reportovat:**
```kotlin
workDataOf(
    "current" to current,              // Aktuální počet zpracovaných
    "total" to total,                  // Celkový počet
    "tagsAssigned" to tagsAssigned,    // Celkem přiřazených tagů
    "activeTagsCount" to activeTags,   // Počet aktivních tagů
    "topTags" to topTagsJson,          // Top 5 tagů jako JSON
    "avgTimePerImage" to avgTime,      // Průměrný čas na obrázek (ms)
    "imagesPerMinute" to speed         // Rychlost zpracování
)
```

### 4. Live aktualizace top tagů

**Zobrazení přiřazených tagů:**
- Top 5 nejčastěji přiřazovaných tagů
- Počet obrázků pro každý tag
- Barevný indikátor podle tagu (color z UserTagEntity)
- Animace při přírůstku čísel

**Příklad:**
```
📋 Přiřazené tagy:
🔴 Porn - Explicit: 234 obrázků
📱 Screenshots: 156 obrázků
🍔 Jídlo: 89 obrázků
🌳 Příroda: 67 obrázků
🎨 Art: 45 obrázků
```

### 5. Progress animace

**LinearProgressIndicator:**
- Smooth animace růstu
- Gradient color (modrá → zelená když se blíží konci)
- Pulse efekt během zpracování

**Dodatečné vizuální prvky:**
- Rotating icon nebo shimmer effect
- Micro-animace při každém +1 obrázku

### 6. Dokončení procesu

**Po dokončení (100%):**
- ✅ Zelený checkmark s animací
- Shrnutí:
  ```
  🎉 Přeznačkování dokončeno!

  ✅ Zpracováno: 2,500 obrázků
  🏷️  Přiřazeno: 8,456 tagů
  ⏱️  Trvání: 2m 15s

  [Zpět na správu tagů]
  ```
- Auto-návrat po 3 sekundách (s odpočítáváním)

### 7. Error handling

**Při chybě:**
- ❌ Červený error stav
- Popis chyby
- Tlačítko "Zkusit znovu" nebo "Zpět"

## 🛠️ Implementační poznámky

### Struktura souborů
```
app/src/main/java/com/fpf/smartscan/
├── ui/screens/retagging/
│   ├── RetaggingScreen.kt          # Fullscreen UI
│   ├── RetaggingViewModel.kt       # StateFlow pro statistiky
│   └── RetaggingStats.kt           # Data class pro stats
└── workers/
    └── RetaggingWorker.kt          # Upravit pro podrobný progress
```

### Navigation
```kotlin
// V Navigation.kt
composable(Routes.RETAGGING) {
    RetaggingScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}

// V TagManagerScreen.kt
onClick = {
    // Spustit worker
    val workRequest = OneTimeWorkRequestBuilder<RetaggingWorker>().build()
    WorkManager.getInstance(context).enqueue(workRequest)

    // Navigovat na retagging screen
    navController.navigate(Routes.RETAGGING)
}
```

### StateFlow pro statistiky
```kotlin
data class RetaggingStats(
    val current: Int = 0,
    val total: Int = 0,
    val tagsAssigned: Int = 0,
    val activeTagsCount: Int = 0,
    val topTags: List<TagStats> = emptyList(),
    val avgTimePerImage: Float = 0f,
    val imagesPerMinute: Float = 0f,
    val isComplete: Boolean = false,
    val error: String? = null
)

data class TagStats(
    val name: String,
    val count: Int,
    val color: Int
)
```

### RetaggingWorker úpravy
```kotlin
// Tracking statistik
private var tagsAssignedTotal = 0
private var startTime = System.currentTimeMillis()
private val tagCountMap = mutableMapOf<String, Int>()

// V onProgress callbacku
val avgTime = (System.currentTimeMillis() - startTime) / current.toFloat()
val imagesPerMin = (current / ((System.currentTimeMillis() - startTime) / 60000f))

setProgressAsync(workDataOf(
    "current" to current,
    "total" to total,
    "tagsAssigned" to tagsAssignedTotal,
    "activeTagsCount" to activeTags.size,
    "topTags" to getTopTagsJson(),
    "avgTimePerImage" to avgTime,
    "imagesPerMinute" to imagesPerMin
))
```

## 🎨 Design inspirace

**Material 3 components:**
- `Card` s elevated elevation pro stats
- `LinearProgressIndicator` s custom styling
- `LazyColumn` pro tag list
- `AnimatedVisibility` pro smooth transitions
- `Text` s weight `Bold` pro čísla

**Color scheme:**
- Primary: Progress bar
- Success (Green): Po dokončení
- Error (Red): Při chybě
- Surface variant: Stats cards

## ✅ Acceptance Criteria

- [ ] Navigace na `/retagging` při spuštění re-taggingu
- [ ] Real-time progress bar (0-100%)
- [ ] Zobrazení current/total obrázků
- [ ] Live statistiky (přiřazené tagy, rychlost, čas)
- [ ] Top 5 tagů s live počítadly
- [ ] Smooth animace přírůstků
- [ ] Success screen po dokončení
- [ ] Auto-návrat po 3s s odpočítáváním
- [ ] Error handling s možností retry
- [ ] WorkManager progress polling každých 500ms

## 📝 Poznámky

- Použít stejný pattern jako MediaIndexForegroundService pro progress tracking
- RetaggingScreen by měl být read-only (žádné user actions během procesu)
- Zabránit navigaci zpět během procesu (disable back button nebo confirmation)
- Po dokončení povolit manuální návrat tlačítkem i auto-návrat
