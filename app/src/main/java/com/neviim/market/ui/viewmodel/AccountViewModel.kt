package com.neviim.market.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.neviim.market.data.model.UserPosition
import com.neviim.market.data.model.UserProfile
import com.neviim.market.data.repository.MarketRepository
import kotlinx.coroutines.flow.StateFlow

class AccountViewModel : ViewModel() {

    val userProfile: StateFlow<UserProfile> = MarketRepository.userProfile
    val positions: StateFlow<List<UserPosition>> = MarketRepository.positions
}
