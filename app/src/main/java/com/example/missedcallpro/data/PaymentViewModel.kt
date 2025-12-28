package com.example.missedcallpro.data

import android.app.Activity
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.missedcallpro.data.billing.PlayBillingManager
import com.example.missedcallpro.data.billing.SubscriptionChangeMode
import com.example.missedcallpro.data.remote.BackendApi
import kotlinx.coroutines.launch

class PaymentViewModel(
    private val api: BackendApi,
    private val billing: PlayBillingManager
) : ViewModel() {

    var uiState by mutableStateOf<PaymentUiState>(PaymentUiState.Loading)
        private set

    var purchaseState by mutableStateOf<PurchaseState>(PurchaseState.Idle)
        private set

    init { loadPlans() }

    fun loadPlans() {
        viewModelScope.launch {
            uiState = PaymentUiState.Loading
            try {
                val plans = api.getPlans()
                uiState = PaymentUiState.Success(plans)
            } catch (e: Exception) {
                uiState = PaymentUiState.Error(e.message ?: "Failed to load plans")
            }
        }
    }

    fun subscribeProduct(activity: Activity, plan: PlanDto) {
        if (plan.play_product_id.isNullOrBlank()) {
            purchaseState = PurchaseState.Error("Missing Play product id")
            return
        }

        purchaseState = PurchaseState.Purchasing

        viewModelScope.launch {
            try {
                val mode =
                    if (plan.id == SubscriptionDto.PLAN_FREE)
                        SubscriptionChangeMode.NEW
                    else
                        SubscriptionChangeMode.UPGRADE_IMMEDIATE
                val purchase = billing.purchaseSubscription(activity, plan.play_product_id, mode)

                api.confirmGoogleSubscription(
                    GoogleConfirmReq(
                        product_id = purchase.productId,
                        purchase_token = purchase.purchaseToken,
                        package_name = null
                    )
                )

                purchaseState = PurchaseState.Success
            } catch (e: Exception) {
                purchaseState = PurchaseState.Error(e.message ?: "Subscription failed")
            }
        }
    }

    fun clearPurchaseError() {
        if (purchaseState is PurchaseState.Error) purchaseState = PurchaseState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        billing.close()
    }
}

// UI state (unchanged)
sealed class PaymentUiState {
    object Loading : PaymentUiState()
    data class Success(val plans: List<PlanDto>) : PaymentUiState()
    data class Error(val message: String) : PaymentUiState()
}

// purchase state (simplified since PlanQuotaScreen refreshes)
sealed class PurchaseState {
    object Idle : PurchaseState()
    object Purchasing : PurchaseState()
    object Success : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}

@Suppress("UNCHECKED_CAST")
class PaymentViewModelFactory(
    private val api: BackendApi,
    private val billing: PlayBillingManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PaymentViewModel(api, billing) as T
    }
}
