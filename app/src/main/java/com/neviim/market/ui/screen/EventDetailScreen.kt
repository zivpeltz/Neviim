package com.neviim.market.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neviim.market.R
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
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    val event by viewModel.event.collectAsState()
    val selectedSide by viewModel.selectedSide.collectAsState()
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
                    Text(
                        text = currentEvent.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Probability bar
                    ProbabilityBar(yesProbability = currentEvent.yesProbability)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${stringResource(R.string.volume_label)}: ${formatSP(currentEvent.totalVolume)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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

                    // Side toggle
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

                    Spacer(modifier = Modifier.height(16.dp))

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
                    Button(
                        onClick = { viewModel.placeTrade() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = amountInput.isNotEmpty() &&
                                (amountInput.toDoubleOrNull() ?: 0.0) > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSide == TradeSide.YES) YesColor else NoColor
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
