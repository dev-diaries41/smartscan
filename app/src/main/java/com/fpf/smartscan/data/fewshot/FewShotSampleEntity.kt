package com.fpf.smartscan.data.fewshot

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Few-Shot Sample Entity
 *
 * Reprezentuje jednotlivý sample embedding použitý pro vytvoření prototype.
 * Ukládá odkaz na původní obrázek, crop oblast a embedding tohoto cropu.
 *
 * Důvod existence:
 * - Umožňuje re-compute průměru při přidání/odebrání samplu
 * - Zobrazení thumbnailů v UI
 * - Historie samplu použitých pro prototype
 * - Možnost odstranit špatný sample a re-compute
 *
 * Příklad:
 * - PrototypeId: 5 (Barunka)
 * - ImageUri: "content://media/external/images/media/123"
 * - CropRect: {"left":100,"top":200,"width":300,"height":400}
 * - Embedding: [0.21, 0.47, 0.10, ...] (embedding tohoto konkrétního cropu)
 *
 * Foreign Key:
 * - Při smazání prototype se automaticky smažou všechny jeho samples (CASCADE)
 */
@Entity(
    tableName = "few_shot_samples",
    foreignKeys = [
        ForeignKey(
            entity = FewShotPrototypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["prototypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["prototypeId"])]
)
data class FewShotSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * ID prototypu ke kterému patří tento sample
     * Foreign key → FewShotPrototypeEntity.id
     */
    val prototypeId: Long,

    /**
     * URI původního obrázku
     * Např.: "content://media/external/images/media/123"
     */
    val imageUri: String,

    /**
     * JSON reprezentace crop oblasti
     * Format: {"left": 100, "top": 200, "width": 300, "height": 400}
     *
     * Používá se pro:
     * - Re-crop pokud je potřeba
     * - Zobrazení crop preview v UI
     */
    val cropRect: String,

    /**
     * Embedding tohoto konkrétního oříznutého obrázku
     * 768 dimenzí pro CLIP model
     */
    val embedding: FloatArray,

    /**
     * Timestamp přidání (milliseconds)
     */
    val addedAt: Long = System.currentTimeMillis(),

    /**
     * Volitelná cesta k uloženému thumbnail cropu
     * Např.: "/data/user/0/com.fpf.smartscan/files/fewshot/thumbnails/sample_123.jpg"
     *
     * Používá se pro:
     * - Rychlé zobrazení v UI bez potřeby re-crop
     * - Offline dostupnost i když původní obrázek není dostupný
     */
    val thumbnailPath: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FewShotSampleEntity

        if (id != other.id) return false
        if (prototypeId != other.prototypeId) return false
        if (imageUri != other.imageUri) return false
        if (cropRect != other.cropRect) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (addedAt != other.addedAt) return false
        if (thumbnailPath != other.thumbnailPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + prototypeId.hashCode()
        result = 31 * result + imageUri.hashCode()
        result = 31 * result + cropRect.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + addedAt.hashCode()
        result = 31 * result + (thumbnailPath?.hashCode() ?: 0)
        return result
    }
}

/**
 * Data class pro reprezentaci crop oblasti
 * Používá se pro parsing/serialization cropRect JSON
 */
data class CropRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
) {
    /**
     * Konverze do JSON stringu
     */
    fun toJson(): String {
        return """{"left":$left,"top":$top,"width":$width,"height":$height}"""
    }

    companion object {
        /**
         * Parsing z JSON stringu
         */
        fun fromJson(json: String): CropRect {
            val regex = """"left":(\d+),"top":(\d+),"width":(\d+),"height":(\d+)""".toRegex()
            val match = regex.find(json) ?: throw IllegalArgumentException("Invalid CropRect JSON: $json")
            val (left, top, width, height) = match.destructured
            return CropRect(
                left = left.toInt(),
                top = top.toInt(),
                width = width.toInt(),
                height = height.toInt()
            )
        }
    }
}
