package br.com.techfeedbr.data

import br.com.techfeedbr.data.local.ContentEntity
import br.com.techfeedbr.data.local.ContentStateEntity
import br.com.techfeedbr.data.local.EvaluationEventEntity
import br.com.techfeedbr.data.local.TechFeedDao
import br.com.techfeedbr.data.remote.ContentItemDto
import br.com.techfeedbr.data.remote.FeedResponseDto
import br.com.techfeedbr.data.remote.SourceStatusDto
import br.com.techfeedbr.data.remote.TechFeedApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedRepositoryTest {
    @Test fun disabledSourcesStayOutOfDownloadedAndCachedFeed() = runBlocking {
        val api = FakeApi().apply { includeDisabled = true }
        val dao = FakeDao()
        dao.upsertContent(listOf(ContentEntity(
            id = "old-tabnews", title = "Notícia antiga", summary = null, imageUrl = null,
            canonicalUrl = "https://tabnews.com.br/old", contentType = "ARTICLE", language = "pt-BR",
            publishedAt = "2026-10-09T12:00:00Z", author = null, primarySource = "TABNEWS",
            sources = setOf("TABNEWS"), categories = emptySet(), tags = emptySet(),
            relevanceScore = 0.5, fetchedAt = "2026-10-09T12:00:00Z",
        )))
        val repository = FeedRepository(api, dao)

        assertTrue(repository.items.first().isEmpty())

        repository.sync()
        assertEquals(listOf("DEV"), repository.items.first().map { it.primarySource })
        api.fail = true
        repository.sync()
        assertEquals(listOf("DEV"), repository.items.first().map { it.primarySource })
    }

    @Test fun loadsAllPagesAndPreservesRealItemsAfterNetworkFailure() = runBlocking {
        val api = FakeApi()
        val dao = FakeDao()
        val repository = FeedRepository(api, dao)

        repository.sync()
        assertEquals(1, repository.items.first().size)
        assertTrue(repository.hasMore.value)
        repository.loadNextPage()
        assertEquals(2, repository.items.first().size)
        assertFalse(repository.hasMore.value)
        assertEquals(listOf(null, "next"), api.requestedCursors)

        api.fail = true
        repository.sync()
        assertEquals(2, repository.items.first().size)
        assertNotNull(repository.error.value)
        assertEquals("Não foi possível atualizar. Exibindo notícias salvas no aparelho.", repository.error.value)
        assertTrue(repository.initialAttemptFinished.value)
    }

    @Test fun manualRefreshPublishesSourceProgressBeforeCompletion() = runBlocking {
        val api = FakeApi().apply { refreshingThenReady = true }
        val repository = FeedRepository(api, FakeDao())

        val refresh = launch { repository.sync(requestRefresh = true) }
        val inProgress = withTimeout(3_000) {
            repository.sourceStatuses.first { statuses -> statuses.any(SourceStatusDto::refreshing) }
        }
        assertTrue(inProgress.single().refreshing)
        assertEquals(1, repository.items.first().size)
        assertTrue(repository.loading.value)

        withTimeout(5_000) { refresh.join() }
        assertFalse(repository.sourceStatuses.value.single().refreshing)
        assertFalse(repository.loading.value)
        assertEquals(2, api.feedCalls)
    }

    @Test fun cancelledRefreshDoesNotFinishInitialAttempt() = runBlocking {
        val repository = FeedRepository(FakeApi().apply { refreshingThenReady = true }, FakeDao())

        val refresh = launch { repository.sync(requestRefresh = true) }
        withTimeout(3_000) {
            repository.sourceStatuses.first { statuses -> statuses.any(SourceStatusDto::refreshing) }
        }
        refresh.cancelAndJoin()

        assertFalse(repository.initialAttemptFinished.value)
        assertFalse(repository.loading.value)
        assertEquals(null, repository.error.value)
    }

    @Test fun changingCombinedFiltersStartsNewCursorSequence() = runBlocking {
        val api = FakeApi()
        val repository = FeedRepository(api, FakeDao())

        repository.sync(query = "Kotlin", categories = setOf("Mobile"), sources = setOf("TABNEWS"))
        repository.loadNextPage()
        repository.sync(query = "Python", categories = setOf("Dados"), sources = setOf("DEV"))

        assertEquals(listOf(null, "next", null), api.requestedCursors)
        assertEquals(listOf("Kotlin", "Kotlin", "Python"), api.requestedQueries)
        assertEquals(listOf(listOf("Mobile"), listOf("Mobile"), listOf("Dados")), api.requestedCategories)
        assertEquals(listOf(listOf("TABNEWS"), listOf("TABNEWS"), listOf("DEV")), api.requestedSources)
        assertEquals(listOf("pt", "pt", "pt"), api.requestedLanguages)
        assertFalse(repository.hasMore.value)
    }

    @Test fun changingLanguageStartsFreshCursorAndRequestsAllLanguages() = runBlocking {
        val api = FakeApi()
        val repository = FeedRepository(api, FakeDao())

        repository.sync()
        repository.loadNextPage()
        repository.sync(portugueseOnly = false)

        assertEquals(listOf(null, "next", null), api.requestedCursors)
        assertEquals(listOf("pt", "pt", null), api.requestedLanguages)
    }

    private class FakeApi : TechFeedApi {
        val requestedCursors = mutableListOf<String?>()
        val requestedQueries = mutableListOf<String?>()
        val requestedCategories = mutableListOf<List<String>>()
        val requestedSources = mutableListOf<List<String>>()
        val requestedLanguages = mutableListOf<String?>()
        var fail = false
        var refreshingThenReady = false
        var includeDisabled = false
        var feedCalls = 0
        override suspend fun feed(cursor: String?, limit: Int, query: String?, categories: List<String>,
                                  sources: List<String>, language: String?, sort: String): FeedResponseDto {
            if (fail) error("offline")
            feedCalls++
            requestedCursors += cursor
            requestedQueries += query
            requestedCategories += categories
            requestedSources += sources
            requestedLanguages += language
            val id = if (query == "Python") "python" else if (cursor == null) "one" else "two"
            val items = listOf(item(id)) + if (includeDisabled) {
                listOf(item("tabnews", "TABNEWS"), item("gnews", "GNEWS"))
            } else emptyList()
            return FeedResponseDto(items = items,
                nextCursor = if (cursor == null && query != "Python") "next" else null,
                sourceStatuses = if (refreshingThenReady) listOf(SourceStatusDto(
                    source = "TABNEWS", displayName = "TabNews", health = "AVAILABLE",
                    refreshing = feedCalls == 1)) else emptyList())
        }
        override suspend fun refresh(): List<SourceStatusDto> = emptyList()
        override suspend fun sourceStatus(): List<SourceStatusDto> = emptyList()
        private fun item(id: String, source: String = "DEV") = ContentItemDto(id = id, title = "Kotlin $id",
            canonicalUrl = "https://example.org/$id", contentType = "ARTICLE", language = "pt-BR",
            publishedAt = "2026-10-09T12:00:00Z", primarySource = source,
            fetchedAt = "2026-10-09T12:00:00Z")
    }

    private class FakeDao : TechFeedDao {
        private val content = MutableStateFlow<List<ContentEntity>>(emptyList())
        private val states = MutableStateFlow<List<ContentStateEntity>>(emptyList())
        override fun observeContent() = content
        override fun observeStates() = states
        override suspend fun upsertContent(items: List<ContentEntity>) {
            content.value = (content.value + items).distinctBy(ContentEntity::id)
        }
        override suspend fun upsertState(state: ContentStateEntity) {
            states.value = (states.value + state).distinctBy(ContentStateEntity::contentId)
        }
        override suspend fun state(contentId: String) = states.value.firstOrNull { it.contentId == contentId }
        override suspend fun insertEvaluationEvent(event: EvaluationEventEntity) = Unit
        override suspend fun evaluationEvents(sessionCode: String): List<EvaluationEventEntity> = emptyList()
        override fun observeEvaluationSessions() = MutableStateFlow(emptyList<String>())
        override suspend fun deleteEvaluationSession(sessionCode: String) = Unit
    }
}
