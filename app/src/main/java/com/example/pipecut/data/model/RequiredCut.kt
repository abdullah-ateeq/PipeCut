package com.example.pipecut.data.model

import java.util.UUID

/**
 * Predefined distinct color hexes for visualizing cut sections clearly on pipes.
 */
val PIECE_PALETTE_HEX = listOf(
    "#0284C7", // Sky Industrial Blue
    "#10B981", // Emerald
    "#F59E0B", // Amber
    "#8B5CF6", // Purple
    "#EC4899", // Pink
    "#14B8A6", // Teal
    "#F97316", // Orange
    "#6366F1", // Indigo
    "#84CC16", // Lime
    "#06B6D4", // Cyan
    "#E11D48", // Rose
    "#D97706"  // Warm Bronze
)

/**
 * Represents a required piece cut with length and requested quantity.
 */
data class RequiredCut(
    val id: String = UUID.randomUUID().toString(),
    val length: Double,
    val quantity: Int = 1,
    val label: String = "",
    val colorHex: String = PIECE_PALETTE_HEX.first()
) {
    init {
        require(length > 0) { "Cut length must be greater than 0" }
        require(quantity > 0) { "Quantity must be at least 1" }
    }

    fun displayLabel(unit: LengthUnit): String {
        val lenStr = unit.format(length)
        return if (label.isNotBlank()) "$label ($lenStr)" else lenStr
    }
}
