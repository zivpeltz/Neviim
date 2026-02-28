package com.neviim.market.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.neviim.market.data.model.Event
import com.neviim.market.data.model.EventOption
import com.neviim.market.data.model.EventType
import com.neviim.market.ui.components.formatPriceAsCents
import com.neviim.market.ui.components.formatSP
import com.neviim.market.ui.theme.*
import com.neviim.market.ui.viewmodel.ExploreViewModel
import com.neviim.market.ui.viewmodel.SortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onEventClick: (eventId: String, side: String?) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val events by viewModel.filteredEvents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val lastRefreshed by viewModel.lastRefreshed.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()

    // Tick every second so the "Updated X ago" label stays accurate
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); now = System.currentTimeMillis() }
    }

    val updatedLabel = when {
        lastRefreshed == 0L -> "Updating…"
        now - lastRefreshed < 5_000 -> "Updated just now"
        now - lastRefreshed < 60_000 -> "Updated ${(now - lastRefreshed) / 1000}s ago"
        else -> "Updated ${(now - lastRefreshed) / 60_000}m ago"
    }

    JewishThemedBackground(
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ── Top Bar ───────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Neviim",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Live update indicator
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            modifier = Modifier.size(7.dp),
                            shape = RoundedCornerShape(50),
                            color = if (lastRefreshed > 0) YesColor else MaterialTheme.colorScheme.outline
                        ) {}
                        Text(
                            text = updatedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearch(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search markets…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ── Dynamic category chips ─────────────────────────
                // Only shows tags that have ≥1 event — never empty chips.
                if (availableTags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // "All" chip
                        item {
                            val isAll = selectedTag == null
                            FilterChip(
                                selected = isAll,
                                onClick = { viewModel.selectTag(null) },
                                label = { Text("All", fontWeight = if (isAll) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        items(availableTags) { tag ->
                            val isSelected = selectedTag == tag
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectTag(tag) },
                                label = {
                                    Text(
                                        text = tag,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // ── Sort row ───────────────────────────────────────
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SortMode.entries) { mode ->
                        val isSelected = sortMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectSort(mode) },
                            leadingIcon = {
                                Text(mode.emoji, fontSize = 13.sp)
                            },
                            label = {
                                Text(
                                    text = mode.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }
            }
        }

        // ── Event Feed ─────────────────────────────────────────────
        if (events.isEmpty() && lastRefreshed == 0L) {
            // First load — show spinner
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    EventFeedCard(
                        event = event,
                        onClick = { onEventClick(event.id, null) },
                        onBuyClick = { side -> onEventClick(event.id, side) }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )
                }
            }
        }
    } // Column
    } // JewishThemedBackground
}

@Composable
private fun EventFeedCard(event: Event, onClick: () -> Unit, onBuyClick: (side: String) -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Top row: image + title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (event.image != null) {
                AsyncImage(
                    model = event.image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.tagLabel.ifBlank { event.tag.displayName }.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Odds display
        if (event.eventType == EventType.BINARY) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatPriceAsCents(event.yesProbability),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = YesColor
                        )
                        Text(text = "chance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                            .clip(RoundedCornerShape(2.dp)).background(NoColor.copy(alpha = 0.25f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(event.yesProbability.toFloat().coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(YesColor, YesColor.copy(alpha = 0.7f))))
                        )
                    }
                }
                FeedBuyButton("Yes", formatPriceAsCents(event.yesProbability), YesColor, onClick = { onBuyClick("YES") })
                FeedBuyButton("No", formatPriceAsCents(event.noProbability), NoColor, onClick = { onBuyClick("NO") })
            }
        } else {
            val sorted = event.options.sortedByDescending { EventOption.probability(it, event.options) }
            sorted.take(3).forEach { option ->
                val p = EventOption.probability(option, event.options)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(option.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(modifier = Modifier.width(60.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.fillMaxWidth(p.toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                    }
                    Text(formatPriceAsCents(p), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (sorted.size > 3) {
                Text("+${sorted.size - 3} more", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("${formatSP(event.totalVolume)} Vol.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FeedBuyButton(
    label: String,
    price: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
            Text(text = price, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}
