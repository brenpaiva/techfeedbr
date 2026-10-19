package br.com.techfeedbr.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.techfeedbr.TechFeedApplication
import br.com.techfeedbr.TechFeedUiState
import br.com.techfeedbr.TechFeedViewModel
import br.com.techfeedbr.data.preferences.UserPreferences
import br.com.techfeedbr.domain.ExampleContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedFallbackUiTest {
    @get:Rule val compose = createComposeRule()
    private val demoNotice = "Conteúdo de exemplo: nenhuma notícia real está disponível agora. Atualize para tentar novamente."

    @Test fun examplesAreClearlyMarkedWhenNoRealItemExists() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TechFeedApplication
        compose.setContent {
            TechFeedTheme {
                TechFeedApp(
                    state = TechFeedUiState(
                        preferences = UserPreferences(onboardingCompleted = true),
                        items = ExampleContent.items,
                        demoMode = true,
                    ),
                    viewModel = TechFeedViewModel(application),
                )
            }
        }

        compose.onNodeWithText(demoNotice, useUnmergedTree = true).assertExists()
        compose.onNodeWithText("EXEMPLO", useUnmergedTree = true).assertExists()
        compose.onNodeWithText(ExampleContent.items.first().title, useUnmergedTree = true).assertExists()
    }

    @Test fun realItemDoesNotCarryExampleLabel() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TechFeedApplication
        val real = ExampleContent.items.first().copy(
            id = "real-test",
            title = "Kotlin real no Android",
            canonicalUrl = "https://example.org/kotlin",
            primarySource = "HARDWARE",
            sources = setOf("HARDWARE"),
            isExample = false,
        )
        compose.setContent {
            TechFeedTheme {
                TechFeedApp(
                    state = TechFeedUiState(
                        preferences = UserPreferences(onboardingCompleted = true),
                        items = listOf(real),
                        allItems = listOf(real),
                    ),
                    viewModel = TechFeedViewModel(application),
                )
            }
        }

        compose.onNodeWithText("Kotlin real no Android", useUnmergedTree = true).assertExists()
        compose.onNodeWithText(demoNotice, useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("EXEMPLO", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun scrollingCollapsesEntireEditorialHeaderAndKeepsSearchAvailable() {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as TechFeedApplication
        val longFeed = List(12) { index ->
            ExampleContent.items[index % ExampleContent.items.size]
                .copy(id = "example:scroll-$index", title = "Exemplo $index")
        }
        compose.setContent {
            TechFeedTheme {
                TechFeedApp(
                    state = TechFeedUiState(
                        preferences = UserPreferences(onboardingCompleted = true),
                        items = longFeed, demoMode = true,
                    ),
                    viewModel = TechFeedViewModel(application),
                )
            }
        }
        compose.onNodeWithText("Tecnologia em foco", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("feed-content-list").performTouchInput { swipeUp() }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Tecnologia em foco", useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Buscar notícia", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Buscar por palavra-chave", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Recolher busca").assertExists()
        compose.onNodeWithContentDescription("Recolher busca").performClick()
        repeat(3) { compose.onNodeWithTag("feed-content-list").performTouchInput { swipeDown() } }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Tecnologia em foco", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
