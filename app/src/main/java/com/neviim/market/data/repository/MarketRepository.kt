package com.neviim.market.data.repository

import com.neviim.market.data.amm.AmmEngine
import com.neviim.market.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory repository for the Neviim prototype.
 * Acts as a single source of truth for events, positions, and user profile.
 */
object MarketRepository {

    // ── Mock price history generator ────────────────────────────────────
    private fun generateMockHistory(basePrice: Double, points: Int = 20): List<PricePoint> {
        val now = System.currentTimeMillis()
        val hourMs = 3_600_000L
        val history = mutableListOf<PricePoint>()
        var price = basePrice
        for (i in 0 until points) {
            val drift = (Math.random() - 0.5) * 0.08
            price = (price + drift).coerceIn(0.05, 0.95)
            history.add(
                PricePoint(
                    timestamp = now - (points - i) * hourMs,
                    yesPrice = price
                )
            )
        }
        return history
    }

    // ── Seed data ───────────────────────────────────────────────────────
    private val seedEvents = listOf(
        Event(
            id = "evt_1",
            title = "Will it rain in Tel Aviv tomorrow?",
            titleHe = "האם ירד גשם בתל אביב מחר?",
            tag = EventTag.SCIENCE,
            yesPool = 400.0,
            noPool = 600.0,
            totalVolume = 2_340.0,
            priceHistory = generateMockHistory(0.40)
        ),
        Event(
            id = "evt_2",
            title = "Bitcoin > \$100k by end of 2026?",
            titleHe = "ביטקוין מעל 100 אלף דולר עד סוף 2026?",
            tag = EventTag.CRYPTO,
            yesPool = 350.0,
            noPool = 650.0,
            totalVolume = 5_120.0,
            priceHistory = generateMockHistory(0.35)
        ),
        Event(
            id = "evt_3",
            title = "Will elections be held before 2027?",
            titleHe = "האם יתקיימו בחירות לפני 2027?",
            tag = EventTag.POLITICS,
            yesPool = 500.0,
            noPool = 500.0,
            totalVolume = 8_900.0,
            priceHistory = generateMockHistory(0.50)
        ),
        Event(
            id = "evt_4",
            title = "Will Netta win Eurovision 2026?",
            titleHe = "האם נטע תזכה באירוויזיון 2026?",
            tag = EventTag.POP_CULTURE,
            yesPool = 200.0,
            noPool = 800.0,
            totalVolume = 1_780.0,
            priceHistory = generateMockHistory(0.20)
        ),
        Event(
            id = "evt_5",
            title = "Israel wins gold at 2028 Olympics?",
            titleHe = "ישראל תזכה בזהב באולימפיאדת 2028?",
            tag = EventTag.SPORTS,
            yesPool = 150.0,
            noPool = 850.0,
            totalVolume = 3_450.0,
            priceHistory = generateMockHistory(0.15)
        )
    )

    // ── State flows ─────────────────────────────────────────────────────
    private val _events = MutableStateFlow(seedEvents)
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _positions = MutableStateFlow<List<UserPosition>>(emptyList())
    val positions: StateFlow<List<UserPosition>> = _positions.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // ── Queries ─────────────────────────────────────────────────────────
    fun getEvent(eventId: String): Event? = _events.value.find { it.id == eventId }

    // ── Trading ─────────────────────────────────────────────────────────

    sealed class TradeError {
        data object InsufficientBalance : TradeError()
        data object EventNotFound : TradeError()
        data object EventResolved : TradeError()
        data object InvalidAmount : TradeError()
    }

    fun placeTrade(eventId: String, side: TradeSide, amount: Double): Result<AmmEngine.TradeResult> {
        val event = getEvent(eventId) ?: return Result.failure(Exception("Event not found"))
        val profile = _userProfile.value

        if (amount <= 0) return Result.failure(Exception("Amount must be positive"))
        if (amount > profile.balance) return Result.failure(Exception("Insufficient balance"))
        if (event.isResolved) return Result.failure(Exception("Event is already resolved"))

        val tradeResult = AmmEngine.executeTrade(event, side, amount)

        // Update event in the list
        _events.update { list ->
            list.map { if (it.id == eventId) tradeResult.updatedEvent else it }
        }

        // Add position
        val position = UserPosition(
            eventId = eventId,
            eventTitle = event.title,
            eventTitleHe = event.titleHe,
            side = side,
            shares = tradeResult.sharesReceived,
            entryPrice = tradeResult.executionPrice,
            amountPaid = amount
        )
        _positions.update { it + position }

        // Deduct balance
        _userProfile.update {
            it.copy(
                balance = it.balance - amount,
                totalBets = it.totalBets + 1
            )
        }

        return Result.success(tradeResult)
    }

    // ── Account actions ─────────────────────────────────────────────────
    fun refillBalance(amount: Double = 1000.0) {
        _userProfile.update { it.copy(balance = it.balance + amount) }
    }

    /**
     * Returns the current price for a given position's side,
     * used for PnL calculation.
     */
    fun getCurrentPrice(eventId: String, side: TradeSide): Double {
        val event = getEvent(eventId) ?: return 0.0
        return when (side) {
            TradeSide.YES -> event.yesProbability
            TradeSide.NO -> event.noProbability
        }
    }
}
