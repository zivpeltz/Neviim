package com.neviim.market.data.network

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
        @Query("offset") offset: Int = 0
    ): List<GammaEvent>

    @GET("events/{id}")
    suspend fun getEventById(
        @Path("id") eventId: String
    ): GammaEvent

    @GET("markets/{id}")
    suspend fun getMarketById(
        @Path("id") marketId: String
    ): GammaMarket

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
