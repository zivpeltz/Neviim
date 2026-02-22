package com.neviim.market

import com.neviim.market.data.network.GammaEvent
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Test
import java.io.File

class MoshiTest {
    @Test
    fun testParsing() {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val type = Types.newParameterizedType(List::class.java, GammaEvent::class.java)
        val adapter = moshi.adapter<List<GammaEvent>>(type)
        
        val json = File("/tmp/gamma_events.json").readText()
        try {
            val events = adapter.fromJson(json)
            if (events == null || events.isEmpty()) {
                throw IllegalStateException("Events list is empty!")
            }
            val validMarkets = events.flatMap { it.markets }.filter { 
                it.volumeNum >= 10 && it.parsedOutcomes().isNotEmpty() 
            }
            if (validMarkets.isEmpty()) {
                throw IllegalStateException("No valid markets found! All outcomes parsed as empty.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
