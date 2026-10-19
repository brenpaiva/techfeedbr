package br.com.techfeedbr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.techfeedbr.ui.TechFeedApp
import br.com.techfeedbr.ui.TechFeedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TechFeedViewModel = viewModel()
            val state = viewModel.uiState.collectAsStateWithLifecycle().value
            SideEffect {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !state.preferences.darkTheme
                    isAppearanceLightNavigationBars = !state.preferences.darkTheme
                }
            }
            TechFeedTheme(darkTheme = state.preferences.darkTheme) {
                TechFeedApp(state = state, viewModel = viewModel)
            }
        }
    }
}
