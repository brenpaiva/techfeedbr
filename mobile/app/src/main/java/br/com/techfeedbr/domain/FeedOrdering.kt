package br.com.techfeedbr.domain

import br.com.techfeedbr.data.preferences.UserPreferences
import java.time.Instant

class FeedOrdering(private val ranker: PersonalizationRanker = PersonalizationRanker()) {
    fun order(items: List<ContentItem>, mode: SortMode, preferences: UserPreferences): List<ContentItem> =
        when (mode) {
            SortMode.RELEVANCE -> ranker.rank(items, preferences)
            SortMode.DATE -> items.sortedWith(
                compareByDescending<ContentItem> { item ->
                    runCatching { Instant.parse(item.publishedAt) }.getOrDefault(Instant.EPOCH)
                }.thenBy(ContentItem::id)
            )
        }
}
