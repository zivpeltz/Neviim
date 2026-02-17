package com.neviim.market.data.repository

import android.content.Context
import com.neviim.market.data.amm.AmmEngine
import com.neviim.market.data.model.*
import com.neviim.market.data.storage.EventStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Repository for the Neviim app.
 * Acts as a single source of truth for events, positions, and user profile.
 * Events are persisted to internal storage as JSON.
 */
object MarketRepository {

    private lateinit var appContext: Context

    /**
     * Initialize with app context. Loads persisted events or seeds on first launch.
     * Must be called from Application.onCreate().
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        val stored = EventStorage.load(appContext)
        if (stored != null) {
            _events.value = stored
            // Set nextEventId higher than any existing event
            nextEventId = stored.maxOfOrNull { id ->
                id.id.removePrefix("evt_").toIntOrNull() ?: 0
            }?.plus(1) ?: 9
        }
    }

    private fun persistEvents() {
        if (::appContext.isInitialized) {
            EventStorage.save(appContext, _events.value)
        }
    }

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

    // ── Helper: build binary event options ──────────────────────────────
    private fun binaryOptions(yesPool: Double, noPool: Double): List<EventOption> = listOf(
        EventOption(id = "yes", label = "Yes", labelHe = "כן", pool = yesPool),
        EventOption(id = "no", label = "No", labelHe = "לא", pool = noPool)
    )

    // ── Seed data ───────────────────────────────────────────────────────
    private val dayMs = 86_400_000L
    private val now = System.currentTimeMillis()

    private val seedEvents = listOf(
        Event(
            id = "evt_1",
            title = "Will it rain in Tel Aviv tomorrow?",
            titleHe = "האם ירד גשם בתל אביב מחר?",
            description = "Resolves YES if any official weather station in Tel Aviv records precipitation.",
            descriptionHe = "יסתיים בכן אם תחנת מזג אוויר רשמית בתל אביב תרשום משקעים.",
            tag = EventTag.SCIENCE,
            eventType = EventType.BINARY,
            options = binaryOptions(400.0, 600.0),
            totalVolume = 2_340.0,
            priceHistory = generateMockHistory(0.40),
            endDate = now + 1 * dayMs,
            totalTraders = 47
        ),
        Event(
            id = "evt_2",
            title = "Bitcoin > \$100k by end of 2026?",
            titleHe = "ביטקוין מעל 100 אלף דולר עד סוף 2026?",
            description = "Resolves YES if BTC/USD exceeds \$100,000 on any major exchange before Jan 1, 2027.",
            descriptionHe = "יסתיים בכן אם BTC/USD יעלה על 100,000 דולר בכל בורסה מרכזית לפני ה-1 בינואר 2027.",
            tag = EventTag.CRYPTO,
            eventType = EventType.BINARY,
            options = binaryOptions(350.0, 650.0),
            totalVolume = 5_120.0,
            priceHistory = generateMockHistory(0.35),
            endDate = now + 300 * dayMs,
            totalTraders = 128
        ),
        Event(
            id = "evt_3",
            title = "Will elections be held before 2027?",
            titleHe = "האם יתקיימו בחירות לפני 2027?",
            description = "Resolves YES if a general election is officially held in Israel before January 1, 2027.",
            descriptionHe = "יסתיים בכן אם יתקיימו בחירות כלליות רשמיות בישראל לפני ה-1 בינואר 2027.",
            tag = EventTag.POLITICS,
            eventType = EventType.BINARY,
            options = binaryOptions(500.0, 500.0),
            totalVolume = 8_900.0,
            priceHistory = generateMockHistory(0.50),
            endDate = now + 300 * dayMs,
            totalTraders = 312
        ),
        Event(
            id = "evt_4",
            title = "Will Netta win Eurovision 2026?",
            titleHe = "האם נטע תזכה באירוויזיון 2026?",
            description = "Resolves YES if Netta Barzilai wins the Eurovision Song Contest 2026.",
            descriptionHe = "יסתיים בכן אם נטע ברזילי תזכה בתחרות אירוויזיון 2026.",
            tag = EventTag.POP_CULTURE,
            eventType = EventType.BINARY,
            options = binaryOptions(200.0, 800.0),
            totalVolume = 1_780.0,
            priceHistory = generateMockHistory(0.20),
            endDate = now + 120 * dayMs,
            totalTraders = 64
        ),
        Event(
            id = "evt_5",
            title = "Israel wins gold at 2028 Olympics?",
            titleHe = "ישראל תזכה בזהב באולימפיאדת 2028?",
            description = "Resolves YES if an Israeli athlete wins a gold medal at the 2028 LA Olympics.",
            descriptionHe = "יסתיים בכן אם ספורטאי ישראלי יזכה במדליית זהב באולימפיאדת לוס אנג׳לס 2028.",
            tag = EventTag.SPORTS,
            eventType = EventType.BINARY,
            options = binaryOptions(150.0, 850.0),
            totalVolume = 3_450.0,
            priceHistory = generateMockHistory(0.15),
            endDate = now + 800 * dayMs,
            totalTraders = 89
        ),
        // ── Multi-choice events ────────────────────────────────────────
        Event(
            id = "evt_6",
            title = "Who will be the next Prime Minister?",
            titleHe = "מי יהיה ראש הממשלה הבא?",
            description = "Resolves to the candidate who becomes the next Prime Minister of Israel.",
            descriptionHe = "יסתיים בשם המועמד שיהפוך לראש הממשלה הבא של ישראל.",
            tag = EventTag.POLITICS,
            eventType = EventType.MULTI_CHOICE,
            options = listOf(
                EventOption(id = "mc6_1", label = "Netanyahu", labelHe = "נתניהו", pool = 300.0),
                EventOption(id = "mc6_2", label = "Gantz", labelHe = "גנץ", pool = 350.0),
                EventOption(id = "mc6_3", label = "Lapid", labelHe = "לפיד", pool = 500.0),
                EventOption(id = "mc6_4", label = "Bennett", labelHe = "בנט", pool = 600.0),
                EventOption(id = "mc6_5", label = "Sa'ar", labelHe = "סער", pool = 800.0)
            ),
            totalVolume = 12_400.0,
            priceHistory = generateMockHistory(0.35),
            endDate = now + 365 * dayMs,
            totalTraders = 456
        ),
        Event(
            id = "evt_7",
            title = "Ethereum price range end of 2026?",
            titleHe = "טווח מחיר אתריום בסוף 2026?",
            description = "Which price range will ETH/USD be on December 31, 2026?",
            descriptionHe = "באיזה טווח מחירים יהיה ETH/USD ב-31 בדצמבר 2026?",
            tag = EventTag.CRYPTO,
            eventType = EventType.MULTI_CHOICE,
            options = listOf(
                EventOption(id = "mc7_1", label = "Under \$2,000", labelHe = "מתחת ל-2,000$", pool = 600.0),
                EventOption(id = "mc7_2", label = "\$2,000 – \$5,000", labelHe = "2,000$ – 5,000$", pool = 300.0),
                EventOption(id = "mc7_3", label = "\$5,000 – \$10,000", labelHe = "5,000$ – 10,000$", pool = 400.0),
                EventOption(id = "mc7_4", label = "Over \$10,000", labelHe = "מעל 10,000$", pool = 700.0)
            ),
            totalVolume = 7_800.0,
            priceHistory = generateMockHistory(0.30),
            endDate = now + 300 * dayMs,
            totalTraders = 203
        ),
        Event(
            id = "evt_8",
            title = "Eurovision 2026 winner country?",
            titleHe = "מדינה מנצחת באירוויזיון 2026?",
            description = "Which country will win the Eurovision Song Contest 2026?",
            descriptionHe = "איזו מדינה תנצח בתחרות אירוויזיון 2026?",
            tag = EventTag.POP_CULTURE,
            eventType = EventType.MULTI_CHOICE,
            options = listOf(
                EventOption(id = "mc8_1", label = "Israel", labelHe = "ישראל", pool = 500.0),
                EventOption(id = "mc8_2", label = "Sweden", labelHe = "שוודיה", pool = 350.0),
                EventOption(id = "mc8_3", label = "Italy", labelHe = "איטליה", pool = 400.0),
                EventOption(id = "mc8_4", label = "France", labelHe = "צרפת", pool = 450.0),
                EventOption(id = "mc8_5", label = "Other", labelHe = "אחר", pool = 300.0)
            ),
            totalVolume = 4_200.0,
            priceHistory = generateMockHistory(0.25),
            endDate = now + 120 * dayMs,
            totalTraders = 178
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

    /** Place a binary trade (Yes/No). */
    fun placeTrade(eventId: String, side: TradeSide, amount: Double): Result<AmmEngine.TradeResult> {
        val event = getEvent(eventId) ?: return Result.failure(Exception("Event not found"))
        val profile = _userProfile.value

        if (amount <= 0) return Result.failure(Exception("Amount must be positive"))
        if (amount > profile.balance) return Result.failure(Exception("Insufficient balance"))
        if (event.isResolved) return Result.failure(Exception("Event is already resolved"))

        val tradeResult = AmmEngine.executeTrade(event, side, amount)

        _events.update { list ->
            list.map { if (it.id == eventId) tradeResult.updatedEvent else it }
        }
        persistEvents()

        val option = if (side == TradeSide.YES) event.options.firstOrNull() else event.options.getOrNull(1)

        val position = UserPosition(
            eventId = eventId,
            eventTitle = event.title,
            eventTitleHe = event.titleHe,
            optionId = option?.id ?: "",
            optionLabel = option?.label ?: side.name,
            side = side,
            shares = tradeResult.sharesReceived,
            entryPrice = tradeResult.executionPrice,
            amountPaid = amount
        )
        _positions.update { it + position }

        _userProfile.update {
            it.copy(
                balance = it.balance - amount,
                totalBets = it.totalBets + 1
            )
        }

        return Result.success(tradeResult)
    }

    /** Place a trade on a specific option (for multi-choice events). */
    fun placeOptionTrade(eventId: String, optionId: String, amount: Double): Result<AmmEngine.TradeResult> {
        val event = getEvent(eventId) ?: return Result.failure(Exception("Event not found"))
        val profile = _userProfile.value

        if (amount <= 0) return Result.failure(Exception("Amount must be positive"))
        if (amount > profile.balance) return Result.failure(Exception("Insufficient balance"))
        if (event.isResolved) return Result.failure(Exception("Event is already resolved"))

        val option = event.options.find { it.id == optionId }
            ?: return Result.failure(Exception("Option not found"))

        val tradeResult = AmmEngine.executeOptionTrade(event, optionId, amount)

        _events.update { list ->
            list.map { if (it.id == eventId) tradeResult.updatedEvent else it }
        }
        persistEvents()

        // For multi-choice, side is always YES (you're betting that option wins)
        val position = UserPosition(
            eventId = eventId,
            eventTitle = event.title,
            eventTitleHe = event.titleHe,
            optionId = optionId,
            optionLabel = option.label,
            side = TradeSide.YES,
            shares = tradeResult.sharesReceived,
            entryPrice = tradeResult.executionPrice,
            amountPaid = amount
        )
        _positions.update { it + position }

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

    // ── Event creation ──────────────────────────────────────────────────
    private var nextEventId = 9

    /** Create a binary event. */
    fun createEvent(
        title: String,
        titleHe: String,
        tag: EventTag,
        initialYesProbability: Double = 0.50,
        description: String = "",
        descriptionHe: String = "",
        endDate: Long? = null
    ): Event {
        val totalPool = 1000.0
        val yesPool = totalPool * initialYesProbability
        val noPool = totalPool * (1.0 - initialYesProbability)
        val event = Event(
            id = "evt_${nextEventId++}",
            title = title,
            titleHe = titleHe.ifBlank { title },
            description = description,
            descriptionHe = descriptionHe.ifBlank { description },
            tag = tag,
            eventType = EventType.BINARY,
            options = binaryOptions(yesPool, noPool),
            totalVolume = 0.0,
            priceHistory = generateMockHistory(initialYesProbability, 5),
            endDate = endDate
        )
        _events.update { it + event }
        persistEvents()
        return event
    }

    /** Create a multi-choice event. */
    fun createMultiChoiceEvent(
        title: String,
        titleHe: String,
        tag: EventTag,
        optionLabels: List<Pair<String, String>>,  // (english, hebrew) pairs
        description: String = "",
        descriptionHe: String = "",
        endDate: Long? = null
    ): Event {
        val poolPerOption = 500.0
        val options = optionLabels.mapIndexed { index, (label, labelHe) ->
            EventOption(
                id = "mc${nextEventId}_${index + 1}",
                label = label,
                labelHe = labelHe.ifBlank { label },
                pool = poolPerOption
            )
        }
        val event = Event(
            id = "evt_${nextEventId++}",
            title = title,
            titleHe = titleHe.ifBlank { title },
            description = description,
            descriptionHe = descriptionHe.ifBlank { description },
            tag = tag,
            eventType = EventType.MULTI_CHOICE,
            options = options,
            totalVolume = 0.0,
            priceHistory = generateMockHistory(1.0 / optionLabels.size, 5),
            endDate = endDate
        )
        _events.update { it + event }
        persistEvents()
        return event
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

    /**
     * Returns the current price for a specific option.
     */
    fun getOptionPrice(eventId: String, optionId: String): Double {
        val event = getEvent(eventId) ?: return 0.0
        val option = event.options.find { it.id == optionId } ?: return 0.0
        return EventOption.probability(option, event.options)
    }
}
