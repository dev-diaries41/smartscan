# Analýza: Sémantické vyhledávání v textových dokumentech

## 📋 Shrnutí

**Odpověď: ANO, je možné přidat sémantické vyhledávání pro PDF, Markdown a textové soubory do SmartScanu.**

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
