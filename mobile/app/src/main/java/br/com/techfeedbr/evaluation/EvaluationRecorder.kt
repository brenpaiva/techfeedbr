package br.com.techfeedbr.evaluation

import android.content.Context
import br.com.techfeedbr.data.local.EvaluationEventEntity
import br.com.techfeedbr.data.local.TechFeedDao
import java.util.UUID

data class EvaluationSession(val code: String, val taskCode: String, val startedAt: Long)

class EvaluationRecorder(private val dao: TechFeedDao, context: Context) {
    private val preferences = context.getSharedPreferences("evaluation_session", Context.MODE_PRIVATE)
    var activeSession: EvaluationSession? = restoreSession()
        private set
    val lastSessionCode: String? get() = preferences.getString("last_code", null)

    suspend fun start(code: String, taskCode: String, consented: Boolean) {
        require(consented) { "É necessário consentir antes de iniciar a sessão" }
        require(code.isNotBlank()) { "Informe o código da sessão" }
        require(taskCode.isNotBlank()) { "Informe o código da tarefa" }
        activeSession = EvaluationSession(code.trim(), taskCode.trim(), System.currentTimeMillis())
        preferences.edit().putString("active_code", code.trim())
            .putString("active_task", taskCode.trim())
            .putLong("active_start", activeSession!!.startedAt).apply()
        record("CONSENT_ACCEPTED")
        record("SESSION_STARTED")
    }

    suspend fun record(type: String, metadata: String? = null) {
        val session = activeSession ?: return
        val now = System.currentTimeMillis()
        dao.insertEvaluationEvent(
            EvaluationEventEntity(
                id = UUID.randomUUID().toString(),
                sessionCode = session.code,
                taskCode = session.taskCode,
                eventType = type,
                timestamp = now,
                elapsedMs = now - session.startedAt,
                metadata = metadata,
            ),
        )
    }

    suspend fun stop() {
        val finishedCode = activeSession?.code
        record("SESSION_ENDED")
        activeSession = null
        preferences.edit().remove("active_code").remove("active_task").remove("active_start")
            .apply { if (finishedCode != null) putString("last_code", finishedCode) }.apply()
    }

    suspend fun events(sessionCode: String) = dao.evaluationEvents(sessionCode)
    suspend fun delete(sessionCode: String) {
        dao.deleteEvaluationSession(sessionCode)
        if (lastSessionCode == sessionCode) preferences.edit().remove("last_code").apply()
    }

    private fun restoreSession(): EvaluationSession? {
        val code = preferences.getString("active_code", null) ?: return null
        val task = preferences.getString("active_task", null) ?: return null
        val start = preferences.getLong("active_start", 0L)
        return if (start > 0) EvaluationSession(code, task, start) else null
    }
}
