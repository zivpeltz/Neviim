package com.neviim.market.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neviim.market.data.model.UserPosition
import com.neviim.market.data.model.UserProfile
import com.neviim.market.data.repository.MarketRepository
import kotlinx.coroutines.flow.*

class AccountViewModel : ViewModel() {

    val userProfile: StateFlow<UserProfile> = MarketRepository.userProfile

    /** Live open positions with current market prices — same source as PortfolioScreen. */
    val activePositions: StateFlow<List<PositionWithPnL>> = combine(
        MarketRepository.positions,
        MarketRepository.events
    ) { positions, _ ->
        positions.map { pos ->
            val currentPrice = MarketRepository.getCurrentPriceForPosition(pos)
            val pnl = (pos.shares * currentPrice) - pos.amountPaid
            val pnlPct = if (pos.amountPaid > 0) (pnl / pos.amountPaid) * 100.0 else 0.0
            PositionWithPnL(pos, currentPrice, pnl, pnlPct)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Settled positions — same source as PortfolioScreen's resolved tab. */
    val resolvedPositions: StateFlow<List<PositionWithPnL>> =
        MarketRepository.resolvedPositions.map { positions ->
            positions
                .sortedByDescending { it.resolvedAt ?: it.timestamp }
                .map { pos ->
                    val finalPrice = if (pos.won == true) 1.0 else 0.0
                    val pnl = (pos.shares * finalPrice) - pos.amountPaid
                    val pnlPct = if (pos.amountPaid > 0) (pnl / pos.amountPaid) * 100.0 else 0.0
                    PositionWithPnL(pos, finalPrice, pnl, pnlPct)
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
