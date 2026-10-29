package com.example.pipecut.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pipecut.data.model.AppThemeMode
import com.example.pipecut.data.model.CuttingSettings
import com.example.pipecut.data.model.LengthUnit
import com.example.pipecut.data.model.OptimizationResult
import com.example.pipecut.data.model.ProjectRecord
import com.example.pipecut.data.model.RequiredCut
import com.example.pipecut.data.model.StockPipe
import com.example.pipecut.data.repository.ProjectRepository
import com.example.pipecut.domain.algorithm.PipeCutOptimizer
import com.example.pipecut.domain.converter.UnitConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    SPLASH,
    HOME,
    RESULTS,
    HISTORY,
    SETTINGS,
    ABOUT
}

data class PipeCutUiState(
    val currentScreen: AppScreen = AppScreen.SPLASH,
    val projectName: String = "Untitled Pipe Project",
    val stockPipes: List<StockPipe> = emptyList(),
    val requiredCuts: List<RequiredCut> = emptyList(),
    val settings: CuttingSettings = CuttingSettings(),
    val validationIssues: List<PipeCutOptimizer.ValidationIssue> = emptyList(),
    val result: OptimizationResult? = null,
    val savedProjects: List<ProjectRecord> = emptyList(),
    val isOptimizing: Boolean = false,
    val userNotification: String? = null
)

class PipeCutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)

    private val _uiState = MutableStateFlow(PipeCutUiState())
    val uiState: StateFlow<PipeCutUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val settings = repository.getSettings()
            val allProjects = repository.getAllProjects()
            val initialProject = allProjects.firstOrNull()

            if (initialProject != null) {
                _uiState.update {
                    it.copy(
                        projectName = initialProject.name,
                        stockPipes = initialProject.stockPipes,
                        requiredCuts = initialProject.requiredCuts,
                        settings = initialProject.settings,
                        result = initialProject.lastResult,
                        savedProjects = allProjects
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        settings = settings,
                        savedProjects = allProjects
                    )
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setProjectName(name: String) {
        _uiState.update { it.copy(projectName = name) }
    }

    // --- Stock Pipe Management ---

    fun addOrUpdateStock(stock: StockPipe) {
        _uiState.update { state ->
            val list = state.stockPipes.toMutableList()
            val index = list.indexOfFirst { it.id == stock.id }
            if (index >= 0) {
                list[index] = stock
            } else {
                list.add(stock)
            }
            state.copy(stockPipes = list, validationIssues = emptyList())
        }
    }

    fun deleteStock(stockId: String) {
        _uiState.update { state ->
            state.copy(
                stockPipes = state.stockPipes.filterNot { it.id == stockId },
                validationIssues = emptyList()
            )
        }
    }

    fun duplicateStock(stock: StockPipe) {
        val dup = stock.copy(
            id = UUID.randomUUID().toString(),
            label = if (stock.label.isNotBlank()) "${stock.label} (Copy)" else "Copy"
        )
        addOrUpdateStock(dup)
    }

    // --- Required Cut Management ---

    fun addOrUpdateCut(cut: RequiredCut) {
        _uiState.update { state ->
            val list = state.requiredCuts.toMutableList()
            val index = list.indexOfFirst { it.id == cut.id }
            if (index >= 0) {
                list[index] = cut
            } else {
                list.add(cut)
            }
            state.copy(requiredCuts = list, validationIssues = emptyList())
        }
    }

    fun deleteCut(cutId: String) {
        _uiState.update { state ->
            state.copy(
                requiredCuts = state.requiredCuts.filterNot { it.id == cutId },
                validationIssues = emptyList()
            )
        }
    }

    fun duplicateCut(cut: RequiredCut) {
        val dup = cut.copy(
            id = UUID.randomUUID().toString(),
            label = if (cut.label.isNotBlank()) "${cut.label} (Copy)" else "Copy"
        )
        addOrUpdateCut(dup)
    }

    // --- Settings & Unit Conversion ---

    fun changeUnit(newUnit: LengthUnit) {
        val currentUnit = _uiState.value.settings.unit
        if (currentUnit == newUnit) return

        _uiState.update { state ->
            // Convert numerical values in stock and cuts
            val convertedStocks = state.stockPipes.map { sp ->
                val newLen = UnitConverter.convert(sp.length, currentUnit, newUnit)
                sp.copy(length = UnitConverter.roundToDecimals(newLen, 2))
            }
            val convertedCuts = state.requiredCuts.map { rc ->
                val newLen = UnitConverter.convert(rc.length, currentUnit, newUnit)
                rc.copy(length = UnitConverter.roundToDecimals(newLen, 2))
            }
            val newSettings = state.settings.convertTo(newUnit)
            repository.saveSettings(newSettings)

            state.copy(
                stockPipes = convertedStocks,
                requiredCuts = convertedCuts,
                settings = newSettings,
                result = null // Invalidate previous result
            )
        }
    }

    fun updateSettings(newSettings: CuttingSettings) {
        repository.saveSettings(newSettings)
        _uiState.update { it.copy(settings = newSettings, result = null) }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        val newSettings = _uiState.value.settings.copy(themeMode = themeMode)
        updateSettings(newSettings)
    }

    // --- Calculation & Optimization ---

    fun calculateCuttingPlan() {
        val state = _uiState.value
        val issues = PipeCutOptimizer.validateInputs(state.stockPipes, state.requiredCuts, state.settings)
        val fatalIssues = issues.filter { it.isFatal }

        if (fatalIssues.isNotEmpty()) {
            _uiState.update { it.copy(validationIssues = issues) }
            return
        }

        _uiState.update { it.copy(isOptimizing = true, validationIssues = issues) }

        viewModelScope.launch {
            val result = PipeCutOptimizer.optimize(
                stockPipes = state.stockPipes,
                requiredCuts = state.requiredCuts,
                settings = state.settings
            )

            _uiState.update {
                it.copy(
                    isOptimizing = false,
                    result = result,
                    currentScreen = AppScreen.RESULTS
                )
            }

            // Auto-save state
            autoSaveCurrentProject()
        }
    }

    // --- Project Persistence & History ---

    fun saveCurrentProject(customName: String? = null) {
        val state = _uiState.value
        val name = customName?.takeIf { it.isNotBlank() } ?: state.projectName
        val project = ProjectRecord(
            name = name,
            stockPipes = state.stockPipes,
            requiredCuts = state.requiredCuts,
            settings = state.settings,
            lastResult = state.result
        )
        repository.saveProject(project)
        val all = repository.getAllProjects()
        _uiState.update {
            it.copy(
                projectName = name,
                savedProjects = all,
                userNotification = "Project '$name' saved successfully"
            )
        }
    }

    private fun autoSaveCurrentProject() {
        val state = _uiState.value
        if (state.stockPipes.isEmpty() && state.requiredCuts.isEmpty()) return
        val project = ProjectRecord(
            name = state.projectName,
            stockPipes = state.stockPipes,
            requiredCuts = state.requiredCuts,
            settings = state.settings,
            lastResult = state.result
        )
        repository.saveProject(project)
        _uiState.update { it.copy(savedProjects = repository.getAllProjects()) }
    }

    fun loadProject(project: ProjectRecord) {
        _uiState.update {
            it.copy(
                projectName = project.name,
                stockPipes = project.stockPipes,
                requiredCuts = project.requiredCuts,
                settings = project.settings,
                result = project.lastResult,
                validationIssues = emptyList(),
                currentScreen = AppScreen.HOME,
                userNotification = "Loaded '${project.name}'"
            )
        }
    }

    fun deleteProject(projectId: String) {
        repository.deleteProject(projectId)
        _uiState.update {
            it.copy(
                savedProjects = repository.getAllProjects(),
                userNotification = "Project removed"
            )
        }
    }

    fun resetCalculator() {
        _uiState.update {
            it.copy(
                projectName = "New Pipe Project",
                stockPipes = emptyList(),
                requiredCuts = emptyList(),
                result = null,
                validationIssues = emptyList(),
                userNotification = "Calculator cleared"
            )
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }
}
