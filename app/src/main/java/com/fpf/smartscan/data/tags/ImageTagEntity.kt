package com.fpf.smartscan.data.tags

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "image_tags",
    indices = [
        Index(value = ["imageId"]),
        Index(value = ["tagName"]),
        Index(value = ["imageId", "tagName"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserTagEntity::class,
            parentColumns = ["name"],
            childColumns = ["tagName"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ImageTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imageId: Long,              // MediaStore ID obrázku
    val tagName: String,            // Reference na UserTagEntity.name
    val confidence: Float,          // Jistota 0.0-1.0 (cosine similarity)
    val isUserAssigned: Boolean = false, // true = user manuálně přiřadil, false = auto
    val assignedAt: Long = System.currentTimeMillis()
)
