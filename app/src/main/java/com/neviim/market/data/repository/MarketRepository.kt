package com.neviim.market.data.repository

import android.content.Context
import android.util.Log
import com.neviim.market.data.model.*
import com.neviim.market.data.network.GammaApi
import com.neviim.market.data.storage.UserDataStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    fun init(context: Context) {
        appContext = context.applicationContext
        val loaded = UserDataStorage.load(appContext)
        if (loaded != null) {
            _userProfile.value = loaded.first
            _positions.value = loaded.second
        }
        refreshEvents()
    }

    private fun persistUserData() {
        if (::appContext.isInitialized) {
            UserDataStorage.save(appContext, _userProfile.value, _positions.value)
        }
    }

    fun refreshEvents() {
        scope.launch {
            try {
                val gammaEvents = gammaApi.getEvents(active = true, limit = 50)
                val mapped = mutableListOf<Event>()

                for (ge in gammaEvents) {
                    for (gm in ge.markets) {
                        if (gm.volumeNum < 10) continue // filter out dead markets

                        val outcomes = gm.parsedOutcomes()
                        val prices = gm.parsedOutcomePrices()
                        if (outcomes.size != prices.size || outcomes.isEmpty()) continue

                        val isBinary = outcomes.size == 2 && outcomes[0].equals("Yes", true)
                        val type = if (isBinary) EventType.BINARY else EventType.MULTI_CHOICE

                        val tag = when (ge.tags.firstOrNull()?.label?.lowercase()) {
                            "politics" -> EventTag.POLITICS
                            "crypto" -> EventTag.CRYPTO
                            "pop culture" -> EventTag.POP_CULTURE
                            "science" -> EventTag.SCIENCE
                            else -> EventTag.SPORTS
                        }

                        val options = outcomes.zip(prices).mapIndexed { i, (label, price) ->
                            EventOption(
                                id = "${gm.id}_$i",
                                label = label,
                                labelHe = label,
                                // Make totalPool around 1000 so the ui logic works.
                                // price = pool / totalPool => pool = price * ~1000.
                                // (To be safe against 0, add minimal base pool to all)
                                pool = (price * 1000.0).coerceAtLeast(1.0)
                            )
                        }

                        mapped.add(
                            Event(
                                id = gm.id,
                                title = gm.question.ifBlank { ge.title },
                                titleHe = gm.question.ifBlank { ge.title },
                                description = ge.description ?: "",
                                tag = tag,
                                eventType = type,
                                options = options,
                                totalVolume = gm.volumeNum,
                                isResolved = gm.closed,
                                image = ge.image
                            )
                        )
                    }
                }
                _events.value = mapped
            } catch (e: Exception) {
                Log.e("MarketRepository", "Failed to fetch events", e)
            }
        }
    }

    fun getEvent(eventId: String): Event? = _events.value.find { it.id == eventId }

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

        val position = UserPosition(
            eventId = eventId,
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

        val position = UserPosition(
            eventId = eventId,
            eventTitle = event.title,
            eventTitleHe = event.titleHe,
            optionId = optionId,
            optionLabel = option.label,
            side = TradeSide.YES, // Treating multi-choice buys as a YES on that specific candidate
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
