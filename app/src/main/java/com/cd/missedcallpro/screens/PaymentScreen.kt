package com.cd.missedcallpro.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cd.missedcallpro.App
import com.cd.missedcallpro.data.PaymentUiState
import com.cd.missedcallpro.data.PaymentViewModel
import com.cd.missedcallpro.data.PaymentViewModelFactory
import com.cd.missedcallpro.data.PlanDto
import com.cd.missedcallpro.data.PlanState
import com.cd.missedcallpro.data.SubscriptionDto
import com.cd.missedcallpro.ui.ScreenScaffold

private fun formatPrice(priceCents: Int, currency: String, period: String): String {
    if (priceCents <= 0) return "$0"
    val dollars = priceCents / 100
    return "$$dollars / $period"
}

@Composable
fun PaymentScreen(
    currentPlan: PlanState,
    onBack: () -> Unit,
    onManageSubscription: () -> Unit
) {
    val context = LocalContext.current
    val activity = LocalContext.current as Activity
    val app = LocalContext.current.applicationContext as App
    val billing = remember { com.cd.missedcallpro.data.billing.PlayBillingManagerImpl.create(activity) }

    val viewModel: PaymentViewModel = viewModel(
        factory = PaymentViewModelFactory(app.container.api, billing)
    )

    LaunchedEffect(viewModel.purchaseState) {
        val ps = viewModel.purchaseState
        if (ps is com.cd.missedcallpro.data.PurchaseState.Error) {
            android.widget.Toast.makeText(context, ps.message, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearPurchaseError()
        }
        if (ps is com.cd.missedcallpro.data.PurchaseState.Success) {
            onBack()
        }
    }

    ScreenScaffold(title = "Upgrade",
        onBack = onBack) { padding ->
        when (val uiState = viewModel.uiState) {
            is PaymentUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PaymentUiState.Error -> {
                Text("Error: ${uiState.message}", color = Color.Red)
            }
            is PaymentUiState.Success -> {
                PlanList(
                    plans = uiState.plans,
                    currentPlan = currentPlan,
                    padding = padding,
                    onManageSubscription = onManageSubscription,
                    onSubscribe = { plan ->
                        viewModel.subscribeProduct(activity, plan)
                    }
                )
            }
        }
    }
}

@Composable
fun PlanList(
    plans: List<PlanDto>,
    currentPlan: PlanState,
    padding: PaddingValues,
    onManageSubscription: () -> Unit,
    onSubscribe: (PlanDto) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var pendingPlan by remember { mutableStateOf<PlanDto?>(null) }

    fun openConfirm(plan: PlanDto) {
        pendingPlan = plan
        showDialog = true
    }

    if (showDialog && pendingPlan != null) {
        val p = pendingPlan!!
        val isFree = p.id == SubscriptionDto.PLAN_FREE

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isFree) "Manage subscription" else "Confirm purchase") },
            text = {
                if (isFree) {
                    Text("To cancel or change your subscription, you’ll be taken to Google Play.")
                } else {
                    Text("Subscribe to ${p.name} for ${formatPrice(p.price_cents, p.currency, p.period)}?")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    if (isFree) {
                        onManageSubscription()
                    } else {
                        onSubscribe(p)
                    }
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(plans.size) { i ->
            val p = plans[i]
            val isCurrent = currentPlan.name == p.name
            val isFree = p.id == SubscriptionDto.PLAN_FREE
            val userIsFree = currentPlan.name.equals(SubscriptionDto.PLAN_FREE, ignoreCase = true)

            val buttonText = when {
                isCurrent -> "Selected"
                isFree && !userIsFree -> "Manage Subscription"
                else -> "Subscribe"
            }

            PlanCard(
                title = p.name,
                price = formatPrice(p.price_cents, p.currency, p.period),
                sms = p.sms_limit,
                email = p.email_limit,
                editable = p.can_edit_templates,
                selected = isCurrent,
                buttonText = buttonText,
                onClick = {
                    if (!isCurrent) openConfirm(p)
                }
            )
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    sms: Int,
    email: Int,
    editable: Boolean,
    selected: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(price, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text("SMS: $sms / month")
            Text("Email: $email / month")
            Text("Template editing: ${if (editable) "Yes" else "No"}")
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !selected
            ) {
                Text(buttonText)
            }
        }
    }
}
