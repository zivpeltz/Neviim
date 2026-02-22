package com.neviim.market.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.neviim.market.data.model.EventOption
import com.neviim.market.data.model.EventType
import com.neviim.market.data.model.PricePoint
import com.neviim.market.data.model.TradeSide
import com.neviim.market.ui.components.formatPriceAsCents
import com.neviim.market.ui.components.formatSP
import com.neviim.market.ui.theme.*
import com.neviim.market.ui.viewmodel.EventDetailViewModel
import kotlin.math.cos
import kotlin.math.sin

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
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsState()

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

        val displayTag = currentEvent.tagLabel.ifBlank { currentEvent.tag.displayName }.uppercase()

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
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp).background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                    )
                }
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(12.dp))
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
                // Category badge
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
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
                Text(text = currentEvent.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, lineHeight = 30.sp)
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
                        Text(text = "chance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(NoColor.copy(alpha = 0.25f))) {
                        Box(modifier = Modifier.fillMaxWidth(currentEvent.yesProbability.toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(YesColor, YesColor.copy(alpha = 0.75f)))))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Yes ${formatPriceAsCents(currentEvent.yesProbability)}", style = MaterialTheme.typography.labelSmall, color = YesColor, fontWeight = FontWeight.SemiBold)
                        Text("No ${formatPriceAsCents(currentEvent.noProbability)}", style = MaterialTheme.typography.labelSmall, color = NoColor, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // ── Multi-choice: Dropdown selector ───────────────
                    val sortedOptions = currentEvent.options
                        .sortedByDescending { EventOption.probability(it, currentEvent.options) }
                    val selectedOption = sortedOptions.find { it.id == selectedOptionId } ?: sortedOptions.firstOrNull()
                    var expanded by remember { mutableStateOf(false) }

                    Text("Select Outcome", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedOption?.let {
                                "${it.label}  ·  ${formatPriceAsCents(EventOption.probability(it, currentEvent.options))}"
                            } ?: "Choose an outcome",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            },
                            shape = RoundedCornerShape(12.dp),
                            label = { Text("Outcome", style = MaterialTheme.typography.labelSmall) }
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            sortedOptions.forEach { option ->
                                val prob = EventOption.probability(option, currentEvent.options)
                                val isSelected = selectedOptionId == option.id
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = option.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = formatPriceAsCents(prob),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectOption(option.id)
                                        expanded = false
                                    },
                                    modifier = Modifier.background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
                                    )
                                )
                            }
                        }
                    }

                    // Show probability bar for selected option
                    selectedOption?.let { opt ->
                        val prob = EventOption.probability(opt, currentEvent.options)
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Box(modifier = Modifier.fillMaxWidth(prob.toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(0.7f)))))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${formatPriceAsCents(prob)} implied probability", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // ── Section 2: Price History Chart ─────────────────
                // Always rendered — shows spinner while loading, chart when ready,
                // and a subtle "no data" state if the API returned nothing.
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Price History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("7d", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoadingHistory -> {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                        }
                        priceHistory.size >= 2 -> {
                            val chartColor = if (priceHistory.last().yesPrice >= priceHistory.first().yesPrice) YesColor else NoColor
                            PriceHistoryChart(
                                points = priceHistory,
                                lineColor = chartColor,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📊", fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("No chart data available", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (priceHistory.size >= 2) {
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
                                    contentColor = if (isSel) Color.White else color
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
                            .clickable(onClick = { try { uriHandler.openUri(currentEvent.resolutionSource!!) } catch (_: Exception) {} })
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

// ── Price History Chart (Canvas) ─────────────────────────────────────

@Composable
fun PriceHistoryChart(
    points: List<PricePoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val fillColor = lineColor.copy(alpha = 0.15f)
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val minT = points.minOf { it.timestamp }.toFloat()
        val maxT = points.maxOf { it.timestamp }.toFloat()
        val minP = (points.minOf { it.yesPrice } - 0.02).coerceAtLeast(0.0).toFloat()
        val maxP = (points.maxOf { it.yesPrice } + 0.02).coerceAtMost(1.0).toFloat()
        val rangeT = (maxT - minT).coerceAtLeast(1f)
        val rangeP = (maxP - minP).coerceAtLeast(0.01f)
        val w = size.width; val h = size.height

        fun xOf(t: Long) = (t.toFloat() - minT) / rangeT * w
        fun yOf(p: Double) = h - ((p.toFloat() - minP) / rangeP * h).coerceIn(0f, h)

        val linePath = Path()
        points.forEachIndexed { i, pt ->
            val x = xOf(pt.timestamp); val y = yOf(pt.yesPrice)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        val fillPath = Path().also { it.addPath(linePath) }
        fillPath.lineTo(xOf(points.last().timestamp), h)
        fillPath.lineTo(xOf(points.first().timestamp), h)
        fillPath.close()

        drawPath(fillPath, brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)))
        drawPath(linePath, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        val lastX = xOf(points.last().timestamp); val lastY = yOf(points.last().yesPrice)
        drawCircle(color = lineColor, radius = 5.dp.toPx(), center = Offset(lastX, lastY))
        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(lastX, lastY))
    }
}

// ── Helper composables ────────────────────────────────────────────────

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
