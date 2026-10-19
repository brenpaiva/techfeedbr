package br.com.techfeedbr.domain

import br.com.techfeedbr.data.preferences.UserPreferences
import java.time.Duration
import java.time.Instant
import kotlin.math.exp

class PersonalizationRanker {
    fun rank(items: List<ContentItem>, preferences: UserPreferences): List<ContentItem> {
        val affinity = mutableMapOf<String, Double>()
        items.forEach { item ->
            val interaction = when {
                item.favorite -> 1.5
                item.readLater -> 1.0
                item.viewedAt != null -> 0.25
                else -> 0.0
            }
            (item.categories + item.tags).forEach { key -> affinity[key] = (affinity[key] ?: 0.0) + interaction }
        }
        return items.map { item ->
            val preferenceScore = item.categories.count(preferences.categories::contains) * 4.0
            val sourceScore = if (item.sources.any(preferences.sources::contains)) 3.0 else 0.0
            val historyScore = (item.categories + item.tags).sumOf { (affinity[it] ?: 0.0).coerceAtMost(3.0) }
                .coerceAtMost(4.0)
            val ageDays = runCatching {
                Duration.between(Instant.parse(item.publishedAt), Instant.now()).toHours().coerceAtLeast(0) / 24.0
            }.getOrDefault(30.0)
            val recency = 3.0 * exp(-ageDays / 7.0)
            val readPenalty = if (item.viewedAt != null && !item.favorite && !item.readLater) 2.0 else 0.0
            item.copy(personalScore = preferenceScore + sourceScore + historyScore + recency + item.backendScore - readPenalty)
        }.sortedByDescending(ContentItem::personalScore)
    }
}

