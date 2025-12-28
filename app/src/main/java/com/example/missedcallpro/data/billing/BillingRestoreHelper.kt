package com.example.missedcallpro.data.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class RestorePurchase(
    val productId: String,
    val purchaseToken: String
)

class BillingRestoreHelper(context: Context) {

    private val billingClient: BillingClient =
        BillingClient.newBuilder(context.applicationContext)
            .enablePendingPurchases()
            .setListener { _, _ -> /* not used for restore */ }
            .build()

    private suspend fun ensureConnected() {
        if (billingClient.isReady) return

        suspendCancellableCoroutine<Unit> { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(Unit)
                    } else {
                        cont.resumeWithException(
                            RuntimeException("Billing setup failed: ${result.debugMessage}")
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // No-op: we'll reconnect next time
                }
            })

            cont.invokeOnCancellation {
                // don't call endConnection here; BillingClient may be reused
            }
        }
    }

    /**
     * Returns current subscription purchases on device.
     * This does NOT guarantee they're "active" on Google's server,
     * but it's exactly what you need to attempt a backend restore.
     */
    suspend fun getActiveSubscriptions(): List<RestorePurchase> {
        ensureConnected()

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val result = billingClient.queryPurchasesAsync(params)

        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            throw RuntimeException(
                "queryPurchases failed: ${result.billingResult.debugMessage}"
            )
        }

        return result.purchasesList
            .orEmpty()
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .flatMap { purchase ->
                purchase.products.map { productId ->
                    RestorePurchase(
                        productId = productId,
                        purchaseToken = purchase.purchaseToken
                    )
                }
            }
            .distinctBy { it.purchaseToken }
    }

    fun close() {
        billingClient.endConnection()
    }
}
