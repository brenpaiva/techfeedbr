package br.com.techfeedbr.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.techfeedbr.AppContainer
import br.com.techfeedbr.TechFeedApplication
import br.com.techfeedbr.TechFeedViewModel
import br.com.techfeedbr.data.FeedRepository
import br.com.techfeedbr.data.local.TechFeedDatabase
import br.com.techfeedbr.data.preferences.PreferencesStore
import br.com.techfeedbr.data.remote.ContentItemDto
import br.com.techfeedbr.data.remote.FeedResponseDto
import br.com.techfeedbr.data.remote.SourceStatusDto
import br.com.techfeedbr.data.remote.TechFeedApi
import br.com.techfeedbr.evaluation.EvaluationExporter
import br.com.techfeedbr.evaluation.EvaluationRecorder
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class FeedSearchUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchAndTopicFilterRequestWholeFeedAfterPagination() {
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TechFeedApplication
        val preferences = PreferencesStore(app)
        runBlocking { preferences.completeOnboarding(emptySet(), setOf("DEV", "HARDWARE")) }
        val database = Room.inMemoryDatabaseBuilder(app, TechFeedDatabase::class.java).build()
        val api = PagedApi()
        val repository = FeedRepository(api, database.dao())
        val viewModel = TechFeedViewModel(app, AppContainer(
            feedRepository = repository,
            preferencesStore = preferences,
            evaluationRecorder = EvaluationRecorder(database.dao(), app),
            evaluationExporter = EvaluationExporter(app),
            evaluationSessionCodes = database.dao().observeEvaluationSessions(),
        ))

        try {
            compose.setContent {
                val state = viewModel.uiState.collectAsStateWithLifecycle().value
                TechFeedTheme { TechFeedApp(state, viewModel) }
            }
            compose.waitUntil(10_000) { api.calls.any { it.cursor == "next" } }
            compose.onNode(hasSetTextAction()).performTextInput("Rust")
            compose.waitUntil(10_000) { api.calls.any { it.query == "Rust" } }
            compose.onNodeWithText("Rust na segunda página", useUnmergedTree = true).assertExists()

            compose.onNodeWithContentDescription("Abrir filtros do feed").performClick()
            compose.onNodeWithText("Filtros do feed", useUnmergedTree = true).assertExists()
            compose.onNodeWithText("Fontes", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithTag("topic-filter-Desenvolvimento")
                .performScrollTo().performClick()
            compose.waitUntil(10_000) {
                api.calls.any { it.query == "Rust" && it.categories == listOf("Desenvolvimento") }
            }
            assertTrue(api.calls.any { it.cursor == null && it.query == "Rust"
                && it.categories == listOf("Desenvolvimento") && it.sources.isEmpty() })
            compose.onNodeWithText("Ver notícias", useUnmergedTree = true).performScrollTo().performClick()
            compose.onNodeWithText("Filtros ativos: 1", useUnmergedTree = true).assertExists()
        } finally {
            database.close()
        }
    }

    private data class Call(val cursor: String?, val query: String?,
                            val categories: List<String>, val sources: List<String>)

    private class PagedApi : TechFeedApi {
        val calls = CopyOnWriteArrayList<Call>()

        override suspend fun feed(cursor: String?, limit: Int, query: String?,
                                  categories: List<String>, sources: List<String>,
                                  language: String?, sort: String): FeedResponseDto {
            calls += Call(cursor, query, categories, sources)
            val item = if (cursor == "next" || query == "Rust") {
                content("rust", "Rust na segunda página", "HARDWARE")
            } else {
                content("kotlin", "Kotlin na primeira página", "DEV")
            }
            return FeedResponseDto(items = listOf(item), nextCursor =
                if (cursor == null && query == null) "next" else null)
        }

        override suspend fun refresh(): List<SourceStatusDto> = emptyList()
        override suspend fun sourceStatus(): List<SourceStatusDto> = emptyList()

        private fun content(id: String, title: String, source: String) = ContentItemDto(
            id = id, title = title, canonicalUrl = "https://example.org/$id",
            contentType = "ARTICLE", language = "pt-BR", publishedAt = "2026-10-09T12:00:00Z",
            primarySource = source, sources = setOf(source), categories = setOf("Desenvolvimento"),
            fetchedAt = "2026-10-09T12:00:00Z",
        )
    }
}
