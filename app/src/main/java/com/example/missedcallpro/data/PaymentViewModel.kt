package com.example.missedcallpro.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.missedcallpro.data.remote.BackendApi
import kotlinx.coroutines.launch

class PaymentViewModel(private val api: BackendApi) : ViewModel() {
    // UI State to track loading, data, and errors
    var uiState by mutableStateOf<PaymentUiState>(PaymentUiState.Loading)
        private set

    init {
        loadPlans()
    }

    fun loadPlans() {
        viewModelScope.launch {
            uiState = PaymentUiState.Loading
            try {
                val plans = api.getPlans()
                uiState = PaymentUiState.Success(plans)
            } catch (e: Exception) {
                uiState = PaymentUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }
}

// Simple state wrapper
sealed class PaymentUiState {
    object Loading : PaymentUiState()
    data class Success(val plans: List<PlanDto>) : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}

class PaymentViewModelFactory(private val api: BackendApi) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PaymentViewModel(api) as T
    }
}