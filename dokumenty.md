# Analýza: Sémantické vyhledávání v textových dokumentech

## 📋 Shrnutí

**Odpověď: ANO, je možné přidat sémantické vyhledávání pro PDF, Markdown a textové soubory do SmartScanu.**

---

## ✅ DETAILNÍ TODO PRO IMPLEMENTACI

### 🎯 FÁZE 1: INFRASTRUKTURA & DEPENDENCIES (Foundation)

#### 1.1 Přidání dependencies
- [ ] **Přidat knihovny do `app/build.gradle.kts`:**
  ```gradle
  // Text embeddings
  implementation("com.github.shubham0204:Sentence-Embeddings-Android:1.0.0")

  // PDF parsing
  implementation("com.tom-roush:pdfbox-android:2.0.27.0")

  // Markdown parsing
  implementation("org.commonmark:commonmark:0.21.0")
  ```
- [ ] **Sync Gradle a ověřit build:** `./gradlew assembleDebug`
- [ ] **Commit:** `🔧 config: Přidání dependencies pro document search`

#### 1.2 Model type extensions
- [ ] **Vytvořit `constants/DocumentModels.kt`:**
  - Enum `DocumentType` (PDF, MARKDOWN, TXT)
  - `DocumentModelInfo` data class
  - Konstanta `TEXT_DOCUMENT_MODEL_PATH`
- [ ] **Rozšířit `data/SmartScanModelType.kt`** (pokud existuje jako enum):
  - Přidat `TEXT_DOCUMENT_ENCODER` typ
- [ ] **Commit:** `✨ feat: Model types pro document embeddings`

#### 1.3 Data models (Room DB + File-based)
- [ ] **Vytvořit `data/documents/TextDocument.kt`:**
  ```kotlin
  @Entity(tableName = "text_documents")
  data class TextDocument(
      @PrimaryKey val id: String,
      val filePath: String,
      val fileName: String,
      val fileType: String,
      val fileSize: Long,
      val chunkCount: Int,
      val indexedAt: Long,
      val lastModified: Long
  )
  ```
- [ ] **Vytvořit `data/documents/DocumentChunk.kt`:**
  ```kotlin
  data class DocumentChunk(
      val id: String,
      val text: String,
      val embedding: FloatArray,
      val metadata: ChunkMetadata
  )

  data class ChunkMetadata(
      val filePath: String,
      val fileName: String,
      val fileType: String,
      val pageNumber: Int?,
      val charOffset: Int,
      val chunkIndex: Int,
      val timestamp: Long
  )
  ```
- [ ] **Vytvořit `data/documents/DocumentScan.kt`:**
  ```kotlin
  @Entity(tableName = "document_scans")
  data class DocumentScan(
      @PrimaryKey(autoGenerate = true) val id: Long = 0,
      val scanDate: Long,
      val documentsFound: Int,
      val documentsIndexed: Int,
      val failedDocuments: Int,
      val duration: Long
  )
  ```
- [ ] **Vytvořit `data/documents/DocumentEmbeddingIndex.kt`:**
  ```kotlin
  @Serializable
  data class DocumentEmbeddingIndex(
      val version: String = "1.0.0",
      val documentCount: Int,
      val chunkCount: Int,
      val lastUpdated: Long,
      val chunks: List<DocumentChunk>
  )
  ```
- [ ] **Commit:** `🗄️ feat: Data models pro document indexing`

#### 1.4 Database setup
- [ ] **Vytvořit `data/documents/DocumentDao.kt`:**
  - `@Dao` interface s CRUD operacemi
  - `insert`, `update`, `delete`, `getAll`, `getByPath`, `getByType`
- [ ] **Vytvořit `data/documents/DocumentScanDao.kt`:**
  - Historie scanů
  - `getRecentScans`, `insertScan`
- [ ] **Rozšířit `AppDatabase.kt`:**
  - Přidat `TextDocument` a `DocumentScan` entities
  - Vytvořit migration z aktuální verze (pravděpodobně v18 → v19)
- [ ] **Vytvořit migration test:**
  - `androidTest/data/MigrationTest.kt`
  - Ověřit upgrade z v18 na v19
- [ ] **Commit:** `🗄️ feat: Database schema pro documents`

#### 1.5 Model download/import
- [ ] **Stáhnout all-MiniLM-L6-v2 ONNX model:**
  - URL: `https://huggingface.co/onnx-models/all-MiniLM-L6-v2-onnx/resolve/main/model.onnx`
  - Target: `app/src/main/assets/models/text_document_encoder.onnx`
- [ ] **Stáhnout tokenizer:**
  - URL: `https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/tokenizer.json`
  - Target: `app/src/main/assets/models/tokenizer.json`
- [ ] **Vytvořit `lib/ModelManager.kt` extension:**
  - `copyTextModelToInternalStorage()`
  - `verifyTextModelExists()`
- [ ] **Test model loading:**
  - Vytvořit unit test v `test/lib/ModelManagerTest.kt`
- [ ] **Commit:** `📦 deps: All-MiniLM-L6-v2 ONNX model + tokenizer`

---

### 🔧 FÁZE 2: DOCUMENT PROCESSING PIPELINE

#### 2.1 Parser Layer
- [ ] **Vytvořit `lib/parsers/DocumentParser.kt` interface:**
  ```kotlin
  interface DocumentParser {
      suspend fun parseDocument(uri: Uri): ParseResult
  }

  data class ParseResult(
      val text: String,
      val pageCount: Int?,
      val metadata: Map<String, String>
  )
  ```
- [ ] **Implementovat `lib/parsers/PdfParser.kt`:**
  - Použít PdfBox-Android (`PDDocument`, `PDFTextStripper`)
  - Page-by-page extraction pro memory efficiency
  - Error handling (corrupted PDFs)
- [ ] **Implementovat `lib/parsers/MarkdownParser.kt`:**
  - CommonMark-Java AST traversal
  - Preserve heading structure
  - Skip/handle code blocks
- [ ] **Implementovat `lib/parsers/TxtParser.kt`:**
  - Simple `File.readText()` s UTF-8 encoding
  - Handle large files (streaming?)
- [ ] **Vytvořit `lib/parsers/ParserFactory.kt`:**
  - Factory method: `getParser(fileType: DocumentType)`
- [ ] **Unit testy:**
  - `test/lib/parsers/PdfParserTest.kt`
  - `test/lib/parsers/MarkdownParserTest.kt`
  - `test/lib/parsers/TxtParserTest.kt`
  - Test s reálnými sample soubory v `test/resources/`
- [ ] **Commit:** `✨ feat: Document parsers (PDF/MD/TXT)`

#### 2.2 Chunking Service
- [ ] **Vytvořit `lib/chunking/TextChunker.kt`:**
  ```kotlin
  interface TextChunker {
      fun chunk(
          text: String,
          maxTokens: Int = 512,
          overlapPercent: Float = 0.15f
      ): List<TextChunk>
  }

  data class TextChunk(
      val text: String,
      val startOffset: Int,
      val endOffset: Int,
      val tokenCount: Int
  )
  ```
- [ ] **Implementovat `lib/chunking/SemanticChunker.kt`:**
  - Sentence splitting (naive: `.split(". ")`; advanced: NLTK-style)
  - Token counting (pomocí tokenizer z model)
  - Overlap logic (keep last N tokens)
  - Handle edge cases (empty chunks, oversized sentences)
- [ ] **Vytvořit `lib/chunking/TokenCounter.kt`:**
  - Wrapper kolem HuggingFace tokenizer
  - `countTokens(text: String): Int`
  - `encodeToTokens(text: String): List<Int>`
- [ ] **Unit testy:**
  - `test/lib/chunking/SemanticChunkerTest.kt`
  - Test overlap correctness
  - Test token count accuracy
  - Test edge cases (very long sentence, empty text)
- [ ] **Commit:** `✨ feat: Semantic text chunking s overlap`

#### 2.3 Text Embedding Extraction
- [ ] **Vytvořit `lib/embeddings/TextEmbeddingExtractor.kt`:**
  ```kotlin
  class TextEmbeddingExtractor(
      private val context: Context
  ) {
      private val model: SentenceEmbedding by lazy {
          SentenceEmbedding(
              modelFile = getModelPath(),
              tokenizerFile = getTokenizerPath(),
              useFP16 = true,
              useXNNPack = true
          )
      }

      suspend fun extractEmbedding(text: String): FloatArray
      suspend fun extractBatch(texts: List<String>): List<FloatArray>
      fun close()
  }
  ```
- [ ] **Implementovat batch processing:**
  - `extractBatch()` s max batch size (10-20 chunks)
  - Memory monitoring
  - Timeout handling
- [ ] **Error handling:**
  - Model not found exception
  - OOM exception (reduce batch size)
  - Invalid input (empty text)
- [ ] **Unit testy:**
  - `test/lib/embeddings/TextEmbeddingExtractorTest.kt`
  - Mock SentenceEmbedding (Mockk)
  - Test batch processing
  - Test error scenarios
- [ ] **Commit:** `✨ feat: Text embedding extraction s batch support`

#### 2.4 Repository Layer
- [ ] **Vytvořit `data/documents/DocumentRepository.kt`:**
  ```kotlin
  class DocumentRepository(
      private val dao: DocumentDao,
      private val context: Context
  ) {
      suspend fun indexDocument(uri: Uri): Result<TextDocument>
      suspend fun getAllDocuments(): Flow<List<TextDocument>>
      suspend fun getDocumentsByType(type: DocumentType): List<TextDocument>
      suspend fun deleteDocument(id: String)
      suspend fun searchDocuments(query: String, topK: Int = 10): List<SearchResult>
  }
  ```
- [ ] **Implementovat file-based embedding storage:**
  - `saveEmbeddingIndex(index: DocumentEmbeddingIndex)`
  - `loadEmbeddingIndex(): DocumentEmbeddingIndex`
  - Location: `context.filesDir/indexes/documents_index.json`
- [ ] **Implementovat search logic:**
  - Load index
  - Query embedding extraction
  - Cosine similarity computation (reuse z image search)
  - Sort by relevance
  - Filter by minSimilarity threshold
- [ ] **Unit testy:**
  - `test/data/documents/DocumentRepositoryTest.kt`
  - Mock DAO a context
  - Test CRUD operations
  - Test search accuracy
- [ ] **Commit:** `✨ feat: Document repository s file-based indexing`

#### 2.5 Background Worker
- [ ] **Vytvořit `workers/DocumentIndexWorker.kt`:**
  ```kotlin
  class DocumentIndexWorker(
      context: Context,
      params: WorkerParameters
  ) : CoroutineWorker(context, params) {
      override suspend fun doWork(): Result {
          // 1. Scan for documents
          // 2. Filter new/modified
          // 3. Parse → Chunk → Embed
          // 4. Save to index
          // 5. Update progress notification
      }
  }
  ```
- [ ] **Implementovat foreground service notification:**
  - `createNotificationChannel()`
  - Progress notification s cancel button
  - Update progress: "Indexování dokumentů... (5/20)"
- [ ] **Implementovat batch processing:**
  - Process 10 documents at a time
  - Clear memory between batches (`System.gc()`)
  - Handle errors (skip corrupted files, log)
- [ ] **Constraints:**
  - Charging preferred (not required)
  - Storage not low
- [ ] **Commit:** `✨ feat: Background document indexing worker`

#### 2.6 Document Scan Service
- [ ] **Vytvořit `services/DocumentScanService.kt`:**
  - Scan specified folders for PDF/MD/TXT
  - Filter by last modified date (skip already indexed)
  - Return list of URIs to index
- [ ] **Implementovat folder picker:**
  - Use SAF (Storage Access Framework)
  - Persist permissions (`takePersistableUriPermission`)
  - Store selected folders v SharedPreferences
- [ ] **Unit testy:**
  - `test/services/DocumentScanServiceTest.kt`
  - Mock ContentResolver
  - Test file filtering logic
- [ ] **Commit:** `✨ feat: Document scan service s folder picker`

---

### 🎨 FÁZE 3: UI & SEARCH INTEGRATION

#### 3.1 Settings Screen Extensions
- [ ] **Rozšířit `ui/screens/settings/SettingsScreen.kt`:**
  - Section: "Document Search"
  - Toggle: Enable/Disable document search
  - Button: "Select Document Folders"
  - Button: "Re-index Documents"
  - Info: Last scan date, document count
- [ ] **Vytvořit `ui/screens/settings/DocumentSettingsViewModel.kt`:**
  - State: `documentSearchEnabled`, `selectedFolders`, `lastScanDate`
  - Actions: `toggleDocumentSearch()`, `addFolder()`, `removeFolder()`, `triggerReindex()`
- [ ] **Implementovat folder picker dialog:**
  - Launch SAF folder picker
  - Display selected folders as chips
  - Remove folder action
- [ ] **Commit:** `✨ feat: Document search settings UI`

#### 3.2 Search Screen Extensions
- [ ] **Rozšířit `ui/screens/search/SearchScreen.kt`:**
  - Tab/Chip filter: "Vše" | "Obrázky" | "Dokumenty"
  - Document result cards (vedle image results)
  - Unified sort by relevance
- [ ] **Vytvořit `ui/components/DocumentResultCard.kt`:**
  ```kotlin
  @Composable
  fun DocumentResultCard(
      result: DocumentSearchResult,
      onClick: () -> Unit
  ) {
      // Display: file icon, title, snippet, relevance score
  }
  ```
- [ ] **Rozšířit `ui/screens/search/SearchViewModel.kt`:**
  - Přidat `searchDocuments()` flow
  - Merge image + document results
  - Filter by selected type
  - State: `documentResults`, `selectedResultType`
- [ ] **Commit:** `✨ feat: Unified search UI (images + documents)`

#### 3.3 Document Viewer
- [ ] **Vytvořit `ui/screens/document/DocumentViewerScreen.kt`:**
  - PDF viewer (použít PdfBox rendering nebo WebView)
  - Markdown renderer (Markwon v TextView)
  - Plain text viewer (ScrollableText)
- [ ] **Vytvořit `ui/screens/document/DocumentViewerViewModel.kt`:**
  - Load document content
  - State: `document`, `currentPage` (pro PDF), `isLoading`
  - Actions: `loadDocument()`, `nextPage()`, `prevPage()`
- [ ] **Implementovat highlight matched chunks:**
  - Find chunk position v dokumentu
  - Scroll to matched section
  - Highlight background color
- [ ] **Navigation:**
  - Rozšířit `constants/Navigation.kt` o `DOCUMENT_VIEWER` route
  - Add to NavHost
- [ ] **Commit:** `✨ feat: Document viewer s PDF/MD/TXT support`

#### 3.4 Search Results Highlighting
- [ ] **Vytvořit `ui/components/HighlightedText.kt`:**
  ```kotlin
  @Composable
  fun HighlightedText(
      text: String,
      highlights: List<IntRange>,
      highlightColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
  )
  ```
- [ ] **Implementovat snippet extraction:**
  - Find best matching chunk
  - Extract ~200 chars around match
  - Add "..." elipsis
- [ ] **Rozšířit `DocumentResultCard`:**
  - Display highlighted snippet
  - Show relevance score (0-100%)
- [ ] **Commit:** `✨ feat: Search result snippet highlighting`

#### 3.5 Indexing Progress UI
- [ ] **Vytvořit `ui/components/DocumentIndexingBanner.kt`:**
  - Zobrazit když indexing běží
  - Progress bar + text "Indexování... (5/20)"
  - Cancel button
- [ ] **Integrovat do `MainActivity.kt`:**
  - Observe WorkManager state
  - Show/hide banner based on worker state
- [ ] **Commit:** `✨ feat: Document indexing progress UI`

---

### 🧪 FÁZE 4: TESTING & OPTIMIZATION

#### 4.1 Unit Tests
- [ ] **Parser tests:**
  - Test všechny parsery s real sample files
  - Test error handling (corrupted files)
- [ ] **Chunking tests:**
  - Test overlap correctness
  - Test token counting accuracy
  - Test edge cases
- [ ] **Embedding tests:**
  - Test batch processing
  - Test memory management
- [ ] **Repository tests:**
  - Test CRUD operations
  - Test search logic
  - Test index persistence
- [ ] **Coverage target:** 80%+ pro core logic
- [ ] **Commit:** `✅ test: Unit tests pro document processing`

#### 4.2 Integration Tests
- [ ] **End-to-end indexing test:**
  - `androidTest/DocumentIndexingTest.kt`
  - Index sample PDF/MD/TXT
  - Verify embeddings saved correctly
  - Verify search returns expected results
- [ ] **Worker test:**
  - Test DocumentIndexWorker s WorkManager TestDriver
  - Verify notification shown
  - Verify progress updates
- [ ] **UI tests:**
  - Test settings screen interactions
  - Test search screen filtering
  - Test document viewer navigation
- [ ] **Commit:** `✅ test: Integration tests pro document search`

#### 4.3 Performance Optimization
- [ ] **Benchmark chunking:**
  - Measure time pro 1000-page PDF
  - Optimize sentence splitting (use regex?)
- [ ] **Benchmark embedding extraction:**
  - Measure time per chunk
  - Tune batch size pro optimal throughput
  - Test FP16 vs FP32 (quality vs speed)
- [ ] **Benchmark search:**
  - Measure query time na 10k chunks
  - Profil cosine similarity (optimize loop?)
- [ ] **Memory profiling:**
  - Monitor peak memory usage během indexování
  - Verify no memory leaks (LeakCanary)
- [ ] **Commit:** `⚡ perf: Document processing optimizations`

#### 4.4 Error Handling & Edge Cases
- [ ] **Graceful degradation:**
  - Model not found → show setup instructions
  - OOM during indexing → reduce batch size
  - Corrupted document → skip a log error
- [ ] **User feedback:**
  - Toast messages pro errors
  - Retry mechanism pro failed documents
  - Error log v Settings (show failed files)
- [ ] **Edge cases:**
  - Empty documents
  - Very large PDFs (1000+ pages)
  - Non-UTF8 text files
  - Scanned PDFs (no text layer)
- [ ] **Commit:** `🐛 fix: Error handling pro document indexing`

---

### 📚 FÁZE 5: DOCUMENTATION & RELEASE

#### 5.1 Code Documentation
- [ ] **Přidat KDoc comments:**
  - Všechny public API funkce
  - Complex algorithms (chunking, similarity)
- [ ] **Update `CLAUDE.md`:**
  - Document search architecture
  - File structure changes
  - Build commands (žádné nové)
- [ ] **Vytvořit `docs/DOCUMENT_SEARCH.md`:**
  - User guide (jak nastavit)
  - Technical overview
  - Troubleshooting
- [ ] **Commit:** `📝 docs: Document search documentation`

#### 5.2 User Guide
- [ ] **In-app help dialog:**
  - "Jak používat vyhledávání v dokumentech?"
  - Screenshot/tutorial steps
- [ ] **Settings tooltips:**
  - Info icon u každého nastavení
  - Explain co dělá
- [ ] **Commit:** `📝 docs: In-app help pro document search`

#### 5.3 Migration & Backward Compatibility
- [ ] **Test upgrade z v1.1.3:**
  - Verify database migration runs
  - Verify existing image embeddings fungují
  - No crashes on first launch
- [ ] **Opt-in feature toggle:**
  - Default: disabled (pro existující users)
  - Show "New Feature" badge v Settings
- [ ] **Commit:** `♻️ refactor: Backward compatible document search`

#### 5.4 Release Preparation
- [ ] **Update `app/build.gradle.kts`:**
  - Bump version: `1.2.0`
  - Update versionCode
- [ ] **Update changelog:**
  - Vytvořit `CHANGELOG.md` entry pro v1.2.0
  - List all new features
- [ ] **F-Droid metadata:**
  - Update `metadata/en-US/changelogs/XX.txt`
  - Add feature description
- [ ] **Test release build:**
  - `./gradlew assembleRelease`
  - Verify ProGuard rules (žádné crashes)
  - Test na real device
- [ ] **Commit:** `🚀 release: SmartScan v1.2.0 - Document Search`

---

### 📊 BONUS: ADVANCED FEATURES (Optional)

#### Bonus 1: OCR Support (pro scanned PDFs)
- [ ] **Integrate Tesseract-Android:**
  - `implementation("com.rmtheis:tess-two:9.1.0")`
- [ ] **Detect scanned PDFs:**
  - Check if text extraction returns empty
  - Render PDF pages to images
  - Run OCR na každou stránku
- [ ] **Commit:** `✨ feat: OCR support pro scanned PDFs`

#### Bonus 2: Multilingual Support
- [ ] **Download multilingual model:**
  - `paraphrase-multilingual-MiniLM-L12-v2` (118 MB)
- [ ] **Language detection:**
  - Use `lingua-kotlin` nebo `fasttext-langdetect`
  - Route to appropriate model
- [ ] **Commit:** `✨ feat: Multilingual document search`

#### Bonus 3: FAISS Integration (pro scale)
- [ ] **Compile FAISS for Android:**
  - NDK build script
  - Wrapper JNI interface
- [ ] **Replace brute-force search:**
  - Use HNSW index
  - 10-100x faster pro large indexes
- [ ] **Commit:** `⚡ perf: FAISS ANN search integration`

#### Bonus 4: Document Auto-Tagging
- [ ] **Vytvořit `DocumentTagger.kt`:**
  - Analyze document content (keywords, topics)
  - Generate tags using text embeddings
  - Cluster similar documents
- [ ] **UI:**
  - Show tags v document cards
  - Filter by tag
- [ ] **Commit:** `✨ feat: Automatic document tagging`

---

## 📈 PROGRESS TRACKING

### Milestones:
- [ ] **M1:** Infrastructure ready (Fáze 1 complete) - ~1 týden
- [ ] **M2:** Processing pipeline working (Fáze 2 complete) - ~2 týdny
- [ ] **M3:** UI integrated (Fáze 3 complete) - ~1 týden
- [ ] **M4:** Tested & optimized (Fáze 4 complete) - ~1 týden
- [ ] **M5:** Release ready (Fáze 5 complete) - ~3 dny

**Estimated total:** ~5-6 týdnů (part-time development)

### Risks:
- ⚠️ **Model size:** Users may complain o +27 MB download
- ⚠️ **Performance:** Indexing může být pomalé na low-end devices
- ⚠️ **English-only:** Multilingual support vyžaduje větší model
- ⚠️ **F-Droid build:** Reproducibility s native libs (tokenizer)

### Dependencies:
- 🔴 **Critical path:** Fáze 1 → Fáze 2 → Fáze 3 (musí být v pořadí)
- 🟡 **Parallel work:** Testing (Fáze 4) může běžet paralelně s UI (Fáze 3)
- 🟢 **Bonus features:** Nezávislé, lze přidat kdykoliv později

---

## 🔍 Zjištění o SmartScan SDK

### Současný stav SDK:
- ❌ **Nepodporuje text embeddings** - pouze image embeddings
- ✅ **CLIP model** - dual-modal (text-to-image search), ale negeneruje pure text embeddings
- ⚠️ **TEXT_ENCODER model typ existuje** v `constants/Models.kt:10` - infrastruktura je připravená
- **Závěr:** Musíme přidat separátní text embedding solution

### Podporované modely v SmartScan SDK:
```kotlin
// constants/Models.kt
SmartScanModelType.FACE           // Facial recognition
SmartScanModelType.OBJECTS        // Object detection
SmartScanModelType.IMAGE_ENCODER  // Image embeddings (CLIP)
SmartScanModelType.TEXT_ENCODER   // Text encoder (CLIP, pro text-to-image)
```

---

## ✅ Technická proveditelnost

| Komponenta | Řešení | Kompatibilita | Velikost |
|-----------|---------|---------------|----------|
| **Text Embeddings** | all-MiniLM-L6-v2 + ONNX Runtime | ✅ Kompatibilní s existujícím stackem | 22 MB |
| **PDF Parsing** | PdfBox-Android (TomRoush fork) | ✅ Pure Java, F-Droid friendly | ~15 MB |
| **Markdown Parsing** | CommonMark-Java / Markwon | ✅ Zero dependencies | ~1 MB |
| **Indexing** | File-based (konzistentní s images) | ✅ Stejný pattern jako v1.1.3 | N/A |
| **Search** | Cosine similarity | ✅ Již implementováno pro images | N/A |

---

## 🎯 Doporučené řešení

### Architektura:

```
Text Documents (PDF/MD/TXT)
        ↓
   Parser Layer
   ├── PdfBox-Android (PDF)
   ├── CommonMark-Java (MD)
   └── File.readText() (TXT)
        ↓
   Chunking Service
   (512 tokens, 15% overlap)
        ↓
   all-MiniLM-L6-v2 ONNX Model
        ↓
   384-dim Embeddings
        ↓
   File-based Index (JSON)
        ↓
   Cosine Similarity Search
```

### Knihovny k přidání:

```gradle
// build.gradle.kts
dependencies {
    // Text embeddings - Sentence Transformers pro Android
    implementation("com.github.shubham0204:Sentence-Embeddings-Android:1.x.x")

    // PDF parsing - Apache PDFBox fork pro Android
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Markdown parsing
    implementation("io.noties.markwon:core:4.6.2")
    // nebo
    implementation("org.commonmark:commonmark:0.21.0")
}
```

---

## 💡 Implementační plán (high-level)

### **Fáze 1: Infrastruktura (Foundation)**
1. ✅ Přidat text embedding dependencies do `build.gradle.kts`
2. ✅ Stáhnout/integrovat all-MiniLM-L6-v2 ONNX model
3. ✅ Rozšířit `SmartScanModelType` enum o `TEXT_DOCUMENT_ENCODER`
4. ✅ Vytvořit data model:
   - `TextDocument` entity (Room DB metadata)
   - `DocumentChunk` data class
   - `DocumentEmbeddingIndex` (file-based storage)

### **Fáze 2: Document Processing**
5. ✅ Implementovat parsery:
   - `PdfParser` (PdfBox-Android)
   - `MarkdownParser` (CommonMark-Java)
   - `TxtParser` (plain text)
6. ✅ Document chunking service:
   - Semantic chunking (split na věty → group)
   - 512 tokens per chunk, 15% overlap
   - Metadata tracking (file path, page, offset)
7. ✅ Text embedding extraction:
   - `TextEmbeddingExtractor` wrapper
   - Batch processing (10 chunks at once)
   - Memory management
8. ✅ Background worker:
   - `DocumentIndexWorker` (WorkManager)
   - Foreground service s notifikací
   - Progress tracking

### **Fáze 3: Search & UI**
9. ✅ Rozšířit search screen:
   - Unified search (images + documents)
   - Document result cards
   - Relevance scoring (cosine similarity)
10. ✅ Document viewer UI:
    - PDF viewer (fragment)
    - Markdown renderer (Markwon)
    - Highlight matched chunks
11. ✅ Settings:
    - Document indexing preferences
    - File type filters
    - Auto-refresh intervals

---

## 🔧 Technické detaily

### 1. Text Embedding Model

**Doporučený model: all-MiniLM-L6-v2**

- **Zdroj:** [HuggingFace - onnx-models/all-MiniLM-L6-v2-onnx](https://huggingface.co/onnx-models/all-MiniLM-L6-v2-onnx)
- **Velikost:** 22 MB (ONNX), 5 MB (tokenizer native lib)
- **Embedding dimension:** 384
- **Performance:** ~60ms per chunk (512 tokens) na mid-range Android
- **Jazyk:** English (pro multilingual použít `paraphrase-multilingual-MiniLM-L12-v2`)

**Implementace (pseudo-code):**
```kotlin
// Inicializace
val sentenceEmbedding = SentenceEmbedding(
    modelFile = context.filesDir.resolve("models/text_encoder.onnx"),
    tokenizerFile = context.filesDir.resolve("models/tokenizer.json"),
    useTokenTypeIds = true,
    outputTensorName = "last_hidden_state",
    useFP16 = true,      // 2x menší storage, <1% quality loss
    useXNNPack = true    // CPU optimization pro ARM
)

// Získání embeddings
val embedding: FloatArray = sentenceEmbedding.encode("Váš text")
// Output: [384] float array
```

**Alternativní modely:**
- **snowflake-arctic-embed-s:** Vyšší kvalita, pomalejší (~100 MB)
- **bge-small-en:** Alternativa k MiniLM (33 MB)
- **model2vec:** Ultra-lightweight (5 MB), nižší kvalita

---

### 2. PDF Parsing

**Knihovna: PdfBox-Android**

- **Repo:** [TomRoush/PdfBox-Android](https://github.com/TomRoush/PdfBox-Android)
- **Licence:** Apache 2.0 (F-Droid compatible)
- **API:** PDDocument, PDFTextStripper

**Best practices:**
```kotlin
// Memory-efficient page-by-page extraction
fun extractPdfText(uri: Uri): List<String> {
    val inputStream = contentResolver.openInputStream(uri)
    val document = PDDocument.load(inputStream)
    val stripper = PDFTextStripper()
    val pages = mutableListOf<String>()

    for (page in 1..document.numberOfPages) {
        stripper.startPage = page
        stripper.endPage = page
        val text = stripper.getText(document)
        pages.add(text)
    }

    document.close()
    return pages
}
```

**Performance poznámky:**
- ⚠️ Kotlin vs Java: ~2s rozdíl v testu (PDFBox 2.0.12)
- ✅ Extrahuj text po stránkách, ne celý dokument najednou
- ✅ Close PDDocument po použití (není thread-safe)

**Alternativy:**
- **iText:** Komerční licence (AGPL pro open-source)
- **PdfiumAndroid:** Nativní, rychlejší, složitější API

---

### 3. Markdown Parsing

**Doporučené knihovny:**

**Option A: Markwon** (Android-native)
- **Repo:** [noties/Markwon](https://github.com/noties/Markwon)
- **Features:** CommonMark spec, HTML support, syntax highlight
- **API Level:** Min 19
- **Use case:** Rendering + text extraction

**Option B: CommonMark-Java** (standalone)
- **Repo:** [commonmark/commonmark-java](https://github.com/commonmark/commonmark-java)
- **Performance:** 10-20x rychlejší než pegdown
- **API:** AST-based, extensible
- **Use case:** Pure text extraction

**Implementace (CommonMark-Java):**
```kotlin
fun extractMarkdownText(markdown: String): String {
    val parser = Parser.builder().build()
    val document = parser.parse(markdown)
    val textBuilder = StringBuilder()

    document.accept(object : AbstractVisitor() {
        override fun visit(text: Text) {
            textBuilder.append(text.literal).append(" ")
        }
        override fun visit(heading: Heading) {
            // Preserve headings for semantic structure
            textBuilder.append("\n\n")
            visitChildren(heading)
            textBuilder.append("\n\n")
        }
    })

    return textBuilder.toString()
}
```

**Plain TXT:**
```kotlin
fun extractPlainText(file: File): String {
    return file.readText(Charset.forName("UTF-8"))
}
```

---

### 4. Chunking Strategie

**Doporučená metoda: Semantic Chunking**

**Parametry:**
- **Chunk size:** 512 tokens (optimal pro all-MiniLM-L6-v2)
- **Overlap:** 15-20% (předejde boundary loss)
- **Granularita:** Split na věty → group po 3-5 větách

**Proč overlap?**
- Informace rozříznutá mezi chunky by byla ztracena
- 15% overlap = poslední ~75 tokenů z předchozího chunku

**Implementace (pseudo-code):**
```kotlin
data class DocumentChunk(
    val id: String,              // "doc1_chunk0"
    val text: String,            // Původní text
    val embedding: FloatArray,   // 384-dim embedding
    val metadata: ChunkMetadata
)

data class ChunkMetadata(
    val filePath: String,
    val pageNumber: Int?,        // Pro PDF
    val charOffset: Int,         // Start position v dokumentu
    val chunkIndex: Int
)

fun chunkDocument(
    text: String,
    maxTokens: Int = 512,
    overlapPercent: Float = 0.15f
): List<DocumentChunk> {
    val sentences = text.splitToSentences()  // NLTK-style
    val chunks = mutableListOf<DocumentChunk>()

    var currentChunk = StringBuilder()
    var currentTokens = 0
    var charOffset = 0

    for (sentence in sentences) {
        val sentenceTokens = tokenizer.encode(sentence).size

        if (currentTokens + sentenceTokens > maxTokens) {
            // Save current chunk
            chunks.add(
                DocumentChunk(
                    id = "doc_chunk${chunks.size}",
                    text = currentChunk.toString(),
                    embedding = embedModel.encode(currentChunk.toString()),
                    metadata = ChunkMetadata(
                        filePath = filePath,
                        pageNumber = currentPage,
                        charOffset = charOffset,
                        chunkIndex = chunks.size
                    )
                )
            )

            // Apply overlap: keep last N tokens
            val overlapSize = (maxTokens * overlapPercent).toInt()
            val overlapText = getLastNTokens(currentChunk.toString(), overlapSize)
            currentChunk = StringBuilder(overlapText)
            currentTokens = overlapSize
        }

        currentChunk.append(sentence).append(" ")
        currentTokens += sentenceTokens
    }

    // Save last chunk
    if (currentChunk.isNotEmpty()) {
        chunks.add(createChunk(currentChunk.toString()))
    }

    return chunks
}
```

**Chunking guidelines podle typu:**

| Document Type | Chunk Size | Overlap | Split Strategy |
|--------------|-----------|---------|----------------|
| PDF (technical) | 512-1024 tokens | 15-25% | Stránky → odstavce → věty |
| Markdown | 256-512 tokens | 10-20% | Headings → odstavce → věty |
| Plain TXT | 256-512 tokens | 10-20% | Odstavce → věty |
| Code snippets | 100-300 tokens | 20% | Function/class scope |

---

### 5. Indexing & Storage

**File-based index (konzistentní s image embeddings od v1.1.3):**

```json
{
  "version": "1.0.0",
  "documentCount": 42,
  "chunks": [
    {
      "id": "doc1_chunk0",
      "text": "This is the first chunk of document 1...",
      "embedding": [0.123, -0.456, 0.789, ...],  // 384 floats
      "metadata": {
        "filePath": "/storage/emulated/0/Documents/report.pdf",
        "fileName": "report.pdf",
        "fileType": "pdf",
        "pageNumber": 1,
        "charOffset": 0,
        "chunkIndex": 0,
        "timestamp": 1704067200000
      }
    }
  ]
}
```

**Storage location:**
```
context.filesDir/
├── models/
│   ├── text_encoder.onnx       (22 MB)
│   ├── tokenizer.json          (5 MB)
│   └── ...
├── indexes/
│   ├── documents_index.json    (embeddings + metadata)
│   └── documents_metadata.db   (Room DB - scan history, jobs)
```

**Room DB schema (pouze metadata):**
```kotlin
@Entity(tableName = "text_documents")
data class TextDocument(
    @PrimaryKey val id: String,
    val filePath: String,
    val fileName: String,
    val fileType: String,      // "pdf", "md", "txt"
    val fileSize: Long,
    val chunkCount: Int,
    val indexedAt: Long,
    val lastModified: Long
)

@Entity(tableName = "document_scans")
data class DocumentScan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanDate: Long,
    val documentsFound: Int,
    val documentsIndexed: Int,
    val duration: Long
)
```

---

### 6. Search Implementation

**Cosine similarity (již existuje pro images):**
```kotlin
fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    require(a.size == b.size) { "Vectors must have same dimension" }

    var dotProduct = 0.0
    var magnitudeA = 0.0
    var magnitudeB = 0.0

    for (i in a.indices) {
        dotProduct += a[i] * b[i]
        magnitudeA += a[i] * a[i]
        magnitudeB += b[i] * b[i]
    }

    magnitudeA = sqrt(magnitudeA)
    magnitudeB = sqrt(magnitudeB)

    return (dotProduct / (magnitudeA * magnitudeB)).toFloat()
}
```

**Search pipeline:**
```kotlin
suspend fun searchDocuments(
    query: String,
    topK: Int = 10,
    minSimilarity: Float = 0.5f
): List<SearchResult> = withContext(Dispatchers.Default) {
    // 1. Generate query embedding
    val queryEmbedding = textEmbedModel.encode(query)

    // 2. Load document index
    val index = loadDocumentIndex()

    // 3. Compute similarities
    val results = index.chunks
        .asSequence()
        .map { chunk ->
            SearchResult(
                chunk = chunk,
                similarity = cosineSimilarity(queryEmbedding, chunk.embedding)
            )
        }
        .filter { it.similarity >= minSimilarity }
        .sortedByDescending { it.similarity }
        .take(topK)
        .toList()

    return@withContext results
}
```

**Unified search (images + documents):**
```kotlin
data class UnifiedSearchResult(
    val type: ResultType,      // IMAGE, DOCUMENT
    val score: Float,
    val data: Any              // ImageResult or DocumentResult
)

suspend fun unifiedSearch(query: String): List<UnifiedSearchResult> {
    // Parallel search
    val imageResults = async { searchImages(query) }
    val docResults = async { searchDocuments(query) }

    // Merge & sort by relevance
    return (imageResults.await() + docResults.await())
        .sortedByDescending { it.score }
}
```

---

## 📊 Performance & Optimizace

### Memory Management

**Model loading:**
```kotlin
// Lazy initialization
private val textEmbedModel: SentenceEmbedding by lazy {
    SentenceEmbedding(
        modelFile = modelPath,
        tokenizerFile = tokenizerPath,
        useFP16 = true,        // 2x menší storage
        useXNNPack = true      // CPU optimization
    )
}

// Clear po použití
fun cleanup() {
    textEmbedModel.close()
}
```

**Batch processing:**
```kotlin
// Process documents in batches
suspend fun indexDocuments(documents: List<File>) {
    val batchSize = 10
    documents.chunked(batchSize).forEach { batch ->
        batch.forEach { doc ->
            val chunks = parseAndChunk(doc)
            chunks.chunked(10).forEach { chunkBatch ->
                // Embed 10 chunks at once
                val embeddings = textEmbedModel.encodeBatch(
                    chunkBatch.map { it.text }
                )
                saveEmbeddings(chunkBatch, embeddings)
            }
        }

        // Clear intermediate buffers
        System.gc()
    }
}
```

### Inference Optimizations

**1. FP16 (Half-precision):**
- Storage: 2x menší
- Quality loss: <1%
- Enable: `useFP16 = true`

**2. XNNPack (CPU optimization):**
- Speedup: 1.4-3x na ARM CPUs
- Enable: `useXNNPack = true`

**3. Quantization (INT8):**
- Storage: 4x menší
- Quality loss: ~5%
- Vyžaduje custom ONNX model

**Benchmark (Samsung M13, 4GB RAM):**
```
Model: all-MiniLM-L6-v2
Input: 128 tokens

Baseline (FP32):         ~90ms
FP16 + XNNPack:         ~60ms
INT8 quantized:         ~45ms
```

---

## ⚠️ Důležité poznámky

### Model Size Impact

| Komponenta | Velikost | Poznámka |
|-----------|----------|----------|
| Stávající CLIP (IMAGE_ENCODER) | ~75 MB | Již v projektu |
| Stávající CLIP (TEXT_ENCODER) | ~75 MB | Již v projektu |
| **Nový text model** | **+22 MB** | all-MiniLM-L6-v2 |
| Tokenizer (Rust native lib) | +5 MB | HuggingFace tokenizers |
| **Celkem** | **~177 MB** | Akceptovatelné pro on-device |

### Performance Expectations

**Indexování (1000-page PDF):**
- Parsing: ~30s (PdfBox-Android)
- Chunking: ~5s (512 tokens/chunk → ~2000 chunks)
- Embedding extraction: ~2 min (60ms × 2000 chunks)
- **Total:** ~2.5 min (background WorkManager job)

**Search (10k chunks index):**
- Query embedding: ~60ms
- Similarity computation: ~50ms (brute-force)
- **Total:** ~110ms per query

**Optimalizace pro větší scale:**
- FAISS index (ANN search): ~5-10ms pro 100k chunks
- Vyžaduje native compile (složitější F-Droid build)

### F-Droid Compatibility

| Knihovna | Licence | F-Droid | Poznámka |
|----------|---------|---------|----------|
| Sentence-Embeddings-Android | Apache 2.0 | ✅ | Open-source, reprodukovatelný build |
| PdfBox-Android | Apache 2.0 | ✅ | Pure Java, zero native deps |
| CommonMark-Java | BSD 2-Clause | ✅ | Zero dependencies |
| ONNX Runtime | MIT | ✅ | Maven Central artifacts |
| HuggingFace Tokenizers | Apache 2.0 | ✅ | Rust → .so prebuilt |

**⚠️ Poznámka k FAISS:**
- ❌ Vyžaduje NDK build (native C++)
- Komplikuje F-Droid reproducible builds
- Doporučení: Start s brute-force, přidej FAISS později pokud nutné

---

## 🔗 Zdroje & Reference

### Implementace

**Text Embeddings:**
- [Sentence-Embeddings-Android GitHub](https://github.com/shubham0204/Sentence-Embeddings-Android)
- [ProAndroidDev článek](https://proandroiddev.com/from-python-to-android-hf-sentence-transformers-embeddings-1ecea0ce94d8)
- [ONNX Runtime Android](https://onnxruntime.ai/docs/get-started/with-java.html)

**PDF Parsing:**
- [PdfBox-Android GitHub](https://github.com/TomRoush/PdfBox-Android)
- [Apache PDFBox Documentation](https://pdfbox.apache.org/)

**Markdown Parsing:**
- [Markwon GitHub](https://github.com/noties/Markwon)
- [CommonMark-Java GitHub](https://github.com/commonmark/commonmark-java)
- [CommonMark Spec](https://commonmark.org/)

**ML Models:**
- [all-MiniLM-L6-v2 (ONNX)](https://huggingface.co/onnx-models/all-MiniLM-L6-v2-onnx)
- [Sentence Transformers Hub](https://huggingface.co/sentence-transformers)
- [ONNX Model Zoo](https://github.com/onnx/models)

### Best Practices

**Chunking Strategies:**
- [Pinecone - Chunking Guide](https://www.pinecone.io/learn/chunking-strategies/)
- [Stack Overflow - RAG Chunking](https://stackoverflow.blog/2024/12/27/breaking-up-is-hard-to-do-chunking-in-rag-applications/)
- [Chroma Research - Evaluating Chunking](https://research.trychroma.com/evaluating-chunking)
- [Late Chunking Paper (2024)](https://arxiv.org/html/2409.04701v3)

**Semantic Search:**
- [OpenAI - Embeddings Guide](https://platform.openai.com/docs/guides/embeddings)
- [Google - Semantic Search Best Practices](https://cloud.google.com/blog/topics/developers-practitioners/meet-ais-multitool-vector-embeddings)

---

## 🚀 Další kroky

### Možnosti pokračování:

**A) Detailní implementační plán**
- Konkrétní TODO items s file paths
- Code snippets pro každou komponentu
- Testovací strategie

**B) Databázový schema design**
- Room entities + DAOs
- Migration strategy z v1.1.3
- Index file format

**C) Proof-of-concept**
- Integrace Sentence-Embeddings-Android
- Minimální PDF parsing demo
- Search prototype

**D) Architecture review**
- Dopad na existující codebase
- Refactoring potřeby
- Backward compatibility

---

**Datum analýzy:** 2025-10-27
**SmartScan verze:** v1.1.3
**Analyzováno pro:** mistře Jardo

---

## 📝 Poznámky k implementaci

### Edge Cases

**1. Multi-language documents:**
- all-MiniLM-L6-v2 = English only
- Pro multilingual: `paraphrase-multilingual-MiniLM-L12-v2` (118 MB)
- Detect language → use appropriate model

**2. PDF s obrázky/tabulkami:**
- PdfBox extracts text v reading order
- Obrázky/tabulky = mezery v textu
- Možnost: OCR integration (Tesseract-Android)

**3. Markdown code blocks:**
- Skip nebo preserve jako single chunk?
- Syntax highlighting metadata
- Language detection

**4. Large documents:**
- Streaming parsing (don't load entire file)
- Progressive indexing (save chunks incrementally)
- Memory pressure monitoring

### Testing Strategy

**Unit tests:**
```kotlin
// Chunking logic
@Test
fun `test semantic chunking with overlap`() { ... }

// Similarity computation
@Test
fun `test cosine similarity calculation`() { ... }

// Parser integration
@Test
fun `test PDF text extraction`() { ... }
```

**Integration tests:**
```kotlin
// End-to-end indexing
@Test
fun `test document indexing pipeline`() { ... }

// Search accuracy
@Test
fun `test search returns relevant results`() { ... }
```

**Performance tests:**
```kotlin
// Benchmark chunking
@Test
fun `benchmark chunk 1000-page PDF`() { ... }

// Benchmark search
@Test
fun `benchmark search in 10k chunks`() { ... }
```

### Migration Path

**Pro existující SmartScan users:**

1. **Opt-in feature** (Settings toggle)
2. **Background migration** (scan documents on first launch)
3. **Progress notification** (jako u image indexing)
4. **Fallback** (graceful degradation pokud modely chybí)

---

## ✅ Závěr

**SmartScan může být rozšířen o sémantické vyhledávání v dokumentech:**

- ✅ Technicky proveditelné (100%)
- ✅ F-Droid compatible stack
- ✅ Konzistentní s existující architekturou
- ✅ Reasonable performance (on-device)
- ✅ Moderate storage impact (+27 MB)

**Doporučení:**
- Start s all-MiniLM-L6-v2 (optimal kvalita/rychlost)
- File-based embeddings storage (konzistence)
- Background WorkManager indexing
- Opt-in feature v Settings

**Rizika:**
- Model download size (+27 MB)
- Indexing time (2-3 min per 1000 pages)
- English-only support (initial version)
- Memory usage (batch processing kritický)

**Celkové hodnocení:** 🟢 **Doporučeno k implementaci**
