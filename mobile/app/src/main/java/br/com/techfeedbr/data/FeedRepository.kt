package br.com.techfeedbr.data

import br.com.techfeedbr.data.local.ContentEntity
import br.com.techfeedbr.data.local.ContentStateEntity
import br.com.techfeedbr.data.local.TechFeedDao
import br.com.techfeedbr.data.remote.SourceStatusDto
import br.com.techfeedbr.data.remote.TechFeedApi
import br.com.techfeedbr.domain.ContentItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

class FeedRepository(
    private val api: TechFeedApi,
    private val dao: TechFeedDao,
) {
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val stale = MutableStateFlow(false)
    val sourceStatuses = MutableStateFlow<List<SourceStatusDto>>(emptyList())
    val hasMore = MutableStateFlow(false)
    val loadingMore = MutableStateFlow(false)
    val initialAttemptFinished = MutableStateFlow(false)
    private val syncMutex = Mutex()
    private var nextCursor: String? = null
    private var activeQuery = ""
    private var activeCategories: Set<String> = emptySet()
    private var activeSources: Set<String> = emptySet()
    private var activePortugueseOnly = true

    val items: Flow<List<ContentItem>> = combine(dao.observeContent(), dao.observeStates()) { content, states ->
        val stateMap = states.associateBy(ContentStateEntity::contentId)
        content.filterNot { it.primarySource == "TABNEWS" || it.primarySource == "GNEWS" }.map { entity ->
            val state = stateMap[entity.id]
            ContentItem(
                id = entity.id,
                title = entity.title,
                summary = entity.summary,
                imageUrl = entity.imageUrl,
                canonicalUrl = entity.canonicalUrl,
                contentType = entity.contentType,
                language = entity.language,
                publishedAt = entity.publishedAt,
                author = entity.author,
                primarySource = entity.primarySource,
                sources = entity.sources,
                categories = entity.categories,
                tags = entity.tags,
                backendScore = entity.relevanceScore,
                favorite = state?.favorite ?: false,
                readLater = state?.readLater ?: false,
                viewedAt = state?.viewedAt,
            )
        }
    }

    suspend fun sync(
        requestRefresh: Boolean = false,
        query: String = "",
        categories: Set<String> = emptySet(),
        sources: Set<String> = emptySet(),
        portugueseOnly: Boolean = true,
    ) = syncMutex.withLock {
        loading.value = true
        error.value = null
        activeQuery = query.trim()
        activeCategories = categories
        activeSources = sources
        activePortugueseOnly = portugueseOnly
        nextCursor = null
        hasMore.value = false
        try {
            if (requestRefresh) api.refresh()
            var response = api.feed(query = activeQuery.ifBlank { null },
                categories = activeCategories.sorted(), sources = activeSources.sorted(),
                language = if (activePortugueseOnly) "pt" else null)
            for (attempt in 0 until 8) {
                save(response)
                if (response.sourceStatuses.none(SourceStatusDto::refreshing)
                    || (!requestRefresh && response.items.isNotEmpty())) break
                delay(1_500)
                response = api.feed(query = activeQuery.ifBlank { null },
                    categories = activeCategories.sorted(), sources = activeSources.sorted(),
                    language = if (activePortugueseOnly) "pt" else null)
            }
            initialAttemptFinished.value = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            val hasCachedNews = dao.observeContent().first().any {
                it.primarySource != "TABNEWS" && it.primarySource != "GNEWS" &&
                    (!activePortugueseOnly || it.language.startsWith("pt", ignoreCase = true))
            }
            error.value = if (hasCachedNews) {
                "Não foi possível atualizar. Exibindo notícias salvas no aparelho."
            } else networkMessage(throwable)
            initialAttemptFinished.value = true
        } finally {
            loading.value = false
        }
    }

    suspend fun loadNextPage() = syncMutex.withLock {
        val cursor = nextCursor ?: return@withLock
        loadingMore.value = true
        try {
            val response = api.feed(cursor = cursor, query = activeQuery.ifBlank { null },
                categories = activeCategories.sorted(), sources = activeSources.sorted(),
                language = if (activePortugueseOnly) "pt" else null)
            save(response)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            error.value = networkMessage(throwable)
        } finally {
            loadingMore.value = false
        }
    }

    private suspend fun save(response: br.com.techfeedbr.data.remote.FeedResponseDto) {
            dao.upsertContent(response.items.filterNot {
                it.primarySource == "TABNEWS" || it.primarySource == "GNEWS"
            }.map { item ->
                ContentEntity(
                    id = item.id,
                    title = item.title,
                    summary = item.summary,
                    imageUrl = item.imageUrl,
                    canonicalUrl = item.canonicalUrl,
                    contentType = item.contentType,
                    language = item.language,
                    publishedAt = item.publishedAt,
                    author = item.author,
                    primarySource = item.primarySource,
                    sources = item.sources,
                    categories = item.categories,
                    tags = item.tags,
                    relevanceScore = item.relevanceScore,
                    fetchedAt = item.fetchedAt,
                )
            })
            stale.value = response.stale
            sourceStatuses.value = response.sourceStatuses
            nextCursor = response.nextCursor
            hasMore.value = nextCursor != null
    }

    private fun networkMessage(throwable: Throwable): String =
            if (throwable.message?.contains("timeout", ignoreCase = true) == true) {
                "O servidor está iniciando. Tente novamente em alguns segundos."
            } else {
                "Não foi possível atualizar. Tente novamente."
            }

    suspend fun toggleFavorite(item: ContentItem) = updateState(item) { copy(favorite = !favorite) }
    suspend fun toggleReadLater(item: ContentItem) = updateState(item) { copy(readLater = !readLater) }
    suspend fun markViewed(item: ContentItem) = updateState(item) { copy(viewedAt = Instant.now().toEpochMilli()) }

    private suspend fun updateState(item: ContentItem, change: ContentStateEntity.() -> ContentStateEntity) {
        val current = dao.state(item.id) ?: ContentStateEntity(contentId = item.id)
        dao.upsertState(current.change().copy(updatedAt = System.currentTimeMillis()))
    }
}
