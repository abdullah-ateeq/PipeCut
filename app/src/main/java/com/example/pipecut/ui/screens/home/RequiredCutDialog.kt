package com.example.pipecut.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pipecut.data.model.LengthUnit
import com.example.pipecut.data.model.PIECE_PALETTE_HEX
import com.example.pipecut.data.model.RequiredCut
import com.example.pipecut.domain.converter.UnitConverter
import com.example.pipecut.ui.components.NumberInputField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequiredCutDialog(
    initialCut: RequiredCut?,
    unit: LengthUnit,
    nextSequenceIndex: Int,
    onDismiss: () -> Unit,
    onSave: (RequiredCut) -> Unit
) {
    var lengthStr by remember {
        mutableStateOf(if (initialCut != null) UnitConverter.formatDouble(initialCut.length) else "")
    }
    var quantityStr by remember {
        mutableStateOf(initialCut?.quantity?.toString() ?: "1")
    }
    var labelStr by remember {
        mutableStateOf(initialCut?.label ?: "Piece $nextSequenceIndex")
    }
    var selectedColorHex by remember {
        mutableStateOf(
            initialCut?.colorHex ?: PIECE_PALETTE_HEX[(nextSequenceIndex - 1).coerceAtLeast(0) % PIECE_PALETTE_HEX.size]
        )
    }

    var lengthError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCut != null) "Edit Required Cut" else "Add Required Cut",
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
                NumberInputField(
                    value = lengthStr,
                    onValueChange = {
                        lengthStr = it
                        lengthError = null
                    },
                    label = "Required Cut Length",
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
                    label = "Quantity Needed",
                    isInteger = true,
                    isError = quantityError != null,
                    errorMessage = quantityError
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = labelStr,
                    onValueChange = { labelStr = it },
                    label = { Text("Piece Tag / Name") },
                    placeholder = { Text("e.g. Leg, Rafter, Brace A") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Color Identification Tag:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PIECE_PALETTE_HEX.forEach { hex ->
                        val color = runCatching {
                            Color(android.graphics.Color.parseColor(hex))
                        }.getOrDefault(MaterialTheme.colorScheme.primary)
                        val isSelected = hex.equals(selectedColorHex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val len = lengthStr.toDoubleOrNull()
                    val qty = quantityStr.toIntOrNull()

                    var hasError = false
                    if (len == null || len <= 0) {
                        lengthError = "Enter a valid positive cut length"
                        hasError = true
                    }
                    if (qty == null || qty <= 0) {
                        quantityError = "Quantity must be at least 1"
                        hasError = true
                    }

                    if (!hasError && len != null && qty != null) {
                        val cut = RequiredCut(
                            id = initialCut?.id ?: java.util.UUID.randomUUID().toString(),
                            length = len,
                            quantity = qty,
                            label = labelStr.trim(),
                            colorHex = selectedColorHex
                        )
                        onSave(cut)
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
