package com.example.missedcallpro.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.missedcallpro.App
import com.example.missedcallpro.auth.GoogleAuthClient
import com.example.missedcallpro.data.AppState
import com.example.missedcallpro.ui.QuotaRow
import com.example.missedcallpro.ui.ScreenScaffold
import com.example.missedcallpro.util.getPhonePermState
import kotlinx.coroutines.launch
import android.Manifest
import android.widget.Toast
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import com.example.missedcallpro.ui.FormEditRow

@Composable
fun PhonePermissionCard() {
    val ctx = LocalContext.current
    var permState by remember { mutableStateOf(getPhonePermState(ctx)) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permState = getPhonePermState(ctx)

        val grantedAll = results[Manifest.permission.READ_PHONE_STATE] == true &&
                results[Manifest.permission.READ_CALL_LOG] == true

        if (grantedAll) {
            Toast.makeText(ctx, "Permissions granted. Missed-call auto-reply enabled.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(ctx, "Permissions needed to detect missed calls.", Toast.LENGTH_SHORT).show()
        }
    }

    if (!permState.allGranted) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Enable missed-call auto-reply here.")

                Button(onClick = {
                    launcher.launch(arrayOf(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.READ_CALL_LOG
                    ))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant permissions")
                }
            }
        }
    }
}

@Composable
fun CompanyNameWarning(companyName: String, onOpenSmsTemplate: () -> Unit) {
    if (companyName.isBlank()) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(12.dp)) {
                Text("Action required", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Company Name is not set. SMS sending will be blocked until you set it.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = onOpenSmsTemplate) { Text("Set Company Name") }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun PlanQuotaScreen(
    state: AppState,
    onOpenSmsTemplate: () -> Unit,
    onUpgrade: () -> Unit,
    onViewMyAccount: () -> Unit,
    onSignOut: () -> Unit,
    onOpenFilterList: () -> Unit,
    onOpenFormEditPage: () -> Unit,
) {
    val plan = state.plan
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = GoogleAuthClient(context)
    val app = LocalContext.current.applicationContext as App
    val api = app.container.api

    val permState by remember { derivedStateOf { getPhonePermState(context) } }

    ScreenScaffold(
        title = "Plan & Quotas",
        actions = {
            TextButton(onClick = {
                scope.launch {
                    try {
                        val resp = api.logout()
                        check(resp.ok)
                        authRepo.signOutUser()
                        onSignOut()
                    } catch (e: Exception) {
                        Log.e("Logout failed", e.message ?: "Server error")
                    }
                }
            }) { Text("Sign out") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (!permState.allGranted) PhonePermissionCard()

            CompanyNameWarning(
                companyName = state.companyName,
                onOpenSmsTemplate = onOpenSmsTemplate
            )

            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Current Plan", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(plan.name, style = MaterialTheme.typography.headlineSmall)
                    if (plan.name != "Free") {
                        Spacer(Modifier.height(10.dp))
                        Text("(${plan.status})", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(Modifier.height(10.dp))
                    if (!plan.can_edit_templates) {
                        Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) {
                            Text("Upgrade →")
                        }
                    } else {
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
            OutlinedButton(onClick = onOpenFilterList, modifier = Modifier.fillMaxWidth()) {
                Text("Filter List")
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
                onClick = null
            )

            Spacer(Modifier.height(16.dp))
            Text("Form", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            FormEditRow("active form", onOpenFormEditPage)
        }
    }
}

