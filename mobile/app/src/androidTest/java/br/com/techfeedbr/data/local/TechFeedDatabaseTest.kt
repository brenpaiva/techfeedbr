package br.com.techfeedbr.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.techfeedbr.evaluation.EvaluationRecorder
import br.com.techfeedbr.evaluation.EvaluationExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TechFeedDatabaseTest {
    @Test fun contentAndSavedStateSurviveDatabaseReopen() {
        runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "techfeed-qa.db"
        context.deleteDatabase(name)
        val first = Room.databaseBuilder(context, TechFeedDatabase::class.java, name).build()
        first.dao().upsertContent(listOf(ContentEntity(
            id = "real-item", title = "Kotlin no Android", summary = null, imageUrl = null,
            canonicalUrl = "https://example.org/kotlin", contentType = "ARTICLE", language = "pt-BR",
            publishedAt = "2026-10-09T12:00:00Z", author = null, primarySource = "TABNEWS",
            sources = setOf("TABNEWS"), categories = setOf("Desenvolvimento"), tags = emptySet(),
            relevanceScore = 0.5, fetchedAt = "2026-10-09T12:00:00Z",
        )))
        first.dao().upsertState(ContentStateEntity(contentId = "real-item", favorite = true))
        first.close()

        val reopened = Room.databaseBuilder(context, TechFeedDatabase::class.java, name).build()
        assertEquals("Kotlin no Android", reopened.dao().observeContent().first().single().title)
        assertTrue(reopened.dao().state("real-item")!!.favorite)
        reopened.close()
        context.deleteDatabase(name)
        }
    }

    @Test fun evaluationSessionAndDeletionSurviveRecorderRecreation() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val name = "techfeed-evaluation-qa.db"
            context.deleteDatabase(name)
            context.getSharedPreferences("evaluation_session", 0).edit().clear().commit()
            val database = Room.databaseBuilder(context, TechFeedDatabase::class.java, name).build()
            val recorder = EvaluationRecorder(database.dao(), context)
            recorder.start("anonymous-qa", "task-1", consented = true)
            recorder.record("FAVORITE_TOGGLED")

            val restored = EvaluationRecorder(database.dao(), context)
            assertEquals("anonymous-qa", restored.activeSession?.code)
            restored.stop()
            assertEquals(4, restored.events("anonymous-qa").size)
            assertEquals(listOf("anonymous-qa"), database.dao().observeEvaluationSessions().first())
            val files = EvaluationExporter(context).exportFiles("anonymous-qa", restored.events("anonymous-qa"))
            assertTrue(files[0].readText().contains("CONSENT_ACCEPTED"))
            assertTrue(files[1].readText().contains("FAVORITE_TOGGLED"))
            assertTrue(files.none { it.readText().contains("EXAMPLE") })
            files.forEach { it.delete() }
            restored.delete("anonymous-qa")
            assertTrue(restored.events("anonymous-qa").isEmpty())
            assertTrue(database.dao().observeEvaluationSessions().first().isEmpty())
            assertEquals(null, restored.lastSessionCode)
            database.close()
            context.deleteDatabase(name)
        }
    }
}
