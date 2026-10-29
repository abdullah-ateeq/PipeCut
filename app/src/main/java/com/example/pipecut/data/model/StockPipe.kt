package com.example.pipecut.data.model

import java.util.UUID

/**
 * Represents an available stock pipe size and its quantity.
 * Length is stored in the current project [unit].
 */
data class StockPipe(
    val id: String = UUID.randomUUID().toString(),
    val length: Double,
    val quantity: Int = 1,
    val label: String = "",
    val costPerUnit: Double? = null
) {
    init {
        require(length > 0) { "Stock length must be greater than 0" }
        require(quantity > 0) { "Stock quantity must be at least 1" }
    }

    fun displayLabel(unit: LengthUnit): String {
        val lenStr = unit.format(length)
        return if (label.isNotBlank()) "$label ($lenStr)" else lenStr
    }
}
