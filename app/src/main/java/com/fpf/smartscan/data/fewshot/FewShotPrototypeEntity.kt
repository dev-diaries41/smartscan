package com.fpf.smartscan.data.fewshot

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Few-Shot Prototype Entity
 *
 * Reprezentuje průměrný embedding vytvořený z několika sample obrázků (5-10).
 * Používá se pro personalizované vyhledávání (např. "Barunka", "Jonasek", "Moje auto").
 *
 * Princip:
 * 1. Uživatel vybere 5-10 obrázků osoby/objektu
 * 2. Ořízne relevantní části (obličej, objekt)
 * 3. Systém vytvoří embedding z každého cropu
 * 4. Spočítá průměrný embedding → prototype
 * 5. Prototype lze použít pro vyhledávání (samostatně nebo s textem)
 *
 * Příklad:
 * - Name: "Barunka"
 * - Embedding: [0.23, 0.45, 0.12, ...] (průměr z 8 samplu)
 * - Samples: 8 oříznutých obličejů z různých fotek
 *
 * Vyhledávání:
 * - "Barunka" → používá prototype embedding
 * - "Barunka v lese" → kombinuje prototype + text embedding
 * - "Barunka a Jonasek" → kombinuje 2 prototypes
 */
@Entity(tableName = "few_shot_prototypes")
data class FewShotPrototypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Název prototypu (zobrazovaný uživateli)
     * Např.: "Barunka", "Jonasek", "Moje auto", "Náš pes"
     */
    val name: String,

    /**
     * Průměrný embedding z všech samplu
     * 768 dimenzí pro CLIP model
     */
    val embedding: FloatArray,

    /**
     * Barva pro UI (hex)
     * Např.: 0xFF9C27B0 (fialová)
     */
    val color: Int,

    /**
     * Počet samplu použitých pro vytvoření průměru
     * Doporučeno: 5-10
     */
    val sampleCount: Int,

    /**
     * Timestamp vytvoření (milliseconds)
     */
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Timestamp poslední úpravy (milliseconds)
     * Aktualizuje se při přidání/odebrání samplu
     */
    val updatedAt: Long = System.currentTimeMillis(),

    /**
     * Volitelný popis
     * Např.: "Moje dcera Barunka"
     */
    val description: String? = null,

    /**
     * Kategorie prototypu
     * Možné hodnoty: "person", "object", "scene", "style"
     * Null = uncategorized
     */
    val category: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FewShotPrototypeEntity

        if (id != other.id) return false
        if (name != other.name) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (color != other.color) return false
        if (sampleCount != other.sampleCount) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false
        if (description != other.description) return false
        if (category != other.category) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + color
        result = 31 * result + sampleCount
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (category?.hashCode() ?: 0)
        return result
    }

    companion object {
        /**
         * Podporované kategorie
         */
        const val CATEGORY_PERSON = "person"
        const val CATEGORY_OBJECT = "object"
        const val CATEGORY_SCENE = "scene"
        const val CATEGORY_STYLE = "style"
    }
}
