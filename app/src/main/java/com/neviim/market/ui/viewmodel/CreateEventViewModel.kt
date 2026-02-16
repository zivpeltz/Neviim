package com.neviim.market.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.neviim.market.data.model.EventTag
import com.neviim.market.data.model.EventType
import com.neviim.market.data.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CreateEventViewModel : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _titleHe = MutableStateFlow("")
    val titleHe: StateFlow<String> = _titleHe.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _descriptionHe = MutableStateFlow("")
    val descriptionHe: StateFlow<String> = _descriptionHe.asStateFlow()

    private val _selectedTag = MutableStateFlow(EventTag.POLITICS)
    val selectedTag: StateFlow<EventTag> = _selectedTag.asStateFlow()

    private val _eventType = MutableStateFlow(EventType.BINARY)
    val eventType: StateFlow<EventType> = _eventType.asStateFlow()

    private val _initialProbability = MutableStateFlow(50f)
    val initialProbability: StateFlow<Float> = _initialProbability.asStateFlow()

    // Multi-choice options: list of (english, hebrew) pairs
    private val _options = MutableStateFlow(
        mutableListOf(
            "Option 1" to "",
            "Option 2" to ""
        )
    )
    val options: StateFlow<List<Pair<String, String>>> = _options.asStateFlow()

    // End date (millis since epoch, null = no end date)
    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate: StateFlow<Long?> = _endDate.asStateFlow()

    private val _created = MutableStateFlow(false)
    val created: StateFlow<Boolean> = _created.asStateFlow()

    fun updateTitle(value: String) { _title.value = value }
    fun updateTitleHe(value: String) { _titleHe.value = value }
    fun updateDescription(value: String) { _description.value = value }
    fun updateDescriptionHe(value: String) { _descriptionHe.value = value }
    fun selectTag(tag: EventTag) { _selectedTag.value = tag }
    fun selectEventType(type: EventType) { _eventType.value = type }
    fun updateProbability(value: Float) { _initialProbability.value = value }
    fun updateEndDate(date: Long?) { _endDate.value = date }

    fun updateOption(index: Int, english: String, hebrew: String) {
        val current = _options.value.toMutableList()
        if (index in current.indices) {
            current[index] = english to hebrew
            _options.value = current
        }
    }

    fun addOption() {
        val current = _options.value.toMutableList()
        if (current.size < 8) {
            current.add("Option ${current.size + 1}" to "")
            _options.value = current
        }
    }

    fun removeOption(index: Int) {
        val current = _options.value.toMutableList()
        if (current.size > 2 && index in current.indices) {
            current.removeAt(index)
            _options.value = current
        }
    }

    fun canCreate(): Boolean {
        if (_title.value.isBlank()) return false
        if (_eventType.value == EventType.MULTI_CHOICE) {
            val opts = _options.value
            if (opts.size < 2) return false
            if (opts.any { it.first.isBlank() }) return false
        }
        return true
    }

    fun createEvent() {
        if (!canCreate()) return

        when (_eventType.value) {
            EventType.BINARY -> {
                MarketRepository.createEvent(
                    title = _title.value.trim(),
                    titleHe = _titleHe.value.trim(),
                    tag = _selectedTag.value,
                    initialYesProbability = (_initialProbability.value / 100.0).coerceIn(0.05, 0.95),
                    description = _description.value.trim(),
                    descriptionHe = _descriptionHe.value.trim(),
                    endDate = _endDate.value
                )
            }
            EventType.MULTI_CHOICE -> {
                MarketRepository.createMultiChoiceEvent(
                    title = _title.value.trim(),
                    titleHe = _titleHe.value.trim(),
                    tag = _selectedTag.value,
                    optionLabels = _options.value.map { (en, he) -> en.trim() to he.trim() },
                    description = _description.value.trim(),
                    descriptionHe = _descriptionHe.value.trim(),
                    endDate = _endDate.value
                )
            }
        }
        _created.value = true
    }
}
