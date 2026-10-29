package com.example.pipecut.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pipecut.data.model.LengthUnit
import com.example.pipecut.data.model.PipeCuttingPlan
import com.example.pipecut.data.model.PlacedCut
import com.example.pipecut.ui.theme.AmberSafety
import com.example.pipecut.ui.theme.EmeraldReusable
import com.example.pipecut.ui.theme.RedTrim
import com.example.pipecut.ui.theme.SlateScrap

/**
 * Visual proportional diagram representing a single cut stock pipe.
 * Renders lead/trail trim, cut pieces, kerf marks, and reusable offcuts or scrap drops.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PipeDiagram(
    plan: PipeCuttingPlan,
    unit: LengthUnit,
    modifier: Modifier = Modifier
) {
    var selectedCut by remember { mutableStateOf<PlacedCut?>(null) }
    var selectedLeftover by remember { mutableStateOf<Boolean?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Pipe index & utilization badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pipe #${plan.pipeIndex}: ${plan.stockLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total Length: ${unit.format(plan.stockLength)} • ${plan.cuts.size} Cuts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (plan.utilizationPercentage >= 85.0) EmeraldReusable.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", plan.utilizationPercentage)}% used",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (plan.utilizationPercentage >= 85.0) EmeraldReusable else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val defaultPrimary = MaterialTheme.colorScheme.primary

            // Proportional Pipe Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            // Clear selection on background tap
                            selectedCut = null
                            selectedLeftover = null
                        }
                ) {
                    val totalLen = plan.stockLength
                    if (totalLen <= 0) return@Canvas
                    val widthPx = size.width
                    val heightPx = size.height

                    val scale = widthPx / totalLen

                    // Helper to get X coordinate
                    val toX = { len: Double -> (len * scale).toFloat() }

                    // 1. Draw Lead Trim if any
                    if (plan.leadTrim > 0) {
                        val trimW = toX(plan.leadTrim)
                        drawRect(
                            color = RedTrim.copy(alpha = 0.45f),
                            topLeft = Offset(0f, 0f),
                            size = Size(trimW, heightPx)
                        )
                        // Diagonal trim hatch lines
                        drawLine(
                            color = RedTrim,
                            start = Offset(0f, heightPx),
                            end = Offset(trimW, 0f),
                            strokeWidth = 2f
                        )
                    }

                    // 2. Draw each cut piece & Kerf cut lines
                    for (cut in plan.cuts) {
                        val startX = toX(cut.startPosition)
                        val cutW = toX(cut.length + cut.extraAllowance)
                        val color = runCatching {
                            Color(android.graphics.Color.parseColor(cut.colorHex))
                        }.getOrDefault(defaultPrimary)

                        // Fill piece rectangle
                        drawRect(
                            color = color.copy(alpha = 0.85f),
                            topLeft = Offset(startX, 0f),
                            size = Size(cutW, heightPx)
                        )

                        // Border around cut piece
                        drawRect(
                            color = color,
                            topLeft = Offset(startX, 0f),
                            size = Size(cutW, heightPx),
                            style = Stroke(width = 2f)
                        )

                        // Kerf cut indicator
                        if (cut.kerfAfter > 0) {
                            val kerfX = startX + cutW
                            val kerfW = maxOf(2f, toX(cut.kerfAfter))
                            drawRect(
                                color = AmberSafety,
                                topLeft = Offset(kerfX, 0f),
                                size = Size(kerfW, heightPx)
                            )
                        }
                    }

                    // 3. Draw Leftover Section
                    if (plan.leftoverLength > 0.001) {
                        val leftoverStart = toX(plan.stockLength - plan.leftoverLength - plan.trailTrim)
                        val leftoverW = toX(plan.leftoverLength)
                        val leftoverColor = if (plan.isReusableOffcut) EmeraldReusable else SlateScrap

                        drawRect(
                            color = leftoverColor.copy(alpha = 0.35f),
                            topLeft = Offset(leftoverStart, 0f),
                            size = Size(leftoverW, heightPx)
                        )

                        // Dashed outline for leftover
                        drawRoundRect(
                            color = leftoverColor,
                            topLeft = Offset(leftoverStart, 2f),
                            size = Size(leftoverW - 2f, heightPx - 4f),
                            cornerRadius = CornerRadius(4f, 4f),
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                        )
                    }

                    // 4. Draw Trail Trim if any
                    if (plan.trailTrim > 0) {
                        val trailStart = toX(plan.stockLength - plan.trailTrim)
                        val trailW = toX(plan.trailTrim)
                        drawRect(
                            color = RedTrim.copy(alpha = 0.45f),
                            topLeft = Offset(trailStart, 0f),
                            size = Size(trailW, heightPx)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Piece chips row (clickable for inspection)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Pieces
                for (cut in plan.cuts) {
                    val pieceColor = runCatching {
                        Color(android.graphics.Color.parseColor(cut.colorHex))
                    }.getOrDefault(MaterialTheme.colorScheme.primary)

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedCut == cut) pieceColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedCut == cut) pieceColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.clickable {
                            selectedLeftover = null
                            selectedCut = if (selectedCut == cut) null else cut
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(pieceColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "#${cut.sequenceNumber} ${cut.label}: ${unit.format(cut.length)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Leftover chip
                if (plan.leftoverLength > 0.001) {
                    val leftoverColor = if (plan.isReusableOffcut) EmeraldReusable else SlateScrap
                    val tagText = if (plan.isReusableOffcut) "Offcut" else "Scrap"

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (selectedLeftover == true) leftoverColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedLeftover == true) leftoverColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.clickable {
                            selectedCut = null
                            selectedLeftover = if (selectedLeftover == true) null else true
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(leftoverColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$tagText: ${unit.format(plan.leftoverLength)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Selected details callout
            AnimatedVisibility(visible = selectedCut != null) {
                selectedCut?.let { cut ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ContentCut,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cut #${cut.sequenceNumber}: ${cut.label}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cut Range: ${unit.format(cut.startPosition)} ➔ ${unit.format(cut.endPosition)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Piece Length: ${unit.format(cut.length)}" +
                                        if (cut.extraAllowance > 0) " (+${unit.format(cut.extraAllowance)} margin)" else "" +
                                        " • Kerf blade width: ${unit.format(cut.kerfAfter)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedLeftover == true) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = if (plan.isReusableOffcut) EmeraldReusable else SlateScrap,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (plan.isReusableOffcut) "Reusable Offcut" else "Unusable Scrap Drop",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (plan.isReusableOffcut) EmeraldReusable else SlateScrap
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Remaining Length: ${unit.format(plan.leftoverLength)} (threshold: ≥ ${unit.format(plan.minReusableThreshold)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (plan.isReusableOffcut)
                                "Can be kept in inventory and reused for future smaller cuts."
                            else
                                "Drop is shorter than reusable threshold; classify as scrap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
