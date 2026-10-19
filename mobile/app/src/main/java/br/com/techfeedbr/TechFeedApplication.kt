package br.com.techfeedbr

import android.app.Application
import androidx.room.Room
import br.com.techfeedbr.data.FeedRepository
import br.com.techfeedbr.data.local.TechFeedDatabase
import br.com.techfeedbr.data.preferences.PreferencesStore
import br.com.techfeedbr.data.remote.TechFeedApi
import br.com.techfeedbr.evaluation.EvaluationExporter
import br.com.techfeedbr.evaluation.EvaluationRecorder
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TechFeedApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(this, TechFeedDatabase::class.java, "techfeed.db").build()
        val gson = GsonBuilder().create()
        val api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(TechFeedApi::class.java)
        container = AppContainer(
            feedRepository = FeedRepository(api, database.dao()),
            preferencesStore = PreferencesStore(this),
            evaluationRecorder = EvaluationRecorder(database.dao(), this),
            evaluationExporter = EvaluationExporter(this),
            evaluationSessionCodes = database.dao().observeEvaluationSessions(),
        )
    }
}

data class AppContainer(
    val feedRepository: FeedRepository,
    val preferencesStore: PreferencesStore,
    val evaluationRecorder: EvaluationRecorder,
    val evaluationExporter: EvaluationExporter,
    val evaluationSessionCodes: Flow<List<String>>,
)
