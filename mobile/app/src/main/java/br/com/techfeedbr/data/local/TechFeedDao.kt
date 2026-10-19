package br.com.techfeedbr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TechFeedDao {
    @Query("SELECT * FROM content ORDER BY publishedAt DESC")
    fun observeContent(): Flow<List<ContentEntity>>

    @Query("SELECT * FROM content_state")
    fun observeStates(): Flow<List<ContentStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContent(items: List<ContentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: ContentStateEntity)

    @Query("SELECT * FROM content_state WHERE contentId = :contentId")
    suspend fun state(contentId: String): ContentStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluationEvent(event: EvaluationEventEntity)

    @Query("SELECT * FROM evaluation_event WHERE sessionCode = :sessionCode ORDER BY timestamp")
    suspend fun evaluationEvents(sessionCode: String): List<EvaluationEventEntity>

    @Query("SELECT sessionCode FROM evaluation_event GROUP BY sessionCode ORDER BY MAX(timestamp) DESC")
    fun observeEvaluationSessions(): Flow<List<String>>

    @Query("DELETE FROM evaluation_event WHERE sessionCode = :sessionCode")
    suspend fun deleteEvaluationSession(sessionCode: String)
}
