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
    val outcomePrices: String? = null,
    val resolutionSource: String? = null
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
        val cleaned = jsonArrayString.trim()
        if (!cleaned.startsWith("[") || !cleaned.endsWith("]")) return emptyList()
        val inner = cleaned.substring(1, cleaned.length - 1).trim()
        if (inner.isEmpty()) return emptyList()
        
        // Polymarket arrays are simple: ["Yes", "No"] or ["0.4", "0.6"]
        return inner.split(",").map { 
            it.trim().removePrefix("\"").removeSuffix("\"").trim() 
        }
    }
}

@JsonClass(generateAdapter = true)
data class GammaTag(
    val id: String,
    val label: String
)
