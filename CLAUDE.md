# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SmartScan je Android aplikace pro organizaci a vyhledávání médií (obrázků a videí) pomocí on-device AI. Aplikace využívá ML modely (CLIP) pro embedding extraction a umožňuje:
- Textové a obrazové vyhledávání v mediální knihovně
- Automatickou organizaci obrázků na základě obsahové podobnosti
- Kompletně offline funkčnost bez potřeby cloudových služeb

**Tech Stack:**
- Kotlin + Jetpack Compose (UI)
- Room Database (pro metadata)
- WorkManager (background jobs)
- ONNX Runtime (ML inference via SmartScan SDK)
- Coil 3 (image loading)
- AndroidX Media3 (video playback)

## Build Commands

### Standardní build operace:
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (s ProGuard optimalizací)
./gradlew assembleRelease

# Install debug APK na připojené zařízení
./gradlew installDebug

# Clean build
./gradlew clean
```

### Testing:
```bash
# Run unit tests (JUnit Jupiter + MockK)
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "com.fpf.smartscan.YourTestClass"
```

### Linting & Code Quality:
```bash
# Run Kotlin lint
./gradlew lint

# Check dependencies
./gradlew dependencies
```

## Architecture & Code Structure

### Hlavní balíčky:

```
com.fpf.smartscan/
├── constants/          # Konstanty pro settings, search, navigation, models
├── data/              # Data layer - Repository pattern
│   ├── images/        # Image embeddings (Room DB) - legacy, nově file-based
│   ├── videos/        # Video embeddings (Room DB) - legacy, nově file-based
│   ├── prototypes/    # Prototype embeddings pro auto-organizaci
│   ├── movehistory/   # Historie přesunutých souborů (undo funkcionalita)
│   ├── scans/         # Scan metadata
│   └── jobs/          # Job tracking pro background workers
├── lib/               # Utility funkce a helpery
├── services/          # Foreground services pro indexování
├── ui/
│   ├── components/    # Reusable Compose komponenty
│   ├── screens/       # Screen-level composables + ViewModels
│   ├── theme/         # Theme management (colors, dark/light mode)
│   └── permissions/   # Permission handling
└── workers/           # WorkManager workers pro background processing
```

### Key Architectural Patterns:

**1. Data Layer - Repository Pattern:**
- Room Database pouze pro metadata (starší data)
- **File-based storage** pro embeddings (od v1.1.3) - rychlejší loading
- Repositories poskytují abstrakci nad data sources
- Použití Flow pro reaktivní updates

**2. Background Processing:**
- `ClassificationBatchWorker` - zpracování obrázků v dávkách pro auto-organizaci
- `MediaIndexForegroundService` - foreground service s notifikací pro indexování
- Batch processing s dynamickou concurrency pro memory management

**3. ML Pipeline:**
- SmartScan SDK (`com.github.dev-diaries41.smartscan-sdk:smartscan-extensions`)
- CLIP model pro image embeddings
- Embeddings stored as file-based index (JSON)
- Cosine similarity pro vyhledávání a organizaci

**4. UI Layer:**
- Jetpack Compose + Material 3
- MVVM pattern (ViewModels + StateFlow)
- Navigation Compose pro screen navigation
- ThemeManager pro centralizované theme handling

### Důležité implementační detaily:

**Memory Management:**
- Dynamická concurrency v workers podle dostupné paměti
- Batch processing místo zpracování všech souborů najednou
- Coil cache konfigurace v MainActivity

**Permission Handling:**
- Minimálně Android 11 (API 30)
- Media permissions (READ_MEDIA_IMAGES, READ_MEDIA_VIDEO)
- Foreground service permissions
- Storage access framework pro folder picking

**Database Migration Note:**
- Od v1.1.3 přechod z Room DB na file-based storage pro embeddings
- Room DB stále používána pro: movehistory, scans, jobs, prototypes
- Legacy embeddings automaticky migrovány při prvním načtení

## Internationalization (i18n)

**Aplikace je plně lokalizovaná - VŽDY používej string resources!**

### String Resources struktura:
```
app/src/main/res/
├── values/strings.xml           # Výchozí jazyk (English)
└── values-cs/strings.xml        # Čeština
```

### ⚠️ KRITICKÉ PRAVIDLO - Používání textů v kódu:

**❌ NIKDY netvrditě nepsat texty přímo v kódu:**
```kotlin
// ❌ ŠPATNĚ - hardcoded text
Text("Vyhledávání")
Button(onClick = {}) { Text("Uložit") }
AlertDialog(title = { Text("Opravdu smazat?") })
```

**✅ VŽDY používat stringResource():**
```kotlin
// ✅ SPRÁVNĚ - použití string resources
import androidx.compose.ui.res.stringResource
import com.fpf.smartscan.R

Text(stringResource(R.string.title_search))
Button(onClick = {}) { Text(stringResource(R.string.action_save)) }
AlertDialog(
    title = { Text(stringResource(R.string.dialog_confirm_delete)) }
)
```

### Workflow pro přidávání nových textů:

**1. Přidej string do OBOU souborů:**

`app/src/main/res/values/strings.xml` (English):
```xml
<string name="your_new_key">Your English text</string>
```

`app/src/main/res/values-cs/strings.xml` (Čeština):
```xml
<string name="your_new_key">Tvůj český text</string>
```

**2. Použij v Compose:**
```kotlin
Text(stringResource(R.string.your_new_key))
```

**3. Pro formátované stringy:**
```xml
<!-- values/strings.xml -->
<string name="welcome_message">Welcome, %1$s!</string>

<!-- values-cs/strings.xml -->
<string name="welcome_message">Vítej, %1$s!</string>
```

```kotlin
// Použití v kódu
Text(stringResource(R.string.welcome_message, userName))
```

### Kategorie string resources:

Stringy jsou organizovány do kategorií s komentáři:
- `<!-- Screen titles -->` - Názvy obrazovek
- `<!-- Actions -->` - Tlačítka a akce (Save, Delete, Cancel, etc.)
- `<!-- Dialogs -->` - Texty dialogů
- `<!-- Notifications -->` - Notifikace
- `<!-- Errors -->` - Chybové hlášky
- `<!-- Settings -->` - Nastavení
- `<!-- Tags -->` - Tagging systém

### Při vytváření nových features:

1. **VŽDY přidej všechny texty do obou strings.xml souborů**
2. **NIKDY nepoužívej hardcoded texty v Kotlin/Compose kódu**
3. **Při refactoringu zkontroluj, zda všechny texty používají stringResource()**
4. **Pro dlouhé texty/multi-line použij CDATA:**
```xml
<string name="long_description"><![CDATA[
    Dlouhý text
    s více řádky
]]></string>
```

### Kontrola před commitem:

```bash
# Zkontroluj, že oba soubory mají stejné klíče:
grep '<string name=' app/src/main/res/values/strings.xml | wc -l
grep '<string name=' app/src/main/res/values-cs/strings.xml | wc -l
# Čísla by měla být stejná!
```

## Development Notes

### Při práci s ML modely:
- Modely jsou stahovány/importovány uživatelem
- Ukládány v internal storage: `context.filesDir/models/`
- Konfigurace v `constants/Models.kt`

### Při práci s indexováním:
- Index refresh je throttled (týdenní/denní podle nastavení)
- Skip již zpracovaných obrázků
- Progress reporting přes SharedPreferences a listeners

### Při práci s WorkManager:
- Použití `setForeground()` pro long-running operace
- Constraints: charging preferováno, ale ne required
- Chaining workers pro sekvenční operace

### F-Droid Compatibility:
- Reproducible builds povoleny (`dependenciesInfo.includeInApk = false`)
- Žádné proprietární závislosti
- Build proces musí být deterministický

### Testing Strategy:
- Unit testy: JUnit Jupiter + MockK
- Instrumented testy: AndroidX Test + Espresso
- `isIncludeAndroidResources = true` pro Robolectric support

## Common Issues & Solutions

### Out of Memory při zpracování velkých obrázků:
- Batch processing je implementován
- Dynamická concurrency
- Skip images after processing

### Video embedding extraction:
- Frame extraction každých X sekund
- Downscaling pro memory efficiency
- Media3 ExoPlayer pro video decoding

### ProGuard:
- Release builds používají ProGuard
- Rules v `app/proguard-rules.pro`
- ONNX runtime a ML modely vyžadují keep rules
