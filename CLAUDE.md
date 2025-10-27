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
