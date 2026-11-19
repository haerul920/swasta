package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.entities.HealthJournal
import com.example.domain.enums.AirQualityLevel
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.MintHighlight
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftMint
import com.example.ui.theme.SubtleSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VibrantEmerald
import java.text.NumberFormat
import java.util.Locale

fun formatRupiah(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
        maximumFractionDigits = 0
    }
    return formatter.format(amount).replace("Rp", "Rp ")
}

@Composable
fun SwasanaLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(listOf(ForestEmerald, PrimaryEmerald))
            )
            .semantics { contentDescription = "Logo Swasana" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val w = this.size.width
            val h = this.size.height
            // Left stylized leaf-lung
            val leftLeaf = Path().apply {
                moveTo(w * 0.46f, h * 0.14f)
                cubicTo(w * 0.14f, h * 0.18f, w * 0.04f, h * 0.62f, w * 0.24f, h * 0.88f)
                cubicTo(w * 0.40f, h * 0.78f, w * 0.46f, h * 0.48f, w * 0.46f, h * 0.14f)
                close()
            }
            // Right stylized leaf-lung
            val rightLeaf = Path().apply {
                moveTo(w * 0.54f, h * 0.14f)
                cubicTo(w * 0.86f, h * 0.18f, w * 0.96f, h * 0.62f, w * 0.76f, h * 0.88f)
                cubicTo(w * 0.60f, h * 0.78f, w * 0.54f, h * 0.48f, w * 0.54f, h * 0.14f)
                close()
            }
            drawPath(leftLeaf, color = MintHighlight)
            drawPath(rightLeaf, color = PureWhite)
        }
    }
}

@Composable
fun EmeraldHeroCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(DeepEmerald, ForestEmerald, Color(0xFF0A4D38)),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 600f)
                )
            )
    ) {
        // Soft atmospheric light glow mirroring the Finora card shimmer
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x30A7F3D0), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.18f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.85f, size.height * 0.18f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1F34D399), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.92f),
                    radius = size.width * 0.42f
                ),
                radius = size.width * 0.42f,
                center = Offset(size.width * 0.12f, size.height * 0.92f)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
fun SwasanaCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PureWhite,
    borderColor: Color = CardBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SegmentedPillBar(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "segment"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(SubtleSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) PrimaryEmerald else Color.Transparent)
                    .clickable { onSelect(index) }
                    .testTag("${testTagPrefix}_$index")
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) PureWhite else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    testTagName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = SoftMint,
    iconTint: Color = ForestEmerald
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTagName)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(containerColor)
                .border(1.dp, CardBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AqiRadialGauge(
    aqi: Int,
    level: AirQualityLevel,
    modifier: Modifier = Modifier
) {
    val targetFraction = (aqi.toFloat() / 250f).coerceIn(0.06f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "aqi_gauge_anim"
    )

    Box(
        modifier = modifier.size(116.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 11.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            // Background track
            drawArc(
                color = Color(0x33FFFFFF),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            // Semantic progress arc
            drawArc(
                color = level.color,
                startAngle = 135f,
                sweepAngle = 270f * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = aqi.toString(),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 30.sp),
                color = PureWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "AQI",
                style = MaterialTheme.typography.labelSmall,
                color = MintHighlight
            )
        }
    }
}

@Composable
fun HealthScoreRing(
    percentage: Int,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (percentage.coerceIn(0, 100) / 100f) * 360f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "health_ring"
    )

    Box(
        modifier = modifier.size(66.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            drawArc(
                color = Color(0x33FFFFFF),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = MintHighlight,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.labelLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BiologicalLungCanvas(
    clarityRatio: Float,
    modifier: Modifier = Modifier
) {
    val animatedClarity by animateFloatAsState(
        targetValue = clarityRatio.coerceIn(0.15f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "lung_clarity"
    )

    // Shifts visually from muted smoky slate to clean vibrant coral-emerald as lungs recover
    val lungPrimary = lerp(Color(0xFF64748B), Color(0xFF10B981), animatedClarity)
    val lungSecondary = lerp(Color(0xFF94A3B8), Color(0xFF6EE7B7), animatedClarity)
    val airwayColor = lerp(Color(0xFFCBD5E1), PureWhite, animatedClarity)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .semantics { contentDescription = "Visualisasi Pemulihan Paru-Paru" }
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Soft oxygen aura behind lungs
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(lungSecondary.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(cx, h * 0.54f),
                radius = h * 0.56f
            ),
            radius = h * 0.56f,
            center = Offset(cx, h * 0.54f)
        )

        // Trachea
        drawLine(
            color = ForestEmerald,
            start = Offset(cx, h * 0.08f),
            end = Offset(cx, h * 0.40f),
            strokeWidth = 9.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Left lung lobe
        val leftLung = Path().apply {
            moveTo(cx - 10.dp.toPx(), h * 0.28f)
            cubicTo(cx - 64.dp.toPx(), h * 0.22f, cx - 88.dp.toPx(), h * 0.60f, cx - 72.dp.toPx(), h * 0.86f)
            cubicTo(cx - 50.dp.toPx(), h * 0.94f, cx - 16.dp.toPx(), h * 0.84f, cx - 10.dp.toPx(), h * 0.68f)
            close()
        }

        // Right lung lobe
        val rightLung = Path().apply {
            moveTo(cx + 10.dp.toPx(), h * 0.28f)
            cubicTo(cx + 64.dp.toPx(), h * 0.22f, cx + 88.dp.toPx(), h * 0.60f, cx + 72.dp.toPx(), h * 0.86f)
            cubicTo(cx + 50.dp.toPx(), h * 0.94f, cx + 16.dp.toPx(), h * 0.84f, cx + 10.dp.toPx(), h * 0.68f)
            close()
        }

        drawPath(
            path = leftLung,
            brush = Brush.verticalGradient(listOf(lungSecondary, lungPrimary))
        )
        drawPath(
            path = rightLung,
            brush = Brush.verticalGradient(listOf(lungSecondary, lungPrimary))
        )

        // Bronchial branches inside left & right lungs
        val branchStroke = 3.dp.toPx()
        drawLine(airwayColor, Offset(cx, h * 0.38f), Offset(cx - 34.dp.toPx(), h * 0.54f), branchStroke, StrokeCap.Round)
        drawLine(airwayColor, Offset(cx - 34.dp.toPx(), h * 0.54f), Offset(cx - 52.dp.toPx(), h * 0.44f), branchStroke * 0.8f, StrokeCap.Round)
        drawLine(airwayColor, Offset(cx - 34.dp.toPx(), h * 0.54f), Offset(cx - 48.dp.toPx(), h * 0.72f), branchStroke * 0.8f, StrokeCap.Round)

        drawLine(airwayColor, Offset(cx, h * 0.38f), Offset(cx + 34.dp.toPx(), h * 0.54f), branchStroke, StrokeCap.Round)
        drawLine(airwayColor, Offset(cx + 34.dp.toPx(), h * 0.54f), Offset(cx + 52.dp.toPx(), h * 0.44f), branchStroke * 0.8f, StrokeCap.Round)
        drawLine(airwayColor, Offset(cx + 34.dp.toPx(), h * 0.54f), Offset(cx + 48.dp.toPx(), h * 0.72f), branchStroke * 0.8f, StrokeCap.Round)

        // Clean oxygen sparkles proportional to clarity
        val sparkleAlpha = animatedClarity * 0.85f
        val dots = listOf(
            Offset(cx - 92.dp.toPx(), h * 0.30f) to 4.dp.toPx(),
            Offset(cx + 96.dp.toPx(), h * 0.34f) to 5.dp.toPx(),
            Offset(cx - 86.dp.toPx(), h * 0.74f) to 3.5.dp.toPx(),
            Offset(cx + 88.dp.toPx(), h * 0.72f) to 4.dp.toPx(),
            Offset(cx + 26.dp.toPx(), h * 0.14f) to 3.dp.toPx()
        )
        dots.forEach { (pos, r) ->
            drawCircle(color = VibrantEmerald.copy(alpha = sparkleAlpha), radius = r, center = pos)
        }
    }
}

@Composable
fun CorrelationChartCard(
    journals: List<HealthJournal>,
    modifier: Modifier = Modifier
) {
    val chartData = remember(journals) { journals.take(10).reversed() }
    var selectedIdx by remember(chartData) {
        mutableIntStateOf((chartData.size - 1).coerceAtLeast(0))
    }

    SwasanaCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Korelasi AQI vs Gejala",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Ketuk batang untuk detail harian",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (chartData.isNotEmpty() && selectedIdx in chartData.indices) {
                val sel = chartData[selectedIdx]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ForestEmerald
                ) {
                    Text(
                        text = "${sel.date} • AQI ${sel.aqiSnapshot} • Skor ${sel.totalSeverity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (chartData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada data jurnal untuk ditampilkan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        } else {
            val maxAqi = (chartData.maxOfOrNull { it.aqiSnapshot } ?: 150).coerceAtLeast(150)
            val maxSeverity = (chartData.maxOfOrNull { it.totalSeverity } ?: 8).coerceAtLeast(6)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .pointerInput(chartData) {
                        detectTapGestures { offset ->
                            val step = size.width.toFloat() / chartData.size.coerceAtLeast(1)
                            val idx = (offset.x / step).toInt().coerceIn(0, chartData.lastIndex)
                            selectedIdx = idx
                        }
                    }
            ) {
                val chartHeight = size.height - 16.dp.toPx()
                val slotWidth = size.width / chartData.size.coerceAtLeast(1)
                val barWidth = (slotWidth * 0.44f).coerceAtMost(24.dp.toPx())

                // Subtle horizontal gridlines
                for (i in 0..3) {
                    val y = chartHeight * (i / 3f)
                    drawLine(
                        color = CardBorder,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Rounded vertical AQI bars (Finora analytics style)
                val linePoints = mutableListOf<Offset>()
                chartData.forEachIndexed { idx, entry ->
                    val centerX = slotWidth * idx + slotWidth / 2f
                    val aqiRatio = (entry.aqiSnapshot.toFloat() / maxAqi).coerceIn(0.08f, 1f)
                    val barHeight = chartHeight * aqiRatio
                    val isSelected = idx == selectedIdx
                    val levelColor = AirQualityLevel.fromAqi(entry.aqiSnapshot).color

                    drawRoundRect(
                        color = if (isSelected) ForestEmerald else levelColor.copy(alpha = 0.55f),
                        topLeft = Offset(centerX - barWidth / 2f, chartHeight - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )

                    val sevRatio = (entry.totalSeverity.toFloat() / maxSeverity).coerceIn(0.05f, 1f)
                    val lineY = chartHeight - (chartHeight * sevRatio * 0.88f)
                    linePoints.add(Offset(centerX, lineY))
                }

                // Smooth Symptom Severity trend line overlay
                if (linePoints.size > 1) {
                    val path = Path().apply {
                        moveTo(linePoints.first().x, linePoints.first().y)
                        for (i in 1 until linePoints.size) {
                            val prev = linePoints[i - 1]
                            val curr = linePoints[i]
                            val midX = (prev.x + curr.x) / 2f
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF0284C7),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                linePoints.forEachIndexed { idx, pt ->
                    val radius = if (idx == selectedIdx) 5.5.dp.toPx() else 3.5.dp.toPx()
                    drawCircle(color = PureWhite, radius = radius + 1.5.dp.toPx(), center = pt)
                    drawCircle(color = Color(0xFF0284C7), radius = radius, center = pt)
                }
            }

            // Date labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                chartData.forEachIndexed { idx, item ->
                    Text(
                        text = item.date.take(2),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (idx == selectedIdx) ForestEmerald else TextMuted,
                        fontWeight = if (idx == selectedIdx) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = VibrantEmerald, label = "Batang: Skor AQI")
                LegendDot(color = Color(0xFF0284C7), label = "Garis: Total Keparahan Gejala")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}
