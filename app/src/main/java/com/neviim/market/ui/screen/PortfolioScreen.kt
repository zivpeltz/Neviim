package com.neviim.market.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neviim.market.R
import com.neviim.market.data.model.TradeSide
import com.neviim.market.ui.components.formatPnL
import com.neviim.market.ui.components.formatPercent
import com.neviim.market.ui.theme.*
import com.neviim.market.ui.viewmodel.PortfolioViewModel
import com.neviim.market.ui.viewmodel.PositionWithPnL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = viewModel()
) {
    val showResolved by viewModel.showResolved.collectAsState()
    val activePositions by viewModel.activePositions.collectAsState()
    val resolvedPositions by viewModel.resolvedPositions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ──────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 0.dp
                )
            ) {
                Text(
                    text = stringResource(R.string.nav_my_bids),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tab row
                TabRow(
                    selectedTabIndex = if (showResolved) 1 else 0,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = !showResolved,
                        onClick = { viewModel.toggleTab(false) },
                        text = {
                            Text(
                                text = stringResource(R.string.active_positions),
                                fontWeight = if (!showResolved) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = showResolved,
                        onClick = { viewModel.toggleTab(true) },
                        text = {
                            Text(
                                text = stringResource(R.string.resolved_positions),
                                fontWeight = if (showResolved) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // ── List ────────────────────────────────────────────────
        val positions = if (showResolved) resolvedPositions else activePositions

        if (positions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(
                        if (showResolved) R.string.no_resolved
                        else R.string.no_positions
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(positions, key = { it.position.id }) { posWithPnL ->
                    PositionCard(posWithPnL)
                }
            }
        }
    }
}

@Composable
private fun PositionCard(data: PositionWithPnL) {
    val pos = data.position
    val isProfitable = data.pnl >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title + side chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pos.eventTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (pos.side == TradeSide.YES) YesColor else NoColor
                ) {
                    Text(
                        text = if (pos.side == TradeSide.YES)
                            stringResource(R.string.yes_label)
                        else stringResource(R.string.no_label),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(
                    label = stringResource(R.string.entry_price),
                    value = formatPercent(pos.entryPrice)
                )
                StatColumn(
                    label = stringResource(R.string.current_price),
                    value = formatPercent(data.currentPrice)
                )
                StatColumn(
                    label = stringResource(R.string.shares_label),
                    value = String.format("%.1f", pos.shares)
                )
                StatColumn(
                    label = stringResource(R.string.pnl),
                    value = formatPnL(data.pnl),
                    valueColor = if (isProfitable) GreenProfit else RedLoss
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
