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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.missedcallpro.telephony.MissedCallMonitoringService
import com.example.missedcallpro.ui.FormEditRow
import android.provider.Settings

object MonitoringServiceStarter {
    fun start(ctx: Context) {
        val i = Intent(ctx, MissedCallMonitoringService::class.java)
        ContextCompat.startForegroundService(ctx, i)
    }
}

@Composable
fun PhonePermissionCard() {
    val ctx = LocalContext.current
    var permState by remember { mutableStateOf(getPhonePermState(ctx)) }

    // Are notifications globally enabled for this app? (User may have disabled them)
    val notificationsEnabled = remember {
        mutableStateOf(NotificationManagerCompat.from(ctx).areNotificationsEnabled())
    }

    fun openNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
    }

    fun hasPostNotificationsPerm(): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun startIfPossible() {
        // Refresh notifications enabled state
        notificationsEnabled.value = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

        val phonePermOk = permState.allGranted
        val notifPermOk = hasPostNotificationsPerm()

        if (!phonePermOk) return

        if (!notifPermOk) {
            Toast.makeText(ctx, "Please allow notifications so monitoring can run.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!notificationsEnabled.value) {
            Toast.makeText(ctx, "Notifications are disabled for this app. Enable them to show monitoring.", Toast.LENGTH_SHORT).show()
            return
        }

        MonitoringServiceStarter.start(ctx)
        Toast.makeText(ctx, "Monitoring enabled.", Toast.LENGTH_SHORT).show()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        permState = getPhonePermState(ctx)
        startIfPossible()
    }

    // If phone permissions already granted but notification settings/perm prevents showing,
    // show a card to guide user.
    val needsPhonePerms = !permState.allGranted
    val needsNotifPerm = Build.VERSION.SDK_INT >= 33 && !hasPostNotificationsPerm()
    val notifBlockedByUser = !notificationsEnabled.value

    if (needsPhonePerms || needsNotifPerm || notifBlockedByUser) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Enable missed-call monitoring")

                Spacer(Modifier.height(8.dp))

                if (needsPhonePerms || needsNotifPerm) {
                    Text("• Allow Phone + Call Log + Notifications permissions so we can detect missed calls.")
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        val perms = mutableListOf(
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_CALL_LOG
                        )
                        if (Build.VERSION.SDK_INT >= 33) {
                            perms += Manifest.permission.POST_NOTIFICATIONS
                        }
                        launcher.launch(perms.toTypedArray())
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant permissions")
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    } else {
        // Everything allowed already — start the service once user reaches this screen
        // (safe even if called again; Android will reuse the same service instance)
        startIfPossible()
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
    val started = rememberSaveable { mutableStateOf(false) }

    val permState by remember { derivedStateOf { getPhonePermState(context) } }

    LaunchedEffect(permState.allGranted) {
        if (permState.allGranted && !started.value) {
            Log.d("MonitoringServiceStarter","MonitoringServiceStarter started")
            MonitoringServiceStarter.start(context)
            started.value = true
        }
    }

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

