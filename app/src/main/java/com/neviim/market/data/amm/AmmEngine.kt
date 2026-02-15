package com.neviim.market.data.amm

import com.neviim.market.data.model.Event
import com.neviim.market.data.model.TradeSide

/**
 * Simplified Automated Market Maker engine using a pool-ratio pricing model.
 *
 * Price(Yes) = NoPool / (YesPool + NoPool)
 * Price(No)  = YesPool / (YesPool + NoPool)
 *
 * When a user buys Yes shares:
 *   - They pay `amount` SP, which is added to the Yes pool
 *   - Shares received = amount / price_at_execution
 *   - If the event resolves Yes, each share pays 1 SP
 *
 * This creates natural price movement: buying Yes raises Yes price.
 */
object AmmEngine {

    data class TradeResult(
        val updatedEvent: Event,
        val sharesReceived: Double,
        val executionPrice: Double,
        val estimatedReturn: Double
    )

    /**
     * Calculate what the user would receive if they trade right now.
     * Does NOT modify state.
     */
    fun estimateReturn(event: Event, side: TradeSide, amount: Double): Double {
        if (amount <= 0) return 0.0

        val price = when (side) {
            TradeSide.YES -> event.yesProbability
            TradeSide.NO -> event.noProbability
        }

        if (price <= 0) return 0.0

        val shares = amount / price
        return shares // Each share pays 1 SP if the outcome is correct
    }

    /**
     * Execute a trade: user buys `amount` worth of shares on `side`.
     * Returns the updated event and trade details.
     */
    fun executeTrade(event: Event, side: TradeSide, amount: Double): TradeResult {
        require(amount > 0) { "Trade amount must be positive" }
        require(!event.isResolved) { "Cannot trade on a resolved event" }

        val currentPrice = when (side) {
            TradeSide.YES -> event.yesProbability
            TradeSide.NO -> event.noProbability
        }

        val shares = amount / currentPrice

        val updatedEvent = when (side) {
            TradeSide.YES -> event.copy(
                yesPool = event.yesPool + amount,
                totalVolume = event.totalVolume + amount
            )
            TradeSide.NO -> event.copy(
                noPool = event.noPool + amount,
                totalVolume = event.totalVolume + amount
            )
        }

        // Add a price history point after the trade
        val newPricePoint = com.neviim.market.data.model.PricePoint(
            timestamp = System.currentTimeMillis(),
            yesPrice = updatedEvent.yesProbability
        )

        val finalEvent = updatedEvent.copy(
            priceHistory = updatedEvent.priceHistory + newPricePoint
        )

        return TradeResult(
            updatedEvent = finalEvent,
            sharesReceived = shares,
            executionPrice = currentPrice,
            estimatedReturn = shares // 1 SP per share if outcome is correct
        )
    }
}
