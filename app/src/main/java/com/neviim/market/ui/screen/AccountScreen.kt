package com.neviim.market.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neviim.market.data.model.TradeSide
import com.neviim.market.data.repository.MarketRepository
import com.neviim.market.ui.components.formatPnL
import com.neviim.market.ui.components.formatPercent
import com.neviim.market.ui.components.formatSP
import com.neviim.market.ui.theme.*
import com.neviim.market.ui.viewmodel.AccountViewModel
import com.neviim.market.ui.viewmodel.PositionWithPnL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onSettingsClick: () -> Unit = {},
    viewModel: AccountViewModel = viewModel()
) {
    val profile by viewModel.userProfile.collectAsState()
    val activePositions by viewModel.activePositions.collectAsState()
    val resolvedPositions by viewModel.resolvedPositions.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    // Derived stats — use profile values which are authoritative
    val pnlPositive = profile.totalWinnings >= 0

    // Initials avatar
    val initials = profile.username.trim().split(" ")
        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        .ifBlank { "?" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
        ) {
            // ── Hero Header ────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = profile.username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Prophet · Neviim",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── Portfolio Value Card ───────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Portfolio Value",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatSP(profile.balance),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // All-time P&L pill
                        val pnlColor = if (pnlPositive) GreenProfit else RedLoss
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = pnlColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (pnlPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = pnlColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${if (pnlPositive) "+" else ""}${formatSP(profile.totalWinnings)} all-time",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = pnlColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ── Stats Row ─────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatChip(
                        label = "Trades",
                        value = "${profile.totalBets}",
                        modifier = Modifier.weight(1f)
                    )
                    StatChip(
                        label = "Won",
                        value = "${profile.wonBets}",
                        modifier = Modifier.weight(1f),
                        valueColor = if (profile.wonBets > 0) GreenProfit else MaterialTheme.colorScheme.onSurface
                    )
                    StatChip(
                        label = "Win Rate",
                        value = "${String.format("%.0f", profile.winRate)}%",
                        modifier = Modifier.weight(1f),
                        valueColor = when {
                            profile.winRate >= 60 -> GreenProfit
                            profile.winRate >= 40 -> MaterialTheme.colorScheme.onSurface
                            else -> RedLoss
                        }
                    )
                    StatChip(
                        label = "Open",
                        value = "${activePositions.size}",
                        modifier = Modifier.weight(1f),
                        valueColor = if (activePositions.isNotEmpty()) YesColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Positions Header + Tabs ────────────────────────────
            item {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Open (${activePositions.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "History (${resolvedPositions.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Position / History List ────────────────────────────
            val listItems = if (selectedTab == 0) activePositions else resolvedPositions

            if (listItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (selectedTab == 0) "🎯" else "📜",
                                fontSize = 36.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedTab == 0) "No open positions" else "No trade history yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (selectedTab == 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Head to Explore to place your first bet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            } else {
                items(listItems, key = { it.position.id }) { posWithPnL ->
                    TradeHistoryCard(
                        data = posWithPnL,
                        isResolved = selectedTab == 1,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TradeHistoryCard(
    data: PositionWithPnL,
    isResolved: Boolean,
    modifier: Modifier = Modifier
) {
    val pos = data.position
    val isProfitable = data.pnl >= 0
    val pnlColor = if (isProfitable) GreenProfit else RedLoss

    // Side / outcome label
    val sideLabel = when {
        pos.optionLabel.isNotBlank() && pos.optionLabel != pos.side.name -> pos.optionLabel
        pos.side == TradeSide.YES -> "Yes"
        else -> "No"
    }
    val sideColor = when {
        pos.side == TradeSide.YES -> YesColor
        else -> NoColor
    }

    // Date — prefer resolvedAt for settled positions
    val displayTime = if (isResolved) (pos.resolvedAt ?: pos.timestamp) else pos.timestamp
    val dateStr = remember(displayTime) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(displayTime))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: title + side chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = pos.eventTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = sideColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = sideLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = sideColor,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(10.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TradeStatCol(label = "Invested", value = formatSP(pos.amountPaid))
                TradeStatCol(label = "Entry", value = formatPercent(pos.entryPrice))
                TradeStatCol(label = "Current", value = formatPercent(data.currentPrice))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (data.pnl >= 0) "+${formatSP(data.pnl)}" else formatSP(data.pnl),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = pnlColor
                    )
                    Text(
                        text = "P&L",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Date line
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${if (isResolved) "Resolved" else "Opened"} $dateStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun TradeStatCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
