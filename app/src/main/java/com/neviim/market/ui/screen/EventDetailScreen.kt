package com.neviim.market.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.neviim.market.data.model.EventOption
import com.neviim.market.data.model.EventType
import com.neviim.market.data.model.PricePoint
import com.neviim.market.data.model.TradeSide
import com.neviim.market.ui.components.*
import com.neviim.market.ui.theme.*
import com.neviim.market.ui.viewmodel.EventDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventDetailViewModel = viewModel()
) {
    LaunchedEffect(eventId) { viewModel.loadEvent(eventId) }

    val event by viewModel.event.collectAsState()
    val selectedSide by viewModel.selectedSide.collectAsState()
    val selectedOptionId by viewModel.selectedOptionId.collectAsState()
    val amountInput by viewModel.amountInput.collectAsState()
    val estimatedReturn by viewModel.estimatedReturn.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val tradeMessage by viewModel.tradeMessage.collectAsState()
    val priceHistory by viewModel.priceHistory.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tradeMessage) {
        tradeMessage?.let { msg ->
            snackbarHostState.showSnackbar(if (msg == "success") "Trade placed! 🎉" else msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        val currentEvent = event
        if (currentEvent == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        // Display tag: prefer the raw API label (e.g. "Soccer", "US Elections"),
        // fall back to the enum's display name
        val displayTag = currentEvent.tagLabel
            .ifBlank { currentEvent.tag.displayName }
            .uppercase()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Banner image with back button overlay ──────────────
            Box {
                if (currentEvent.image != null) {
                    AsyncImage(
                        model = currentEvent.image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                    // Gradient fade
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            // ── Scrollable body ────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Category badge — dynamic from API
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = displayTag,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = currentEvent.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 30.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Section 1: Current Odds ────────────────────────
                if (currentEvent.eventType == EventType.BINARY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatPriceAsCents(currentEvent.yesProbability),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = YesColor
                        )
                        Text(
                            text = "chance",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Split bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(NoColor.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(currentEvent.yesProbability.toFloat().coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(YesColor, YesColor.copy(alpha = 0.75f))))
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Yes ${formatPriceAsCents(currentEvent.yesProbability)}", style = MaterialTheme.typography.labelSmall, color = YesColor, fontWeight = FontWeight.SemiBold)
                        Text("No ${formatPriceAsCents(currentEvent.noProbability)}", style = MaterialTheme.typography.labelSmall, color = NoColor, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // Multi-choice outcomes
                    Text("Outcomes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    val sortedOptions = currentEvent.options.sortedByDescending { EventOption.probability(it, currentEvent.options) }
                    sortedOptions.forEach { option ->
                        val prob = EventOption.probability(option, currentEvent.options)
                        val isSelected = selectedOptionId == option.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectOption(option.id) },
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f)) {
                                    Column {
                                        Text(option.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(3.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(prob.toFloat().coerceIn(0f, 1f))
                                                    .fillMaxHeight()
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(0.4f))
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(formatPriceAsCents(prob), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // ── Section 2: Price History Chart ─────────────────
                if (priceHistory.size >= 2) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Price History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(12.dp))
                    PriceHistoryChart(
                        points = priceHistory,
                        lineColor = when {
                            priceHistory.last().yesPrice >= priceHistory.first().yesPrice -> YesColor
                            else -> NoColor
                        },
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                    // Min / max labels
                    val minP = priceHistory.minOf { it.yesPrice }
                    val maxP = priceHistory.maxOf { it.yesPrice }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatPriceAsCents(minP), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatPriceAsCents(maxP), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(20.dp))

                // ── Section 3: Trade ───────────────────────────────
                Text("Place a trade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(12.dp))

                if (currentEvent.eventType == EventType.BINARY) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(TradeSide.YES, TradeSide.NO).forEach { side ->
                            val isSel = selectedSide == side
                            val color = if (side == TradeSide.YES) YesColor else NoColor
                            Button(
                                onClick = { viewModel.selectSide(side) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) color else color.copy(alpha = 0.12f),
                                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else color
                                )
                            ) {
                                Text(if (side == TradeSide.YES) "Buy Yes" else "Buy No", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { viewModel.updateAmount(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount (SP)") },
                    placeholder = { Text("Enter amount…") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    supportingText = { Text("Balance: ${formatSP(balance)}", style = MaterialTheme.typography.labelSmall) }
                )

                if (estimatedReturn > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Potential return", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatSP(estimatedReturn), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GreenProfit)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val canTrade = amountInput.isNotEmpty() &&
                        (amountInput.toDoubleOrNull() ?: 0.0) > 0 &&
                        (currentEvent.eventType != EventType.MULTI_CHOICE || selectedOptionId != null)

                val tradeButtonColor = if (currentEvent.eventType == EventType.BINARY) {
                    if (selectedSide == TradeSide.YES) YesColor else NoColor
                } else MaterialTheme.colorScheme.primary

                Button(
                    onClick = { viewModel.placeTrade() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = canTrade,
                    colors = ButtonDefaults.buttonColors(containerColor = tradeButtonColor)
                ) {
                    Text("Confirm Trade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(20.dp))

                // ── Section 4: Stats ───────────────────────────────
                Text("Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatItem("Volume", formatSP(currentEvent.totalVolume), modifier = Modifier.weight(1f))
                    StatItem("Liquidity", formatSP(currentEvent.totalLiquidity), modifier = Modifier.weight(1f))
                    if (currentEvent.totalTraders > 0) {
                        StatItem("Traders", currentEvent.totalTraders.toString(), modifier = Modifier.weight(1f))
                    }
                }

                // ── Section 5: About ───────────────────────────────
                if (currentEvent.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("About this market", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(currentEvent.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                }

                // ── Section 6: Resolution Source ───────────────────
                if (!currentEvent.resolutionSource.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Resolution Source", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(6.dp))
                    val uriHandler = LocalUriHandler.current
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .clickable { try { uriHandler.openUri(currentEvent.resolutionSource!!) } catch (_: Exception) {} }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(currentEvent.resolutionSource!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 2, lineHeight = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// ── Price History Chart ────────────────────────────────────────────

@Composable
fun PriceHistoryChart(
    points: List<PricePoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val fillColor = lineColor.copy(alpha = 0.12f)

    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val minT = points.minOf { it.timestamp }.toFloat()
        val maxT = points.maxOf { it.timestamp }.toFloat()
        val minP = points.minOf { it.yesPrice }.toFloat().coerceAtMost(0.9f)
        val maxP = points.maxOf { it.yesPrice }.toFloat().coerceAtLeast(0.1f)
        val rangeT = (maxT - minT).coerceAtLeast(1f)
        val rangeP = (maxP - minP).coerceAtLeast(0.01f)

        val w = size.width
        val h = size.height

        fun xOf(t: Long) = (t.toFloat() - minT) / rangeT * w
        fun yOf(p: Double) = h - ((p.toFloat() - minP) / rangeP * h).coerceIn(0f, h)

        // Build line path
        val linePath = Path()
        points.forEachIndexed { i, pt ->
            val x = xOf(pt.timestamp)
            val y = yOf(pt.yesPrice)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        // Fill path (closed)
        val fillPath = Path().also { it.addPath(linePath) }
        fillPath.lineTo(xOf(points.last().timestamp), h)
        fillPath.lineTo(xOf(points.first().timestamp), h)
        fillPath.close()

        // Draw fill
        drawPath(fillPath, brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)))

        // Draw line
        drawPath(
            linePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw last price dot
        val lastX = xOf(points.last().timestamp)
        val lastY = yOf(points.last().yesPrice)
        drawCircle(color = lineColor, radius = 5.dp.toPx(), center = Offset(lastX, lastY))
        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(lastX, lastY))
    }
}

// ── Helper components ──────────────────────────────────────────────

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
