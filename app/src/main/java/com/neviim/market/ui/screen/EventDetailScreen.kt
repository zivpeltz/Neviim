package com.neviim.market.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.neviim.market.R
import com.neviim.market.data.model.EventOption
import com.neviim.market.data.model.EventType
import com.neviim.market.data.model.TradeSide
import com.neviim.market.ui.components.*
import com.neviim.market.ui.theme.*
import com.neviim.market.ui.viewmodel.EventDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onBack: () -> Unit,
    viewModel: EventDetailViewModel = viewModel()
) {
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    val event by viewModel.event.collectAsState()
    val selectedSide by viewModel.selectedSide.collectAsState()
    val selectedOptionId by viewModel.selectedOptionId.collectAsState()
    val amountInput by viewModel.amountInput.collectAsState()
    val estimatedReturn by viewModel.estimatedReturn.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val tradeMessage by viewModel.tradeMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val successMsg = stringResource(R.string.trade_successful)
    val insufficientMsg = stringResource(R.string.insufficient_balance)

    LaunchedEffect(tradeMessage) {
        tradeMessage?.let { msg ->
            val displayMsg = if (msg == "success") successMsg else msg
            snackbarHostState.showSnackbar(displayMsg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trade)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val currentEvent = event
        if (currentEvent == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Event Header ──────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    if (currentEvent.image != null) {
                        AsyncImage(
                            model = currentEvent.image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = currentEvent.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Description
                    if (currentEvent.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentEvent.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Probability bar (for binary events)
                    if (currentEvent.eventType == EventType.BINARY) {
                        ProbabilityBar(yesProbability = currentEvent.yesProbability)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // ── Stats Row ─────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EventInfoChip(
                            icon = Icons.Default.TrendingUp,
                            label = stringResource(R.string.volume_label),
                            value = formatSP(currentEvent.totalVolume),
                            modifier = Modifier.weight(1f)
                        )
                        EventInfoChip(
                            icon = Icons.Default.ShowChart,
                            label = stringResource(R.string.liquidity_label),
                            value = formatSP(currentEvent.totalLiquidity),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EventInfoChip(
                            icon = Icons.Default.Group,
                            label = stringResource(R.string.traders_label),
                            value = "${currentEvent.totalTraders}",
                            modifier = Modifier.weight(1f)
                        )

                        if (currentEvent.endDate != null) {
                            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                .format(Date(currentEvent.endDate))
                            val daysLeft = ((currentEvent.endDate - System.currentTimeMillis()) / 86_400_000L)
                                .coerceAtLeast(0)

                            EventInfoChip(
                                icon = Icons.Default.AccessTime,
                                label = stringResource(R.string.ends_label),
                                value = if (daysLeft > 0) "${daysLeft}d" else dateStr,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Options / Pool Breakdown ─────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (currentEvent.eventType == EventType.MULTI_CHOICE)
                            stringResource(R.string.options_label)
                        else stringResource(R.string.pool_breakdown_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    currentEvent.options.forEach { option ->
                        val probability = EventOption.probability(option, currentEvent.options)

                        OptionPoolRow(
                            label = option.label,
                            probability = probability,
                            poolAmount = option.pool,
                            isSelected = if (currentEvent.eventType == EventType.MULTI_CHOICE)
                                selectedOptionId == option.id
                            else (option.id == "yes" && selectedSide == TradeSide.YES) ||
                                    (option.id == "no" && selectedSide == TradeSide.NO),
                            onClick = {
                                if (currentEvent.eventType == EventType.MULTI_CHOICE) {
                                    viewModel.selectOption(option.id)
                                } else {
                                    viewModel.selectSide(
                                        if (option.id == "yes") TradeSide.YES else TradeSide.NO
                                    )
                                }
                            }
                        )

                        if (option != currentEvent.options.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Price Chart ───────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.probability_history),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PriceLineChart(
                        priceHistory = currentEvent.priceHistory,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Trade Panel ───────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .animateContentSize()
                ) {
                    Text(
                        text = stringResource(R.string.trade),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // For binary events: Yes/No toggle
                    if (currentEvent.eventType == EventType.BINARY) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.selectSide(TradeSide.YES) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedSide == TradeSide.YES)
                                        YesColor else YesColor.copy(alpha = 0.2f),
                                    contentColor = if (selectedSide == TradeSide.YES)
                                        MaterialTheme.colorScheme.onPrimary
                                    else YesColor
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.buy_yes),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { viewModel.selectSide(TradeSide.NO) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedSide == TradeSide.NO)
                                        NoColor else NoColor.copy(alpha = 0.2f),
                                    contentColor = if (selectedSide == TradeSide.NO)
                                        MaterialTheme.colorScheme.onPrimary
                                    else NoColor
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.buy_no),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // For multi-choice: show selected option
                    if (currentEvent.eventType == EventType.MULTI_CHOICE) {
                        val selectedOption = currentEvent.options.find { it.id == selectedOptionId }
                        if (selectedOption != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.selected_option),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = selectedOption.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Amount input
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { viewModel.updateAmount(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.amount_sp)) },
                        placeholder = { Text(stringResource(R.string.enter_amount)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        supportingText = {
                            Text(
                                text = "${stringResource(R.string.balance)}: ${formatSP(balance)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Estimated return
                    if (estimatedReturn > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.estimated_return),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatSP(estimatedReturn),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = GreenProfit,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Place trade button
                    val canTrade = amountInput.isNotEmpty() &&
                            (amountInput.toDoubleOrNull() ?: 0.0) > 0 &&
                            (currentEvent.eventType != EventType.MULTI_CHOICE || selectedOptionId != null)

                    Button(
                        onClick = { viewModel.placeTrade() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = canTrade,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentEvent.eventType == EventType.BINARY) {
                                if (selectedSide == TradeSide.YES) YesColor else NoColor
                            } else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.place_trade),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Reusable Components ──────────────────────────────────────────────

@Composable
private fun EventInfoChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun OptionPoolRow(
    label: String,
    probability: Double,
    poolAmount: Double,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val percent = (probability * 100).toInt()
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Option label + probability
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatSP(poolAmount)} ${stringResource(R.string.in_pool_label)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Probability badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$percent%",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
