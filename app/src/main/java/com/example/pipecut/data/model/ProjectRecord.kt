package com.example.pipecut.data.model

import java.util.UUID

/**
 * Persisted model representing a saved pipe cutting project.
 */
data class ProjectRecord(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val timestamp: Long = System.currentTimeMillis(),
    val stockPipes: List<StockPipe>,
    val requiredCuts: List<RequiredCut>,
    val settings: CuttingSettings,
    val lastResult: OptimizationResult? = null
)
