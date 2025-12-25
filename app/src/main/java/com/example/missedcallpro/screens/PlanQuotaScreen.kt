package com.example.missedcallpro.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.missedcallpro.auth.GoogleAuthClient
import com.example.missedcallpro.data.AppState
import com.example.missedcallpro.data.PlanTier
import com.example.missedcallpro.ui.QuotaRow
import com.example.missedcallpro.ui.ScreenScaffold
import kotlinx.coroutines.launch

@Composable
fun PlanQuotaScreen(
    state: AppState,
    onOpenSmsTemplate: () -> Unit,
    onOpenEmailTemplate: () -> Unit,
    onUpgrade: () -> Unit,
    onResetMonth: () -> Unit,
    onSignOut: () -> Unit
) {
    val plan = state.plan
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = GoogleAuthClient(context)

    ScreenScaffold(
        title = "Plan & Quotas",
        actions = {
            TextButton(onClick = {

                scope.launch {
                    authRepo.signOutUser()
                    onSignOut()
                }

            }) {
                Text("Sign out")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Current Plan", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(plan.display, style = MaterialTheme.typography.headlineSmall)

                    if (plan == PlanTier.FREE) {
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) {
                            Text("Upgrade →")
                        }
                    } else {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) {
                            Text("Change Plan")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Quotas", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            QuotaRow(
                label = "SMS notifications",
                used = state.quotas.smsUsed,
                limit = plan.smsLimit,
                onClick = onOpenSmsTemplate
            )
            QuotaRow(
                label = "Email notifications",
                used = state.quotas.emailUsed,
                limit = plan.emailLimit,
                onClick = onOpenEmailTemplate
            )

            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onResetMonth, modifier = Modifier.fillMaxWidth()) {
                Text("Reset month (demo)")
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Demo note: quotas are local only. Next step is backend-tracked quotas + Twilio sending.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
