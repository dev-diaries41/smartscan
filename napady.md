# Nápady na rozšíření SmartScan - Embedding Features

## 📋 Přehled

Tento dokument obsahuje kompletní analýzu **co všechno lze vektorovat** pro sémantické vyhledávání v SmartScan aplikaci. Nápady jsou prioritizované podle hodnoty, implementační náročnosti a F-Droid kompatibility.

**Datum analýzy:** 2025-10-27
**Aktuální verze:** v1.1.3
**Plánovaná verze:** v1.2.0 (Document Search)

---

## 🎯 TIER 1: TOP PRIORITY - Ihned implementovatelné

### 1. OCR Embeddings (Screenshots, Business Cards, Handwritten Notes) ⭐⭐⭐⭐⭐

**Status:** 🟢 HIGHLY RECOMMENDED - Nejlepší value/effort ratio

**Model: PaddleOCR**
- **Velikost:** 2.8-3.5 MB (ultra-lightweight!)
- **ONNX:** ✅ YES - [RapidAI Conversion](https://github.com/RapidAI/PaddleOCRModelConvert)
- **F-Droid:** ✅ Apache 2.0
- **Performance:** <100ms per image
- **Licence:** Apache 2.0 (F-Droid compatible)

**Use cases:**
1. **Screenshot search:**
   - "Najdi screenshot s settings"
   - "Find screenshot with password reset email"
   - "Screenshot kde je QR code"

2. **Business card organization:**
   - Extrakce jméno, telefon, email, firma
   - Automatic contact creation
   - Search by company name

3. **Handwritten notes:**
   - OCR + embeddings
   - Search by content
   - Note organization

4. **Document text extraction:**
   - Combine with existing PDF search
   - Scanned documents support
   - Receipt/invoice organization

**Proč je to KILLER FEATURE:**
- ✅ Každý má stovky screenshotů - nikdo je neumí najít
- ✅ OCR text → embeddings → semantic search
- ✅ Nízká konkurence (málo Android apps to má)
- ✅ Malý model (3 MB)
- ✅ F-Droid compatible

**Technická implementace:**
```kotlin
// Pipeline
Screenshot Image
    ↓
PaddleOCR (text extraction) - 2.8 MB
    ↓
Text Chunks
    ↓
MiniLM-L6-v2 (embeddings) - 23 MB (už budeš mít)
    ↓
Searchable Index
```

**Implementační náročnost:** STŘEDNÍ
- Integrate PaddleOCR ONNX model
- Reuse existing text embedding pipeline (z document search)
- UI: Screenshot viewer + OCR overlay

**Dependencies:**
- ✅ Text embeddings (v1.2.0 - Document Search)
- ✅ ONNX Runtime (už máš)
- ✅ Image loading (Coil, už máš)

**Estimated effort:** 2-3 týdny

**GitHub References:**
- [PaddleOCR Official](https://github.com/PaddlePaddle/PaddleOCR)
- [PaddleOCR ONNX Conversion](https://github.com/RapidAI/PaddleOCRModelConvert)
- [Paddle2ONNX Docs](https://paddlepaddle.github.io/PaddleOCR/main/en/version3.x/deployment/obtaining_onnx_models.html)

---

### 2. Document Search (PDF, Markdown, TXT) ⭐⭐⭐⭐⭐

**Status:** 🟡 IN PROGRESS - Plánováno pro v1.2.0

**Model: all-MiniLM-L6-v2**
- **Velikost:** 23 MB
- **ONNX:** ✅ YES
- **F-Droid:** ✅ Apache 2.0
- **Performance:** ~60ms per 512-token chunk

**Detaily:** Viz `dokumenty.md`

**Estimated effort:** 5-6 týdnů (part-time)

---

### 3. Audio Embeddings (Voice Memos, Podcasts, Audiobooks) ⭐⭐⭐⭐

**Status:** 🟡 FEASIBLE - Možné po v1.2.0

**Model: Wav2Vec 2.0**
- **Velikost:** 50-100 MB (závisí na variantě)
- **ONNX:** ✅ YES - [HuggingFace Collection](https://huggingface.co/darjusul/wav2vec2-ONNX-collection)
- **F-Droid:** ✅ Apache 2.0 / MIT
- **Performance:** Real-time capable (s constraints)

**Use cases:**
1. **Voice memo search:**
   - "Najdi nahrávku o projektu X"
   - Semantic search v audio obsahu
   - Transcription + embeddings

2. **Podcast organization:**
   - Search by topic/content
   - Episode recommendations
   - Semantic grouping

3. **Speaker recognition:**
   - Group recordings by speaker
   - "Find all recordings of person X"
   - Speaker similarity

4. **Audio similarity:**
   - Find similar music/sounds
   - Duplicate audio detection
   - Genre classification

**Technická implementace:**
```kotlin
// Pipeline
Audio File
    ↓
Wav2Vec 2.0 (audio embeddings) - 50-100 MB
    ↓
384-dim embeddings
    ↓
Cosine similarity search

// Optional: Combine with transcription
Audio File
    ↓
Whisper (transcription)
    ↓
MiniLM-L6-v2 (text embeddings)
    ↓
Multi-modal search
```

**Výhody:**
- ✅ Voice memo search (jedinečná feature)
- ✅ Podcast/audiobook organization
- ✅ F-Droid compatible
- ✅ ONNX Runtime compatible

**Nevýhody:**
- ⚠️ Model size: 50-100 MB (větší než text embeddings)
- ⚠️ Processing time: Audio je delší než text/images
- ⚠️ Memory consumption: Audio buffering

**Implementační náročnost:** VYSOKÁ
- Audio decoding (Media3 ExoPlayer, už máš)
- Wav2Vec2 ONNX integration
- Batch processing pro long audio files
- Background worker (WorkManager)
- UI: Audio player + search results

**Estimated effort:** 3-4 týdny

**GitHub References:**
- [Wav2Vec2 ONNX Collection](https://huggingface.co/darjusul/wav2vec2-ONNX-collection)
- [Wespeaker (Speaker Embeddings)](https://github.com/wenet-e2e/wespeaker)
- [Audio Processing Research 2024](https://www.sciencedirect.com/science/article/abs/pii/S0167639324000761)

---

### 4. Metadata Embeddings (EXIF, File Properties) ⭐⭐⭐⭐

**Status:** 🟢 EASY - Reuse existing infrastructure

**Model: all-MiniLM-L6-v2** (reuse z document search)
- **Velikost:** 0 MB (už budeš mít model)
- **ONNX:** ✅ YES
- **F-Droid:** ✅ Apache 2.0

**Use cases:**
1. **Camera settings search:**
   - "Photos shot at f/1.8"
   - "Images with ISO 3200+"
   - "Photos from 50mm lens"

2. **Location descriptions:**
   - "Photos from Prague"
   - "Beach vacation photos"
   - "Mountain hiking"

3. **Time-based queries:**
   - "Summer 2024 photos"
   - "Morning photos"
   - "Weekend pictures"

4. **Device/camera search:**
   - "Photos from Canon EOS"
   - "Smartphone photos"
   - "Drone footage"

**Technická implementace:**
```kotlin
// Extract EXIF metadata
val metadata = """
Camera: Canon EOS R5
Lens: RF 50mm f/1.2
Settings: f/1.8, 1/500s, ISO 400
Location: Prague, Czech Republic
Date: 2024-08-15 14:30
"""

// Convert to embeddings (reuse MiniLM-L6-v2)
val embedding = textEmbedModel.encode(metadata)

// Search: "photos from Canon with shallow depth of field"
val query = "Canon photos with shallow depth of field"
val queryEmbedding = textEmbedModel.encode(query)
val results = cosineSimilarity(queryEmbedding, allMetadataEmbeddings)
```

**Výhody:**
- ✅ Zero additional model size
- ✅ Semantic search v metadata
- ✅ Natural language queries
- ✅ Reuse existing infrastructure

**Implementační náročnost:** NÍZKÁ
- EXIF extraction (Android MediaMetadataRetriever, už máš)
- Format metadata jako text
- Reuse text embedding model
- Minimal UI changes

**Estimated effort:** 1 týden

---

### 5. Video Content Embeddings (Frame-level + Transcript) ⭐⭐⭐

**Status:** 🟡 PARTIAL - Můžeš reuse existing CLIP

**Approach:** Kombinace stávajících nástrojů
- **Frame embeddings:** CLIP (už máš!)
- **Audio transcript:** Whisper + text embeddings (optional)
- **Combined search:** Multi-modal similarity

**Use cases:**
1. **Frame-level search:**
   - "Find video with blue car"
   - "Video showing cooking"
   - "Sunset timelapses"

2. **Transcript search** (optional):
   - "Video where person says 'machine learning'"
   - Search v dialogu/narration

3. **Scene detection:**
   - Group similar scenes
   - Find duplicate videos
   - Highlight detection

**Technická implementace:**
```kotlin
// Already implemented (v1.1.3)
Video File
    ↓
Frame extraction (every X seconds) - Media3 ExoPlayer
    ↓
CLIP embeddings per frame
    ↓
Average/max pooling
    ↓
Video-level embedding

// NEW: Add transcript embeddings
Video File
    ↓
Audio extraction
    ↓
Whisper (transcription) - optional
    ↓
MiniLM-L6-v2 (text embeddings)
    ↓
Combined search (frame + transcript)
```

**Výhody:**
- ✅ Frame embeddings už máš (CLIP)
- ✅ Infrastructure ready (video processing)
- ✅ Incremental improvement

**Nevýhody:**
- ⚠️ Whisper model size (varies: 40 MB - 1.5 GB)
- ⚠️ Transcription latency
- ⚠️ Language support (English focus)

**Implementační náročnost:** STŘEDNÍ
- Už máš frame extraction + CLIP
- Přidej: Whisper integration (optional)
- Combine embeddings (weighted average)

**Estimated effort:** 2 týdny (jen transcript addition)

**GitHub References:**
- [Whisper ONNX Models](https://github.com/openai/whisper)
- [Whisper.cpp (lightweight)](https://github.com/ggerganov/whisper.cpp)

---

### 6. UI Element Embeddings (Screenshot Analysis) ⭐⭐⭐⭐

**Status:** 🟢 EASY - Reuse existing CLIP

**Model: CLIP** (už máš!)
- **Velikost:** 0 MB (already have)
- **ONNX:** ✅ YES
- **F-Droid:** ✅ Already in project

**Use cases:**
1. **UI element search:**
   - "Find screenshot with blue button"
   - "Screenshots showing settings screen"
   - "App with navigation drawer"

2. **App identification:**
   - "Find Instagram screenshots"
   - "Screenshots from Chrome browser"
   - Visual app detection

3. **UI pattern recognition:**
   - "Find login screens"
   - "Screenshots with forms"
   - "Error messages"

**Technická implementace:**
```kotlin
// Use existing CLIP embeddings
Screenshot Image
    ↓
CLIP image encoder (už máš!)
    ↓
Image embedding
    ↓
Query: "blue settings button"
    ↓
CLIP text encoder
    ↓
Cosine similarity
```

**Výhody:**
- ✅ Zero additional implementation
- ✅ Already have CLIP model
- ✅ Works out-of-the-box

**Kombinace s OCR:**
```kotlin
// Best approach: Combine CLIP + OCR
Screenshot
    ↓
    ├─→ CLIP (visual features) - "blue button", "settings UI"
    └─→ OCR (text content) - "WiFi password: 12345"
    ↓
Combined embedding
    ↓
Multi-modal search
```

**Implementační náročnost:** VELMI NÍZKÁ
- Pouze UI/UX improvements
- Query suggestions ("Find screenshot with...")
- No new models needed

**Estimated effort:** 3-5 dnů

---

## 🔬 TIER 2: POKROČILÉ - Možné, ale složitější

### 7. MobileCLIP (Upgrade současného CLIP modelu) ⭐⭐⭐⭐

**Status:** 🟡 RESEARCH NEEDED - License unclear

**Apple ML Research (2024)**
- **Velikost:**
  - MobileCLIP-S0: ~40 MB (nejmenší, 4.8x rychlejší)
  - MobileCLIP-S2: ~80 MB (balanced, 2.3x rychlejší)
  - MobileCLIP-B(LT): 173 MB (nejvyšší kvalita)
- **ONNX:** ⚠️ Možné (CoreML → ONNX conversion)
- **F-Droid:** ⚠️ **LICENSE NEEDS RESEARCH**
- **Performance:**
  - Latency: 3.6ms (image) + 3.3ms (text) na iPhone 12
  - 2-5x rychlejší než standardní CLIP
  - 2.8x menší než ViT-B/16

**Výhody:**
- ✅ 2-5x rychlejší inference
- ✅ Menší model size
- ✅ Lepší accuracy (v některých benchmarks)
- ✅ Lower power consumption

**Nevýhody:**
- ⚠️ Apple license unclear (research needed)
- ⚠️ CoreML → ONNX conversion complexity
- ⚠️ Less mature než OpenAI CLIP

**Upgrade path:**
```
Current: OpenAI CLIP (~150 MB, ~30ms latency)
    ↓
Replace with: MobileCLIP-S0 (~40 MB, ~7ms latency)
    ↓
Result: 3x menší, 4x rychlejší, lepší battery life
```

**Implementační náročnost:** STŘEDNÍ-VYSOKÁ
- Research Apple ML license
- CoreML → ONNX conversion
- Benchmark comparison (accuracy, speed)
- Migration from CLIP → MobileCLIP
- Regression testing

**Estimated effort:** 2-3 týdny (včetně research)

**Action Items:**
- [ ] Research MobileCLIP license (MIT? Apache? Proprietary?)
- [ ] Test CoreML → ONNX conversion
- [ ] Benchmark vs current CLIP
- [ ] F-Droid compatibility check

**GitHub References:**
- [MobileCLIP GitHub](https://github.com/apple/ml-mobileclip)
- [Apple ML Research](https://machinelearning.apple.com/research/mobileclip)
- [arXiv Paper](https://arxiv.org/abs/2311.17049)

---

### 8. Music Embeddings ⭐⭐

**Status:** 🔴 NOT FEASIBLE - Too heavyweight

**Modely:**
- **Jukebox (OpenAI):** >1 GB model size
- **MusicGen (Meta):** >500 MB model size
- **MusicLM (Google):** Heavy model
- **ONNX status:** In progress (2024) - [GitHub Issue](https://github.com/huggingface/optimum/issues/1297)

**Use cases:**
1. Music library organization
2. "Find similar songs"
3. Genre classification
4. Mood-based search

**Proč NE:**
- ❌ Model size >1 GB (impractical pro mobile)
- ❌ ONNX conversion není ready
- ❌ Inference latency vysoká
- ❌ Battery consumption significant

**Alternativa:**
- Music metadata embeddings (artist, genre, lyrics)
- Použij text embeddings (MiniLM-L6-v2)
- Acoustic features (tempo, key) + lightweight ML

**Verdict:** Zatím **NE** - wait for lightweight models

**GitHub References:**
- [Jukebox Embeddings Dataset](https://huggingface.co/datasets/jonflynn/musicnet_jukebox_embeddings)
- [MusicGen (Meta)](https://huggingface.co/facebook/musicgen-melody)
- [MusicLM PyTorch](https://github.com/lucidrains/musiclm-pytorch)

---

### 9. Time Series Embeddings (User Behavior Patterns) ⭐⭐

**Status:** 🟡 INTERESTING - Ale ne core feature

**Modely (2024-2025):**
- **Tiny Time Mixers (IBM)** - Released June 2024
  - MLP-based (ne transformer)
  - Lightweight pro edge deployment
  - Adaptive patching

- **Foundation Models:**
  - TimesFM (Google)
  - Chronos (Amazon)
  - Moirai (Salesforce)
  - TimeGPT

**ONNX:** ✅ Conversions podporovány (ONNX/TensorRT)

**Use cases:**
1. **Photo-taking behavior:**
   - "When do you usually take photos?"
   - Temporal pattern detection
   - Predict next photo session

2. **App usage patterns:**
   - App launch sequences
   - Usage prediction
   - Behavioral insights

3. **Location patterns:**
   - Regular routes
   - Favorite places
   - Travel pattern detection

4. **Sensor data embeddings:**
   - Accelerometer/gyroscope patterns
   - Activity recognition
   - Context awareness

**Technická implementace:**
```kotlin
// Time series data
val photoTimestamps = listOf(
    "2024-08-15 14:30",
    "2024-08-15 14:35",
    "2024-08-20 09:00",
    // ...
)

// Extract temporal features
val features = extractTemporalFeatures(photoTimestamps)

// Time series embedding
val embedding = timeSeriesModel.encode(features)

// Pattern detection
val patterns = detectPatterns(embedding)
// Result: "You usually take photos on weekends between 2-5 PM"
```

**Výhody:**
- ✅ Unique insights
- ✅ Behavioral recommendations
- ✅ Lightweight models exist

**Nevýhody:**
- ⚠️ Privacy concerns (tracking behavior)
- ⚠️ Limited use case pro media app
- ⚠️ Complexity vs value

**Relevance:** STŘEDNÍ - zajímavé, ale ne core feature

**Implementační náročnost:** VYSOKÁ
- Time series feature engineering
- Model selection & integration
- Privacy-preserving design
- UI/UX pro insights

**Estimated effort:** 3-4 týdny

**Verdict:** Zajímavé pro **future research**, ale ne pro nearest releases

**GitHub References:**
- [Tiny Time Mixers (IBM)](https://huggingface.co/ibm/TTM)
- [TimesFM (Google)](https://github.com/google-research/timesfm)
- [Chronos (Amazon)](https://github.com/amazon-science/chronos-forecasting)

---

## ❌ TIER 3: NE PRO SMARTSCAN

### Code Embeddings (CodeBERT, GraphCodeBERT)

**Proč NE:**
- ❌ Irelevantní pro media organization app
- ❌ Wrong domain

**Model:** CodeBERT (Microsoft)
- **GitHub:** [microsoft/CodeBERT](https://github.com/microsoft/CodeBERT)
- **Use case:** Code search, documentation

**Verdict:** Skip

---

### 3D Model Embeddings

**Proč NE:**
- ❌ Irelevantní pro SmartScan
- ❌ Výzkumná oblast
- ❌ Heavy models

**Verdict:** Skip

---

### Graph Neural Networks (Knowledge Graphs)

**Proč NE:**
- ❌ Příliš komplexní
- ❌ Overkill pro SmartScan
- ❌ Implementační overhead

**Use cases (theoretical):**
- Contact relationship graphs
- Photo social networks
- App usage graphs

**Model:** GNN embeddings
- **Research:** [Nature Scientific Reports 2025](https://www.nature.com/articles/s41598-025-05260-1)
- **ONNX:** Supported

**Verdict:** Zajímavé pro research, ale ne praktické

---

### Medical/Legal/Scientific Domain Embeddings

**Proč NE:**
- ❌ Wrong domain
- ❌ Irelevantní

**Modely:**
- MedCLIP (medical imaging)
- BioBERT (clinical notes)
- LegalBERT (legal documents)
- SciBERT (scientific papers)

**Verdict:** Skip

---

## 📊 PRIORITY MATRIX

| Feature | Model | Size | F-Droid | Value | Effort | Priority |
|---------|-------|------|---------|-------|--------|----------|
| **OCR embeddings** | PaddleOCR | 3 MB | ✅ | ⭐⭐⭐⭐⭐ | Střední | **#1** |
| **Document search** | MiniLM-L6-v2 | 23 MB | ✅ | ⭐⭐⭐⭐⭐ | Vysoký | **#2** (v1.2.0) |
| **Metadata search** | MiniLM-L6-v2 | 0 MB | ✅ | ⭐⭐⭐⭐ | Nízký | **#3** |
| **UI element search** | CLIP (existing) | 0 MB | ✅ | ⭐⭐⭐⭐ | Velmi nízký | **#4** |
| **Audio embeddings** | Wav2Vec2 | 50-100 MB | ✅ | ⭐⭐⭐⭐ | Vysoký | **#5** |
| **Video transcripts** | Whisper | varies | ✅ | ⭐⭐⭐ | Vysoký | **#6** |
| **MobileCLIP upgrade** | MobileCLIP-S0 | 40 MB | ⚠️ | ⭐⭐⭐⭐ | Střední | **#7** (research) |
| **Time series** | TTM/TimesFM | varies | ✅ | ⭐⭐ | Vysoký | #8 (future) |
| **Music embeddings** | Jukebox | >1 GB | ✅ | ⭐⭐ | Velmi vysoký | ❌ (too heavy) |

---

## 🗺️ IMPLEMENTAČNÍ ROADMAP

### **v1.2.0 - Document Search** (aktuálně v plánu)
- ✅ PDF/MD/TXT embeddings
- ✅ Semantic document search
- ✅ File-based indexing
- **Timeline:** 5-6 týdnů

### **v1.3.0 - OCR & Screenshot Search** 🔥 HIGHLY RECOMMENDED
- PaddleOCR integration (3 MB)
- Screenshot text extraction
- Business card organization
- Combined text + image search
- **Timeline:** 2-3 týdny

### **v1.3.x - Quick Wins**
- Metadata embeddings (reuse MiniLM)
- UI element search improvements (reuse CLIP)
- Query suggestions
- **Timeline:** 1-2 týdny

### **v1.4.0 - Audio & Voice**
- Wav2Vec2 integration (50-100 MB)
- Voice memo search
- Audio similarity
- Speaker recognition
- **Timeline:** 3-4 týdny

### **v1.5.0 - Enhanced Video**
- Video transcript embeddings (Whisper)
- Combined frame + transcript search
- Multi-modal video search
- **Timeline:** 2-3 týdny

### **v2.0.0 - Performance & Research**
- MobileCLIP upgrade (research license first)
- 2-5x faster inference
- Reduced battery consumption
- Advanced features (time series, patterns)
- **Timeline:** TBD

---

## 💡 QUICK WINS - Nejjednodušší implementace

### 1. **UI Element Search** (3-5 dnů)
- Zero new models
- Reuse CLIP
- Pouze UX improvements

### 2. **Metadata Search** (1 týden)
- Reuse MiniLM-L6-v2
- EXIF extraction (už máš)
- Minimal code changes

### 3. **Query Suggestions** (2-3 dny)
- Hardcoded suggestions
- "Try searching for..."
- Better UX

---

## 🚀 KILLER FEATURES - Nejvyšší hodnota

### 1. **OCR Screenshot Search** 🔥
- **Proč:** Každý má stovky screenshotů, nikdo je neumí najít
- **Konkurence:** Téměř žádná Android app to nemá
- **Model:** Pouze 3 MB
- **Value:** ⭐⭐⭐⭐⭐

### 2. **Voice Memo Search**
- **Proč:** Unique feature
- **Konkurence:** Velmi málo apps
- **Use case:** Profesionální users (novináři, studenti)
- **Value:** ⭐⭐⭐⭐

### 3. **Document Search** (už plánuješ)
- **Proč:** PDF search je standard, ale málo apps má semantic search
- **Value:** ⭐⭐⭐⭐⭐

---

## ⚠️ DŮLEŽITÉ POZNÁMKY

### Model Size Management
**Celková velikost modelů:**
```
Current (v1.1.3):
- CLIP IMAGE_ENCODER: ~75 MB
- CLIP TEXT_ENCODER: ~75 MB
Total: ~150 MB

After v1.2.0 (Document Search):
- + MiniLM-L6-v2: +23 MB
Total: ~173 MB

After v1.3.0 (OCR):
- + PaddleOCR: +3 MB
Total: ~176 MB

After v1.4.0 (Audio):
- + Wav2Vec2: +50-100 MB
Total: ~226-276 MB

After v1.5.0 (Video Transcript):
- + Whisper-tiny: +40 MB
Total: ~266-316 MB
```

**Recommendations:**
- ✅ Opt-in model downloads (user choice)
- ✅ Model pruning (remove unused models)
- ✅ Quantization (FP16, INT8)
- ✅ On-demand loading (lazy initialization)

### Memory Management
**Concurrent models:**
```kotlin
// Bad: Load all models at once
val clipModel = loadCLIP()
val textModel = loadMiniLM()
val ocrModel = loadPaddleOCR()
val audioModel = loadWav2Vec2()
// Total RAM: ~500 MB+

// Good: Lazy loading + unloading
val modelManager = ModelManager()
modelManager.loadOnDemand(ModelType.OCR)
// Use OCR model
modelManager.unload(ModelType.OCR)
```

**Best practices:**
- Only load models when needed
- Unload models after use
- Monitor memory pressure
- Implement model cache

### Privacy Considerations
**Sensitive data:**
- ⚠️ OCR může extrahovat personal info (passwords, credit cards)
- ⚠️ Audio embeddings = voice recording privacy
- ⚠️ Metadata může obsahovat location data
- ⚠️ Time series = behavioral tracking

**Privacy-preserving design:**
- ✅ On-device processing (žádný cloud)
- ✅ Optional features (user consent)
- ✅ Data encryption (embeddings at rest)
- ✅ Clear privacy policy

### F-Droid Compatibility Checklist
**Pro každý nový model:**
- [ ] Open-source license (Apache, MIT, BSD)
- [ ] No proprietary dependencies
- [ ] Reproducible builds
- [ ] No telemetry/tracking
- [ ] Source code available

**Problematic models:**
- ❌ Google ML Kit (proprietary)
- ⚠️ MobileCLIP (license unclear)
- ❌ Closed-source embeddings APIs

---

## 🔗 HLAVNÍ ZDROJE & REFERENCE

### OCR
- [PaddleOCR GitHub](https://github.com/PaddlePaddle/PaddleOCR)
- [PaddleOCR ONNX Conversion](https://github.com/RapidAI/PaddleOCRModelConvert)
- [PaddleOCR Docs](https://paddlepaddle.github.io/PaddleOCR/)

### Audio Embeddings
- [Wav2Vec2 ONNX Collection](https://huggingface.co/darjusul/wav2vec2-ONNX-collection)
- [Wespeaker (Speaker Embeddings)](https://github.com/wenet-e2e/wespeaker)
- [Whisper GitHub](https://github.com/openai/whisper)
- [Whisper.cpp (lightweight)](https://github.com/ggerganov/whisper.cpp)

### Text Embeddings
- [Sentence-Embeddings-Android](https://github.com/shubham0204/Sentence-Embeddings-Android)
- [all-MiniLM-L6-v2 ONNX](https://huggingface.co/onnx-models/all-MiniLM-L6-v2-onnx)
- [Sentence Transformers](https://www.sbert.net/)

### MobileCLIP
- [MobileCLIP GitHub](https://github.com/apple/ml-mobileclip)
- [Apple ML Research](https://machinelearning.apple.com/research/mobileclip)
- [arXiv Paper](https://arxiv.org/abs/2311.17049)

### Time Series
- [Tiny Time Mixers (IBM)](https://huggingface.co/ibm/TTM)
- [TimesFM (Google)](https://github.com/google-research/timesfm)
- [Chronos (Amazon)](https://github.com/amazon-science/chronos-forecasting)

### ONNX Runtime
- [ONNX Runtime Mobile](https://onnxruntime.ai/docs/tutorials/mobile/)
- [ONNX Model Zoo](https://github.com/onnx/models)
- [HuggingFace Optimum](https://huggingface.co/docs/optimum/onnxruntime/modeling_ort)

### Research Papers (2024-2025)
- [MobileCLIP Paper](https://arxiv.org/abs/2311.17049)
- [MobileViCLIP (Video)](https://arxiv.org/html/2508.07312v1)
- [GNN for Android App Prediction](https://www.nature.com/articles/s41598-025-05260-1)
- [Audio Processing Research](https://www.sciencedirect.com/science/article/abs/pii/S0167639324000761)

---

## 🎯 DOPORUČENÍ PRO JAROSLAVA

### Immediate Actions (po v1.2.0 Document Search)

**1. Implementuj OCR embeddings (v1.3.0)** 🔥
- Nejlepší value/effort ratio
- Killer feature (screenshot search)
- Malý model (3 MB)
- 2-3 týdny práce

**2. Quick wins (v1.3.x)**
- Metadata search (1 týden)
- UI element search improvements (3-5 dnů)
- Query suggestions (2-3 dny)

**3. Research MobileCLIP license**
- Ověř F-Droid compatibility
- Pokud OK → upgrade CLIP v v2.0.0
- 2-5x performance boost

### Medium-term (v1.4.0+)

**4. Audio embeddings (pokud má smysl pro user base)**
- Voice memo search
- 3-4 týdny práce
- Unique feature

### Long-term Research

**5. Time series behavioral insights**
- Zajímavé pro future
- Privacy-preserving design kritický
- Možná collaboration s research institutions

---

## ✅ ZÁVĚR

**SmartScan má potenciál stát se THE Swiss Army Knife pro media organization:**

### Aktuální stav (v1.1.3):
- ✅ Image search (CLIP)
- ✅ Video frame search (CLIP)
- ✅ Auto-organization (prototypes)

### Po v1.2.0 (Document Search):
- ✅ PDF/MD/TXT search
- ✅ Semantic document organization

### Po v1.3.0 (OCR - HIGHLY RECOMMENDED):
- ✅ Screenshot search 🔥
- ✅ Business card organization 🔥
- ✅ Handwritten notes
- ✅ Metadata search

### Po v1.4.0 (Audio):
- ✅ Voice memo search
- ✅ Podcast organization
- ✅ Speaker recognition

### Vision (v2.0.0+):
- ✅ Unified multi-modal search
- ✅ Behavioral insights (time series)
- ✅ Performance optimizations (MobileCLIP)
- ✅ Advanced features (contextual recommendations)

**Celkové hodnocení:** 🟢 **SmartScan má jasnou roadmap pro 2-3 roky development**

---

**Datum vytvoření:** 2025-10-27
**Autor analýzy:** Claude Code + Badatel (research specialist)
**Pro:** Jaroslav (SmartScan developer)
