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

    fun save(context: Context, profile: UserProfile, positions: List<UserPosition>) {
        val root = JSONObject()

        val jsonProfile = JSONObject().apply {
            put("username", profile.username)
            put("balance", profile.balance)
            put("totalWinnings", profile.totalWinnings)
            put("totalBets", profile.totalBets)
            put("wonBets", profile.wonBets)
        }
        root.put("profile", jsonProfile)

        val jsonPositions = JSONArray()
        positions.forEach { pos ->
            jsonPositions.put(JSONObject().apply {
                put("id", pos.id)
                put("eventId", pos.eventId)
                put("eventTitle", pos.eventTitle)
                put("eventTitleHe", pos.eventTitleHe)
                put("optionId", pos.optionId)
                put("optionLabel", pos.optionLabel)
                put("side", pos.side.name)
                put("shares", pos.shares)
                put("entryPrice", pos.entryPrice)
                put("amountPaid", pos.amountPaid)
                put("timestamp", pos.timestamp)
            })
        }
        root.put("positions", jsonPositions)

        val file = File(context.filesDir, FILE_NAME)
        file.writeText(root.toString())
    }

    fun load(context: Context): Pair<UserProfile, List<UserPosition>>? {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return null
        return try {
            val text = file.readText()
            val root = JSONObject(text)
            
            val jp = root.getJSONObject("profile")
            val profile = UserProfile(
                username = jp.optString("username", "Prophet"),
                balance = jp.optDouble("balance", 5000.0),
                totalWinnings = jp.optDouble("totalWinnings", 0.0),
                totalBets = jp.optInt("totalBets", 0),
                wonBets = jp.optInt("wonBets", 0)
            )

            val jarr = root.getJSONArray("positions")
            val positions = mutableListOf<UserPosition>()
            for (i in 0 until jarr.length()) {
                val jpos = jarr.getJSONObject(i)
                positions.add(UserPosition(
                    id = jpos.getString("id"),
                    eventId = jpos.getString("eventId"),
                    eventTitle = jpos.getString("eventTitle"),
                    eventTitleHe = jpos.optString("eventTitleHe", ""),
                    optionId = jpos.optString("optionId", ""),
                    optionLabel = jpos.optString("optionLabel", ""),
                    side = TradeSide.valueOf(jpos.getString("side")),
                    shares = jpos.getDouble("shares"),
                    entryPrice = jpos.getDouble("entryPrice"),
                    amountPaid = jpos.getDouble("amountPaid"),
                    timestamp = jpos.optLong("timestamp", System.currentTimeMillis())
                ))
            }

            Pair(profile, positions)
        } catch (e: Exception) {
            null
        }
    }
}
