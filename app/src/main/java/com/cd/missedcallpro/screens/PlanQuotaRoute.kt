package com.cd.missedcallpro.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cd.missedcallpro.App
import com.cd.missedcallpro.data.AppState
import com.cd.missedcallpro.data.AppStateStore
import com.cd.missedcallpro.data.GoogleRestoreItemReq
import com.cd.missedcallpro.data.GoogleRestoreReq
import com.cd.missedcallpro.data.PlanState
import com.cd.missedcallpro.data.SubscriptionDto
import com.cd.missedcallpro.data.billing.BillingRestoreHelper
import com.cd.missedcallpro.ui.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private sealed class LoadState {
    data object Loading : LoadState()
    data object Ready: LoadState()
    data class Error(val message: String) : LoadState()
}

@Composable
fun PlanQuotaRoute(
    store: AppStateStore,
    onOpenSmsTemplate: () -> Unit,
    onUpgrade: () -> Unit,
    onViewMyAccount: () -> Unit,
    onSignOut: () -> Unit,
    onOpenFilterList: () -> Unit,
    onOpenMissedCallRecord: () -> Unit,
    onOpenFormEditPage: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val api = app.container.api

    val uiState by store.state.collectAsStateWithLifecycle(
        initialValue = AppState.initial() // add an initial() factory if you don’t have one
    )

    var loadState by remember { mutableStateOf<LoadState>(LoadState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }

    suspend fun load(): LoadState = coroutineScope {
        return@coroutineScope try {

            // 1) Load from backend
            val sub = withContext(Dispatchers.IO) { api.getSubscription() }

            // 2) If already active paid, no restore
            val isBackendPaidActive = sub.status == SubscriptionDto.STATUS_ACTIVE && sub.plan_id != SubscriptionDto.PLAN_FREE

            if (!isBackendPaidActive) {

                // 3) Backend says free/inactive -> check Play purchases
                val billingRestore = BillingRestoreHelper(context)
                val restorePurchases = billingRestore.getActiveSubscriptions()

                if (restorePurchases.isNotEmpty()) {
                    api.restoreGoogleSubscription(
                        GoogleRestoreReq(
                            purchases = restorePurchases.map {
                                GoogleRestoreItemReq(
                                    product_id = it.productId,
                                    purchase_token = it.purchaseToken,
                                    package_name = null
                                )
                            }
                        )
                    )
                    // then re-fetch /subscriptions/me
                }

            }

            // Do network IO off main thread
            val (subResp, planResp) = withContext(Dispatchers.IO) {
                val subResp = api.getSubscription()
                val plansResp = api.getPlans()
                subResp to plansResp
            }
            val planDto = planResp.first { it.id == subResp.plan_id }

            val planState = PlanState(
                name = planDto.name,
                smsLimit = planDto.sms_limit,
                emailLimit = planDto.email_limit,
                can_edit_templates = planDto.can_edit_templates,
                currentPeriodStart = subResp.current_period_start,
                currentPeriodEnd = subResp.current_period_end,
                status = subResp.status
            )
            val updateTask = async {
                store.setPlan(planState)
                store.setQuotas(
                    smsUsed = subResp.sms.used,
                    emailUsed = subResp.email.used
                )
            }
            updateTask.await()

            LoadState.Ready
        } catch (e: HttpException) {
            val msg = "HTTP ${e.code()} loading subscription"
            Log.e("PlanQuotaRoute", msg, e)
            LoadState.Error(msg)
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown error loading subscription"
            Log.e("PlanQuotaRoute", msg, e)
            LoadState.Error(msg)
        }
    }

    LaunchedEffect(reloadKey) {
        loadState = LoadState.Loading
        loadState = load()
    }

    when (val s = loadState) {
        is LoadState.Loading -> {
            ScreenScaffold(title = "Plan & Quotas") { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        is LoadState.Error -> {
            ScreenScaffold(title = "Plan & Quotas") { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Failed to load subscription.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(s.message)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        reloadKey++
                    }) {
                        Text("Retry")
                    }
                }
            }
        }

        is LoadState.Ready -> {
            PlanQuotaScreen(
                state = uiState,
                onOpenSmsTemplate = onOpenSmsTemplate,
                onUpgrade = onUpgrade,
                onViewMyAccount = onViewMyAccount,
                onSignOut = onSignOut,
                onOpenFilterList = onOpenFilterList,
                onOpenMissedCallRecord = onOpenMissedCallRecord,
                onOpenFormEditPage = onOpenFormEditPage
            )
        }
    }
}
