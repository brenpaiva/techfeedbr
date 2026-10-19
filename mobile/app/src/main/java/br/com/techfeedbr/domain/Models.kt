package br.com.techfeedbr.domain

data class ContentItem(
    val id: String,
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
    val backendScore: Double,
    val favorite: Boolean,
    val readLater: Boolean,
    val viewedAt: Long?,
    val personalScore: Double = 0.0,
    val isExample: Boolean = false,
)

enum class SavedMode { FAVORITES, READ_LATER }
enum class SortMode { DATE, RELEVANCE }
