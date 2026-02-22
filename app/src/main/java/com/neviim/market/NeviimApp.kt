package com.neviim.market

import android.app.Application
import com.neviim.market.data.repository.MarketRepository
import com.neviim.market.data.updater.ResolutionWorker

class NeviimApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MarketRepository.init(this)
        ResolutionWorker.enqueue(this)
    }
}
