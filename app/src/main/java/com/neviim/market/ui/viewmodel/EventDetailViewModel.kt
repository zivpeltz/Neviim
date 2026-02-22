package com.neviim.market.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neviim.market.data.model.Event
import com.neviim.market.data.model.EventOption
import com.neviim.market.data.model.EventType
import com.neviim.market.data.model.TradeSide
import com.neviim.market.data.repository.MarketRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EventDetailViewModel : ViewModel() {

    private val _selectedEventId = MutableStateFlow<String?>(null)

    val event: StateFlow<Event?> = combine(
        _selectedEventId,
        MarketRepository.events
    ) { id, events ->
        if (id != null) events.find { it.id == id } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Binary trade side
    private val _selectedSide = MutableStateFlow(TradeSide.YES)
    val selectedSide: StateFlow<TradeSide> = _selectedSide.asStateFlow()

    // Multi-choice selected option
    private val _selectedOptionId = MutableStateFlow<String?>(null)
    val selectedOptionId: StateFlow<String?> = _selectedOptionId.asStateFlow()

    private val _amountInput = MutableStateFlow("")
    val amountInput: StateFlow<String> = _amountInput.asStateFlow()

    private val _tradeMessage = MutableStateFlow<String?>(null)
    val tradeMessage: StateFlow<String?> = _tradeMessage.asStateFlow()

    val balance: StateFlow<Double> = MarketRepository.userProfile
        .map { it.balance }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val estimatedReturn: StateFlow<Double> = combine(
        event,
        _selectedSide,
        _selectedOptionId,
        _amountInput
    ) { evt, side, optionId, amtStr ->
        val amount = amtStr.toDoubleOrNull() ?: 0.0
        if (evt != null && amount > 0) {
            when (evt.eventType) {
                EventType.BINARY -> MarketRepository.estimateReturn(evt, side, amount)
                EventType.MULTI_CHOICE -> {
                    val oid = optionId ?: return@combine 0.0
                    MarketRepository.estimateOptionReturn(evt, oid, amount)
                }
            }
        } else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun loadEvent(eventId: String) {
        _selectedEventId.value = eventId
        // Auto-select first option for multi-choice events
        val evt = MarketRepository.getEvent(eventId)
        if (evt?.eventType == EventType.MULTI_CHOICE && evt.options.isNotEmpty()) {
            _selectedOptionId.value = evt.options.first().id
        }
    }

    fun selectSide(side: TradeSide) {
        _selectedSide.value = side
    }

    fun selectOption(optionId: String) {
        _selectedOptionId.value = optionId
    }

    fun updateAmount(amount: String) {
        // Allow only digits and one decimal point
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _amountInput.value = amount
        }
    }

    fun placeTrade() {
        val eventId = _selectedEventId.value ?: return
        val amount = _amountInput.value.toDoubleOrNull() ?: return
        if (amount <= 0) return

        val evt = MarketRepository.getEvent(eventId) ?: return

        val result = when (evt.eventType) {
            EventType.BINARY -> MarketRepository.placeTrade(eventId, _selectedSide.value, amount)
            EventType.MULTI_CHOICE -> {
                val optionId = _selectedOptionId.value ?: return
                MarketRepository.placeOptionTrade(eventId, optionId, amount)
            }
        }

        result.fold(
            onSuccess = {
                _tradeMessage.value = "success"
                _amountInput.value = ""
            },
            onFailure = { e ->
                _tradeMessage.value = e.message
            }
        )
    }

    fun clearMessage() {
        _tradeMessage.value = null
    }
}
