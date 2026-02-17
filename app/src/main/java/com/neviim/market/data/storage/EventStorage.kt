package com.neviim.market.data.storage

import android.content.Context
import com.neviim.market.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persists events as JSON to internal storage.
 * No external dependencies — uses Android's built-in org.json.
 */
object EventStorage {

    private const val FILE_NAME = "events.json"

    // ── Save ────────────────────────────────────────────────────────────

    fun save(context: Context, events: List<Event>) {
        val json = JSONArray()
        events.forEach { json.put(eventToJson(it)) }
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(json.toString())
    }

    // ── Load ────────────────────────────────────────────────────────────

    fun load(context: Context): List<Event>? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return try {
            val text = file.readText()
            val arr = JSONArray(text)
            val events = mutableListOf<Event>()
            for (i in 0 until arr.length()) {
                events.add(jsonToEvent(arr.getJSONObject(i)))
            }
            events
        } catch (e: Exception) {
            null // Corrupt file — fall back to seed data
        }
    }

    // ── Event ↔ JSON ────────────────────────────────────────────────────

    private fun eventToJson(event: Event): JSONObject = JSONObject().apply {
        put("id", event.id)
        put("title", event.title)
        put("titleHe", event.titleHe)
        put("description", event.description)
        put("descriptionHe", event.descriptionHe)
        put("tag", event.tag.name)
        put("eventType", event.eventType.name)
        put("options", JSONArray().apply {
            event.options.forEach { put(optionToJson(it)) }
        })
        put("totalVolume", event.totalVolume)
        put("isResolved", event.isResolved)
        put("resolvedOptionId", event.resolvedOptionId ?: JSONObject.NULL)
        put("priceHistory", JSONArray().apply {
            event.priceHistory.forEach { put(pricePointToJson(it)) }
        })
        put("createdAt", event.createdAt)
        put("endDate", event.endDate ?: JSONObject.NULL)
        put("totalTraders", event.totalTraders)
    }

    private fun jsonToEvent(json: JSONObject): Event {
        val optionsArr = json.getJSONArray("options")
        val options = mutableListOf<EventOption>()
        for (i in 0 until optionsArr.length()) {
            options.add(jsonToOption(optionsArr.getJSONObject(i)))
        }

        val historyArr = json.getJSONArray("priceHistory")
        val history = mutableListOf<PricePoint>()
        for (i in 0 until historyArr.length()) {
            history.add(jsonToPricePoint(historyArr.getJSONObject(i)))
        }

        return Event(
            id = json.getString("id"),
            title = json.getString("title"),
            titleHe = json.getString("titleHe"),
            description = json.optString("description", ""),
            descriptionHe = json.optString("descriptionHe", ""),
            tag = EventTag.valueOf(json.getString("tag")),
            eventType = EventType.valueOf(json.optString("eventType", "BINARY")),
            options = options,
            totalVolume = json.getDouble("totalVolume"),
            isResolved = json.getBoolean("isResolved"),
            resolvedOptionId = if (json.isNull("resolvedOptionId")) null
                else json.optString("resolvedOptionId", "").takeIf { it.isNotEmpty() },
            priceHistory = history,
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            endDate = if (json.isNull("endDate")) null else json.optLong("endDate"),
            totalTraders = json.optInt("totalTraders", 0)
        )
    }

    // ── EventOption ↔ JSON ──────────────────────────────────────────────

    private fun optionToJson(option: EventOption): JSONObject = JSONObject().apply {
        put("id", option.id)
        put("label", option.label)
        put("labelHe", option.labelHe)
        put("pool", option.pool)
    }

    private fun jsonToOption(json: JSONObject): EventOption = EventOption(
        id = json.getString("id"),
        label = json.getString("label"),
        labelHe = json.optString("labelHe", ""),
        pool = json.getDouble("pool")
    )

    // ── PricePoint ↔ JSON ───────────────────────────────────────────────

    private fun pricePointToJson(point: PricePoint): JSONObject = JSONObject().apply {
        put("timestamp", point.timestamp)
        put("yesPrice", point.yesPrice)
    }

    private fun jsonToPricePoint(json: JSONObject): PricePoint = PricePoint(
        timestamp = json.getLong("timestamp"),
        yesPrice = json.getDouble("yesPrice")
    )
}
