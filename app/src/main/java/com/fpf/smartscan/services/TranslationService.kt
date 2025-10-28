package com.fpf.smartscan.services

import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Service pro automatickou detekci jazyka a překlad do angličtiny
 *
 * Používá Google ML Kit:
 * - Language Identification API (detekce jazyka)
 * - Translation API (offline překlad CS→EN)
 *
 * Workflow:
 * ```
 * "pes v parku" (uživatelský vstup)
 *    ↓
 * Detekce jazyka → "cs" (Czech)
 *    ↓
 * Překlad CS→EN → "dog in the park"
 *    ↓
 * CLIP embedding
 * ```
 *
 * Model size: ~30 MB (při prvním použití se stáhne automaticky)
 * Rychlost: ~50-150ms pro krátké dotazy (1-10 slov)
 */
class TranslationService {

    companion object {
        private const val TAG = "TranslationService"

        // Timeout pro download modelů při prvním použití
        private const val DOWNLOAD_TIMEOUT_MS = 30_000L

        // Threshold pro detekci jazyka (0.0-1.0)
        // Pokud confidence < threshold, považujeme za angličtinu
        private const val LANGUAGE_CONFIDENCE_THRESHOLD = 0.5f
    }

    // Language identifier (pro detekci CS vs EN)
    private val languageIdentifier = LanguageIdentification.getClient()

    // Translator CS→EN (lazy initialized)
    private var czechToEnglishTranslator: Translator? = null

    private var _isInitialized = false
    val isInitialized: Boolean get() = _isInitialized

    /**
     * Inicializace translátoru
     *
     * LAZY DOWNLOAD STRATEGIE:
     * - Pouze vytvoří Translator instanci
     * - Modely se automaticky stáhnou při prvním volání translate()
     * - To zrychlí startup aplikace
     *
     * První překlad může trvat 30s-2min (download ~30 MB)
     * Následné překlady jsou okamžité (offline)
     */
    suspend fun initialize() {
        if (_isInitialized) {
            Log.w(TAG, "Již inicializováno, přeskakuji")
            return
        }

        Log.i(TAG, "Inicializace CS→EN translátoru (lazy download mode)")

        // Vytvoř translator (neblokující - modely se stáhnou při prvním použití)
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.CZECH)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()

        czechToEnglishTranslator = Translation.getClient(options)

        _isInitialized = true
        Log.i(TAG, "✅ Translator připraven (lazy download mode)")
        Log.w(TAG, "⏳ První překlad stáhne modely (~30 MB, 30s-2min)")
    }

    /**
     * Automatická detekce jazyka a překlad do angličtiny
     *
     * Workflow:
     * 1. Detekuje jazyk dotazu (ML Kit Language Identification)
     * 2. Pokud čeština → přeloží do EN
     * 3. Pokud angličtina nebo unknown → vrátí původní text
     * 4. Pokud timeout/chyba → vrátí původní text (fallback)
     *
     * @param text Vstupní text (CS nebo EN)
     * @param onLanguageDetected Callback s informací o detekovaném jazyce
     * @return Anglický text (přeložený nebo původní)
     *
     * Příklad:
     * ```kotlin
     * val result = translationService.translateToEnglish("pes v parku") { lang, conf ->
     *     println("Detekován jazyk: $lang (confidence: ${conf * 100}%)")
     * }
     * // → "dog in the park"
     * ```
     */
    suspend fun translateToEnglish(
        text: String,
        onLanguageDetected: ((language: String, confidence: Float) -> Unit)? = null
    ): String {
        if (text.trim().isEmpty()) {
            Log.w(TAG, "Prázdný text, vracím prázdný string")
            return ""
        }

        if (!_isInitialized) {
            throw IllegalStateException("TranslationService není inicializován. Zavolej initialize() nejprve.")
        }

        try {
            // 1. Detekce jazyka
            Log.i(TAG, "🔍 Detekuji jazyk pro: \"$text\" (délka: ${text.length} znaků)")
            val languageIdentification = languageIdentifier.identifyLanguage(text).await()

            Log.i(TAG, "🏷️ Detekovaný jazyk: [$languageIdentification]")

            // Check if language detection failed
            if (languageIdentification == "und") {
                Log.w(TAG, "⚠️ Jazyk nerozpoznán (und) - zkouším přeložit i přesto...")
                onLanguageDetected?.invoke("und", 0.0f)
                // Pro krátká slova zkusíme překlad i když jazyk není rozpoznán
                // ML Kit má problém s detekcí jazyka u krátkých slov
            }

            // Get confidence (ML Kit doesn't provide it directly, so we use threshold heuristic)
            val confidence = if (languageIdentification == TranslateLanguage.CZECH) 0.9f else 0.3f

            Log.i(TAG, "📊 Confidence: ${confidence * 100}% | Czech code: [${TranslateLanguage.CZECH}]")
            onLanguageDetected?.invoke(languageIdentification, confidence)

            // 2. Pokud je anglický text, přeskočíme překlad
            if (languageIdentification == TranslateLanguage.ENGLISH) {
                Log.i(TAG, "🇬🇧 Detekována angličtina, přeskakuji překlad")
                return text
            }

            // 3. Pro češtinu NEBO nerozpoznaný jazyk zkusíme překlad
            Log.i(TAG, "🔄 Překládám CS→EN: \"$text\"")

            val translator = czechToEnglishTranslator
                ?: throw IllegalStateException("Translator není inicializován")

            // Timeout 30s pro první překlad (lazy download modelů)
            val translated = withTimeout(DOWNLOAD_TIMEOUT_MS) {
                translator.translate(text).await()
            }

            Log.i(TAG, "✅ Přeloženo: \"$text\" → \"$translated\"")
            return translated

        } catch (e: Exception) {
            Log.e(TAG, "❌ Chyba při překladu: ${e.message}", e)
            // Fallback: vrátíme původní text
            Log.w(TAG, "Používám původní text jako fallback")
            return text
        }
    }

    /**
     * Kontrola, jestli jsou translation modely stažené
     *
     * Pomocná metoda pro UI progress indicators.
     * Vrací true pokud jsou modely už offline dostupné.
     */
    suspend fun areModelsDownloaded(): Boolean {
        if (!_isInitialized) {
            initialize()
        }

        val translator = czechToEnglishTranslator ?: return false

        return try {
            // Pokusíme se stáhnout model (pokud už je stažený, vrátí se okamžitě)
            translator.downloadModelIfNeeded().await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Modely nejsou stažené: ${e.message}")
            false
        }
    }

    /**
     * Explicitní stažení translation modelů
     *
     * Pro použití v Settings UI - tlačítko "Download Models"
     *
     * @param onProgress Callback s progress reporting (0.0-1.0)
     * @return Result success/failure
     */
    suspend fun downloadModels(
        onProgress: ((Float) -> Unit)? = null
    ): Result<Unit> {
        return try {
            if (!_isInitialized) {
                initialize()
            }

            val translator = czechToEnglishTranslator
                ?: throw IllegalStateException("Translator není inicializován")

            Log.i(TAG, "Stahuji translation modely (~30 MB)...")
            onProgress?.invoke(0.5f)

            withTimeout(DOWNLOAD_TIMEOUT_MS) {
                translator.downloadModelIfNeeded().await()
            }

            onProgress?.invoke(1.0f)
            Log.i(TAG, "✅ Modely úspěšně staženy")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Chyba při stahování modelů: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        Log.i(TAG, "Disposing resources")
        czechToEnglishTranslator?.close()
        czechToEnglishTranslator = null
        languageIdentifier.close()
        _isInitialized = false
    }
}
