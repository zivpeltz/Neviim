package com.neviim.market.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neviim.market.data.amm.AmmEngine
import com.neviim.market.data.model.Event
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

    private val _selectedSide = MutableStateFlow(TradeSide.YES)
    val selectedSide: StateFlow<TradeSide> = _selectedSide.asStateFlow()

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
        _amountInput
    ) { evt, side, amtStr ->
        val amount = amtStr.toDoubleOrNull() ?: 0.0
        if (evt != null && amount > 0) {
            AmmEngine.estimateReturn(evt, side, amount)
        } else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun loadEvent(eventId: String) {
        _selectedEventId.value = eventId
    }

    fun selectSide(side: TradeSide) {
        _selectedSide.value = side
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

        val result = MarketRepository.placeTrade(eventId, _selectedSide.value, amount)
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
