package com.fpf.smartscan.data.tags

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_tags")
data class UserTagEntity(
    @PrimaryKey
    val name: String,              // "Rekonstrukce domu" - zobrazované jméno
    val description: String,       // "fotografie renovace domu..." - popis pro CLIP
    val embedding: FloatArray,     // CLIP embedding z description
    val threshold: Float = 0.30f,  // Práh pro automatické přiřazení (0.0-1.0)
    val color: Int = 0xFF2196F3.toInt(), // Barva tagu pro UI (Material Blue default)
    val isActive: Boolean = true,  // Zapnuto/vypnuto pro auto-tagging
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserTagEntity

        if (name != other.name) return false
        if (description != other.description) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (threshold != other.threshold) return false
        if (color != other.color) return false
        if (isActive != other.isActive) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + threshold.hashCode()
        result = 31 * result + color
        result = 31 * result + isActive.hashCode()
        return result
    }
}
