package com.example.missedcallpro.data.billing

import android.app.Activity
import com.android.billingclient.api.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PlayBillingManagerImpl(
    private val billingClient: BillingClient
) : PlayBillingManager {

    companion object {
        fun create(activity: Activity): PlayBillingManagerImpl {
            val client = BillingClient.newBuilder(activity.applicationContext)
                .setListener { _, _ -> /* not used on main client */ }
                .enablePendingPurchases()
                .build()
            return PlayBillingManagerImpl(client)
        }
    }

    private suspend fun ensureConnected() {
        if (billingClient.isReady) return
        suspendCancellableCoroutine<Unit> { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(Unit)
                    } else {
                        cont.resumeWithException(RuntimeException("Billing setup failed: ${result.debugMessage}"))
                    }
                }
                override fun onBillingServiceDisconnected() {
                    // reconnect next time
                }
            })
        }
    }

    private suspend fun queryProductDetails(productId: String): ProductDetails {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        val resp = billingClient.queryProductDetails(params)
        val details = resp.productDetailsList?.firstOrNull()
        return details ?: throw RuntimeException("Product not found in Play: $productId")
    }

    /**
     * Callback-based query purchases (works without KTX).
     * We only use PURCHASED items.
     */
    private suspend fun queryCurrentSubsPurchases(): List<Purchase> {
        ensureConnected()

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        return suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED })
                } else {
                    cont.resumeWithException(RuntimeException("queryPurchases failed: ${billingResult.debugMessage}"))
                }
            }
        }
    }

    override suspend fun getCurrentSubscriptionPurchaseToken(): String? {
        val purchases = queryCurrentSubsPurchases()
        // Usually one active sub per app, pick the most recent
        return purchases.maxByOrNull { it.purchaseTime }?.purchaseToken
    }

    private fun pickOfferToken(details: ProductDetails): String {
        return details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: throw RuntimeException("No subscription offers for ${details.productId}")
    }

    private suspend fun launchAndWaitForPurchase(
        activity: Activity,
        details: ProductDetails,
        updateParams: BillingFlowParams.SubscriptionUpdateParams? = null
    ): Purchase {
        val offerToken = pickOfferToken(details)

        val flowBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )

        if (updateParams != null) {
            flowBuilder.setSubscriptionUpdateParams(updateParams)
        }

        val flowParams = flowBuilder.build()

        val purchasesFlow = callbackFlow<Purchase> {
            val listener = PurchasesUpdatedListener { result, purchases ->
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        val p = purchases?.firstOrNull()
                        if (p != null) trySend(p)
                        close()
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> close(CancellationException("User canceled"))
                    else -> close(RuntimeException("Purchase failed: ${result.debugMessage}"))
                }
            }

            val tmpClient = BillingClient.newBuilder(activity.applicationContext)
                .setListener(listener)
                .enablePendingPurchases()
                .build()

            tmpClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        close(RuntimeException("Billing setup failed: ${result.debugMessage}"))
                        return
                    }
                    val launchResult = tmpClient.launchBillingFlow(activity, flowParams)
                    if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        close(RuntimeException("Launch billing failed: ${launchResult.debugMessage}"))
                    }
                }
                override fun onBillingServiceDisconnected() {}
            })

            awaitClose { tmpClient.endConnection() }
        }

        return purchasesFlow.first()
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val ackParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        val result = billingClient.acknowledgePurchase(ackParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            throw RuntimeException("Acknowledge failed: ${result.debugMessage}")
        }
    }

    override suspend fun purchaseSubscription(
        activity: Activity,
        productId: String,
        mode: SubscriptionChangeMode
    ): PurchaseResult {
        ensureConnected()

        val details = queryProductDetails(productId)

        // Determine old token for upgrade/downgrade flows
        val oldToken = when (mode) {
            SubscriptionChangeMode.NEW -> null
            SubscriptionChangeMode.UPGRADE_IMMEDIATE,
            SubscriptionChangeMode.DOWNGRADE_DEFERRED -> getCurrentSubscriptionPurchaseToken()
        }

        val updateParams = if (oldToken != null) {
            val replacementMode = when (mode) {
                SubscriptionChangeMode.UPGRADE_IMMEDIATE ->
                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
                SubscriptionChangeMode.DOWNGRADE_DEFERRED ->
                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED
                else -> null
            }

            if (replacementMode != null) {
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(oldToken)
                    .setSubscriptionReplacementMode(replacementMode)
                    .build()
            } else null
        } else null

        val purchase = launchAndWaitForPurchase(activity, details, updateParams)

        // sanity
        if (!purchase.products.contains(productId)) {
            throw RuntimeException("Purchased product mismatch")
        }

        // Acknowledge after purchase UI success
        acknowledgeIfNeeded(purchase)

        // ✅ Google Play handles the correct charge/proration. You do not compute it.
        return PurchaseResult(productId = productId, purchaseToken = purchase.purchaseToken)
    }

    override fun close() {
        billingClient.endConnection()
    }
}
