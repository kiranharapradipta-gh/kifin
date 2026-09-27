package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DailyChartPoint
import com.example.ui.NoteGroupSummary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyUtils
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun MonthlyLineChart(
    points: List<DailyChartPoint>,
    modifier: Modifier = Modifier,
    title: String = "Grafik Arus Kas Bulanan"
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val maxIncome = points.maxOfOrNull { it.income } ?: 0L
    val maxExpense = points.maxOfOrNull { it.expense } ?: 0L
    val maxY = max(100000L, max(maxIncome, maxExpense)).toFloat()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_line_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(IncomeGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Masuk",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(ExpenseRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Keluar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tooltip banner if point selected
            val selectedPoint = selectedPointIndex?.let { if (it in points.indices) points[it] else null }
            if (selectedPoint != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tgl ${selectedPoint.dateLabel}:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "+${CurrencyUtils.formatRupiah(selectedPoint.income)}",
                                color = IncomeGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "-${CurrencyUtils.formatRupiah(selectedPoint.expense)}",
                                color = ExpenseRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Ketuk titik grafik untuk melihat rincian tanggal",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Canvas drawing
            val strokeGrid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            val n = points.size

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                if (points.isNotEmpty()) {
                                    val paddingLeft = 32f
                                    val paddingRight = 16f
                                    val chartWidth = size.width - paddingLeft - paddingRight
                                    val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth

                                    var closestIdx = 0
                                    var minDistance = Float.MAX_VALUE
                                    for (i in points.indices) {
                                        val px = paddingLeft + i * stepX
                                        val dist = kotlin.math.abs(offset.x - px)
                                        if (dist < minDistance) {
                                            minDistance = dist
                                            closestIdx = i
                                        }
                                    }
                                    selectedPointIndex = closestIdx
                                }
                            }
                        }
                ) {
                    val paddingLeft = 32f
                    val paddingRight = 16f
                    val paddingTop = 16f
                    val paddingBottom = 28f

                    val chartWidth = size.width - paddingLeft - paddingRight
                    val chartHeight = size.height - paddingTop - paddingBottom

                    if (points.isEmpty()) return@Canvas

                    // Draw 3 horizontal grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = paddingTop + chartHeight * (i.toFloat() / gridLines)
                        drawLine(
                            color = strokeGrid,
                            start = Offset(paddingLeft, y),
                            end = Offset(size.width - paddingRight, y),
                            strokeWidth = 1f
                        )
                    }

                    val stepX = if (n > 1) chartWidth / (n - 1) else chartWidth

                    val incomePath = Path()
                    val expensePath = Path()

                    val incomeFillPath = Path()
                    val expenseFillPath = Path()

                    val incomeCoords = mutableListOf<Offset>()
                    val expenseCoords = mutableListOf<Offset>()

                    points.forEachIndexed { i, p ->
                        val x = paddingLeft + i * stepX
                        val yInc = paddingTop + chartHeight - (p.income.toFloat() / maxY) * chartHeight
                        val yExp = paddingTop + chartHeight - (p.expense.toFloat() / maxY) * chartHeight

                        val ptInc = Offset(x, yInc)
                        val ptExp = Offset(x, yExp)

                        incomeCoords.add(ptInc)
                        expenseCoords.add(ptExp)

                        if (i == 0) {
                            incomePath.moveTo(x, yInc)
                            expensePath.moveTo(x, yExp)

                            incomeFillPath.moveTo(x, paddingTop + chartHeight)
                            incomeFillPath.lineTo(x, yInc)

                            expenseFillPath.moveTo(x, paddingTop + chartHeight)
                            expenseFillPath.lineTo(x, yExp)
                        } else {
                            incomePath.lineTo(x, yInc)
                            expensePath.lineTo(x, yExp)

                            incomeFillPath.lineTo(x, yInc)
                            expenseFillPath.lineTo(x, yExp)
                        }
                    }

                    if (points.isNotEmpty()) {
                        val lastX = paddingLeft + (n - 1) * stepX
                        val bottomY = paddingTop + chartHeight

                        incomeFillPath.lineTo(lastX, bottomY)
                        incomeFillPath.close()

                        expenseFillPath.lineTo(lastX, bottomY)
                        expenseFillPath.close()

                        // Draw subtle gradient under income
                        drawPath(
                            path = incomeFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(IncomeGreen.copy(alpha = 0.18f), IncomeGreen.copy(alpha = 0.01f)),
                                startY = paddingTop,
                                endY = bottomY
                            )
                        )

                        // Draw subtle gradient under expense
                        drawPath(
                            path = expenseFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(ExpenseRed.copy(alpha = 0.14f), ExpenseRed.copy(alpha = 0.01f)),
                                startY = paddingTop,
                                endY = bottomY
                            )
                        )

                        // Draw lines
                        drawPath(
                            path = incomePath,
                            color = IncomeGreen,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        drawPath(
                            path = expensePath,
                            color = ExpenseRed,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Draw points
                        incomeCoords.forEachIndexed { idx, pt ->
                            if (points[idx].income > 0L) {
                                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
                                drawCircle(color = IncomeGreen, radius = 3.dp.toPx(), center = pt)
                            }
                        }

                        expenseCoords.forEachIndexed { idx, pt ->
                            if (points[idx].expense > 0L) {
                                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
                                drawCircle(color = ExpenseRed, radius = 3.dp.toPx(), center = pt)
                            }
                        }

                        // Highlight selected point
                        selectedPointIndex?.let { selIdx ->
                            if (selIdx in points.indices) {
                                val selX = paddingLeft + selIdx * stepX
                                drawLine(
                                    color = Color.Gray.copy(alpha = 0.5f),
                                    start = Offset(selX, paddingTop),
                                    end = Offset(selX, bottomY),
                                    strokeWidth = 1.5.dp.toPx()
                                )

                                drawCircle(
                                    color = IncomeGreen,
                                    radius = 6.dp.toPx(),
                                    center = incomeCoords[selIdx]
                                )
                                drawCircle(
                                    color = ExpenseRed,
                                    radius = 6.dp.toPx(),
                                    center = expenseCoords[selIdx]
                                )
                            }
                        }
                    }
                }
            }

            // X-axis day indicators (start, middle, end)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (points.isNotEmpty()) {
                    Text(
                        text = "Tgl ${points.first().dateLabel}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (points.size > 2) {
                        val mid = points[points.size / 2]
                        Text(
                            text = "Tgl ${mid.dateLabel}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Tgl ${points.last().dateLabel}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

val NoteGroupColors = listOf(
    Color(0xFF0D9488), // Teal
    Color(0xFF0284C7), // Sky
    Color(0xFFF59E0B), // Amber
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899), // Pink
    Color(0xFF10B981), // Emerald
    Color(0xFF6366F1), // Indigo
    Color(0xFFF97316), // Orange
    Color(0xFF14B8A6), // Cyan
    Color(0xFF64748B)  // Slate
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteGroupDonutChart(
    groups: List<NoteGroupSummary>,
    selectedGroup: NoteGroupSummary?,
    onSelectGroup: (NoteGroupSummary?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_group_donut_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Proporsi Kategori Catatan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dikelompokkan berdasarkan catatan transaksi yang sama",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada transaksi pada periode ini",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                // Donut Chart Canvas
                val totalAmount = groups.sumOf { it.totalAmount }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(190.dp)
                            .pointerInput(groups) {
                                detectTapGestures { offset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = offset.x - center.x
                                    val dy = offset.y - center.y
                                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                                    val outerRadius = size.width / 2f
                                    val innerRadius = outerRadius * 0.58f

                                    if (distance in innerRadius..outerRadius) {
                                        var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < 0) angle += 360f

                                        // Slices start at -90 degrees (top)
                                        var adjustedAngle = (angle + 90f) % 360f

                                        var accumulatedAngle = 0f
                                        var clicked: NoteGroupSummary? = null
                                        for (group in groups) {
                                            val sweep = (group.percentage / 100f) * 360f
                                            if (adjustedAngle >= accumulatedAngle && adjustedAngle <= accumulatedAngle + sweep) {
                                                clicked = group
                                                break
                                            }
                                            accumulatedAngle += sweep
                                        }
                                        onSelectGroup(if (selectedGroup?.note == clicked?.note) null else clicked)
                                    } else {
                                        onSelectGroup(null)
                                    }
                                }
                            }
                    ) {
                        val strokeWidth = 32.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                        val arcSize = Size(diameter, diameter)

                        var startAngle = -90f
                        groups.forEachIndexed { index, group ->
                            val sweepAngle = (group.percentage / 100f) * 360f
                            val color = NoteGroupColors[index % NoteGroupColors.size]
                            val isSelected = selectedGroup?.note == group.note

                            drawArc(
                                color = if (isSelected) color else color.copy(alpha = 0.88f),
                                startAngle = startAngle,
                                sweepAngle = max(sweepAngle - 2f, 1f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = if (isSelected) strokeWidth * 1.2f else strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Center label inside Donut
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        if (selectedGroup != null) {
                            Text(
                                text = selectedGroup.note,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = CurrencyUtils.formatRupiah(selectedGroup.totalAmount),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = String.format("%.1f%% (%d entri)", selectedGroup.percentage, selectedGroup.count),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Total Nominal",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatRupiah(totalAmount),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ketuk irisan",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Group Chips with colors and clickability
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.forEachIndexed { index, group ->
                        val color = NoteGroupColors[index % NoteGroupColors.size]
                        val isSelected = selectedGroup?.note == group.note

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
                            modifier = Modifier
                                .clickable {
                                    onSelectGroup(if (isSelected) null else group)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${group.note} (${String.format("%.0f%%", group.percentage)})",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
