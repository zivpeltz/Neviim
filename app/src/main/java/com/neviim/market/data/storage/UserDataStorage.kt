package com.neviim.market.data.storage

import android.content.Context
import com.neviim.market.data.model.TradeSide
import com.neviim.market.data.model.UserPosition
import com.neviim.market.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object UserDataStorage {
    private const val FILE_NAME = "user_data.json"

    // ── Onboarding ────────────────────────────────────────────────────

    /** Returns true if the user has already completed onboarding. */
    fun loadOnboardingState(context: Context): Boolean {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return false
        return try {
            JSONObject(file.readText()).optBoolean("hasOnboarded", false)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Saves the chosen display name and marks onboarding as complete.
     * Creates a fresh profile with 100k SP if one doesn't already exist.
     */
    fun saveOnboardingComplete(context: Context, displayName: String) {
        val file = File(context.filesDir, FILE_NAME)
        val root = if (file.exists()) {
            try { JSONObject(file.readText()) } catch (e: Exception) { JSONObject() }
        } else {
            JSONObject()
        }

        // Create fresh profile
        val jsonProfile = JSONObject().apply {
            put("username", displayName.trim())
            put("balance", 100_000.0)
            put("totalWinnings", 0.0)
            put("totalBets", 0)
            put("wonBets", 0)
        }
        root.put("profile", jsonProfile)
        root.put("hasOnboarded", true)
        if (!root.has("positions")) root.put("positions", JSONArray())

        file.writeText(root.toString())
    }

    // ── Full save / load ─────────────────────────────────────────────

    fun save(
        context: Context,
        profile: UserProfile,
        positions: List<UserPosition>,
        resolvedPositions: List<UserPosition> = emptyList()
    ) {
        val root = JSONObject()

        val jsonProfile = JSONObject().apply {
            put("username", profile.username)
            put("balance", profile.balance)
            put("totalWinnings", profile.totalWinnings)
            put("totalBets", profile.totalBets)
            put("wonBets", profile.wonBets)
        }
        root.put("profile", jsonProfile)
        root.put("hasOnboarded", true)

        fun serializePositions(list: List<UserPosition>): JSONArray {
            val arr = JSONArray()
            list.forEach { pos ->
                arr.put(JSONObject().apply {
                    put("id", pos.id)
                    put("eventId", pos.eventId)
                    put("marketId", pos.marketId)
                    put("eventTitle", pos.eventTitle)
                    put("eventTitleHe", pos.eventTitleHe)
                    put("optionId", pos.optionId)
                    put("optionLabel", pos.optionLabel)
                    put("side", pos.side.name)
                    put("shares", pos.shares)
                    put("entryPrice", pos.entryPrice)
                    put("amountPaid", pos.amountPaid)
                    put("timestamp", pos.timestamp)
                    pos.resolvedAt?.let { put("resolvedAt", it) }
                    pos.won?.let { put("won", it) }
                })
            }
            return arr
        }

        root.put("positions", serializePositions(positions))
        root.put("resolvedPositions", serializePositions(resolvedPositions))

        val file = File(context.filesDir, FILE_NAME)
        file.writeText(root.toString())
    }

    /**
     * Returns (profile, openPositions, resolvedPositions), or null if no save file exists.
     */
    fun load(context: Context): Triple<UserProfile, List<UserPosition>, List<UserPosition>>? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return try {
            val root = JSONObject(file.readText())

            val jp = root.getJSONObject("profile")
            val profile = UserProfile(
                username = jp.optString("username", "Prophet"),
                balance = jp.optDouble("balance", 100_000.0),
                totalWinnings = jp.optDouble("totalWinnings", 0.0),
                totalBets = jp.optInt("totalBets", 0),
                wonBets = jp.optInt("wonBets", 0)
            )

            fun parsePositions(jarr: JSONArray): List<UserPosition> {
                val list = mutableListOf<UserPosition>()
                for (i in 0 until jarr.length()) {
                    val jpos = jarr.getJSONObject(i)
                    list.add(UserPosition(
                        id = jpos.getString("id"),
                        eventId = jpos.getString("eventId"),
                        marketId = jpos.optString("marketId", jpos.getString("eventId")),
                        eventTitle = jpos.getString("eventTitle"),
                        eventTitleHe = jpos.optString("eventTitleHe", ""),
                        optionId = jpos.optString("optionId", ""),
                        optionLabel = jpos.optString("optionLabel", ""),
                        side = TradeSide.valueOf(jpos.getString("side")),
                        shares = jpos.getDouble("shares"),
                        entryPrice = jpos.getDouble("entryPrice"),
                        amountPaid = jpos.getDouble("amountPaid"),
                        timestamp = jpos.optLong("timestamp", System.currentTimeMillis()),
                        resolvedAt = jpos.optLong("resolvedAt", 0L).takeIf { it > 0 },
                        won = if (jpos.has("won")) jpos.optBoolean("won") else null
                    ))
                }
                return list
            }

            val openPositions = parsePositions(root.getJSONArray("positions"))
            val resolvedPositions = if (root.has("resolvedPositions"))
                parsePositions(root.getJSONArray("resolvedPositions"))
            else emptyList()

            Triple(profile, openPositions, resolvedPositions)
        } catch (e: Exception) {
            null
        }
    }
}
