package com.neviim.market.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.neviim.market.data.model.EventTag
import com.neviim.market.data.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CreateEventViewModel : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _titleHe = MutableStateFlow("")
    val titleHe: StateFlow<String> = _titleHe.asStateFlow()

    private val _selectedTag = MutableStateFlow(EventTag.POLITICS)
    val selectedTag: StateFlow<EventTag> = _selectedTag.asStateFlow()

    private val _initialProbability = MutableStateFlow(50f)
    val initialProbability: StateFlow<Float> = _initialProbability.asStateFlow()

    private val _created = MutableStateFlow(false)
    val created: StateFlow<Boolean> = _created.asStateFlow()

    fun updateTitle(value: String) { _title.value = value }
    fun updateTitleHe(value: String) { _titleHe.value = value }
    fun selectTag(tag: EventTag) { _selectedTag.value = tag }
    fun updateProbability(value: Float) { _initialProbability.value = value }

    fun canCreate(): Boolean = _title.value.isNotBlank()

    fun createEvent() {
        if (!canCreate()) return
        MarketRepository.createEvent(
            title = _title.value.trim(),
            titleHe = _titleHe.value.trim(),
            tag = _selectedTag.value,
            initialYesProbability = (_initialProbability.value / 100.0).coerceIn(0.05, 0.95)
        )
        _created.value = true
    }
}
