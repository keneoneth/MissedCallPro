package com.example.missedcallpro.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.missedcallpro.App
import com.example.missedcallpro.data.AppState
import com.example.missedcallpro.data.AppStateStore
import com.example.missedcallpro.data.PlanTier
import com.example.missedcallpro.data.Quotas
import com.example.missedcallpro.ui.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private sealed class LoadState {
    data object Loading : LoadState()
    data class Ready(val screenState: AppState) : LoadState()
    data class Error(val message: String) : LoadState()
}

@Composable
fun PlanQuotaRoute(
    state: AppState,
    store: AppStateStore,
    onOpenSmsTemplate: () -> Unit,
    onOpenEmailTemplate: () -> Unit,
    onUpgrade: () -> Unit,
    onViewMyAccount: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val api = app.container.api

    var loadState by remember { mutableStateOf<LoadState>(LoadState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }

    suspend fun load(): LoadState = coroutineScope {
        return@coroutineScope try {
            // Do network IO off main thread
            val (subResp, planResp) = withContext(Dispatchers.IO) {
                val subResp = api.getSubscription()
                val plansResp = api.getPlans()
                subResp to plansResp
            }
            val planDto = planResp.first { it.id == subResp.plan_id }

            val planTier = PlanTier(
                name = planDto.name,
                smsLimit = planDto.sms_limit,
                emailLimit = planDto.email_limit,
                can_edit_templates = planDto.can_edit_templates,
                currentPeriodStart = subResp.current_period_start,
                currentPeriodEnd = subResp.current_period_end,
                status = subResp.status
            )
            val updateTask = async {
                store.setPlan(planTier)
                store.setQuotas(
                    smsUsed = subResp.sms.used,
                    emailUsed = subResp.email.used
                )
            }
            updateTask.await()

            val newState = state.copy(
                plan = planTier,
                quotas = Quotas(
                    smsUsed = subResp.sms.used,
                    emailUsed = subResp.email.used
                )
            )

            LoadState.Ready(newState)
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
                    verticalArrangement = Arrangement.Center
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
                state = s.screenState,
                onOpenSmsTemplate = onOpenSmsTemplate,
                onOpenEmailTemplate = onOpenEmailTemplate,
                onUpgrade = onUpgrade,
                onViewMyAccount = onViewMyAccount,
                onSignOut = onSignOut
            )
        }
    }
}
