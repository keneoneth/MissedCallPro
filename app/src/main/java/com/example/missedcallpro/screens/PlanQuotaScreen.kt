package com.example.missedcallpro.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.missedcallpro.App
import com.example.missedcallpro.auth.GoogleAuthClient
import com.example.missedcallpro.data.AppState
import com.example.missedcallpro.data.PlanTier
import com.example.missedcallpro.ui.QuotaRow
import com.example.missedcallpro.ui.ScreenScaffold
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun PlanQuotaScreen(
    state: AppState,
    onOpenSmsTemplate: () -> Unit,
    onOpenEmailTemplate: () -> Unit,
    onUpgrade: () -> Unit,
    onViewMyAccount: () -> Unit,
    onSignOut: () -> Unit
) {
    val plan = state.plan
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = GoogleAuthClient(context)
    val app = LocalContext.current.applicationContext as App
    val api = app.container.api

    ScreenScaffold(
        title = "Plan & Quotas",
        actions = {
            TextButton(onClick = {

                scope.launch {
                    try {
                        val resp = api.logout()
                        assert(resp.ok)
                        authRepo.signOutUser()
                        onSignOut()
                    } catch (e: Exception) {
                        Log.e("Logout failed", e.message ?: "Server error")
                    }
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
                    Text(plan.name, style = MaterialTheme.typography.headlineSmall)

                    if (!plan.can_edit_templates) {
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
            OutlinedButton(onClick = onViewMyAccount, modifier = Modifier.fillMaxWidth()) {
                Text("My Account")
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

        }
    }
}
