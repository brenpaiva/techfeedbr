package br.com.techfeedbr.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "techfeed_preferences")
private val OriginalSources = setOf("GNEWS", "EBC", "DEV", "TABNEWS")
private val PreviousSources = OriginalSources + setOf("TECNOBLOG", "OLHARDIGITAL")
private val AllSources = (PreviousSources - setOf("TABNEWS", "GNEWS")) + setOf(
    "TUDOCELULAR", "HARDWARE", "MUNDOCONECTADO", "MACMAGAZINE", "ADRENALINE",
)

data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val darkTheme: Boolean = false,
    val portugueseOnly: Boolean = true,
    val categories: Set<String> = emptySet(),
    val sources: Set<String> = AllSources,
)

class PreferencesStore(private val context: Context) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_completed")
        val darkTheme = booleanPreferencesKey("dark_theme")
        val portugueseOnly = booleanPreferencesKey("portuguese_only")
        val categories = stringPreferencesKey("categories")
        val sources = stringPreferencesKey("sources")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { values ->
        UserPreferences(
            onboardingCompleted = values[Keys.onboarding] ?: false,
            darkTheme = values[Keys.darkTheme] ?: false,
            portugueseOnly = values[Keys.portugueseOnly] ?: true,
            categories = decode(values[Keys.categories]),
            sources = decode(values[Keys.sources]).let { saved ->
                if (saved.isEmpty() || saved == OriginalSources || saved == PreviousSources) AllSources
                else saved - setOf("TABNEWS", "GNEWS")
            },
        )
    }

    suspend fun completeOnboarding(categories: Set<String>, sources: Set<String>) {
        context.dataStore.edit {
            it[Keys.onboarding] = true
            it[Keys.categories] = encode(categories)
            it[Keys.sources] = encode(sources)
        }
    }

    suspend fun updatePreferences(categories: Set<String>, sources: Set<String>) {
        context.dataStore.edit {
            it[Keys.categories] = encode(categories)
            it[Keys.sources] = encode(sources)
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.darkTheme] = enabled }
    }

    suspend fun setPortugueseOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.portugueseOnly] = enabled }
    }

    private fun encode(values: Set<String>) = values.joinToString("|")
    private fun decode(value: String?) = value?.split('|')?.filter(String::isNotBlank)?.toSet().orEmpty()
}
