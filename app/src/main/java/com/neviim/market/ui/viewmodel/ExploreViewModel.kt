package com.neviim.market.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neviim.market.data.model.Event
import com.neviim.market.data.model.EventTag
import com.neviim.market.data.repository.MarketRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExploreViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<EventTag?>(null)
    val selectedTag: StateFlow<EventTag?> = _selectedTag.asStateFlow()

    val filteredEvents: StateFlow<List<Event>> = combine(
        MarketRepository.events,
        _searchQuery,
        _selectedTag
    ) { events, query, tag ->
        events.filter { event ->
            val matchesSearch = query.isBlank() ||
                event.title.contains(query, ignoreCase = true) ||
                event.titleHe.contains(query, ignoreCase = true)
            val matchesTag = tag == null || event.tag == tag
            matchesSearch && matchesTag && !event.isResolved
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun selectTag(tag: EventTag?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }
}
