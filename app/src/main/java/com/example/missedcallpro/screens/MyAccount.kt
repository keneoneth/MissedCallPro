package com.example.missedcallpro.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.missedcallpro.App
import com.example.missedcallpro.auth.GoogleAuthClient
import com.example.missedcallpro.data.AppState
import com.example.missedcallpro.ui.ScreenScaffold

/**
 * Similar structure to your PaymentScreen:
 * - uses ScreenScaffold(title, onBack) { padding -> LazyColumn(...) }
 * - shows username + email
 * - Delete account button -> confirm dialog (Yes/Cancel)
 *
 * Wire it like:
 * composable("account") {
 *   AccountScreen(
 *     username = state.username,
 *     email = state.email,
 *     onBack = { nav.popBackStack() },
 *     onConfirmDelete = { viewModel.requestAccountDeletion() }
 *   )
 * }
 */
@Composable
fun AccountScreen(
    state: AppState,
    username: String,
    email: String,
    onBack: () -> Unit,
    onConfirmDelete: suspend () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authRepo = GoogleAuthClient(context)
    val app = LocalContext.current.applicationContext as App
    val api = app.container.api

    ScreenScaffold(title = "My Account", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Account details", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Manage your profile and delete your account.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
            }

            item {
                // You can replace this with a Card if you want to match PlanCard style.
                Text("Username", style = MaterialTheme.typography.labelLarge)
                Text(username.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))

                Text("Email", style = MaterialTheme.typography.labelLarge)
                Text(email.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
            }

            item {
                if (err != null) {
                    Text(
                        text = err!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Button(
                    onClick = { showDeleteDialog = true },
                    enabled = !isDeleting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isDeleting) "Deleting..." else "Delete account")
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "Deleting your account will remove your profile and associated data. " +
                            "Some records (e.g., billing) may be retained if legally required." +
                            "Subscription (if any) will be cancelled automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
            title = { Text("Delete account?") },
            text = {
                Text(
                    "This will deactivate your account and request deletion of your data. " +
                            "Are you sure you want to continue?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        isDeleting = true
                        err = null

                        scope.launch {
                            try {
                                val resp = api.deleteAccount()
                                assert(resp.ok)
                                authRepo.signOutUser()
                                onConfirmDelete()
                            } catch (t: Throwable) {
                                err = t.message ?: "Failed to delete account"
                            } finally {
                                isDeleting = false
                            }
                        }
                    }
                ) { Text("Yes, delete") }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !isDeleting,
                    onClick = { showDeleteDialog = false }
                ) { Text("Cancel") }
            }
        )
    }
}