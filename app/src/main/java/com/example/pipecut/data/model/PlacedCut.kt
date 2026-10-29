package com.example.pipecut.data.model

/**
 * A cut piece assigned and positioned within a specific stock pipe.
 */
data class PlacedCut(
    val pieceId: String,
    val label: String,
    val length: Double,
    val extraAllowance: Double,
    val effectiveLength: Double,
    val startPosition: Double,
    val endPosition: Double,
    val kerfAfter: Double,
    val sequenceNumber: Int,
    val colorHex: String
)
