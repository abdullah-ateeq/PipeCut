package com.example.pipecut.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isInteger: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    onDone: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Filter to valid numeric characters
            val filtered = if (isInteger) {
                input.filter { it.isDigit() }
            } else {
                var decimalSeen = false
                input.filter {
                    if (it.isDigit()) true
                    else if (it == '.' && !decimalSeen) {
                        decimalSeen = true
                        true
                    } else false
                }
            }
            onValueChange(filtered)
        },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
        } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isInteger) KeyboardType.Number else KeyboardType.Decimal,
            imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone?.invoke() }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.fillMaxWidth()
    )
}
