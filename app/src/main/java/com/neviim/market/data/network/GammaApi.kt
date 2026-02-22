package com.neviim.market.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GammaApi {

    @GET("events")
    suspend fun getEvents(
        @Query("limit") limit: Int = 100,
        @Query("active") active: Boolean? = null,
        @Query("closed") closed: Boolean? = null,
        @Query("archived") archived: Boolean? = null,
        @Query("offset") offset: Int = 0,
        @Query("order") order: String? = null,
        @Query("ascending") ascending: Boolean? = null
    ): List<GammaEvent>

    @GET("events/{id}")
    suspend fun getEventById(
        @Path("id") eventId: String
    ): GammaEvent

    @GET("markets/{id}")
    suspend fun getMarketById(
        @Path("id") marketId: String
    ): GammaMarket

    /**
     * Fetches price history for a market.
     * @param marketId  the market's conditionId (CLOB id)
     * @param interval  e.g. "1d", "1w", "1m", "all"
     * @param fidelity  data point granularity in minutes (60 = hourly)
     */
    @GET("prices-history")
    suspend fun getPricesHistory(
        @Query("market") marketId: String,
        @Query("interval") interval: String = "1w",
        @Query("fidelity") fidelity: Int = 60
    ): GammaHistoryResponse

    companion object {
        private const val BASE_URL = "https://gamma-api.polymarket.com/"

        fun create(): GammaApi {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(GammaApi::class.java)
        }
    }
}

@JsonClass(generateAdapter = true)
data class GammaHistoryResponse(
    val history: List<GammaHistoryPoint> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GammaHistoryPoint(
    @Json(name = "t") val timestamp: Long = 0L,    // unix seconds
    @Json(name = "p") val price: Double = 0.0       // probability 0-1
)
