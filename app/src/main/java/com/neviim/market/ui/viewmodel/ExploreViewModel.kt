package com.neviim.market.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neviim.market.data.model.Event
import com.neviim.market.data.repository.MarketRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortMode(val label: String, val emoji: String) {
    HOT       ("Trending",    "🔥"),  // highest 24h volume (API default)
    ENDING    ("Ending Soon", "⏰"),  // nearest end date
    NEWEST    ("Newest",      "🆕"),  // most recently created
    CLOSE_CALL("Close Call",  "🎯"),  // probability closest to 50%
    BIG_VOL   ("High Volume", "📈"),  // all-time total volume
}

class ExploreViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Tag label string (e.g. "Soccer", "US Elections") or null for All. */
    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.HOT)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    /** Deduplicated, sorted tag labels derived from whatever events are currently loaded.
     *  Only tags that actually have ≥1 event are included — no empty chips. */
    val availableTags: StateFlow<List<String>> = MarketRepository.events
        .map { events ->
            events
                .mapNotNull { it.tagLabel.ifBlank { null } }
                .toSortedSet()
                .toList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredEvents: StateFlow<List<Event>> = combine(
        MarketRepository.events,
        _searchQuery,
        _selectedTag,
        _sortMode
    ) { events, query, tag, sort ->
        val now = System.currentTimeMillis()

        events
            .filter { event ->
                val matchesSearch = query.isBlank() ||
                    event.title.contains(query, ignoreCase = true) ||
                    event.titleHe.contains(query, ignoreCase = true)
                val matchesTag = tag == null || event.tagLabel.equals(tag, ignoreCase = true)
                matchesSearch && matchesTag && !event.isResolved
            }
            .let { filtered ->
                when (sort) {
                    SortMode.HOT ->
                        filtered.sortedByDescending { it.totalVolume }
                    SortMode.ENDING ->
                        // Soonest end date first; events with no endDate go last
                        filtered.sortedBy { event ->
                            val end = event.endDate
                            if (end != null && end > now) end else Long.MAX_VALUE
                        }
                    SortMode.NEWEST ->
                        filtered.sortedByDescending { it.createdAt }
                    SortMode.CLOSE_CALL ->
                        // Events whose top probability is closest to 50¢ are most uncertain / interesting
                        filtered.sortedBy { event ->
                            val topProb = event.options.maxOfOrNull { opt ->
                                com.neviim.market.data.model.EventOption.probability(opt, event.options)
                            } ?: event.yesProbability
                            Math.abs(topProb - 0.5)
                        }
                    SortMode.BIG_VOL ->
                        filtered.sortedByDescending { it.totalVolume + it.totalLiquidity }
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Epoch millis of last successful fetch — 0 means never loaded yet. */
    val lastRefreshed: StateFlow<Long> = MarketRepository.lastRefreshed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun updateSearch(query: String) { _searchQuery.value = query }

    fun selectTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun selectSort(mode: SortMode) { _sortMode.value = mode }

    fun refresh() {
        viewModelScope.launch { MarketRepository.refreshEvents() }
    }
}
