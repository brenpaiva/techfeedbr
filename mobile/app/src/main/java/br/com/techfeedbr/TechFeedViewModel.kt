package br.com.techfeedbr

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.techfeedbr.data.preferences.UserPreferences
import br.com.techfeedbr.data.remote.SourceStatusDto
import br.com.techfeedbr.domain.ContentItem
import br.com.techfeedbr.domain.ExampleContent
import br.com.techfeedbr.domain.FeedOrdering
import br.com.techfeedbr.domain.SavedMode
import br.com.techfeedbr.domain.SortMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview

data class TechFeedUiState(
    val preferences: UserPreferences = UserPreferences(),
    val items: List<ContentItem> = emptyList(),
    val allItems: List<ContentItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val stale: Boolean = false,
    val sourceStatuses: List<SourceStatusDto> = emptyList(),
    val query: String = "",
    val selectedCategories: Set<String> = emptySet(),
    val selectedSources: Set<String> = emptySet(),
    val unreadOnly: Boolean = false,
    val sortMode: SortMode = SortMode.DATE,
    val savedMode: SavedMode = SavedMode.FAVORITES,
    val evaluationSessionCode: String? = null,
    val lastEvaluationSessionCode: String? = null,
    val evaluationSessionCodes: List<String> = emptyList(),
    val hasMore: Boolean = false,
    val loadingMore: Boolean = false,
    val demoMode: Boolean = false,
)

@OptIn(FlowPreview::class)
class TechFeedViewModel @JvmOverloads constructor(
    application: Application,
    private val container: AppContainer = (application as TechFeedApplication).container,
) : AndroidViewModel(application) {
    private val repository = container.feedRepository
    private val ordering = FeedOrdering()
    private val controls = MutableStateFlow(TechFeedUiState())
    private data class FeedQuery(val query: String, val categories: Set<String>,
                                 val sources: Set<String>, val portugueseOnly: Boolean)

    val uiState = combine(
        repository.items,
        container.preferencesStore.preferences,
        repository.loading,
        repository.error,
        repository.stale,
        repository.sourceStatuses,
        controls,
        repository.hasMore,
        repository.loadingMore,
        repository.initialAttemptFinished,
        container.evaluationSessionCodes,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allItems = values[0] as List<ContentItem>
        val preferences = values[1] as UserPreferences
        val loading = values[2] as Boolean
        val error = values[3] as String?
        val stale = values[4] as Boolean
        @Suppress("UNCHECKED_CAST")
        val statuses = values[5] as List<SourceStatusDto>
        val control = values[6] as TechFeedUiState
        val hasMore = values[7] as Boolean
        val loadingMore = values[8] as Boolean
        val initialAttemptFinished = values[9] as Boolean
        @Suppress("UNCHECKED_CAST")
        val evaluationSessionCodes = values[10] as List<String>
        val categories = control.selectedCategories
        val sources = control.selectedSources
        val demoMode = allItems.isEmpty() && initialAttemptFinished && !loading
        val visibleItems = if (demoMode) ExampleContent.items else allItems
        var filtered = visibleItems.filter { item ->
            (!preferences.portugueseOnly || item.language.startsWith("pt", ignoreCase = true) || item.isExample)
                    && (control.query.isBlank() || item.title.contains(control.query, ignoreCase = true)
                    || item.summary?.contains(control.query, ignoreCase = true) == true)
                    && (categories.isEmpty() || item.categories.any(categories::contains))
                    && (sources.isEmpty() || item.sources.any(sources::contains))
                    && (!control.unreadOnly || item.viewedAt == null)
        }
        filtered = ordering.order(filtered, control.sortMode, preferences)
        control.copy(
            preferences = preferences,
            items = filtered,
            allItems = allItems,
            loading = loading,
            error = error,
            stale = stale,
            sourceStatuses = statuses,
            evaluationSessionCode = container.evaluationRecorder.activeSession?.code,
            lastEvaluationSessionCode = container.evaluationRecorder.lastSessionCode ?: control.lastEvaluationSessionCode,
            evaluationSessionCodes = evaluationSessionCodes,
            hasMore = hasMore,
            loadingMore = loadingMore,
            demoMode = demoMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TechFeedUiState())

    init {
        viewModelScope.launch {
            combine(controls, container.preferencesStore.preferences) { control, preferences ->
                FeedQuery(control.query.trim(), control.selectedCategories,
                    control.selectedSources, preferences.portugueseOnly)
            }
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { selection ->
                    repository.sync(query = selection.query, categories = selection.categories,
                        sources = selection.sources, portugueseOnly = selection.portugueseOnly)
                }
        }
    }

    fun refresh() = viewModelScope.launch {
        if (uiState.value.allItems.isNotEmpty()) container.evaluationRecorder.record("FEED_REFRESHED")
        val current = controls.value
        repository.sync(requestRefresh = true, query = current.query,
            categories = current.selectedCategories, sources = current.selectedSources,
            portugueseOnly = uiState.value.preferences.portugueseOnly)
    }

    fun loadNextPage() = viewModelScope.launch { repository.loadNextPage() }

    fun setQuery(value: String) { controls.value = controls.value.copy(query = value) }
    fun recordSearch() = viewModelScope.launch {
        if (uiState.value.allItems.isNotEmpty()) container.evaluationRecorder.record("SEARCH")
    }
    fun setCategory(category: String) = toggleSet(category, true)
    fun setSource(source: String) = toggleSet(source, false)
    fun setUnreadOnly(value: Boolean) { controls.value = controls.value.copy(unreadOnly = value) }
    fun setSortMode(value: SortMode) { controls.value = controls.value.copy(sortMode = value) }
    fun setSavedMode(value: SavedMode) { controls.value = controls.value.copy(savedMode = value) }
    fun clearFeedFilters() {
        controls.value = controls.value.copy(
            selectedCategories = emptySet(), selectedSources = emptySet(),
            unreadOnly = false, sortMode = SortMode.DATE,
        )
        setPortugueseOnly(true)
    }

    fun favorite(item: ContentItem) = viewModelScope.launch {
        if (item.isExample) return@launch
        repository.toggleFavorite(item)
        container.evaluationRecorder.record("FAVORITE_TOGGLED")
    }

    fun readLater(item: ContentItem) = viewModelScope.launch {
        if (item.isExample) return@launch
        repository.toggleReadLater(item)
        container.evaluationRecorder.record("READ_LATER_TOGGLED")
    }

    fun viewed(item: ContentItem) = viewModelScope.launch {
        if (item.isExample) return@launch
        repository.markViewed(item)
        container.evaluationRecorder.record("ITEM_OPENED", item.primarySource)
    }

    fun completeOnboarding(categories: Set<String>, sources: Set<String>) = viewModelScope.launch {
        container.preferencesStore.completeOnboarding(categories, sources)
    }

    fun savePreferences(categories: Set<String>, sources: Set<String>) = viewModelScope.launch {
        container.preferencesStore.updatePreferences(categories, sources)
    }

    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch {
        container.preferencesStore.setDarkTheme(enabled)
    }

    fun setPortugueseOnly(enabled: Boolean) = viewModelScope.launch {
        container.preferencesStore.setPortugueseOnly(enabled)
    }

    fun startEvaluation(code: String, task: String, consented: Boolean) = viewModelScope.launch {
        if (uiState.value.allItems.isEmpty()) return@launch
        container.evaluationRecorder.start(code, task, consented)
        controls.value = controls.value.copy(
            evaluationSessionCode = code.trim(),
            lastEvaluationSessionCode = code.trim(),
        )
    }

    fun completeEvaluationTask() = viewModelScope.launch {
        container.evaluationRecorder.record("TASK_MARKED_COMPLETE")
    }

    fun stopEvaluation() = viewModelScope.launch {
        val completedCode = container.evaluationRecorder.activeSession?.code
        container.evaluationRecorder.stop()
        controls.value = controls.value.copy(
            evaluationSessionCode = null,
            lastEvaluationSessionCode = completedCode ?: controls.value.lastEvaluationSessionCode,
        )
    }

    fun exportEvaluation(code: String) = viewModelScope.launch {
        val events = container.evaluationRecorder.events(code)
        if (events.isNotEmpty()) container.evaluationExporter.share(code, events)
    }

    fun deleteEvaluation(code: String) = viewModelScope.launch {
        if (container.evaluationRecorder.activeSession?.code == code) {
            container.evaluationRecorder.stop()
        }
        container.evaluationRecorder.delete(code)
        controls.value = controls.value.copy(
            evaluationSessionCode = null,
            lastEvaluationSessionCode = null,
        )
    }

    private fun toggleSet(value: String, category: Boolean) {
        val current = if (category) controls.value.selectedCategories else controls.value.selectedSources
        val updated = if (value in current) current - value else current + value
        controls.value = if (category) controls.value.copy(selectedCategories = updated)
        else controls.value.copy(selectedSources = updated)
        if (uiState.value.allItems.isNotEmpty()) {
            viewModelScope.launch { container.evaluationRecorder.record("FILTER_APPLIED", value) }
        }
    }
}
