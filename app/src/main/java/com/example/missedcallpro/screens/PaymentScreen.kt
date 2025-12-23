package com.example.missedcallpro.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.missedcallpro.data.PlanTier
import com.example.missedcallpro.ui.ScreenScaffold

@Composable
fun PaymentScreen(
    currentPlan: PlanTier,
    onBack: () -> Unit,
    onSelectPlan: (PlanTier) -> Unit
) {
    ScreenScaffold(title = "Upgrade", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text("Choose a plan", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Limits are per month. Templates editable on paid plans.")
            Spacer(Modifier.height(16.dp))

            PlanCard(
                title = "Free",
                price = "$0",
                sms = 15,
                email = 15,
                editable = false,
                selected = currentPlan == PlanTier.FREE,
                onClick = { onSelectPlan(PlanTier.FREE) }
            )

            Spacer(Modifier.height(12.dp))
            PlanCard(
                title = "Starter",
                price = "$5 / month",
                sms = 100,
                email = 100,
                editable = true,
                selected = currentPlan == PlanTier.STARTER,
                onClick = { onSelectPlan(PlanTier.STARTER) }
            )

            Spacer(Modifier.height(12.dp))
            PlanCard(
                title = "Pro",
                price = "$15 / month",
                sms = 300,
                email = 300,
                editable = true,
                selected = currentPlan == PlanTier.PRO,
                onClick = { onSelectPlan(PlanTier.PRO) }
            )

            Spacer(Modifier.height(12.dp))
            PlanCard(
                title = "Pro+",
                price = "$49 / month",
                sms = 1000,
                email = 1000,
                editable = true,
                selected = currentPlan == PlanTier.PRO_PLUS,
                onClick = { onSelectPlan(PlanTier.PRO_PLUS) }
            )

            Spacer(Modifier.height(18.dp))
            Text(
                "Demo note: plan selection is local-only. Next step: Google Play Billing subscription.",
                style = MaterialTheme.typography.bodySmall
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
