package com.neviim.market.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GammaEvent(
    val id: String,
    val title: String,
    val description: String? = null,
    val image: String? = null,
    val active: Boolean = false,
    val closed: Boolean = false,
    val volume: Double = 0.0,
    val endDateIso: String? = null,
    val markets: List<GammaMarket> = emptyList(),
    val tags: List<GammaTag> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GammaMarket(
    val id: String,
    val question: String,
    val active: Boolean = false,
    val closed: Boolean = false,
    val volumeNum: Double = 0.0,
    val liquidityNum: Double = 0.0,
    // Note: Polymarket API often returns outcomes and outcomePrices as JSON string arrays.
    // e.g. "[\"Yes\", \"No\"]"
    val outcomes: String? = null,
    val outcomePrices: String? = null
) {
    /** Helper to parse the JSON string array of outcomes. */
    fun parsedOutcomes(): List<String> {
        val raw = outcomes ?: return emptyList()
        return parseStringArray(raw)
    }

    /** Helper to parse the JSON string array of prices. */
    fun parsedOutcomePrices(): List<Double> {
        val raw = outcomePrices ?: return emptyList()
        return parseStringArray(raw).mapNotNull { it.toDoubleOrNull() }
    }

    private fun parseStringArray(jsonArrayString: String): List<String> {
        return try {
            val cleaned = jsonArrayString.trim().removePrefix("[").removeSuffix("]")
            // Split by comma, but handle quotes inside the array.
            // A simple naive split might fail if commas are inside strings,
            // but for Polymarket outcomes, they are simple strings.
            org.json.JSONArray(jsonArrayString).let { jsonArray ->
                List(jsonArray.length()) { i -> jsonArray.getString(i) }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@JsonClass(generateAdapter = true)
data class GammaTag(
    val id: String,
    val label: String
)
