package com.example.missedcallpro.data.billing

import android.app.Activity

data class PurchaseResult(
    val productId: String,
    val purchaseToken: String
)

enum class SubscriptionChangeMode {
    NEW,                 // no existing sub
    UPGRADE_IMMEDIATE,   // immediate with proration
    DOWNGRADE_DEFERRED   // optional: downgrade at next renewal
}

interface PlayBillingManager {
    suspend fun purchaseSubscription(
        activity: Activity,
        productId: String,
        mode: SubscriptionChangeMode
    ): PurchaseResult

    suspend fun getCurrentSubscriptionPurchaseToken(): String?

    fun close()
}
