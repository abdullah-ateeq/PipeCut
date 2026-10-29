package com.example.pipecut.ui.screens.results

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pipecut.data.model.OptimizationResult
import com.example.pipecut.domain.exporter.CuttingPlanExporter
import com.example.pipecut.ui.components.MetricCard
import com.example.pipecut.ui.components.PipeDiagram
import com.example.pipecut.ui.theme.AmberSafety
import com.example.pipecut.ui.theme.EmeraldReusable
import com.example.pipecut.ui.theme.RedTrim
import com.example.pipecut.ui.theme.SlateScrap
import com.example.pipecut.ui.viewmodel.AppScreen
import com.example.pipecut.ui.viewmodel.PipeCutViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    viewModel: PipeCutViewModel,
    result: OptimizationResult?
) {
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf(false) }
    var selectedPipeFilter by remember { mutableIntStateOf(0) } // 0 = All pipes, >0 = specific pipeIndex

    if (result == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No cutting plan calculated yet")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                    Text("Return to Calculator")
                }
            }
        }
        return
    }

    val unit = result.unit

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cutting Plan & Results",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${result.totalStockPipesUsed} Pipes • ${result.totalPiecesCut} Cuts • ${String.format(Locale.US, "%.1f", result.materialUtilizationPercentage)}% Utilization",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val planText = CuttingPlanExporter.generateTextPlan(result)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("PipeCut Plan", planText))
                        Toast.makeText(context, "Cutting plan copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Plan")
                    }

                    IconButton(onClick = { showShareMenu = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }

                    DropdownMenu(
                        expanded = showShareMenu,
                        onDismissRequest = { showShareMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share Text Cutting Plan") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showShareMenu = false
                                val text = CuttingPlanExporter.generateTextPlan(result)
                                CuttingPlanExporter.shareText(context, text)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export CSV Cutting Table") },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                            onClick = {
                                showShareMenu = false
                                val csv = CuttingPlanExporter.generateCsv(result)
                                CuttingPlanExporter.shareCsv(context, csv)
                            }
                        )
                    }

                    IconButton(onClick = {
                        viewModel.saveCurrentProject()
                        Toast.makeText(context, "Project saved to history", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save Project")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Insufficient Stock Alert Banner if any
            if (result.hasUnfulfilled) {
                item {
                    UnfulfilledAlertCard(result = result)
                }
            }

            // KPI Scorecards Row 1
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Material Utilization",
                        value = "${String.format(Locale.US, "%.1f", result.materialUtilizationPercentage)}%",
                        subtitle = "Useful piece ratio",
                        icon = Icons.Default.PieChart,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Total Usable Yield",
                        value = "${String.format(Locale.US, "%.1f", result.totalYieldPercentage)}%",
                        subtitle = "Includes reusable offcuts",
                        icon = Icons.Default.Recycling,
                        accentColor = EmeraldReusable,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // KPI Scorecards Row 2
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Stock Pipes Used",
                        value = "${result.totalStockPipesUsed}",
                        subtitle = unit.format(result.totalStockLengthConsumed),
                        icon = Icons.Default.Inventory,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Pieces Cut",
                        value = "${result.totalPiecesCut} / ${result.totalPiecesRequested}",
                        subtitle = unit.format(result.totalUsefulLength),
                        icon = Icons.Default.CheckCircle,
                        accentColor = if (result.isFullySatisfied) EmeraldReusable else AmberSafety,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // KPI Scorecards Row 3 (Kerf, Reusable Offcuts, Scrap Waste)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Kerf Loss",
                        value = unit.format(result.totalKerfLoss),
                        subtitle = "${result.totalPiecesCut} saw cuts",
                        accentColor = AmberSafety,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Reusable Offcuts",
                        value = unit.format(result.totalReusableOffcutLength),
                        subtitle = "${result.reusableOffcutsCount} offcuts saved",
                        accentColor = EmeraldReusable,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Scrap Waste",
                        value = "${String.format(Locale.US, "%.1f", result.wastePercentage)}%",
                        subtitle = unit.format(result.totalScrapWasteLength),
                        accentColor = SlateScrap,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Pipe Filter Chips
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedPipeFilter == 0,
                        onClick = { selectedPipeFilter = 0 },
                        label = { Text("All Pipes (${result.plans.size})") }
                    )
                    result.plans.forEach { plan ->
                        FilterChip(
                            selected = selectedPipeFilter == plan.pipeIndex,
                            onClick = { selectedPipeFilter = plan.pipeIndex },
                            label = { Text("Pipe #${plan.pipeIndex}") }
                        )
                    }
                }
            }

            // Display Pipes (Filtered)
            val displayedPlans = if (selectedPipeFilter == 0) {
                result.plans
            } else {
                result.plans.filter { it.pipeIndex == selectedPipeFilter }
            }

            items(displayedPlans, key = { it.pipeIndex }) { plan ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pipe visual diagram
                    PipeDiagram(plan = plan, unit = unit)

                    // Step-by-step cut list table
                    CutSequenceTable(plan = plan, unit = unit)

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun UnfulfilledAlertCard(result: OptimizationResult) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = RedTrim.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, RedTrim),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = RedTrim,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Insufficient Stock Available",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = RedTrim
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "The available stock pipes could not satisfy all pieces. The following cuts remain unfulfilled:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            result.unfulfilledCuts.forEach { un ->
                Text(
                    text = "• ${un.quantity}x ${un.label.ifBlank { "Piece" }} (${result.unit.format(un.length)})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = RedTrim
                )
            }
        }
    }
}

@Composable
private fun CutSequenceTable(
    plan: com.example.pipecut.data.model.PipeCuttingPlan,
    unit: com.example.pipecut.data.model.LengthUnit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Cutting Sequence & Markings",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lead trim
            if (plan.leadTrim > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Lead Trim: Square pipe end",
                        style = MaterialTheme.typography.bodySmall,
                        color = RedTrim,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Cut at ${unit.format(plan.leadTrim)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = RedTrim
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }

            // Cuts
            plan.cuts.forEach { cut ->
                val pieceColor = runCatching {
                    Color(android.graphics.Color.parseColor(cut.colorHex))
                }.getOrDefault(MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(pieceColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Cut #${cut.sequenceNumber}: ${cut.label}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Length: ${unit.format(cut.length)}" +
                                        if (cut.extraAllowance > 0) " (+${unit.format(cut.extraAllowance)} margin)" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${unit.format(cut.startPosition)} ➔ ${unit.format(cut.endPosition)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Kerf: ${unit.format(cut.kerfAfter)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }

            // Leftover drop
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val leftoverColor = if (plan.isReusableOffcut) EmeraldReusable else SlateScrap
                Text(
                    text = if (plan.isReusableOffcut) "Reusable Offcut Remnant" else "Scrap Drop Remnant",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = leftoverColor
                )
                Text(
                    text = unit.format(plan.leftoverLength),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = leftoverColor
                )
            }
        }
    }
}
