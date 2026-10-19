package br.com.techfeedbr.domain

import br.com.techfeedbr.data.preferences.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalizationRankerTest {
    private val ranker = PersonalizationRanker()

    @Test
    fun preferredCategoryAndSourceAreRankedFirst() {
        val preferred = item(
            id = "preferred",
            source = "EBC",
            categories = setOf("Segurança"),
        )
        val other = item(
            id = "other",
            source = "DEV",
            categories = setOf("Carreira"),
        )

        val ranked = ranker.rank(
            listOf(other, preferred),
            UserPreferences(categories = setOf("Segurança"), sources = setOf("EBC")),
        )

        assertEquals("preferred", ranked.first().id)
    }

    @Test
    fun viewedUnsavedItemReceivesReadPenalty() {
        val unread = item(id = "unread")
        val viewed = item(id = "viewed").copy(viewedAt = 1L)

        val ranked = ranker.rank(listOf(viewed, unread), UserPreferences())

        assertEquals("unread", ranked.first().id)
    }

    private fun item(
        id: String,
        source: String = "DEV",
        categories: Set<String> = setOf("Dados"),
    ) = ContentItem(
        id = id,
        title = id,
        summary = null,
        imageUrl = null,
        canonicalUrl = "https://example.com/$id",
        contentType = "ARTICLE",
        language = "pt-BR",
        publishedAt = "2026-08-25T12:00:00Z",
        author = null,
        primarySource = source,
        sources = setOf(source),
        categories = categories,
        tags = emptySet(),
        backendScore = 0.5,
        favorite = false,
        readLater = false,
        viewedAt = null,
    )
}
