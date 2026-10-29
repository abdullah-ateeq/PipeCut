package com.example.pipecut.ui.screens.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pipecut.data.model.LengthUnit
import com.example.pipecut.data.model.StockPipe
import com.example.pipecut.domain.converter.UnitConverter
import com.example.pipecut.ui.components.NumberInputField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StockPipeDialog(
    initialStock: StockPipe?,
    unit: LengthUnit,
    onDismiss: () -> Unit,
    onSave: (StockPipe) -> Unit
) {
    var lengthStr by remember {
        mutableStateOf(if (initialStock != null) UnitConverter.formatDouble(initialStock.length) else "")
    }
    var quantityStr by remember {
        mutableStateOf(initialStock?.quantity?.toString() ?: "1")
    }
    var labelStr by remember {
        mutableStateOf(initialStock?.label ?: "")
    }

    var lengthError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }

    // Quick presets based on current unit
    val presets = when (unit) {
        LengthUnit.MM -> listOf(6000.0 to "6m Standard", 3000.0 to "3m Half", 12000.0 to "12m Industrial")
        LengthUnit.CM -> listOf(600.0 to "6m", 300.0 to "3m", 1200.0 to "12m")
        LengthUnit.INCH -> listOf(240.0 to "20ft (240\")", 120.0 to "10ft (120\")", 84.0 to "7ft (84\")")
        LengthUnit.FT -> listOf(20.0 to "20 ft", 10.0 to "10 ft", 21.0 to "21 ft Schedule 40")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialStock != null) "Edit Stock Pipe" else "Add Stock Pipe",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Presets
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (len, title) ->
                        FilterChip(
                            selected = lengthStr == UnitConverter.formatDouble(len),
                            onClick = {
                                lengthStr = UnitConverter.formatDouble(len)
                                if (labelStr.isBlank()) labelStr = title
                            },
                            label = { Text(title, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                NumberInputField(
                    value = lengthStr,
                    onValueChange = {
                        lengthStr = it
                        lengthError = null
                    },
                    label = "Stock Pipe Length",
                    suffix = unit.symbol,
                    isError = lengthError != null,
                    errorMessage = lengthError
                )

                Spacer(modifier = Modifier.height(10.dp))

                NumberInputField(
                    value = quantityStr,
                    onValueChange = {
                        quantityStr = it
                        quantityError = null
                    },
                    label = "Available Quantity",
                    isInteger = true,
                    isError = quantityError != null,
                    errorMessage = quantityError
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = labelStr,
                    onValueChange = { labelStr = it },
                    label = { Text("Label / Description (Optional)") },
                    placeholder = { Text("e.g. Schedule 40, Stainless 2\", etc.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val len = lengthStr.toDoubleOrNull()
                    val qty = quantityStr.toIntOrNull()

                    var hasError = false
                    if (len == null || len <= 0) {
                        lengthError = "Enter a valid positive length"
                        hasError = true
                    }
                    if (qty == null || qty <= 0) {
                        quantityError = "Quantity must be at least 1"
                        hasError = true
                    }

                    if (!hasError && len != null && qty != null) {
                        val stock = StockPipe(
                            id = initialStock?.id ?: java.util.UUID.randomUUID().toString(),
                            length = len,
                            quantity = qty,
                            label = labelStr.trim()
                        )
                        onSave(stock)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
