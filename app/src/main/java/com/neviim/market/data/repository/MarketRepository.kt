package com.neviim.market.data.repository

import android.content.Context
import android.util.Log
import com.neviim.market.data.model.*
import com.neviim.market.data.network.GammaApi
import com.neviim.market.data.storage.UserDataStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object MarketRepository {

    private lateinit var appContext: Context
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gammaApi by lazy { GammaApi.create() }

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _positions = MutableStateFlow<List<UserPosition>>(emptyList())
    val positions: StateFlow<List<UserPosition>> = _positions.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    /** Epoch millis of the last successful events fetch, 0 if never. */
    private val _lastRefreshed = MutableStateFlow(0L)
    val lastRefreshed: StateFlow<Long> = _lastRefreshed.asStateFlow()

    private const val REFRESH_INTERVAL_MS = 30_000L  // 30 seconds

    fun init(context: Context) {
        appContext = context.applicationContext
        val loaded = UserDataStorage.load(appContext)
        if (loaded != null) {
            _userProfile.value = loaded.first
            _positions.value = loaded.second
        }
        refreshEvents()
        startAutoRefresh()
    }

    /** Kicks off a coroutine that re-fetches market data every 30 seconds. */
    private fun startAutoRefresh() {
        scope.launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                refreshEvents()
            }
        }
    }

    private fun persistUserData() {
        if (::appContext.isInitialized) {
            UserDataStorage.save(appContext, _userProfile.value, _positions.value)
        }
    }

    /** Re-reads the user profile from disk. Called after onboarding saves a new name. */
    fun reloadProfile() {
        if (!::appContext.isInitialized) return
        val loaded = UserDataStorage.load(appContext) ?: return
        _userProfile.value = loaded.first
        _positions.value = loaded.second
    }

    fun refreshEvents() {
        scope.launch {
            try {
                // FIX: Must pass closed=false and archived=false.
                // Without these, the API returns only old, closed/archived events
                // which all get filtered out by our market validation below.
                // Also sort by 24h volume descending so users see the most active markets first.
                val gammaEvents = gammaApi.getEvents(
                    active = true,
                    closed = false,
                    archived = false,
                    limit = 50,
                    order = "volume24hr",
                    ascending = false
                )
                val mapped = mutableListOf<Event>()

                for (ge in gammaEvents) {
                    // FIX: Filter at both the event and market level.
                    // A market must be active=true AND closed=false AND have volume.
                    val validMarkets = ge.markets.filter { gm ->
                        gm.active && !gm.closed && gm.volumeNum >= 10
                    }
                    if (validMarkets.isEmpty()) continue

                    val firstTag = ge.tags.firstOrNull()?.label ?: ""
                    val tag = when {
                        ge.tags.any { it.label.equals("Politics", true) } -> EventTag.POLITICS
                        ge.tags.any { it.label.equals("Crypto", true) } -> EventTag.CRYPTO
                        ge.tags.any { it.label.equals("Pop Culture", true) } -> EventTag.POP_CULTURE
                        ge.tags.any { it.label.equals("Science", true) } -> EventTag.SCIENCE
                        else -> EventTag.SPORTS
                    }

                    // FIX: Group multi-market events into a single MULTI_CHOICE card.
                    // e.g. "2028 Democratic Primary" has 44 markets (one per candidate).
                    // We create one event with each market as an option.
                    if (validMarkets.size > 1) {
                        // Check if the first market is binary (Yes/No)
                        val firstOutcomes = validMarkets.first().parsedOutcomes()
                        val isBinaryGroup = firstOutcomes.size == 2 &&
                                firstOutcomes[0].equals("Yes", true) &&
                                firstOutcomes[1].equals("No", true)

                        if (isBinaryGroup) {
                            // Multiple Yes/No markets under one event = each market is a candidate/option
                            // e.g. "Will Biden win?" + "Will Trump win?" → options: Biden, Trump
                            val options = validMarkets.mapIndexed { i, gm ->
                                val prices = gm.parsedOutcomePrices()
                                val yesPrice = prices.firstOrNull() ?: 0.5
                                EventOption(
                                    id = gm.id,
                                    label = gm.question.ifBlank { ge.title },
                                    labelHe = gm.question.ifBlank { ge.title },
                                    pool = (yesPrice * 1000.0).coerceAtLeast(1.0)
                                )
                            }
                            mapped.add(
                                Event(
                                    id = ge.id,
                                    title = ge.title,
                                    titleHe = ge.title,
                                    description = ge.description ?: "",
                                    tag = tag,
                                    tagLabel = firstTag,
                                    conditionId = validMarkets.first().conditionId,
                                    eventType = EventType.MULTI_CHOICE,
                                    options = options,
                                    totalVolume = validMarkets.sumOf { it.volumeNum },
                                    isResolved = false,
                                    resolutionSource = validMarkets.first().resolutionSource,
                                    image = ge.image
                                )
                            )
                        } else {
                            // Mixed or unusual multi-market format — add each as a separate card
                            for (gm in validMarkets) {
                                addMarketAsEvent(gm, ge, tag, mapped)
                            }
                        }
                    } else {
                        // Single market: add it as its own card (most likely BINARY)
                        addMarketAsEvent(validMarkets.first(), ge, tag, mapped)
                    }
                }
                _events.value = mapped
                _lastRefreshed.value = System.currentTimeMillis()
                Log.d("MarketRepository", "Loaded ${mapped.size} events from ${gammaEvents.size} Polymarket events")
                // Check if any open positions can now be resolved based on fresh data
                resolvePositions()
            } catch (e: Exception) {
                Log.e("MarketRepository", "Failed to fetch events", e)
            }
        }
    }

    private fun addMarketAsEvent(
        gm: com.neviim.market.data.network.GammaMarket,
        ge: com.neviim.market.data.network.GammaEvent,
        tag: EventTag,
        list: MutableList<Event>
    ) {
        val outcomes = gm.parsedOutcomes()
        val prices = gm.parsedOutcomePrices()
        if (outcomes.size != prices.size || outcomes.isEmpty()) return

        val isBinary = outcomes.size == 2 && outcomes[0].equals("Yes", true)
        val type = if (isBinary) EventType.BINARY else EventType.MULTI_CHOICE

        val options = outcomes.zip(prices).mapIndexed { i, (label, price) ->
            EventOption(
                id = "${gm.id}_$i",
                label = label,
                labelHe = label,
                pool = (price * 1000.0).coerceAtLeast(1.0)
            )
        }

        list.add(
            Event(
                id = gm.id,
                title = gm.question.ifBlank { ge.title },
                titleHe = gm.question.ifBlank { ge.title },
                description = ge.description ?: "",
                tag = tag,
                tagLabel = ge.tags.firstOrNull()?.label ?: "",
                conditionId = gm.conditionId,
                eventType = type,
                options = options,
                totalVolume = gm.volumeNum,
                isResolved = gm.closed,
                resolutionSource = gm.resolutionSource,
                image = ge.image
            )
        )
    }

    fun getEvent(eventId: String): Event? = _events.value.find { it.id == eventId }

    suspend fun fetchPriceHistory(conditionId: String): List<PricePoint> = try {
        gammaApi.getPricesHistory(conditionId, interval = "1m", fidelity = 60)
            .history
            .map { PricePoint(timestamp = it.timestamp * 1000L, yesPrice = it.price) }
    } catch (e: Exception) {
        Log.w("MarketRepository", "Price history fetch failed for $conditionId", e)
        emptyList()
    }

    fun estimateReturn(event: Event, side: TradeSide, amount: Double): Double {
        val price = getCurrentPrice(event.id, side)
        if (price <= 0.0) return 0.0
        return amount / price
    }

    fun estimateOptionReturn(event: Event, optionId: String, amount: Double): Double {
        val price = getOptionPrice(event.id, optionId)
        if (price <= 0.0) return 0.0
        return amount / price
    }

    fun placeTrade(eventId: String, side: TradeSide, amount: Double): Result<Unit> {
        val event = getEvent(eventId) ?: return Result.failure(Exception("Event not found"))
        val profile = _userProfile.value

        if (amount <= 0) return Result.failure(Exception("Amount must be positive"))
        if (amount > profile.balance) return Result.failure(Exception("Insufficient balance"))
        if (event.isResolved) return Result.failure(Exception("Event is already resolved"))

        val price = getCurrentPrice(eventId, side)
        if (price <= 0) return Result.failure(Exception("Invalid market price"))

        val shares = amount / price
        val option = if (side == TradeSide.YES) event.options.firstOrNull() else event.options.getOrNull(1)

        // For binary events, the market ID IS the event ID.
        // The option ID is like "<marketId>_0" or "<marketId>_1".
        val marketId = option?.id?.substringBeforeLast("_") ?: eventId

        val position = UserPosition(
            eventId = eventId,
            marketId = marketId,
            eventTitle = event.title,
            eventTitleHe = event.titleHe,
            optionId = option?.id ?: "",
            optionLabel = option?.label ?: side.name,
            side = side,
            shares = shares,
            entryPrice = price,
            amountPaid = amount
        )

        _positions.update { it + position }
        _userProfile.update {
            it.copy(
                balance = it.balance - amount,
                totalBets = it.totalBets + 1
            )
        }

        persistUserData()
        return Result.success(Unit)
    }

    fun placeOptionTrade(eventId: String, optionId: String, amount: Double): Result<Unit> {
        val event = getEvent(eventId) ?: return Result.failure(Exception("Event not found"))
        val profile = _userProfile.value

        if (amount <= 0) return Result.failure(Exception("Amount must be positive"))
        if (amount > profile.balance) return Result.failure(Exception("Insufficient balance"))
        if (event.isResolved) return Result.failure(Exception("Event is already resolved"))

        val option = event.options.find { it.id == optionId }
            ?: return Result.failure(Exception("Option not found"))

        val price = getOptionPrice(eventId, optionId)
        if (price <= 0) return Result.failure(Exception("Invalid market price"))

        val shares = amount / price

        // For multi-choice, the optionId IS the market ID (it's the GammaMarket.id)
        val position = UserPosition(
            eventId = eventId,
            marketId = optionId,
            eventTitle = event.title,
            eventTitleHe = event.titleHe,
            optionId = optionId,
            optionLabel = option.label,
            side = TradeSide.YES,
            shares = shares,
            entryPrice = price,
            amountPaid = amount
        )

        _positions.update { it + position }
        _userProfile.update {
            it.copy(
                balance = it.balance - amount,
                totalBets = it.totalBets + 1
            )
        }

        persistUserData()
        return Result.success(Unit)
    }

    fun refillBalance(amount: Double = 1000.0) {
        _userProfile.update { it.copy(balance = it.balance + amount) }
        persistUserData()
    }

    /**
     * Scans all open positions against the current in-memory event list.
     * Called automatically after every refreshEvents(). No extra network
     * calls — we already have fresh closed/isResolved state from the fetch.
     *
     * Resolution logic:
     *  - For BINARY: the position's side (YES/NO) matches the winning option.
     *  - For MULTI_CHOICE: the position's marketId matches a closed market
     *    whose winning outcome price is >= 0.95 (Polymarket convention).
     * Winning positions receive a 1:1 payout (shares × 1.0 SP).
     */
    fun resolvePositions() {
        val events = _events.value
        val openPositions = _positions.value
        if (openPositions.isEmpty()) return

        var changed = false
        val remaining = mutableListOf<UserPosition>()
        var balanceDelta = 0.0
        var winDelta = 0
        var winningsDelta = 0.0

        for (pos in openPositions) {
            // Find the resolved event that matches this position
            val event = events.find { it.id == pos.eventId }

            if (event == null || !event.isResolved) {
                // Also check if the specific market option is resolved (multi-choice case)
                // We look for an option whose id == pos.marketId with price ~1.0
                val resolvedOption = event?.options?.find { opt ->
                    opt.id == pos.marketId &&
                    EventOption.probability(opt, event.options) >= 0.95
                }

                if (resolvedOption == null) {
                    remaining.add(pos)  // not yet resolved, keep
                    continue
                }

                // Multi-choice option won
                val payout = pos.shares * 1.0
                balanceDelta += payout
                winningsDelta += payout
                winDelta++
                changed = true
                Log.d("MarketRepository", "Position resolved (multi-choice win): ${pos.optionLabel} → +$payout SP")
                continue
            }

            // Event is fully resolved
            changed = true
            val won = when {
                event.eventType == EventType.BINARY -> {
                    val winningOption = event.options.find { it.id == event.resolvedOptionId }
                    val winningSide = if (winningOption == event.options.firstOrNull()) TradeSide.YES else TradeSide.NO
                    pos.side == winningSide
                }
                else -> {
                    // For multi-choice, check if user's option was the winner
                    val winnerOption = event.options.find { it.id == event.resolvedOptionId }
                    pos.optionId == winnerOption?.id
                }
            }

            if (won) {
                val payout = pos.shares * 1.0
                balanceDelta += payout
                winningsDelta += payout
                winDelta++
                Log.d("MarketRepository", "Position won: ${pos.optionLabel} → +$payout SP")
            } else {
                Log.d("MarketRepository", "Position lost: ${pos.optionLabel}")
            }
            // Either way, remove the resolved position
        }

        if (changed) {
            _positions.value = remaining
            _userProfile.update { profile ->
                profile.copy(
                    balance = profile.balance + balanceDelta,
                    wonBets = profile.wonBets + winDelta,
                    totalWinnings = profile.totalWinnings + winningsDelta
                )
            }
            persistUserData()
            Log.d("MarketRepository", "Resolved ${openPositions.size - remaining.size} positions. Balance delta: +$balanceDelta SP")
        }
    }

    fun getCurrentPrice(eventId: String, side: TradeSide): Double {
        val event = getEvent(eventId) ?: return 0.0
        return when (side) {
            TradeSide.YES -> event.yesProbability
            TradeSide.NO -> event.noProbability
        }
    }

    fun getOptionPrice(eventId: String, optionId: String): Double {
        val event = getEvent(eventId) ?: return 0.0
        val option = event.options.find { it.id == optionId } ?: return 0.0
        return EventOption.probability(option, event.options)
    }

    private var nextMockId = 1000
    fun createEvent(
        title: String,
        titleHe: String,
        tag: EventTag,
        initialYesProbability: Double = 0.50,
        description: String = "",
        descriptionHe: String = "",
        endDate: Long? = null
    ): Event {
        val event = Event(
            id = "mock_${nextMockId++}",
            title = title,
            titleHe = titleHe,
            description = description,
            tag = tag,
            eventType = EventType.BINARY,
            options = listOf(
                EventOption(id = "mock_yes", label = "Yes", pool = initialYesProbability * 1000.0),
                EventOption(id = "mock_no", label = "No", pool = (1.0 - initialYesProbability) * 1000.0)
            )
        )
        _events.update { it + event }
        return event
    }

    fun createMultiChoiceEvent(
        title: String,
        titleHe: String,
        tag: EventTag,
        optionLabels: List<Pair<String, String>>,
        description: String = "",
        descriptionHe: String = "",
        endDate: Long? = null
    ): Event {
        val options = optionLabels.mapIndexed { i, pair -> 
            EventOption(id = "mock_mc_$i", label = pair.first, pool = 500.0) 
        }
        val event = Event(
            id = "mock_${nextMockId++}",
            title = title,
            titleHe = titleHe,
            description = description,
            tag = tag,
            eventType = EventType.MULTI_CHOICE,
            options = options
        )
        _events.update { it + event }
        return event
    }
}
