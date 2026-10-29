package com.example.pipecut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pipecut.ui.screens.about.AboutScreen
import com.example.pipecut.ui.screens.history.HistoryScreen
import com.example.pipecut.ui.screens.home.HomeScreen
import com.example.pipecut.ui.screens.results.ResultsScreen
import com.example.pipecut.ui.screens.settings.SettingsScreen
import com.example.pipecut.ui.screens.splash.SplashScreen
import com.example.pipecut.ui.theme.PipeCutTheme
import com.example.pipecut.ui.viewmodel.AppScreen
import com.example.pipecut.ui.viewmodel.PipeCutViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PipeCutViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            PipeCutTheme(themeMode = uiState.settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PipeCutApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun PipeCutApp(viewModel: PipeCutViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Back handling navigation
    if (uiState.currentScreen != AppScreen.HOME && uiState.currentScreen != AppScreen.SPLASH) {
        BackHandler {
            viewModel.navigateTo(AppScreen.HOME)
        }
    }

    when (uiState.currentScreen) {
        AppScreen.SPLASH -> {
            SplashScreen(
                onContinue = { viewModel.navigateTo(AppScreen.HOME) }
            )
        }
        AppScreen.HOME -> {
            HomeScreen(
                viewModel = viewModel,
                uiState = uiState
            )
        }
        AppScreen.RESULTS -> {
            ResultsScreen(
                viewModel = viewModel,
                result = uiState.result
            )
        }
        AppScreen.HISTORY -> {
            HistoryScreen(
                viewModel = viewModel,
                savedProjects = uiState.savedProjects
            )
        }
        AppScreen.SETTINGS -> {
            SettingsScreen(
                viewModel = viewModel,
                settings = uiState.settings
            )
        }
        AppScreen.ABOUT -> {
            AboutScreen(viewModel = viewModel)
        }
    }
}
