package com.neviim.market.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.neviim.market.data.model.UserProfile
import com.neviim.market.data.repository.MarketRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountViewModel : ViewModel() {

    val userProfile: StateFlow<UserProfile> = MarketRepository.userProfile

    private val _refillMessage = MutableStateFlow<String?>(null)
    val refillMessage: StateFlow<String?> = _refillMessage.asStateFlow()

    fun refillBalance() {
        MarketRepository.refillBalance()
        _refillMessage.value = "refilled"
    }

    fun clearMessage() {
        _refillMessage.value = null
    }
}
