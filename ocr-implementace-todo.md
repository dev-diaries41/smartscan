# TODO: OCR Multi-Vector Implementation

## 📋 Přehled

**Cíl:** Implementovat OCR embeddings s multi-vector search pro SmartScan v1.3.0

**Architektura:** Multi-Vector Retriever Pattern
- Visual embeddings (512-dim) + OCR embeddings (384-dim) stored separately
- Weighted fusion search (alpha=0.7)
- Background OCR processing s pre-detection
- File-based embeddings + Room DB metadata

**Odhadovaný čas:** 2-3 týdny (part-time development)

**Dependencies:**
- ✅ v1.2.0 Document Search (MiniLM-L6-v2 model)
- ✅ ONNX Runtime (už máš)
- ✅ File-based embeddings infrastructure (v1.1.3)

---

## 🎯 FÁZE 1: INFRASTRUKTURA & DEPENDENCIES

### 1.1 Přidání PaddleOCR dependency

- [ ] **Přidat PaddleOCR ONNX knihovnu do `app/build.gradle.kts`:**
  ```gradle
  dependencies {
      // PaddleOCR - ultra-lightweight OCR (2.8 MB)
      implementation("com.github.rapidai:RapidOcrOnnxJvm:1.2.0")
      // nebo přímá integrace ONNX modelu

      // Text embeddings (už budeš mít z v1.2.0)
      implementation("com.github.shubham0204:Sentence-Embeddings-Android:1.0.0")
  }
  ```

- [ ] **Sync Gradle a ověřit build:** `./gradlew assembleDebug`

- [ ] **Commit:** `🔧 config: Přidání PaddleOCR dependency pro OCR embeddings`

---

### 1.2 PaddleOCR model download

- [ ] **Stáhnout PaddleOCR ONNX modely:**
  - Detection model: `ch_PP-OCRv4_det_infer.onnx` (~3 MB)
  - Recognition model: `ch_PP-OCRv4_rec_infer.onnx` (~10 MB)
  - Classifier model (optional): `ch_ppocr_mobile_v2.0_cls_infer.onnx` (~1 MB)

  **URLs:**
  - [PaddleOCR Model Zoo](https://github.com/PaddlePaddle/PaddleOCR/blob/main/doc/doc_en/models_list_en.md)
  - [RapidAI ONNX Models](https://github.com/RapidAI/RapidOcrOnnx)

- [ ] **Uložit modely do `app/src/main/assets/models/ocr/`:**
  ```
  app/src/main/assets/models/ocr/
  ├── det_model.onnx        # Text detection
  ├── rec_model.onnx        # Text recognition
  └── cls_model.onnx        # Orientation classifier (optional)
  ```

- [ ] **Vytvořit `constants/OCRModels.kt`:**
  ```kotlin
  object OCRModels {
      const val OCR_DIR = "models/ocr"
      const val DETECTION_MODEL = "det_model.onnx"
      const val RECOGNITION_MODEL = "rec_model.onnx"
      const val CLASSIFIER_MODEL = "cls_model.onnx"

      const val MODEL_VERSION = "paddleocr-v4"
  }
  ```

- [ ] **Test model loading:**
  - Unit test v `test/lib/ocr/OCRModelLoaderTest.kt`

- [ ] **Commit:** `📦 deps: PaddleOCR ONNX models (detection + recognition)`

---

### 1.3 Database schema extension

- [ ] **Vytvořit `data/ocr/OCRData.kt` entity:**
  ```kotlin
  package com.fpf.smartscan.data.ocr

  import androidx.room.ColumnInfo
  import androidx.room.Entity
  import androidx.room.ForeignKey
  import androidx.room.PrimaryKey
  import com.fpf.smartscan.data.images.ImageEntity

  @Entity(
      tableName = "ocr_data",
      foreignKeys = [
          ForeignKey(
              entity = ImageEntity::class,
              parentColumns = ["id"],
              childColumns = ["image_id"],
              onDelete = ForeignKey.CASCADE
          )
      ]
  )
  data class OCRData(
      @PrimaryKey
      @ColumnInfo(name = "image_id")
      val imageId: String,

      @ColumnInfo(name = "extracted_text")
      val extractedText: String,

      @ColumnInfo(name = "language")
      val language: String? = null,  // 'en', 'cs', 'auto'

      @ColumnInfo(name = "confidence")
      val confidence: Float,  // 0.0-1.0

      @ColumnInfo(name = "has_text")
      val hasText: Boolean,

      @ColumnInfo(name = "word_count")
      val wordCount: Int,

      @ColumnInfo(name = "created_at")
      val createdAt: Long,

      @ColumnInfo(name = "model_version")
      val modelVersion: String
  )
  ```

- [ ] **Vytvořit `data/ocr/OCRDataDao.kt`:**
  ```kotlin
  package com.fpf.smartscan.data.ocr

  import androidx.room.*
  import kotlinx.coroutines.flow.Flow

  @Dao
  interface OCRDataDao {
      @Query("SELECT * FROM ocr_data WHERE has_text = 1")
      fun getAllImagesWithText(): Flow<List<OCRData>>

      @Query("SELECT * FROM ocr_data WHERE image_id = :imageId")
      suspend fun getOCRData(imageId: String): OCRData?

      @Query("SELECT * FROM ocr_data WHERE image_id IN (:imageIds)")
      suspend fun getOCRDataBatch(imageIds: List<String>): List<OCRData>

      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun insertOCRData(ocrData: OCRData)

      @Insert(onConflict = OnConflictStrategy.REPLACE)
      suspend fun insertOCRDataBatch(ocrDataList: List<OCRData>)

      @Query("DELETE FROM ocr_data WHERE image_id = :imageId")
      suspend fun deleteOCRData(imageId: String)

      @Query("SELECT COUNT(*) FROM ocr_data WHERE has_text = 1")
      suspend fun getImagesWithTextCount(): Int

      @Query("SELECT COUNT(*) FROM ocr_data")
      suspend fun getTotalProcessedCount(): Int

      @Query("SELECT * FROM ocr_data WHERE language = :language AND has_text = 1")
      fun getByLanguage(language: String): Flow<List<OCRData>>
  }
  ```

- [ ] **Rozšířit `AppDatabase.kt`:**
  ```kotlin
  @Database(
      entities = [
          // Existing entities...
          ImageEntity::class,
          VideoEntity::class,
          // New entity
          OCRData::class
      ],
      version = 19,  // Increment from current version
      exportSchema = true
  )
  abstract class AppDatabase : RoomDatabase() {
      // Existing DAOs...
      abstract fun imageDao(): ImageDao
      abstract fun videoDao(): VideoDao

      // New DAO
      abstract fun ocrDataDao(): OCRDataDao
  }
  ```

- [ ] **Vytvořit migration v `data/migrations/Migration_18_19.kt`:**
  ```kotlin
  package com.fpf.smartscan.data.migrations

  import androidx.room.migration.Migration
  import androidx.sqlite.db.SupportSQLiteDatabase

  val MIGRATION_18_19 = object : Migration(18, 19) {
      override fun migrate(database: SupportSQLiteDatabase) {
          // Create ocr_data table
          database.execSQL("""
              CREATE TABLE IF NOT EXISTS ocr_data (
                  image_id TEXT PRIMARY KEY NOT NULL,
                  extracted_text TEXT NOT NULL,
                  language TEXT,
                  confidence REAL NOT NULL,
                  has_text INTEGER NOT NULL,
                  word_count INTEGER NOT NULL,
                  created_at INTEGER NOT NULL,
                  model_version TEXT NOT NULL,
                  FOREIGN KEY(image_id) REFERENCES images(id) ON DELETE CASCADE
              )
          """.trimIndent())

          // Create indexes
          database.execSQL("""
              CREATE INDEX IF NOT EXISTS idx_ocr_has_text
              ON ocr_data(has_text)
          """.trimIndent())

          database.execSQL("""
              CREATE INDEX IF NOT EXISTS idx_ocr_language
              ON ocr_data(language)
          """.trimIndent())
      }
  }
  ```

- [ ] **Přidat migration do `AppDatabase` companion object:**
  ```kotlin
  companion object {
      @Volatile
      private var INSTANCE: AppDatabase? = null

      fun getDatabase(context: Context): AppDatabase {
          return INSTANCE ?: synchronized(this) {
              val instance = Room.databaseBuilder(
                  context.applicationContext,
                  AppDatabase::class.java,
                  "smartscan_database"
              )
              .addMigrations(
                  // Existing migrations...
                  MIGRATION_18_19
              )
              .build()
              INSTANCE = instance
              instance
          }
      }
  }
  ```

- [ ] **Vytvořit migration test v `androidTest/data/MigrationTest.kt`:**
  ```kotlin
  @RunWith(AndroidJUnit4::class)
  class MigrationTest {
      @Test
      fun migrate18To19() {
          // Test migration
          val db = helper.createDatabase(TEST_DB, 18)

          // Insert test data in v18
          db.execSQL("INSERT INTO images VALUES (...)")

          // Run migration
          db.close()
          helper.runMigrationsAndValidate(TEST_DB, 19, true, MIGRATION_18_19)

          // Verify ocr_data table exists
          val dbV19 = helper.runMigrationsAndValidate(TEST_DB, 19, true)
          val cursor = dbV19.query("SELECT * FROM ocr_data")
          // Assert table structure
      }
  }
  ```

- [ ] **Commit:** `🗄️ feat: Database schema pro OCR data (migration v18→v19)`

---

### 1.4 File-based embeddings extension

- [ ] **Rozšířit `data/embeddings/EmbeddingIndex.kt`:**
  ```kotlin
  package com.fpf.smartscan.data.embeddings

  import kotlinx.serialization.Serializable

  @Serializable
  data class MultiVectorIndex(
      val version: String = "1.0.0",
      val lastUpdated: Long,
      val imageCount: Int,

      // Visual embeddings (already exists from v1.1.3)
      val visualEmbeddings: Map<String, VisualEmbedding>,

      // OCR embeddings (new)
      val ocrEmbeddings: Map<String, OCREmbedding>
  )

  @Serializable
  data class VisualEmbedding(
      val imageId: String,
      val embedding: List<Float>,  // 512-dim
      val dimension: Int = 512,
      val modelVersion: String = "clip-vit-b-32",
      val createdAt: Long
  )

  @Serializable
  data class OCREmbedding(
      val imageId: String,
      val embedding: List<Float>,  // 384-dim
      val dimension: Int = 384,
      val modelVersion: String = "all-MiniLM-L6-v2",
      val createdAt: Long
  )
  ```

- [ ] **Vytvořit `data/embeddings/MultiVectorRepository.kt`:**
  ```kotlin
  package com.fpf.smartscan.data.embeddings

  import android.content.Context
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext
  import kotlinx.serialization.json.Json
  import java.io.File

  class MultiVectorRepository(private val context: Context) {

      private val embeddingsDir = File(context.filesDir, "embeddings")
      private val json = Json { prettyPrint = true }

      init {
          embeddingsDir.mkdirs()
      }

      suspend fun loadMultiVectorIndex(): MultiVectorIndex = withContext(Dispatchers.IO) {
          val visualFile = File(embeddingsDir, "visual_embeddings.json")
          val ocrFile = File(embeddingsDir, "ocr_embeddings.json")

          val visualData = if (visualFile.exists()) {
              json.decodeFromString<Map<String, VisualEmbedding>>(visualFile.readText())
          } else emptyMap()

          val ocrData = if (ocrFile.exists()) {
              json.decodeFromString<Map<String, OCREmbedding>>(ocrFile.readText())
          } else emptyMap()

          MultiVectorIndex(
              lastUpdated = System.currentTimeMillis(),
              imageCount = visualData.size,
              visualEmbeddings = visualData,
              ocrEmbeddings = ocrData
          )
      }

      suspend fun saveOCREmbedding(
          imageId: String,
          embedding: FloatArray
      ) = withContext(Dispatchers.IO) {
          val ocrFile = File(embeddingsDir, "ocr_embeddings.json")

          // Load existing
          val existing = if (ocrFile.exists()) {
              json.decodeFromString<Map<String, OCREmbedding>>(ocrFile.readText())
          } else emptyMap()

          // Add new
          val updated = existing.toMutableMap()
          updated[imageId] = OCREmbedding(
              imageId = imageId,
              embedding = embedding.toList(),
              createdAt = System.currentTimeMillis()
          )

          // Save
          ocrFile.writeText(json.encodeToString(updated))
      }

      suspend fun saveOCREmbeddingBatch(
          embeddings: Map<String, FloatArray>
      ) = withContext(Dispatchers.IO) {
          val ocrFile = File(embeddingsDir, "ocr_embeddings.json")

          // Load existing
          val existing = if (ocrFile.exists()) {
              json.decodeFromString<Map<String, OCREmbedding>>(ocrFile.readText())
          } else emptyMap()

          // Merge
          val updated = existing.toMutableMap()
          val timestamp = System.currentTimeMillis()
          embeddings.forEach { (imageId, embedding) ->
              updated[imageId] = OCREmbedding(
                  imageId = imageId,
                  embedding = embedding.toList(),
                  createdAt = timestamp
              )
          }

          // Save
          ocrFile.writeText(json.encodeToString(updated))
      }

      suspend fun deleteOCREmbedding(imageId: String) = withContext(Dispatchers.IO) {
          val ocrFile = File(embeddingsDir, "ocr_embeddings.json")
          if (!ocrFile.exists()) return@withContext

          val existing = json.decodeFromString<Map<String, OCREmbedding>>(ocrFile.readText())
          val updated = existing.filterKeys { it != imageId }
          ocrFile.writeText(json.encodeToString(updated))
      }
  }
  ```

- [ ] **Unit test v `test/data/embeddings/MultiVectorRepositoryTest.kt`:**
  ```kotlin
  @Test
  fun `save and load OCR embedding`() = runBlocking {
      val repo = MultiVectorRepository(context)
      val testEmbedding = FloatArray(384) { Random.nextFloat() }

      // Save
      repo.saveOCREmbedding("test_image_1", testEmbedding)

      // Load
      val index = repo.loadMultiVectorIndex()

      // Assert
      assertTrue(index.ocrEmbeddings.containsKey("test_image_1"))
      assertEquals(384, index.ocrEmbeddings["test_image_1"]?.dimension)
  }
  ```

- [ ] **Commit:** `✨ feat: File-based storage pro OCR embeddings`

---

## 🔧 FÁZE 2: OCR PROCESSING PIPELINE

### 2.1 OCR Engine wrapper

- [ ] **Vytvořit `lib/ocr/OCREngine.kt` interface:**
  ```kotlin
  package com.fpf.smartscan.lib.ocr

  import android.graphics.Bitmap

  interface OCREngine {
      suspend fun extractText(bitmap: Bitmap): OCRResult
      suspend fun extractTextBatch(bitmaps: List<Bitmap>): List<OCRResult>
      fun close()
  }

  data class OCRResult(
      val text: String,
      val confidence: Float,
      val language: String?,
      val wordCount: Int,
      val boundingBoxes: List<BoundingBox> = emptyList()
  )

  data class BoundingBox(
      val text: String,
      val x: Int,
      val y: Int,
      val width: Int,
      val height: Int,
      val confidence: Float
  )
  ```

- [ ] **Implementovat `lib/ocr/PaddleOCREngine.kt`:**
  ```kotlin
  package com.fpf.smartscan.lib.ocr

  import android.content.Context
  import android.graphics.Bitmap
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext
  import java.io.File

  class PaddleOCREngine(private val context: Context) : OCREngine {

      private lateinit var detectionModel: ONNXModel
      private lateinit var recognitionModel: ONNXModel

      init {
          loadModels()
      }

      private fun loadModels() {
          val modelsDir = File(context.filesDir, "models/ocr")

          detectionModel = ONNXModel(
              modelPath = File(modelsDir, "det_model.onnx").absolutePath,
              useFP16 = true
          )

          recognitionModel = ONNXModel(
              modelPath = File(modelsDir, "rec_model.onnx").absolutePath,
              useFP16 = true
          )
      }

      override suspend fun extractText(bitmap: Bitmap): OCRResult = withContext(Dispatchers.Default) {
          try {
              // 1. Text detection (find text regions)
              val textBoxes = detectTextRegions(bitmap)

              if (textBoxes.isEmpty()) {
                  return@withContext OCRResult(
                      text = "",
                      confidence = 1.0f,
                      language = null,
                      wordCount = 0
                  )
              }

              // 2. Text recognition (OCR on each region)
              val recognizedTexts = textBoxes.map { box ->
                  recognizeText(bitmap, box)
              }

              // 3. Combine results
              val fullText = recognizedTexts.joinToString(" ") { it.text }
              val avgConfidence = recognizedTexts.map { it.confidence }.average().toFloat()
              val wordCount = fullText.split("\\s+".toRegex()).size

              OCRResult(
                  text = fullText,
                  confidence = avgConfidence,
                  language = detectLanguage(fullText),
                  wordCount = wordCount,
                  boundingBoxes = recognizedTexts
              )

          } catch (e: Exception) {
              // Fallback: empty result
              OCRResult(
                  text = "",
                  confidence = 0f,
                  language = null,
                  wordCount = 0
              )
          }
      }

      private fun detectTextRegions(bitmap: Bitmap): List<TextRegion> {
          // PaddleOCR detection model inference
          // Returns bounding boxes of text regions
          // TODO: Implement using ONNX Runtime
          return emptyList()
      }

      private fun recognizeText(bitmap: Bitmap, region: TextRegion): BoundingBox {
          // PaddleOCR recognition model inference
          // Returns recognized text + confidence
          // TODO: Implement using ONNX Runtime
          return BoundingBox("", 0, 0, 0, 0, 0f)
      }

      private fun detectLanguage(text: String): String? {
          // Simple heuristic: check character ranges
          return when {
              text.isEmpty() -> null
              text.any { it in 'a'..'z' || it in 'A'..'Z' } -> "en"
              text.any { it in 'á'..'ž' } -> "cs"
              else -> "auto"
          }
      }

      override suspend fun extractTextBatch(bitmaps: List<Bitmap>): List<OCRResult> {
          return bitmaps.map { extractText(it) }
      }

      override fun close() {
          detectionModel.close()
          recognitionModel.close()
      }
  }

  private data class TextRegion(
      val x: Int,
      val y: Int,
      val width: Int,
      val height: Int
  )
  ```

- [ ] **Unit test v `test/lib/ocr/PaddleOCREngineTest.kt`:**
  ```kotlin
  @Test
  fun `extract text from image with text`() = runBlocking {
      val engine = PaddleOCREngine(context)
      val testBitmap = loadTestBitmap("screenshot_with_text.png")

      val result = engine.extractText(testBitmap)

      assertTrue(result.text.isNotEmpty())
      assertTrue(result.confidence > 0.5f)
      assertTrue(result.wordCount > 0)

      engine.close()
  }
  ```

- [ ] **Commit:** `✨ feat: PaddleOCR engine wrapper pro text extraction`

---

### 2.2 Pre-detection (Text vs No-Text)

- [ ] **Vytvořit `lib/ocr/TextDetector.kt`:**
  ```kotlin
  package com.fpf.smartscan.lib.ocr

  import android.graphics.Bitmap
  import org.opencv.android.Utils
  import org.opencv.core.Mat
  import org.opencv.imgproc.Imgproc

  class TextDetector {

      /**
       * Fast heuristic to detect if image likely contains text
       * Uses edge density analysis
       *
       * @param bitmap Input image
       * @return true if likely contains text (edge density > threshold)
       */
      fun hasTextContent(bitmap: Bitmap, threshold: Float = 0.05f): Boolean {
          // Convert to grayscale Mat
          val mat = Mat()
          Utils.bitmapToMat(bitmap, mat)
          val gray = Mat()
          Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)

          // Canny edge detection
          val edges = Mat()
          Imgproc.Canny(gray, edges, 100.0, 200.0)

          // Calculate edge density
          val edgePixels = edges.total() - org.opencv.core.Core.countNonZero(edges)
          val density = edgePixels.toFloat() / edges.total()

          // Cleanup
          mat.release()
          gray.release()
          edges.release()

          return density > threshold
      }

      /**
       * More sophisticated detection using variance analysis
       * High variance in local regions indicates potential text
       */
      fun hasTextContentAdvanced(bitmap: Bitmap): Boolean {
          // TODO: Implement variance-based detection
          // - Divide image into grid (e.g., 8x8 blocks)
          // - Calculate variance in each block
          // - Text regions have high local variance
          // - Natural images have lower variance
          return false
      }
  }
  ```

- [ ] **Přidat OpenCV dependency (pokud ještě není):**
  ```gradle
  dependencies {
      // OpenCV pro edge detection
      implementation("org.opencv:opencv:4.8.0")
  }
  ```

- [ ] **Unit test v `test/lib/ocr/TextDetectorTest.kt`:**
  ```kotlin
  @Test
  fun `detect text in screenshot`() {
      val detector = TextDetector()
      val screenshot = loadTestBitmap("screenshot_with_text.png")
      val photo = loadTestBitmap("landscape_photo.jpg")

      assertTrue(detector.hasTextContent(screenshot))
      assertFalse(detector.hasTextContent(photo))
  }
  ```

- [ ] **Commit:** `✨ feat: Fast text pre-detection using edge density`

---

### 2.3 Text Embedding Extractor (reuse from v1.2.0)

- [ ] **Ověřit, že `TextEmbeddingExtractor` existuje z v1.2.0:**
  ```kotlin
  // Již implementováno v document search (v1.2.0)
  class TextEmbeddingExtractor(private val context: Context) {
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

- [ ] **Pokud neexistuje, implementovat podle `dokumenty.md` TODO**

- [ ] **Vytvořit wrapper pro OCR-specific use case:**
  ```kotlin
  package com.fpf.smartscan.lib.embeddings

  class OCRTextEmbedder(private val context: Context) {

      private val textEmbedder = TextEmbeddingExtractor(context)

      suspend fun embedOCRText(ocrText: String): FloatArray {
          // Preprocess OCR text (cleanup, normalize)
          val cleaned = preprocessOCRText(ocrText)

          // Generate embedding (384-dim)
          return textEmbedder.extractEmbedding(cleaned)
      }

      suspend fun embedOCRTextBatch(texts: List<String>): List<FloatArray> {
          val cleaned = texts.map { preprocessOCRText(it) }
          return textEmbedder.extractBatch(cleaned)
      }

      private fun preprocessOCRText(text: String): String {
          // Remove excessive whitespace
          var cleaned = text.replace("\\s+".toRegex(), " ").trim()

          // Remove low-confidence artifacts (e.g., "|||", "___")
          cleaned = cleaned.replace("[|_]{2,}".toRegex(), "")

          // Truncate to max length (512 tokens ~ 2048 chars)
          if (cleaned.length > 2048) {
              cleaned = cleaned.substring(0, 2048)
          }

          return cleaned
      }

      fun close() {
          textEmbedder.close()
      }
  }
  ```

- [ ] **Commit:** `✨ feat: OCR text embedding wrapper (reuse MiniLM from v1.2.0)`

---

### 2.4 OCR Repository Layer

- [ ] **Vytvořit `data/ocr/OCRRepository.kt`:**
  ```kotlin
  package com.fpf.smartscan.data.ocr

  import android.content.Context
  import android.graphics.BitmapFactory
  import com.fpf.smartscan.data.embeddings.MultiVectorRepository
  import com.fpf.smartscan.lib.embeddings.OCRTextEmbedder
  import com.fpf.smartscan.lib.ocr.OCREngine
  import com.fpf.smartscan.lib.ocr.TextDetector
  import kotlinx.coroutines.flow.Flow

  class OCRRepository(
      private val context: Context,
      private val ocrDao: OCRDataDao,
      private val embeddingRepo: MultiVectorRepository,
      private val ocrEngine: OCREngine,
      private val textDetector: TextDetector,
      private val textEmbedder: OCRTextEmbedder
  ) {

      /**
       * Process image: detect text → OCR → embed → store
       */
      suspend fun processImage(imageId: String, imagePath: String): Result<OCRData> {
          return try {
              // 1. Load bitmap
              val bitmap = BitmapFactory.decodeFile(imagePath)
                  ?: return Result.failure(Exception("Failed to load image"))

              // 2. Pre-detection
              val hasText = textDetector.hasTextContent(bitmap)

              if (!hasText) {
                  // Store as "no text" (skip OCR + embedding)
                  val emptyData = OCRData(
                      imageId = imageId,
                      extractedText = "",
                      language = null,
                      confidence = 1.0f,
                      hasText = false,
                      wordCount = 0,
                      createdAt = System.currentTimeMillis(),
                      modelVersion = "paddleocr-v4"
                  )
                  ocrDao.insertOCRData(emptyData)
                  return Result.success(emptyData)
              }

              // 3. OCR extraction
              val ocrResult = ocrEngine.extractText(bitmap)

              // 4. Generate embedding (if text found)
              if (ocrResult.text.isNotBlank()) {
                  val embedding = textEmbedder.embedOCRText(ocrResult.text)
                  embeddingRepo.saveOCREmbedding(imageId, embedding)
              }

              // 5. Store metadata
              val ocrData = OCRData(
                  imageId = imageId,
                  extractedText = ocrResult.text,
                  language = ocrResult.language,
                  confidence = ocrResult.confidence,
                  hasText = ocrResult.text.isNotBlank(),
                  wordCount = ocrResult.wordCount,
                  createdAt = System.currentTimeMillis(),
                  modelVersion = "paddleocr-v4"
              )
              ocrDao.insertOCRData(ocrData)

              Result.success(ocrData)

          } catch (e: Exception) {
              Result.failure(e)
          }
      }

      /**
       * Batch processing for background worker
       */
      suspend fun processImageBatch(
          images: List<Pair<String, String>>  // (imageId, imagePath)
      ): BatchProcessingResult {
          var successCount = 0
          var failedCount = 0
          val failedImages = mutableListOf<String>()

          images.forEach { (imageId, imagePath) ->
              val result = processImage(imageId, imagePath)
              if (result.isSuccess) {
                  successCount++
              } else {
                  failedCount++
                  failedImages.add(imageId)
              }
          }

          return BatchProcessingResult(
              total = images.size,
              success = successCount,
              failed = failedCount,
              failedImageIds = failedImages
          )
      }

      suspend fun getOCRData(imageId: String): OCRData? {
          return ocrDao.getOCRData(imageId)
      }

      fun getAllImagesWithText(): Flow<List<OCRData>> {
          return ocrDao.getAllImagesWithText()
      }

      suspend fun getOCRCoverageRatio(): Float {
          val total = ocrDao.getTotalProcessedCount()
          val withText = ocrDao.getImagesWithTextCount()
          return if (total > 0) withText.toFloat() / total else 0f
      }

      suspend fun deleteOCRData(imageId: String) {
          ocrDao.deleteOCRData(imageId)
          embeddingRepo.deleteOCREmbedding(imageId)
      }
  }

  data class BatchProcessingResult(
      val total: Int,
      val success: Int,
      val failed: Int,
      val failedImageIds: List<String>
  )
  ```

- [ ] **Unit test v `test/data/ocr/OCRRepositoryTest.kt`:**
  ```kotlin
  @Test
  fun `process image with text`() = runBlocking {
      val repo = OCRRepository(...)
      val result = repo.processImage("test_img", "path/to/screenshot.png")

      assertTrue(result.isSuccess)
      val ocrData = result.getOrNull()
      assertNotNull(ocrData)
      assertTrue(ocrData!!.hasText)
      assertTrue(ocrData.extractedText.isNotEmpty())
  }

  @Test
  fun `skip image without text`() = runBlocking {
      val repo = OCRRepository(...)
      val result = repo.processImage("landscape", "path/to/photo.jpg")

      assertTrue(result.isSuccess)
      val ocrData = result.getOrNull()
      assertNotNull(ocrData)
      assertFalse(ocrData!!.hasText)
      assertTrue(ocrData.extractedText.isEmpty())
  }
  ```

- [ ] **Commit:** `✨ feat: OCR repository s batch processing`

---

### 2.5 Background OCR Worker

- [ ] **Vytvořit `workers/OCRIndexWorker.kt`:**
  ```kotlin
  package com.fpf.smartscan.workers

  import android.app.NotificationChannel
  import android.app.NotificationManager
  import android.content.Context
  import android.os.Build
  import androidx.core.app.NotificationCompat
  import androidx.work.*
  import com.fpf.smartscan.R
  import com.fpf.smartscan.data.images.ImageRepository
  import com.fpf.smartscan.data.ocr.OCRRepository
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext

  class OCRIndexWorker(
      context: Context,
      params: WorkerParameters
  ) : CoroutineWorker(context, params) {

      private val imageRepo = ImageRepository(context)
      private val ocrRepo = OCRRepository(context)

      override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
          try {
              // 1. Set foreground (show notification)
              setForeground(createForegroundInfo(0, 0))

              // 2. Get unprocessed images
              val unprocessedImages = getUnprocessedImages()

              if (unprocessedImages.isEmpty()) {
                  return@withContext Result.success()
              }

              // 3. Process in batches (10 images at a time)
              val batchSize = 10
              var processedCount = 0

              unprocessedImages.chunked(batchSize).forEach { batch ->
                  // Process batch
                  val result = ocrRepo.processImageBatch(batch)
                  processedCount += result.success

                  // Update notification
                  setForeground(createForegroundInfo(processedCount, unprocessedImages.size))

                  // Check if cancelled
                  if (isStopped) {
                      return@withContext Result.failure()
                  }

                  // Memory cleanup
                  System.gc()
              }

              Result.success(
                  workDataOf(
                      "processed_count" to processedCount,
                      "total_count" to unprocessedImages.size
                  )
              )

          } catch (e: Exception) {
              Result.failure(
                  workDataOf("error" to e.message)
              )
          }
      }

      private suspend fun getUnprocessedImages(): List<Pair<String, String>> {
          // Query images that don't have OCR data yet
          val allImages = imageRepo.getAllImages()
          val processedIds = ocrRepo.getAllImagesWithText().map { it.imageId }.toSet()

          return allImages
              .filterNot { it.id in processedIds }
              .map { it.id to it.filePath }
      }

      private fun createForegroundInfo(progress: Int, total: Int): ForegroundInfo {
          val channelId = "ocr_indexing_channel"
          createNotificationChannel(channelId)

          val notification = NotificationCompat.Builder(applicationContext, channelId)
              .setContentTitle("Indexování textu v obrázcích")
              .setContentText(
                  if (total > 0) "Zpracováno $progress z $total"
                  else "Příprava..."
              )
              .setSmallIcon(R.drawable.ic_notification)
              .setProgress(total, progress, total == 0)
              .setOngoing(true)
              .build()

          return ForegroundInfo(NOTIFICATION_ID, notification)
      }

      private fun createNotificationChannel(channelId: String) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              val channel = NotificationChannel(
                  channelId,
                  "OCR Indexování",
                  NotificationManager.IMPORTANCE_LOW
              ).apply {
                  description = "Probíhá indexování textu v obrázcích"
              }

              val notificationManager = applicationContext.getSystemService(
                  Context.NOTIFICATION_SERVICE
              ) as NotificationManager
              notificationManager.createNotificationChannel(channel)
          }
      }

      companion object {
          private const val NOTIFICATION_ID = 2  // Different from image indexing (1)
          const val WORK_NAME = "OCRIndexWorker"

          fun enqueue(context: Context) {
              val constraints = Constraints.Builder()
                  .setRequiresCharging(true)  // Prefer charging
                  .setRequiresStorageNotLow(true)
                  .build()

              val request = OneTimeWorkRequestBuilder<OCRIndexWorker>()
                  .setConstraints(constraints)
                  .addTag("ocr_indexing")
                  .build()

              WorkManager.getInstance(context).enqueueUniqueWork(
                  WORK_NAME,
                  ExistingWorkPolicy.KEEP,  // Don't restart if already running
                  request
              )
          }
      }
  }
  ```

- [ ] **Commit:** `✨ feat: Background OCR indexing worker`

---

## 🔍 FÁZE 3: MULTI-VECTOR SEARCH

### 3.1 Search Logic Implementation

- [ ] **Vytvořit `lib/search/MultiVectorSearch.kt`:**
  ```kotlin
  package com.fpf.smartscan.lib.search

  import com.fpf.smartscan.data.embeddings.MultiVectorRepository
  import com.fpf.smartscan.data.ocr.OCRRepository
  import com.fpf.smartscan.lib.embeddings.TextEmbeddingExtractor
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.async
  import kotlinx.coroutines.withContext
  import kotlin.math.sqrt

  class MultiVectorSearch(
      private val embeddingRepo: MultiVectorRepository,
      private val ocrRepo: OCRRepository,
      private val textEmbedder: TextEmbeddingExtractor
  ) {

      /**
       * Multi-modal search: visual + OCR
       *
       * @param query Search query text
       * @param alpha Weight for visual vs OCR (0.0-1.0)
       *              1.0 = visual only, 0.0 = OCR only, 0.7 = default (balanced)
       * @param minScore Minimum similarity threshold (0.0-1.0)
       * @param topK Number of results to return
       */
      suspend fun search(
          query: String,
          alpha: Float = 0.7f,
          minScore: Float = 0.5f,
          topK: Int = 20
      ): List<SearchResult> = withContext(Dispatchers.Default) {

          // 1. Generate query embedding
          val queryEmbedding = textEmbedder.extractEmbedding(query)

          // 2. Load indexes (parallel)
          val indexDeferred = async { embeddingRepo.loadMultiVectorIndex() }
          val index = indexDeferred.await()

          // 3. Search visual index
          val visualScores = index.visualEmbeddings.mapValues { (_, embedding) ->
              cosineSimilarity(queryEmbedding, embedding.embedding.toFloatArray())
          }

          // 4. Search OCR index
          val ocrScores = index.ocrEmbeddings.mapValues { (_, embedding) ->
              cosineSimilarity(queryEmbedding, embedding.embedding.toFloatArray())
          }

          // 5. Weighted fusion
          val allImageIds = (visualScores.keys + ocrScores.keys).distinct()

          val results = allImageIds.mapNotNull { imageId ->
              val vScore = visualScores[imageId] ?: 0f
              val oScore = ocrScores[imageId] ?: 0f

              // Weighted combination
              val combined = alpha * vScore + (1 - alpha) * oScore

              // Filter low scores
              if (combined < minScore) return@mapNotNull null

              SearchResult(
                  imageId = imageId,
                  combinedScore = combined,
                  visualScore = vScore,
                  ocrScore = oScore
              )
          }

          // 6. Sort and limit
          return@withContext results
              .sortedByDescending { it.combinedScore }
              .take(topK)
      }

      /**
       * Cosine similarity between two vectors
       */
      private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
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

          return if (magnitudeA > 0 && magnitudeB > 0) {
              (dotProduct / (magnitudeA * magnitudeB)).toFloat()
          } else {
              0f
          }
      }
  }

  data class SearchResult(
      val imageId: String,
      val combinedScore: Float,
      val visualScore: Float,
      val ocrScore: Float
  ) {
      val matchedInVisual: Boolean get() = visualScore >= 0.5f
      val matchedInOCR: Boolean get() = ocrScore >= 0.5f
  }
  ```

- [ ] **Unit test v `test/lib/search/MultiVectorSearchTest.kt`:**
  ```kotlin
  @Test
  fun `search returns results sorted by score`() = runBlocking {
      val search = MultiVectorSearch(...)
      val results = search.search("sunset photo", alpha = 0.7f)

      // Assert sorted descending
      assertTrue(results.zipWithNext().all { (a, b) -> a.combinedScore >= b.combinedScore })
  }

  @Test
  fun `alpha=1 returns visual-only results`() = runBlocking {
      val search = MultiVectorSearch(...)
      val results = search.search("blue sky", alpha = 1.0f)

      // Assert OCR scores are ignored
      results.forEach { result ->
          assertEquals(result.visualScore, result.combinedScore, 0.01f)
      }
  }
  ```

- [ ] **Commit:** `✨ feat: Multi-vector search s weighted fusion`

---

### 3.2 Query Intent Detection

- [ ] **Vytvořit `lib/search/QueryIntentDetector.kt`:**
  ```kotlin
  package com.fpf.smartscan.lib.search

  enum class QueryIntent {
      VISUAL_ONLY,    // "sunset", "red car", "landscape"
      TEXT_ONLY,      // "receipt total", "screenshot password"
      MIXED,          // "business card email", "document title"
      UNKNOWN
  }

  class QueryIntentDetector {

      private val textIndicators = setOf(
          // Text content keywords
          "text", "word", "receipt", "document", "number", "invoice",
          "email", "phone", "address", "name", "total", "price",
          "password", "username", "code", "message", "note",

          // Screenshot-specific
          "screenshot", "screen", "capture",

          // Document types
          "pdf", "scan", "paper", "letter", "form",

          // OCR-specific
          "written", "printed", "typed"
      )

      private val visualIndicators = setOf(
          // Scene types
          "photo", "image", "picture", "scene", "view",
          "landscape", "portrait", "selfie",

          // Colors
          "color", "red", "blue", "green", "yellow", "black", "white",

          // Objects
          "car", "person", "face", "building", "tree", "animal",
          "food", "nature", "sky", "sunset", "mountain",

          // Visual attributes
          "dark", "bright", "blurry", "clear"
      )

      fun detectIntent(query: String): QueryIntent {
          val lowercaseQuery = query.lowercase()
          val words = lowercaseQuery.split("\\s+".toRegex())

          val hasTextIntent = words.any { it in textIndicators }
          val hasVisualIntent = words.any { it in visualIndicators }

          return when {
              hasTextIntent && !hasVisualIntent -> QueryIntent.TEXT_ONLY
              hasVisualIntent && !hasTextIntent -> QueryIntent.VISUAL_ONLY
              hasTextIntent && hasVisualIntent -> QueryIntent.MIXED
              else -> QueryIntent.UNKNOWN
          }
      }

      /**
       * Get recommended alpha based on query intent
       *
       * @return alpha value (0.0-1.0)
       *         1.0 = visual only
       *         0.7 = balanced (default)
       *         0.5 = equal weight
       *         0.3 = OCR-heavy
       *         0.0 = OCR only
       */
      fun getAlpha(intent: QueryIntent): Float = when (intent) {
          QueryIntent.VISUAL_ONLY -> 1.0f
          QueryIntent.TEXT_ONLY -> 0.0f
          QueryIntent.MIXED -> 0.5f
          QueryIntent.UNKNOWN -> 0.7f  // Default: slight visual preference
      }
  }
  ```

- [ ] **Rozšířit `MultiVectorSearch` o intent detection:**
  ```kotlin
  suspend fun searchWithIntentDetection(
      query: String,
      minScore: Float = 0.5f,
      topK: Int = 20
  ): SearchResultWithIntent {
      val detector = QueryIntentDetector()
      val intent = detector.detectIntent(query)
      val alpha = detector.getAlpha(intent)

      val results = search(query, alpha, minScore, topK)

      return SearchResultWithIntent(
          results = results,
          intent = intent,
          alpha = alpha
      )
  }

  data class SearchResultWithIntent(
      val results: List<SearchResult>,
      val intent: QueryIntent,
      val alpha: Float
  )
  ```

- [ ] **Unit test:**
  ```kotlin
  @Test
  fun `detect text-only intent`() {
      val detector = QueryIntentDetector()

      val intent1 = detector.detectIntent("find receipt with total")
      assertEquals(QueryIntent.TEXT_ONLY, intent1)

      val intent2 = detector.detectIntent("screenshot password")
      assertEquals(QueryIntent.TEXT_ONLY, intent2)
  }

  @Test
  fun `detect visual-only intent`() {
      val detector = QueryIntentDetector()

      val intent1 = detector.detectIntent("sunset photo")
      assertEquals(QueryIntent.VISUAL_ONLY, intent1)

      val intent2 = detector.detectIntent("red car")
      assertEquals(QueryIntent.VISUAL_ONLY, intent2)
  }
  ```

- [ ] **Commit:** `✨ feat: Query intent detection pro adaptive alpha`

---

## 🎨 FÁZE 4: UI INTEGRATION

### 4.1 Settings Screen Extension

- [ ] **Rozšířit `ui/screens/settings/SettingsScreen.kt`:**
  ```kotlin
  @Composable
  fun SettingsScreen(...) {
      // Existing settings...

      // New: OCR Settings Section
      SettingsSection(title = "Vyhledávání textu (OCR)") {

          // Toggle: Enable/Disable OCR
          SwitchSetting(
              title = "Povolit vyhledávání textu",
              subtitle = "Vyhledávání v textu na obrázcích (screenshoty, dokumenty)",
              checked = ocrEnabled,
              onCheckedChange = { viewModel.toggleOCR(it) }
          )

          // Stats
          if (ocrEnabled) {
              InfoSetting(
                  title = "Zpracováno obrázků",
                  subtitle = "$processedCount / $totalCount"
              )

              InfoSetting(
                  title = "Obrázky s textem",
                  subtitle = "$withTextCount (${coveragePercent}%)"
              )

              // Button: Trigger re-indexing
              ButtonSetting(
                  title = "Přeindexovat texty",
                  subtitle = "Spustit OCR pro všechny obrázky",
                  onClick = { viewModel.triggerOCRReindex() }
              )
          }
      }
  }
  ```

- [ ] **Rozšířit `ui/screens/settings/SettingsViewModel.kt`:**
  ```kotlin
  class SettingsViewModel : ViewModel() {

      private val _ocrEnabled = MutableStateFlow(false)
      val ocrEnabled: StateFlow<Boolean> = _ocrEnabled

      private val _ocrStats = MutableStateFlow(OCRStats())
      val ocrStats: StateFlow<OCRStats> = _ocrStats

      init {
          loadOCRSettings()
          loadOCRStats()
      }

      fun toggleOCR(enabled: Boolean) {
          viewModelScope.launch {
              _ocrEnabled.value = enabled
              saveOCREnabled(enabled)

              if (enabled) {
                  // Trigger initial indexing
                  OCRIndexWorker.enqueue(context)
              }
          }
      }

      fun triggerOCRReindex() {
          viewModelScope.launch {
              OCRIndexWorker.enqueue(context)
          }
      }

      private suspend fun loadOCRStats() {
          // Load from OCRRepository
          val stats = ocrRepo.getStats()
          _ocrStats.value = stats
      }
  }

  data class OCRStats(
      val processedCount: Int = 0,
      val totalCount: Int = 0,
      val withTextCount: Int = 0,
      val coveragePercent: Int = 0
  )
  ```

- [ ] **Commit:** `✨ feat: OCR settings UI (enable/disable, stats, re-index)`

---

### 4.2 Search Screen Extension

- [ ] **Rozšířit `ui/screens/search/SearchScreen.kt`:**
  ```kotlin
  @Composable
  fun SearchScreen(...) {
      // Existing search UI...

      Column {
          // Search bar
          SearchBar(
              query = searchQuery,
              onQueryChange = { viewModel.updateQuery(it) }
          )

          // Filter chips (new)
          Row(
              modifier = Modifier.horizontalScroll(rememberScrollState())
          ) {
              FilterChip(
                  selected = resultFilter == ResultFilter.ALL,
                  onClick = { viewModel.setFilter(ResultFilter.ALL) },
                  label = { Text("Vše") }
              )

              FilterChip(
                  selected = resultFilter == ResultFilter.VISUAL_ONLY,
                  onClick = { viewModel.setFilter(ResultFilter.VISUAL_ONLY) },
                  label = { Text("Vizuální") }
              )

              FilterChip(
                  selected = resultFilter == ResultFilter.OCR_ONLY,
                  onClick = { viewModel.setFilter(ResultFilter.OCR_ONLY) },
                  label = { Text("Text (OCR)") }
              )
          }

          // Intent indicator (new)
          if (detectedIntent != QueryIntent.UNKNOWN) {
              IntentChip(intent = detectedIntent)
          }

          // Results
          LazyColumn {
              items(searchResults) { result ->
                  SearchResultCard(
                      result = result,
                      onClick = { viewModel.openImage(result.imageId) }
                  )
              }
          }
      }
  }

  @Composable
  fun IntentChip(intent: QueryIntent) {
      val (text, icon) = when (intent) {
          QueryIntent.VISUAL_ONLY -> "Vizuální vyhledávání" to Icons.Default.Image
          QueryIntent.TEXT_ONLY -> "Textové vyhledávání" to Icons.Default.TextFields
          QueryIntent.MIXED -> "Kombinované vyhledávání" to Icons.Default.Search
          else -> return
      }

      Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          shape = RoundedCornerShape(16.dp)
      ) {
          Row(
              modifier = Modifier.padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
              Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(text, style = MaterialTheme.typography.labelSmall)
          }
      }
  }
  ```

- [ ] **Rozšířit `ui/components/SearchResultCard.kt`:**
  ```kotlin
  @Composable
  fun SearchResultCard(result: SearchResultWithMetadata, onClick: () -> Unit) {
      Card(
          modifier = Modifier
              .fillMaxWidth()
              .clickable(onClick = onClick)
      ) {
          Row {
              // Image thumbnail
              AsyncImage(
                  model = result.imagePath,
                  contentDescription = null,
                  modifier = Modifier.size(80.dp)
              )

              Column(modifier = Modifier.padding(8.dp)) {
                  // Score
                  Text(
                      text = "Relevance: ${(result.combinedScore * 100).toInt()}%",
                      style = MaterialTheme.typography.labelSmall
                  )

                  // Match indicators (new)
                  Row {
                      if (result.matchedInVisual) {
                          MatchChip("Vizuální")
                      }
                      if (result.matchedInOCR) {
                          MatchChip("Text")
                      }
                  }

                  // OCR text preview (new)
                  if (result.ocrText != null && result.matchedInOCR) {
                      Text(
                          text = result.ocrText.take(100) + "...",
                          style = MaterialTheme.typography.bodySmall,
                          maxLines = 2,
                          overflow = TextOverflow.Ellipsis
                      )
                  }
              }
          }
      }
  }

  @Composable
  fun MatchChip(text: String) {
      Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(8.dp)
      ) {
          Text(
              text = text,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              style = MaterialTheme.typography.labelSmall
          )
      }
  }
  ```

- [ ] **Rozšířit `ui/screens/search/SearchViewModel.kt`:**
  ```kotlin
  class SearchViewModel : ViewModel() {

      private val multiVectorSearch = MultiVectorSearch(...)

      private val _searchResults = MutableStateFlow<List<SearchResultWithMetadata>>(emptyList())
      val searchResults: StateFlow<List<SearchResultWithMetadata>> = _searchResults

      private val _detectedIntent = MutableStateFlow(QueryIntent.UNKNOWN)
      val detectedIntent: StateFlow<QueryIntent> = _detectedIntent

      private val _resultFilter = MutableStateFlow(ResultFilter.ALL)
      val resultFilter: StateFlow<ResultFilter> = _resultFilter

      fun updateQuery(query: String) {
          viewModelScope.launch {
              if (query.isBlank()) {
                  _searchResults.value = emptyList()
                  return@launch
              }

              // Search with intent detection
              val searchResult = multiVectorSearch.searchWithIntentDetection(query)

              _detectedIntent.value = searchResult.intent

              // Load metadata (OCR text, image paths)
              val resultsWithMetadata = searchResult.results.map { result ->
                  val ocrData = ocrRepo.getOCRData(result.imageId)
                  val imagePath = imageRepo.getImagePath(result.imageId)

                  SearchResultWithMetadata(
                      imageId = result.imageId,
                      imagePath = imagePath,
                      combinedScore = result.combinedScore,
                      visualScore = result.visualScore,
                      ocrScore = result.ocrScore,
                      ocrText = ocrData?.extractedText,
                      matchedInVisual = result.matchedInVisual,
                      matchedInOCR = result.matchedInOCR
                  )
              }

              // Apply filter
              _searchResults.value = applyFilter(resultsWithMetadata, _resultFilter.value)
          }
      }

      fun setFilter(filter: ResultFilter) {
          _resultFilter.value = filter
          // Re-apply filter to current results
          _searchResults.value = applyFilter(_searchResults.value, filter)
      }

      private fun applyFilter(
          results: List<SearchResultWithMetadata>,
          filter: ResultFilter
      ): List<SearchResultWithMetadata> {
          return when (filter) {
              ResultFilter.ALL -> results
              ResultFilter.VISUAL_ONLY -> results.filter { it.matchedInVisual }
              ResultFilter.OCR_ONLY -> results.filter { it.matchedInOCR }
          }
      }
  }

  enum class ResultFilter {
      ALL, VISUAL_ONLY, OCR_ONLY
  }

  data class SearchResultWithMetadata(
      val imageId: String,
      val imagePath: String,
      val combinedScore: Float,
      val visualScore: Float,
      val ocrScore: Float,
      val ocrText: String?,
      val matchedInVisual: Boolean,
      val matchedInOCR: Boolean
  )
  ```

- [ ] **Commit:** `✨ feat: Search UI s OCR results, filtering a intent indicators`

---

### 4.3 OCR Text Highlighting

- [ ] **Vytvořit `ui/components/HighlightedText.kt`:**
  ```kotlin
  package com.fpf.smartscan.ui.components

  import androidx.compose.material3.MaterialTheme
  import androidx.compose.material3.Text
  import androidx.compose.runtime.Composable
  import androidx.compose.ui.graphics.Color
  import androidx.compose.ui.text.SpanStyle
  import androidx.compose.ui.text.buildAnnotatedString
  import androidx.compose.ui.text.withStyle

  @Composable
  fun HighlightedText(
      text: String,
      query: String,
      highlightColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
  ) {
      val keywords = query.split("\\s+".toRegex())

      val annotatedString = buildAnnotatedString {
          var currentIndex = 0
          val lowercaseText = text.lowercase()

          keywords.forEach { keyword ->
              val lowercaseKeyword = keyword.lowercase()
              var index = lowercaseText.indexOf(lowercaseKeyword, currentIndex)

              while (index != -1) {
                  // Append text before match
                  append(text.substring(currentIndex, index))

                  // Append highlighted match
                  withStyle(SpanStyle(background = highlightColor)) {
                      append(text.substring(index, index + keyword.length))
                  }

                  currentIndex = index + keyword.length
                  index = lowercaseText.indexOf(lowercaseKeyword, currentIndex)
              }
          }

          // Append remaining text
          if (currentIndex < text.length) {
              append(text.substring(currentIndex))
          }
      }

      Text(annotatedString)
  }
  ```

- [ ] **Použít v `SearchResultCard`:**
  ```kotlin
  // OCR text with highlighting
  if (result.ocrText != null && result.matchedInOCR) {
      HighlightedText(
          text = result.ocrText.take(100) + "...",
          query = searchQuery
      )
  }
  ```

- [ ] **Commit:** `✨ feat: OCR text highlighting v search results`

---

### 4.4 Indexing Progress Notification

- [ ] **Update `OCRIndexWorker` notification s progress:**
  ```kotlin
  // Already implemented in section 2.5
  // Verify progress updates work correctly
  ```

- [ ] **Vytvořit `ui/components/OCRIndexingBanner.kt` (optional):**
  ```kotlin
  @Composable
  fun OCRIndexingBanner(
      progress: Int,
      total: Int,
      onCancel: () -> Unit
  ) {
      Surface(
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier = Modifier.fillMaxWidth()
      ) {
          Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
              CircularProgressIndicator(
                  progress = if (total > 0) progress.toFloat() / total else 0f,
                  modifier = Modifier.size(24.dp)
              )

              Spacer(Modifier.width(16.dp))

              Column(modifier = Modifier.weight(1f)) {
                  Text("Indexování textu", style = MaterialTheme.typography.titleSmall)
                  Text("$progress / $total obrázků", style = MaterialTheme.typography.bodySmall)
              }

              TextButton(onClick = onCancel) {
                  Text("Zrušit")
              }
          }
      }
  }
  ```

- [ ] **Integrovat do `MainActivity.kt` (observe WorkManager state):**
  ```kotlin
  @Composable
  fun MainScreen() {
      val workInfo by WorkManager.getInstance(context)
          .getWorkInfosForUniqueWorkLiveData(OCRIndexWorker.WORK_NAME)
          .observeAsState()

      val isIndexing = workInfo?.any { it.state == WorkInfo.State.RUNNING } == true

      Column {
          if (isIndexing) {
              OCRIndexingBanner(
                  progress = workInfo?.firstOrNull()?.progress?.getInt("processed_count", 0) ?: 0,
                  total = workInfo?.firstOrNull()?.progress?.getInt("total_count", 0) ?: 0,
                  onCancel = {
                      WorkManager.getInstance(context).cancelUniqueWork(OCRIndexWorker.WORK_NAME)
                  }
              )
          }

          // Main content...
      }
  }
  ```

- [ ] **Commit:** `✨ feat: OCR indexing progress banner v UI`

---

## 🧪 FÁZE 5: TESTING & OPTIMIZATION

### 5.1 Unit Tests

- [ ] **Test OCR engine:**
  - [ ] `PaddleOCREngineTest.kt` - text extraction accuracy
  - [ ] `TextDetectorTest.kt` - pre-detection accuracy

- [ ] **Test embeddings:**
  - [ ] `OCRTextEmbedderTest.kt` - embedding generation
  - [ ] `MultiVectorRepositoryTest.kt` - file I/O

- [ ] **Test search:**
  - [ ] `MultiVectorSearchTest.kt` - search accuracy
  - [ ] `QueryIntentDetectorTest.kt` - intent classification

- [ ] **Coverage target:** 80%+ pro core logic

- [ ] **Commit:** `✅ test: Unit tests pro OCR processing a search`

---

### 5.2 Integration Tests

- [ ] **End-to-end OCR indexing test:**
  ```kotlin
  @Test
  fun `end to end OCR indexing`() = runBlocking {
      // 1. Setup test images
      val testImages = listOf(
          "screenshot_with_text.png",
          "landscape_photo.jpg"
      )

      // 2. Trigger indexing
      OCRIndexWorker.enqueue(context)

      // 3. Wait for completion
      val workInfo = WorkManager.getInstance(context)
          .getWorkInfosForUniqueWork(OCRIndexWorker.WORK_NAME)
          .await()

      assertEquals(WorkInfo.State.SUCCEEDED, workInfo.firstOrNull()?.state)

      // 4. Verify OCR data stored
      val ocrData = ocrRepo.getOCRData("screenshot_with_text")
      assertNotNull(ocrData)
      assertTrue(ocrData!!.hasText)

      // 5. Verify embeddings stored
      val index = embeddingRepo.loadMultiVectorIndex()
      assertTrue(index.ocrEmbeddings.containsKey("screenshot_with_text"))
  }
  ```

- [ ] **Multi-vector search integration test:**
  ```kotlin
  @Test
  fun `search returns OCR matched results`() = runBlocking {
      // Setup indexed images
      setupTestData()

      // Search with text query
      val results = multiVectorSearch.search("password reset email")

      // Assert
      assertTrue(results.isNotEmpty())
      assertTrue(results.first().matchedInOCR)
  }
  ```

- [ ] **Commit:** `✅ test: Integration tests pro OCR multi-vector search`

---

### 5.3 Performance Testing & Optimization

- [ ] **Benchmark OCR processing:**
  ```kotlin
  @Test
  fun `benchmark OCR processing speed`() = runBlocking {
      val engine = PaddleOCREngine(context)
      val testBitmap = loadTestBitmap("screenshot.png")

      val times = mutableListOf<Long>()
      repeat(10) {
          val start = System.currentTimeMillis()
          engine.extractText(testBitmap)
          val end = System.currentTimeMillis()
          times.add(end - start)
      }

      val avgTime = times.average()
      println("Average OCR time: ${avgTime}ms")

      // Assert reasonable performance
      assertTrue(avgTime < 600)  // < 600ms average
  }
  ```

- [ ] **Benchmark search latency:**
  ```kotlin
  @Test
  fun `benchmark search latency with 10k images`() = runBlocking {
      // Setup 10k test embeddings
      setupLargeTestDataset(10000)

      val start = System.currentTimeMillis()
      val results = multiVectorSearch.search("test query")
      val end = System.currentTimeMillis()

      val latency = end - start
      println("Search latency (10k images): ${latency}ms")

      // Assert acceptable latency
      assertTrue(latency < 500)  // < 500ms
  }
  ```

- [ ] **Memory profiling:**
  - [ ] Monitor peak memory usage během OCR indexing
  - [ ] Verify no memory leaks (LeakCanary)
  - [ ] Optimize batch size pokud OOM

- [ ] **Optimalizace:**
  - [ ] Tune OCR batch size (current: 10)
  - [ ] Parallel search (visual + OCR async)
  - [ ] Cache frequently accessed embeddings

- [ ] **Commit:** `⚡ perf: OCR processing optimizations`

---

### 5.4 Error Handling & Edge Cases

- [ ] **Graceful degradation:**
  ```kotlin
  // Model not found
  if (!ocrModelExists()) {
      showSetupInstructions()
      return
  }

  // OOM during indexing
  try {
      ocrRepo.processImageBatch(batch)
  } catch (e: OutOfMemoryError) {
      reduceBatchSize()
      retry()
  }

  // Corrupted image
  try {
      val bitmap = BitmapFactory.decodeFile(path)
  } catch (e: Exception) {
      logError(imageId, "Failed to decode image")
      skipImage()
  }
  ```

- [ ] **User feedback:**
  - [ ] Toast messages pro errors
  - [ ] Retry mechanism pro failed images
  - [ ] Error log v Settings (show failed files)

- [ ] **Edge cases:**
  - [ ] Empty images (0×0 pixels)
  - [ ] Very large images (>10 MB)
  - [ ] Non-UTF8 OCR text
  - [ ] Images with no text (verify pre-detection works)

- [ ] **Commit:** `🐛 fix: Error handling pro OCR indexing`

---

## 📚 FÁZE 6: DOCUMENTATION & RELEASE

### 6.1 Code Documentation

- [ ] **Přidat KDoc comments:**
  ```kotlin
  /**
   * Extracts text from image using PaddleOCR ONNX model.
   *
   * @param bitmap Input image
   * @return OCR result with extracted text, confidence, and bounding boxes
   * @throws Exception if OCR processing fails
   */
  suspend fun extractText(bitmap: Bitmap): OCRResult
  ```

- [ ] **Update `CLAUDE.md`:**
  - [ ] OCR multi-vector architecture
  - [ ] File structure changes
  - [ ] Database schema (v19)

- [ ] **Vytvořit `docs/OCR_SEARCH.md`:**
  - [ ] User guide (jak povolit OCR search)
  - [ ] Technical overview
  - [ ] Troubleshooting

- [ ] **Commit:** `📝 docs: OCR search documentation`

---

### 6.2 User Guide

- [ ] **In-app help dialog:**
  ```kotlin
  @Composable
  fun OCRHelpDialog(onDismiss: () -> Unit) {
      AlertDialog(
          onDismissRequest = onDismiss,
          title = { Text("Vyhledávání textu (OCR)") },
          text = {
              Column {
                  Text("OCR umožňuje vyhledat obrázky podle textu, který obsahují.")
                  Spacer(Modifier.height(8.dp))
                  Text("Příklady:")
                  Text("• \"Screenshot s heslem\"")
                  Text("• \"Faktura celkem 500\"")
                  Text("• \"Vizitka email\"")
              }
          },
          confirmButton = {
              TextButton(onClick = onDismiss) {
                  Text("Rozumím")
              }
          }
      )
  }
  ```

- [ ] **Settings tooltips:**
  - [ ] Info icon u každého nastavení
  - [ ] Explain co dělá OCR

- [ ] **Commit:** `📝 docs: In-app help pro OCR search`

---

### 6.3 Migration & Backward Compatibility

- [ ] **Test upgrade z v1.2.0:**
  - [ ] Verify database migration v18→v19 runs
  - [ ] Verify existing image/document embeddings fungují
  - [ ] No crashes on first launch

- [ ] **Opt-in feature toggle:**
  - [ ] Default: disabled (pro existující users)
  - [ ] Show "New Feature" badge v Settings

- [ ] **Commit:** `♻️ refactor: Backward compatible OCR search`

---

### 6.4 Release Preparation

- [ ] **Update `app/build.gradle.kts`:**
  ```gradle
  android {
      defaultConfig {
          versionCode 13  // Increment
          versionName "1.3.0"
      }
  }
  ```

- [ ] **Update changelog:**
  ```markdown
  ## v1.3.0 - OCR Search (2025-XX-XX)

  ### New Features
  - 🔍 OCR text search v obrázcích (screenshoty, dokumenty, business cards)
  - 🎯 Multi-vector search (visual + text embeddings)
  - 🤖 Automatic query intent detection
  - 📊 OCR statistics v Settings

  ### Technical
  - PaddleOCR integration (2.8 MB model)
  - Multi-vector retriever pattern
  - Background OCR indexing
  - Database migration v18→v19

  ### Performance
  - Pre-detection: Skip images without text (~60-80% reduction)
  - Search latency: ~200-400ms (10k images)
  - Storage: +15-20 MB (10k images with OCR)
  ```

- [ ] **F-Droid metadata:**
  - [ ] Update `metadata/en-US/changelogs/13.txt`
  - [ ] Add feature description

- [ ] **Test release build:**
  ```bash
  ./gradlew assembleRelease
  ```
  - [ ] Verify ProGuard rules (žádné crashes)
  - [ ] Test na real device
  - [ ] Verify model loading works

- [ ] **Commit:** `🚀 release: SmartScan v1.3.0 - OCR Search`

---

## 📈 PROGRESS TRACKING

### Milestones:

- [ ] **M1:** Infrastructure ready (Fáze 1) - ~3-4 dny
- [ ] **M2:** OCR processing working (Fáze 2) - ~5-7 dnů
- [ ] **M3:** Multi-vector search working (Fáze 3) - ~3-4 dny
- [ ] **M4:** UI integrated (Fáze 4) - ~3-4 dny
- [ ] **M5:** Tested & optimized (Fáze 5) - ~2-3 dny
- [ ] **M6:** Release ready (Fáze 6) - ~1-2 dny

**Estimated total:** 2-3 týdny (part-time development)

---

## ⚠️ RISKS & MITIGATION

### Technical Risks:

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| PaddleOCR accuracy nízká | HIGH | MEDIUM | Test na real screenshots, fallback to simpler OCR |
| OOM při batch processing | MEDIUM | MEDIUM | Dynamic batch size, memory monitoring |
| Search latency vysoká | MEDIUM | LOW | Parallel search, FAISS index (optional) |
| Database migration fails | HIGH | LOW | Extensive migration testing |

### User Experience Risks:

| Risk | Impact | Mitigation |
|------|--------|------------|
| Users očekávají instant results | MEDIUM | Show indexing progress, "Index more" prompt |
| OCR errors v results | MEDIUM | Show confidence scores, allow feedback |
| Storage overhead concerns | LOW | Opt-in feature, show storage usage |

---

## 📊 SUCCESS METRICS

### Performance KPIs:

- [ ] **OCR accuracy:** >85% na test dataset (100 screenshots)
- [ ] **Pre-detection accuracy:** >90% (correctly identify images with/without text)
- [ ] **Search latency:** <500ms (10k images)
- [ ] **Indexing speed:** <600ms per image (average)
- [ ] **Memory usage:** <100 MB peak (during indexing)

### User Impact:

- [ ] **Recall improvement:** +60-80% pro text queries (measured on test set)
- [ ] **User satisfaction:** Collect feedback after 1 week
- [ ] **Feature adoption:** >30% users enable OCR within 1 month

---

## ✅ ZÁVĚR

**Implementační TODO pro OCR multi-vector search je připraveno!**

**Key highlights:**
- ✅ 6 fází development (infrastructure → release)
- ✅ ~110 konkrétních TODO items
- ✅ Odhadovaný čas: 2-3 týdny (part-time)
- ✅ Multi-vector retriever pattern (industry standard)
- ✅ Background processing s pre-detection
- ✅ F-Droid compatible stack

**Dependencies:**
- Vyžaduje v1.2.0 (Document Search) pro text embeddings
- Reuse MiniLM-L6-v2 model (384-dim)
- Přidání PaddleOCR (2.8 MB)

**Expected outcome:**
- Killer feature: screenshot search by content 🎉
- +60-80% recall pro text queries
- ~200-400ms search latency
- Reasonable storage overhead (~15-20 MB per 10k images)

---

**Datum vytvoření:** 2025-10-27
**Pro:** SmartScan v1.3.0 - OCR Search
**Developer:** Jaroslav
