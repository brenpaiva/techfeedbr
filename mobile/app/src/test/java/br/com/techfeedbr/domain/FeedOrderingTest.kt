package br.com.techfeedbr.domain

import br.com.techfeedbr.data.preferences.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedOrderingTest {
    private val ordering = FeedOrdering()

    @Test fun recentSortUsesInstantAcrossTimeZones() {
        val later = item("later", "2026-10-09T08:00:00-03:00")
        val earlier = item("earlier", "2026-10-09T10:30:00Z")

        val result = ordering.order(listOf(earlier, later), SortMode.DATE, UserPreferences())

        assertEquals(listOf("later", "earlier"), result.map(ContentItem::id))
    }

    @Test fun forYouPromotesPreferredCategoryWhileRecentUsesDate() {
        val preferred = item("preferred", "2026-10-08T12:00:00Z", "Inteligência Artificial")
        val newest = item("newest", "2026-10-09T12:00:00Z", "Desenvolvimento")
        val preferences = UserPreferences(categories = setOf("Inteligência Artificial"))

        assertEquals("preferred", ordering.order(listOf(newest, preferred), SortMode.RELEVANCE, preferences).first().id)
        assertEquals("newest", ordering.order(listOf(newest, preferred), SortMode.DATE, preferences).first().id)
    }

    private fun item(id: String, publishedAt: String, category: String = "Desenvolvimento") = ContentItem(
        id = id, title = id, summary = null, imageUrl = null,
        canonicalUrl = "https://example.org/$id", contentType = "ARTICLE", language = "pt-BR",
        publishedAt = publishedAt, author = null, primarySource = "DEV", sources = setOf("DEV"),
        categories = setOf(category), tags = emptySet(), backendScore = 0.5,
        favorite = false, readLater = false, viewedAt = null,
    )
}
