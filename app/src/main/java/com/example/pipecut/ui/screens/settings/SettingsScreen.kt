package com.example.pipecut.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pipecut.data.model.AppThemeMode
import com.example.pipecut.data.model.LengthUnit
import com.example.pipecut.domain.converter.UnitConverter
import com.example.pipecut.ui.components.NumberInputField
import com.example.pipecut.ui.components.UnitSelector
import com.example.pipecut.ui.theme.RedTrim
import com.example.pipecut.ui.viewmodel.AppScreen
import com.example.pipecut.ui.viewmodel.PipeCutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PipeCutViewModel,
    settings: com.example.pipecut.data.model.CuttingSettings
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // THEME MODE
            Text(
                text = "APPEARANCE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ThemeOptionRow(
                        title = "System Default",
                        subtitle = "Follows Android system appearance",
                        icon = Icons.Default.BrightnessAuto,
                        selected = settings.themeMode == AppThemeMode.SYSTEM,
                        onSelect = { viewModel.setThemeMode(AppThemeMode.SYSTEM) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ThemeOptionRow(
                        title = "Light Theme",
                        subtitle = "High contrast clean white palette",
                        icon = Icons.Default.LightMode,
                        selected = settings.themeMode == AppThemeMode.LIGHT,
                        onSelect = { viewModel.setThemeMode(AppThemeMode.LIGHT) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ThemeOptionRow(
                        title = "Dark Theme",
                        subtitle = "Industrial charcoal dark mode",
                        icon = Icons.Default.DarkMode,
                        selected = settings.themeMode == AppThemeMode.DARK,
                        onSelect = { viewModel.setThemeMode(AppThemeMode.DARK) }
                    )
                }
            }

            // MEASUREMENT & UNIT
            Text(
                text = "MEASUREMENT UNIT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            UnitSelector(
                selectedUnit = settings.unit,
                onUnitSelected = { viewModel.changeUnit(it) }
            )

            // WORKSHOP DEFAULTS
            Text(
                text = "WORKSHOP CUTTING DEFAULTS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NumberInputField(
                        value = UnitConverter.formatDouble(settings.sawKerf),
                        onValueChange = {
                            val v = it.toDoubleOrNull() ?: 0.0
                            viewModel.updateSettings(settings.copy(sawKerf = v))
                        },
                        label = "Default Saw Kerf (Blade Width)",
                        suffix = settings.unit.symbol
                    )

                    NumberInputField(
                        value = UnitConverter.formatDouble(settings.endTrim),
                        onValueChange = {
                            val v = it.toDoubleOrNull() ?: 0.0
                            viewModel.updateSettings(settings.copy(endTrim = v))
                        },
                        label = "Default End-Trim Allowance",
                        suffix = settings.unit.symbol
                    )

                    NumberInputField(
                        value = UnitConverter.formatDouble(settings.minReusableLength),
                        onValueChange = {
                            val v = it.toDoubleOrNull() ?: 0.0
                            viewModel.updateSettings(settings.copy(minReusableLength = v))
                        },
                        label = "Min Reusable Offcut Length",
                        suffix = settings.unit.symbol
                    )
                }
            }

            // DATA STORAGE & MAINTENANCE
            Text(
                text = "DATA & STORAGE",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearConfirm = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RedTrim)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Clear Saved Projects",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = RedTrim
                            )
                            Text(
                                text = "Removes all saved calculation history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear All Projects?") },
            text = { Text("This will permanently remove all saved project calculations from your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProject("")
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear", color = RedTrim)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
    }
}
