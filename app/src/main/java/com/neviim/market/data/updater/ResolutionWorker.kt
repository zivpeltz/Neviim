package com.neviim.market.data.updater

import android.content.Context
import android.util.Log
import androidx.work.*
import com.neviim.market.data.model.UserPosition
import com.neviim.market.data.network.GammaApi
import com.neviim.market.data.repository.MarketRepository
import com.neviim.market.data.storage.UserDataStorage
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class ResolutionWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val api = GammaApi.create()
            // We need to resolve against the local data
            val loaded = UserDataStorage.load(context) ?: return Result.success()
            val profile = loaded.first
            var currentBalance = profile.balance
            val positions = loaded.second.toMutableList()

            var modified = false
            var wonCount = profile.wonBets
            var totalWinnings = profile.totalWinnings

            val iter = positions.iterator()
            while (iter.hasNext()) {
                val pos = iter.next()
                
                try {
                    // Fetch market state
                    val market = api.getMarketById(pos.eventId)
                    if (market.closed) {
                        modified = true
                        
                        // Check if the user's chosen outcome won.
                        // Polymarket resolves the winning outcome to price ~1.0
                        val prices = market.parsedOutcomePrices()
                        val outcomes = market.parsedOutcomes()
                        
                        // pos.optionId is like "239167_0"
                        val outcomeIndex = pos.optionId.substringAfterLast("_").toIntOrNull() ?: 0
                        val finalPrice = prices.getOrNull(outcomeIndex) ?: 0.0
                        
                        if (finalPrice > 0.95) {
                            // User won! 1 share pays out $1.00 fake shekels
                            val payout = pos.shares * 1.0
                            currentBalance += payout
                            totalWinnings += payout
                            wonCount++
                        }
                        
                        // Remove resolved position from active list
                        iter.remove()
                    }
                } catch (e: Exception) {
                    Log.e("ResolutionWorker", "Failed to check market ${pos.eventId}", e)
                }
                
                // Be gentle to the API
                delay(200)
            }

            if (modified) {
                val newProfile = profile.copy(
                    balance = currentBalance,
                    wonBets = wonCount,
                    totalWinnings = totalWinnings
                )
                UserDataStorage.save(context, newProfile, positions)
                
                // Instruct MarketRepository to reload from disk since we modified it behind its back,
                // or we could just inject it. But since MarketRepository is an object:
                MarketRepository.init(context)
            }
            
            return Result.success()
        } catch (e: Exception) {
            Log.e("ResolutionWorker", "Worker failed", e)
            return Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ResolutionWorker>(2, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("polymarket_resolution", ExistingPeriodicWorkPolicy.KEEP, request)
        }
        
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<ResolutionWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
                
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
