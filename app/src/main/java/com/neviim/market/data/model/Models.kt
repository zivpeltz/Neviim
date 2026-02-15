package com.neviim.market.data.model

import java.util.UUID

/**
 * Represents a binary prediction market event with AMM pools.
 */
data class Event(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val titleHe: String,
    val tag: EventTag,
    val yesPool: Double,
    val noPool: Double,
    val totalVolume: Double = 0.0,
    val isResolved: Boolean = false,
    val resolvedOutcome: Boolean? = null, // true = Yes won, false = No won
    val priceHistory: List<PricePoint> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val yesProbability: Double
        get() = if (yesPool + noPool > 0) noPool / (yesPool + noPool) else 0.5

    val noProbability: Double
        get() = if (yesPool + noPool > 0) yesPool / (yesPool + noPool) else 0.5
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
    val eventTitle: String,
    val eventTitleHe: String,
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
    val balance: Double = 5000.0,
    val totalWinnings: Double = 0.0,
    val totalBets: Int = 0,
    val wonBets: Int = 0
) {
    val winRate: Double
        get() = if (totalBets > 0) (wonBets.toDouble() / totalBets) * 100.0 else 0.0
}
