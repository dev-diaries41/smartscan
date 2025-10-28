package com.fpf.smartscan.data.tags

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_tags",
    indices = [
        Index(value = ["mediaId"]),
        Index(value = ["tagName"]),
        Index(value = ["mediaId", "tagName"], unique = true)
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
data class MediaTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaId: Long,              // MediaStore ID média (obrázek nebo video)
    val tagName: String,            // Reference na UserTagEntity.name
    val confidence: Float,          // Jistota 0.0-1.0 (cosine similarity)
    val isUserAssigned: Boolean = false, // true = user manuálně přiřadil, false = auto
    val assignedAt: Long = System.currentTimeMillis()
)
