package com.neviim.market.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neviim.market.data.model.UserPosition
import com.neviim.market.data.repository.MarketRepository
import kotlinx.coroutines.flow.*

data class PositionWithPnL(
    val position: UserPosition,
    val currentPrice: Double,
    val pnl: Double,
    val pnlPercent: Double
)

class PortfolioViewModel : ViewModel() {

    private val _showResolved = MutableStateFlow(false)
    val showResolved: StateFlow<Boolean> = _showResolved.asStateFlow()

    /** Active (open) positions with live P&L based on current market prices. */
    val activePositions: StateFlow<List<PositionWithPnL>> = combine(
        MarketRepository.positions,
        MarketRepository.events
    ) { positions, _ ->
        positions.map { pos ->
            val currentPrice = MarketRepository.getCurrentPriceForPosition(pos)
            val currentValue = pos.shares * currentPrice
            val pnl = currentValue - pos.amountPaid
            val pnlPercent = if (pos.amountPaid > 0) (pnl / pos.amountPaid) * 100.0 else 0.0
            PositionWithPnL(pos, currentPrice, pnl, pnlPercent)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Resolved (settled) positions read directly from MarketRepository.
     * The `won` and `resolvedAt` fields on the position are authoritative.
     */
    val resolvedPositions: StateFlow<List<PositionWithPnL>> =
        MarketRepository.resolvedPositions.map { positions ->
            positions
                .sortedByDescending { it.resolvedAt ?: it.timestamp }
                .map { pos ->
                    val finalPrice = if (pos.won == true) 1.0 else 0.0
                    val pnl = (pos.shares * finalPrice) - pos.amountPaid
                    val pnlPercent = if (pos.amountPaid > 0) (pnl / pos.amountPaid) * 100.0 else 0.0
                    PositionWithPnL(pos, finalPrice, pnl, pnlPercent)
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTab(showResolved: Boolean) {
        _showResolved.value = showResolved
    }
}
