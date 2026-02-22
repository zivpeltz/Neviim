package com.neviim.market.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neviim.market.R
import com.neviim.market.data.model.PricePoint
import com.neviim.market.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

// ── Probability Bar ─────────────────────────────────────────────────

@Composable
fun ProbabilityBar(
    yesProbability: Double,
    modifier: Modifier = Modifier
) {
    val yesPercent = (yesProbability * 100).toInt()
    val noPercent = 100 - yesPercent

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(R.string.yes_label)} ${formatPriceAsCents(yesProbability)}",
                style = MaterialTheme.typography.labelLarge,
                color = YesColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${stringResource(R.string.no_label)} ${formatPriceAsCents(1.0 - yesProbability)}",
                style = MaterialTheme.typography.labelLarge,
                color = NoColor,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            if (yesPercent > 0) {
                Box(
                    modifier = Modifier
                        .weight(yesProbability.toFloat().coerceAtLeast(0.02f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(YesColor, YesColorLight)
                            )
                        )
                )
            }
            if (noPercent > 0) {
                Box(
                    modifier = Modifier
                        .weight((1 - yesProbability).toFloat().coerceAtLeast(0.02f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(NoColorLight, NoColor)
                            )
                        )
                )
            }
        }
    }
}

// ── Line Chart (Canvas-based) ───────────────────────────────────────

@Composable
fun PriceLineChart(
    priceHistory: List<PricePoint>,
    modifier: Modifier = Modifier
) {
    if (priceHistory.size < 2) {
        Box(
            modifier = modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Not enough data points",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        val width = size.width
        val height = size.height
        val padding = 16f

        val prices = priceHistory.map { it.yesPrice }
        val minPrice = (prices.minOrNull() ?: 0.0).coerceAtMost(0.0)
        val maxPrice = (prices.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
        val range = maxPrice - minPrice

        // Grid lines
        for (i in 0..4) {
            val y = padding + (height - 2 * padding) * i / 4
            drawLine(
                color = ChartGrid,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        // Build path
        val path = Path()
        val fillPath = Path()
        val pointCount = prices.size

        prices.forEachIndexed { index, price ->
            val x = padding + (width - 2 * padding) * index / (pointCount - 1)
            val y = padding + (height - 2 * padding) * (1 - (price - minPrice) / range).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height - padding)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Fill area under curve
        fillPath.lineTo(width - padding, height - padding)
        fillPath.close()
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(ChartFill, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw line
        drawPath(
            path = path,
            color = ChartLine,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Draw last point dot
        val lastX = width - padding
        val lastY = padding + (height - 2 * padding) * (1 - (prices.last() - minPrice) / range).toFloat()
        drawCircle(
            color = ChartLine,
            radius = 6f,
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = Color.White,
            radius = 3f,
            center = Offset(lastX, lastY)
        )
    }
}

// ── Stat Card ───────────────────────────────────────────────────────

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Format helpers ──────────────────────────────────────────────────

fun formatSP(amount: Double): String {
    if (amount < 1000) {
        return NumberFormat.getNumberInstance(Locale.US).let {
            it.maximumFractionDigits = 0
            "${it.format(amount)} SP"
        }
    }
    
    val exp = (Math.log10(amount) / 3).toInt()
    val scaled = amount / Math.pow(10.0, exp * 3.0)
    val suffix = "KMGTPE"[exp - 1]
    
    val formatted = String.format(Locale.US, "%.1f", scaled).removeSuffix(".0")
    return "$formatted$suffix SP"
}

fun formatPriceAsCents(value: Double): String {
    var cents = (value * 100).toInt()
    if (cents == 0 && value > 0) cents = 1
    if (cents == 100 && value < 1.0) cents = 99
    return "${cents}¢"
}

fun formatPercent(value: Double): String {
    return "${(value * 100).toInt()}%"
}

fun formatPnL(pnl: Double): String {
    val sign = if (pnl >= 0) "+" else ""
    return "$sign${String.format("%.1f", pnl)} SP"
}
