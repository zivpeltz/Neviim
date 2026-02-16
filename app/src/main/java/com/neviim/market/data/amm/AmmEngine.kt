package com.neviim.market.data.amm

import com.neviim.market.data.model.Event
import com.neviim.market.data.model.EventOption
import com.neviim.market.data.model.TradeSide

/**
 * Simplified Automated Market Maker engine using a pool-ratio pricing model.
 *
 * For binary events:
 *   Price(Yes) = NoPool / (YesPool + NoPool)
 *   Price(No)  = YesPool / (YesPool + NoPool)
 *
 * For multi-choice events:
 *   Each option has a pool; probability is derived via inverse-pool weighting
 *   across all options.
 *
 * When a user buys shares of an option:
 *   - They pay `amount` SP, which is added to that option's pool
 *   - Shares received = amount / price_at_execution
 *   - If the event resolves to that option, each share pays 1 SP
 */
object AmmEngine {

    data class TradeResult(
        val updatedEvent: Event,
        val sharesReceived: Double,
        val executionPrice: Double,
        val estimatedReturn: Double
    )

    // ── Binary convenience methods ─────────────────────────────────

    /**
     * Calculate what the user would receive if they trade right now.
     * Does NOT modify state. (Binary events only.)
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
     * Execute a trade on a binary event.
     * Returns the updated event and trade details.
     */
    fun executeTrade(event: Event, side: TradeSide, amount: Double): TradeResult {
        require(amount > 0) { "Trade amount must be positive" }
        require(!event.isResolved) { "Cannot trade on a resolved event" }

        val optionIndex = when (side) {
            TradeSide.YES -> 0
            TradeSide.NO -> 1
        }

        return executeOptionTrade(event, event.options[optionIndex].id, amount)
    }

    // ── Multi-choice methods ───────────────────────────────────────

    /**
     * Estimate return for a specific option in any event type.
     */
    fun estimateOptionReturn(event: Event, optionId: String, amount: Double): Double {
        if (amount <= 0) return 0.0
        val option = event.options.find { it.id == optionId } ?: return 0.0
        val price = EventOption.probability(option, event.options)
        if (price <= 0) return 0.0
        return amount / price
    }

    /**
     * Execute a trade on a specific option.
     * Works for both binary and multi-choice events.
     */
    fun executeOptionTrade(event: Event, optionId: String, amount: Double): TradeResult {
        require(amount > 0) { "Trade amount must be positive" }
        require(!event.isResolved) { "Cannot trade on a resolved event" }

        val option = event.options.find { it.id == optionId }
            ?: throw IllegalArgumentException("Option not found")

        val currentPrice = EventOption.probability(option, event.options)
        val shares = amount / currentPrice

        // Update the target option's pool
        val updatedOptions = event.options.map {
            if (it.id == optionId) it.copy(pool = it.pool + amount) else it
        }

        val updatedEvent = event.copy(
            options = updatedOptions,
            totalVolume = event.totalVolume + amount,
            totalTraders = event.totalTraders + 1
        )

        // Add a price history point after the trade (uses Yes option probability)
        val newPricePoint = com.neviim.market.data.model.PricePoint(
            timestamp = System.currentTimeMillis(),
            yesPrice = if (updatedEvent.options.isNotEmpty()) {
                EventOption.probability(updatedEvent.options.first(), updatedEvent.options)
            } else 0.5
        )

        val finalEvent = updatedEvent.copy(
            priceHistory = updatedEvent.priceHistory + newPricePoint
        )

        return TradeResult(
            updatedEvent = finalEvent,
            sharesReceived = shares,
            executionPrice = currentPrice,
            estimatedReturn = shares
        )
    }
}
