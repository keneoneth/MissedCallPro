package com.example.missedcallpro.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.missedcallpro.App
import com.example.missedcallpro.data.AppState
import com.example.missedcallpro.data.PaymentUiState
import com.example.missedcallpro.data.PaymentViewModel
import com.example.missedcallpro.data.PaymentViewModelFactory
import com.example.missedcallpro.data.PlanDto
import com.example.missedcallpro.data.PlanTier
import com.example.missedcallpro.ui.ScreenScaffold


private fun formatPrice(priceCents: Int, currency: String, period: String): String {
    if (priceCents <= 0) return "$0"
    val dollars = priceCents / 100
    return "$$dollars / $period" // keep simple for now
}

@Composable
fun PaymentScreen(
    state: AppState,
    currentPlan: PlanTier,
    onBack: () -> Unit,
    onSelectPlan: (PlanTier) -> Unit
) {
    val app = LocalContext.current.applicationContext as App
    // Initialize ViewModel with our custom factory
    val viewModel: PaymentViewModel = viewModel(
        factory = PaymentViewModelFactory(app.container.api)
    )

    ScreenScaffold(title = "Upgrade", onBack = onBack) { padding ->
        // Handle different UI states
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
                    state = state,
                    plans = uiState.plans,
                    currentPlan = currentPlan,
                    padding = padding,
                    onSelectPlan = onSelectPlan
                )
            }
        }
    }
}

@Composable
fun PlanList(
    state: AppState,
    plans: List<PlanDto>,
    currentPlan: PlanTier,
    padding: PaddingValues,
    onSelectPlan: (PlanTier) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Choose a plan",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Limits are per month. Templates editable on paid plans.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }


        items(plans.size ) { i ->
            val p = plans[i]
            PlanCard(
                title = p.name,
                price = formatPrice(p.price_cents, p.currency, p.period),
                sms = p.sms_limit,
                email = p.email_limit,
                editable = p.can_edit_templates,
                selected = currentPlan.name == p.name,
                onClick = {
                    val newPlan = PlanTier(p.name,p.sms_limit, p.email_limit, p.can_edit_templates)
                    onSelectPlan(newPlan)
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(if (selected) "Selected" else "Select")
            }
        }
    }
}
