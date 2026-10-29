package com.example.pipecut.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pipecut.data.model.LengthUnit
import com.example.pipecut.data.model.RequiredCut
import com.example.pipecut.data.model.StockPipe
import com.example.pipecut.domain.converter.UnitConverter
import com.example.pipecut.ui.components.NumberInputField
import com.example.pipecut.ui.components.UnitSelector
import com.example.pipecut.ui.theme.AmberSafety
import com.example.pipecut.ui.theme.RedTrim
import com.example.pipecut.ui.viewmodel.AppScreen
import com.example.pipecut.ui.viewmodel.PipeCutUiState
import com.example.pipecut.ui.viewmodel.PipeCutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PipeCutViewModel,
    uiState: PipeCutUiState
) {
    val snackbarHostState = remember { SnackbarHostState() }

    var showStockDialog by remember { mutableStateOf(false) }
    var editingStock by remember { mutableStateOf<StockPipe?>(null) }

    var showCutDialog by remember { mutableStateOf(false) }
    var editingCut by remember { mutableStateOf<RequiredCut?>(null) }

    var showSettingsExpander by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PipeCut",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = uiState.projectName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.HISTORY) }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = { viewModel.saveCurrentProject() }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.navigateTo(AppScreen.SETTINGS)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("About PipeCut") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.navigateTo(AppScreen.ABOUT)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear All Entries") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.resetCalculator()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    val totalStockQty = uiState.stockPipes.sumOf { it.quantity }
                    val totalCutsQty = uiState.requiredCuts.sumOf { it.quantity }

                    Button(
                        onClick = { viewModel.calculateCuttingPlan() },
                        enabled = totalStockQty > 0 && totalCutsQty > 0 && !uiState.isOptimizing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isOptimizing) "Optimizing Cuts..." else "Calculate Cutting Plan ($totalCutsQty pieces)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
                // Unit Selector
                UnitSelector(
                    selectedUnit = uiState.settings.unit,
                    onUnitSelected = { viewModel.changeUnit(it) }
                )
            }

            // Cutting Parameters Card (Collapsible)
            item {
                CuttingSettingsCard(
                    settings = uiState.settings,
                    unit = uiState.settings.unit,
                    isExpanded = showSettingsExpander,
                    onToggleExpand = { showSettingsExpander = !showSettingsExpander },
                    onUpdateSettings = { viewModel.updateSettings(it) }
                )
            }

            // Validation Issues Banner
            if (uiState.validationIssues.isNotEmpty()) {
                item {
                    ValidationBanner(issues = uiState.validationIssues)
                }
            }

            // SECTION 1: Stock Pipes
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AVAILABLE STOCK PIPES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${uiState.stockPipes.sumOf { it.quantity }} pipes available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            editingStock = null
                            showStockDialog = true
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Stock")
                    }
                }
            }

            if (uiState.stockPipes.isEmpty()) {
                item {
                    EmptySectionPlaceholder(
                        title = "No stock pipes added",
                        subtitle = "Add your available full pipe lengths (e.g. 6000 mm or 20 ft) to begin."
                    )
                }
            } else {
                items(uiState.stockPipes, key = { it.id }) { stock ->
                    StockPipeCard(
                        stock = stock,
                        unit = uiState.settings.unit,
                        onEdit = {
                            editingStock = stock
                            showStockDialog = true
                        },
                        onDuplicate = { viewModel.duplicateStock(stock) },
                        onDelete = { viewModel.deleteStock(stock.id) }
                    )
                }
            }

            // SECTION 2: Required Cut Pieces
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REQUIRED CUT PIECES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${uiState.requiredCuts.sumOf { it.quantity }} pieces needed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            editingCut = null
                            showCutDialog = true
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Piece")
                    }
                }
            }

            if (uiState.requiredCuts.isEmpty()) {
                item {
                    EmptySectionPlaceholder(
                        title = "No required pieces added",
                        subtitle = "Enter the lengths and quantities of the pieces you need to cut."
                    )
                }
            } else {
                items(uiState.requiredCuts, key = { it.id }) { cut ->
                    RequiredCutCard(
                        cut = cut,
                        unit = uiState.settings.unit,
                        onEdit = {
                            editingCut = cut
                            showCutDialog = true
                        },
                        onDuplicate = { viewModel.duplicateCut(cut) },
                        onDelete = { viewModel.deleteCut(cut.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Dialogs
    if (showStockDialog) {
        StockPipeDialog(
            initialStock = editingStock,
            unit = uiState.settings.unit,
            onDismiss = { showStockDialog = false },
            onSave = {
                viewModel.addOrUpdateStock(it)
                showStockDialog = false
            }
        )
    }

    if (showCutDialog) {
        RequiredCutDialog(
            initialCut = editingCut,
            unit = uiState.settings.unit,
            nextSequenceIndex = uiState.requiredCuts.size + 1,
            onDismiss = { showCutDialog = false },
            onSave = {
                viewModel.addOrUpdateCut(it)
                showCutDialog = false
            }
        )
    }
}

@Composable
private fun CuttingSettingsCard(
    settings: com.example.pipecut.data.model.CuttingSettings,
    unit: LengthUnit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdateSettings: (com.example.pipecut.data.model.CuttingSettings) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cutting Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(Kerf: ${unit.format(settings.sawKerf)} • Trim: ${unit.format(settings.endTrim)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NumberInputField(
                            value = UnitConverter.formatDouble(settings.sawKerf),
                            onValueChange = {
                                val v = it.toDoubleOrNull() ?: 0.0
                                onUpdateSettings(settings.copy(sawKerf = v))
                            },
                            label = "Saw Kerf (Blade Width)",
                            suffix = unit.symbol,
                            modifier = Modifier.weight(1f)
                        )

                        NumberInputField(
                            value = UnitConverter.formatDouble(settings.endTrim),
                            onValueChange = {
                                val v = it.toDoubleOrNull() ?: 0.0
                                onUpdateSettings(settings.copy(endTrim = v))
                            },
                            label = "End-Trim",
                            suffix = unit.symbol,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NumberInputField(
                            value = UnitConverter.formatDouble(settings.extraAllowancePerPiece),
                            onValueChange = {
                                val v = it.toDoubleOrNull() ?: 0.0
                                onUpdateSettings(settings.copy(extraAllowancePerPiece = v))
                            },
                            label = "Allowance / Piece",
                            suffix = unit.symbol,
                            modifier = Modifier.weight(1f)
                        )

                        NumberInputField(
                            value = UnitConverter.formatDouble(settings.minReusableLength),
                            onValueChange = {
                                val v = it.toDoubleOrNull() ?: 0.0
                                onUpdateSettings(settings.copy(minReusableLength = v))
                            },
                            label = "Min Reusable Offcut",
                            suffix = unit.symbol,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trim both ends of stock pipe",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = settings.trimBothEnds,
                            onCheckedChange = { onUpdateSettings(settings.copy(trimBothEnds = it)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockPipeCard(
    stock: StockPipe,
    unit: LengthUnit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${stock.quantity}x",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = unit.format(stock.length),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (stock.label.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stock.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = RedTrim)
                }
            }
        }
    }
}

@Composable
private fun RequiredCutCard(
    cut: RequiredCut,
    unit: LengthUnit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val pieceColor = runCatching {
        Color(android.graphics.Color.parseColor(cut.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(pieceColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${cut.quantity}x",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = unit.format(cut.length),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (cut.label.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = cut.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = RedTrim)
                }
            }
        }
    }
}

@Composable
private fun ValidationBanner(issues: List<com.example.pipecut.domain.algorithm.PipeCutOptimizer.ValidationIssue>) {
    val fatal = issues.any { it.isFatal }
    val bgColor = if (fatal) RedTrim.copy(alpha = 0.15f) else AmberSafety.copy(alpha = 0.15f)
    val borderColor = if (fatal) RedTrim else AmberSafety

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (fatal) Icons.Default.Warning else Icons.Default.Info,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (fatal) "Validation Error" else "Stock Notice",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            issues.forEach { issue ->
                Text(
                    text = "• ${issue.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun EmptySectionPlaceholder(title: String, subtitle: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
