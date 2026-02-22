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

/**
 * Background WorkManager task that runs every 2 hours to resolve any
 * open positions against the live Polymarket API.
 *
 * This is a safety net for markets that resolved while the app was in the
 * background (or the device was offline during the 30-second in-process
 * refresh cycle). The in-process [MarketRepository.resolvePositions] handles
 * the common case while the app is open.
 *
 * Resolution algorithm:
 *  1. For each open position, fetch its market via [GammaApi.getMarketById]
 *     using [UserPosition.marketId] (the actual CLOB market ID).
 *  2. If the market is closed, check outcome prices: a price >= 0.95 means
 *     that outcome won (Polymarket convention).
 *  3. Match the position's optionId index against the winning outcome.
 *  4. Credit payout (shares × 1.0 SP) and remove the position.
 */
class ResolutionWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val api = GammaApi.create()
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
                    // Use marketId (the actual CLOB market ID) — not eventId, which for
                    // multi-choice events is the parent event ID, not the specific market.
                    val market = api.getMarketById(pos.marketId)
                    if (market.closed) {
                        modified = true

                        // Polymarket resolves the winning outcome to price ~1.0.
                        val prices = market.parsedOutcomePrices()
                        val outcomes = market.parsedOutcomes()

                        // optionId format: "<marketId>_<outcomeIndex>" for binary,
                        // or just the marketId for multi-choice options.
                        val outcomeIndex = pos.optionId.substringAfterLast("_").toIntOrNull() ?: 0
                        val finalPrice = prices.getOrNull(outcomeIndex) ?: 0.0

                        if (finalPrice >= 0.95) {
                            val payout = pos.shares * 1.0
                            currentBalance += payout
                            totalWinnings += payout
                            wonCount++
                            Log.d("ResolutionWorker", "Win: ${pos.optionLabel} → +$payout SP")
                        } else {
                            Log.d("ResolutionWorker", "Loss: ${pos.optionLabel}")
                        }

                        iter.remove()
                    }
                } catch (e: Exception) {
                    Log.e("ResolutionWorker", "Failed to check market ${pos.marketId}", e)
                }

                // Be polite to the API — 200ms between calls
                delay(200)
            }

            if (modified) {
                val newProfile = profile.copy(
                    balance = currentBalance,
                    wonBets = wonCount,
                    totalWinnings = totalWinnings
                )
                UserDataStorage.save(context, newProfile, positions)
                // Reload in-memory state without triggering a full re-init
                MarketRepository.reloadProfile()
                Log.d("ResolutionWorker", "Saved resolved state. New balance: $currentBalance SP")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("ResolutionWorker", "Worker failed", e)
            return Result.retry()
        }
    }

    companion object {
        /**
         * Schedules a periodic resolution check every 2 hours.
         * Uses KEEP policy so repeated app opens don't pile up workers.
         */
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ResolutionWorker>(2, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "polymarket_resolution",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        /** Runs a one-shot resolution check immediately (e.g. triggered from Settings). */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<ResolutionWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
