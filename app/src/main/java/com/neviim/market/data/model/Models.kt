package com.neviim.market.data.model

import java.util.UUID

/**
 * Represents a prediction market event. Supports both binary (Yes/No)
 * and multi-choice events (e.g. "Who wins the election?" with 3+ options).
 *
 * For binary events, [options] contains exactly two entries (Yes and No).
 * For multi-choice events, [options] contains 2+ custom entries.
 *
 * Each option has its own AMM pool; probabilities are derived via
 * pool-ratio pricing across all options.
 */
data class Event(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val titleHe: String,
    val description: String = "",
    val descriptionHe: String = "",
    val tag: EventTag,
    val tagLabel: String = "",          // raw label from Polymarket API e.g. "Politics", "Soccer"
    val conditionId: String? = null,    // CLOB condition ID for fetching price history
    val eventType: EventType = EventType.BINARY,
    val options: List<EventOption> = emptyList(),
    val totalVolume: Double = 0.0,
    val isResolved: Boolean = false,
    val resolvedOptionId: String? = null,
    val resolutionSource: String? = null,
    val priceHistory: List<PricePoint> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val totalTraders: Int = 0,
    val image: String? = null
) {
    // ── Binary compatibility helpers ────────────────────────────────
    // Kept so existing binary UI code doesn't break.

    /** Legacy pool values for binary events. */
    val yesPool: Double
        get() = options.firstOrNull()?.pool ?: 0.0

    val noPool: Double
        get() = if (options.size >= 2) options[1].pool else 0.0

    val yesProbability: Double
        get() {
            val yes = yesPool
            val no = noPool
            return if (yes + no > 0) yes / (yes + no) else 0.5
        }

    val noProbability: Double
        get() {
            val yes = yesPool
            val no = noPool
            return if (yes + no > 0) no / (yes + no) else 0.5
        }

    /** Legacy resolvedOutcome for binary events. */
    val resolvedOutcome: Boolean?
        get() = when {
            !isResolved -> null
            resolvedOptionId == options.firstOrNull()?.id -> true
            else -> false
        }

    /** Total liquidity across all option pools. */
    val totalLiquidity: Double
        get() = options.sumOf { it.pool }
}

enum class EventType {
    /** Classic Yes / No market. */
    BINARY,
    /** Multiple mutually-exclusive outcomes. */
    MULTI_CHOICE
}

/**
 * A single outcome option within an event.
 */
data class EventOption(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val labelHe: String = "",
    val pool: Double = 500.0,
    /**
     * Direct real-time price from Polymarket (0.0–1.0).
     * When set, this is used as-is rather than computing pool ratios.
     * Null for locally-created mock events (which use pool ratios instead).
     */
    val directPrice: Double? = null
) {
    companion object {
        /**
         * Returns the probability for this option.
         * - If [option.directPrice] is set → use it directly (Polymarket live price).
         * - Otherwise → pool-ratio AMM model (used for local/mock events only).
         */
        fun probability(option: EventOption, allOptions: List<EventOption>): Double {
            // Prefer direct price when available (Polymarket API data)
            option.directPrice?.let { return it.coerceIn(0.001, 0.999) }

            // Fallback: pool-ratio (local mock events)
            if (allOptions.size < 2) return 1.0
            val totalPool = allOptions.sumOf { it.pool }
            if (totalPool <= 0) return 1.0 / allOptions.size
            return option.pool / totalPool
        }
    }
}

enum class EventTag(val displayName: String, val displayNameHe: String) {
    POLITICS("Politics", "פוליטיקה"),
    POP_CULTURE("Pop Culture", "תרבות פופ"),
    CRYPTO("Crypto", "קריפטו"),
    SCIENCE("Science", "מדע"),
    SPORTS("Sports", "ספורט")
}

/**
 * A single historical data point for the probability chart.
 */
data class PricePoint(
    val timestamp: Long,
    val yesPrice: Double
)

/**
 * Represents a user's position (bet) on an event.
 */
data class UserPosition(
    val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val marketId: String = eventId,   // actual Polymarket market ID for API resolution checks
    val eventTitle: String,
    val eventTitleHe: String,
    val optionId: String = "",
    val optionLabel: String = "",
    val side: TradeSide,
    val shares: Double,
    val entryPrice: Double,
    val amountPaid: Double,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TradeSide { YES, NO }

/**
 * The user's profile / wallet.
 */
data class UserProfile(
    val username: String = "Prophet",
    val balance: Double = 100_000.0,
    val totalWinnings: Double = 0.0,
    val totalBets: Int = 0,
    val wonBets: Int = 0
) {
    val winRate: Double
        get() = if (totalBets > 0) (wonBets.toDouble() / totalBets) * 100.0 else 0.0
}
