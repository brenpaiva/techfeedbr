package br.com.techfeedbr.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content")
data class ContentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String?,
    val imageUrl: String?,
    val canonicalUrl: String,
    val contentType: String,
    val language: String,
    val publishedAt: String,
    val author: String?,
    val primarySource: String,
    val sources: Set<String>,
    val categories: Set<String>,
    val tags: Set<String>,
    val relevanceScore: Double,
    val fetchedAt: String,
)

@Entity(tableName = "content_state")
data class ContentStateEntity(
    @PrimaryKey val contentId: String,
    val favorite: Boolean = false,
    val readLater: Boolean = false,
    val viewedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "evaluation_event")
data class EvaluationEventEntity(
    @PrimaryKey val id: String,
    val sessionCode: String,
    val taskCode: String,
    val eventType: String,
    val timestamp: Long,
    val elapsedMs: Long,
    val metadata: String? = null,
)

