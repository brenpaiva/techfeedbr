package br.com.techfeedbr.evaluation

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import br.com.techfeedbr.data.local.EvaluationEventEntity
import com.google.gson.GsonBuilder
import java.io.File

class EvaluationExporter(private val context: Context) {
    fun exportFiles(sessionCode: String, events: List<EvaluationEventEntity>): List<File> {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeCode = sessionCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val csv = File(directory, "techfeed-avaliacao-$safeCode.csv")
        val json = File(directory, "techfeed-avaliacao-$safeCode.json")
        csv.writeText(buildCsv(events), Charsets.UTF_8)
        json.writeText(GsonBuilder().setPrettyPrinting().create().toJson(events), Charsets.UTF_8)
        return listOf(csv, json)
    }

    fun share(sessionCode: String, events: List<EvaluationEventEntity>) {
        val uris = exportFiles(sessionCode, events).mapTo(ArrayList()) {
            FileProvider.getUriForFile(context, "${context.packageName}.files", it)
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/plain"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, "TechFeed BR avaliação", uris.first()).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
        }
        context.startActivity(Intent.createChooser(intent, "Exportar sessão de avaliação").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    private fun buildCsv(events: List<EvaluationEventEntity>): String = buildString {
        appendLine("session_code,task_code,event_type,timestamp,elapsed_ms,metadata")
        events.forEach { event ->
            appendLine(listOf(event.sessionCode, event.taskCode, event.eventType, event.timestamp,
                event.elapsedMs, event.metadata.orEmpty()).joinToString(",") { csvEscape(it.toString()) })
        }
    }

    private fun csvEscape(value: String) = "\"${value.replace("\"", "\"\"")}\""
}
