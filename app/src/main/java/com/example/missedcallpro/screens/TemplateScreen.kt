package com.example.missedcallpro.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.missedcallpro.App
import com.example.missedcallpro.data.AppState
import com.example.missedcallpro.data.SmsSettingsDto
import com.example.missedcallpro.ui.ScreenScaffold
import kotlinx.coroutines.launch

private const val PH_COMPANY = "{{COMPANY}}"
private const val PH_FORM_LINK = "{{FORM_LINK}}"

private fun removeFormLinkToken(s: String): String {
    // Remove token and clean extra spaces
    return s.replace(PH_FORM_LINK, "")
        .replace("  ", " ")
        .replace("  ", " ")
        .trim()
}

@Composable
fun TemplateScreen(
    title: String,
    state: AppState,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    saveSmsSettings: (company: String, template: String, includeLink: Boolean) -> Unit
) {
    val app = LocalContext.current.applicationContext as App
    val api = app.container.api
    val isPaid = state.plan.can_edit_templates

    var showEditor by remember { mutableStateOf(false) }

    // Local edit buffers
    var companyName by remember(state.companyName) { mutableStateOf(state.companyName) }
    var includeLink by remember(state.includeFormLinkInSms) { mutableStateOf(state.includeFormLinkInSms) }
    var editingTemplate by remember(state.smsTemplate) { mutableStateOf(state.smsTemplate) }

    val isChanged = companyName.trim() != state.companyName ||
        includeLink != state.includeFormLinkInSms ||
        editingTemplate != state.smsTemplate

    // Warnings
    val companyMissing = companyName.trim().isBlank()
    val needsRemovedFormLinkWarning = !includeLink && editingTemplate.contains(PH_FORM_LINK)

    // If user disables link: remove token automatically
    fun onToggleIncludeLink(newValue: Boolean) {
        includeLink = newValue
    }

    ScreenScaffold(title = title, onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize()
        ) {

            if (companyMissing) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Action required", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "SMS sending will be blocked until you set a Company Name.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Company Name", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g., ABC Plumbing Inc.") }
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Include Active Form Link", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "If OFF, we won't insert the form link into SMS.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = includeLink,
                            onCheckedChange = { onToggleIncludeLink(it) }
                        )
                    }

                    if (needsRemovedFormLinkWarning) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Form link is disabled, but your SMS template does not include $PH_FORM_LINK. It will be automatically removed when sms is sent.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("SMS Template", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(editingTemplate, style = MaterialTheme.typography.bodyMedium)

                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { showEditor = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Edit") }
                }
            }

            Spacer(Modifier.height(16.dp))

            val scope = rememberCoroutineScope()
            Button(
                onClick = {
                    scope.launch {
                        val templateToSave = if (isPaid) editingTemplate else state.smsTemplate
                        val resp = api.putSmsSettings(
                            SmsSettingsDto(
                                company_name = companyName.trim(),
                                sms_template = templateToSave,
                                include_form_link = includeLink
                            )
                        )
                        saveSmsSettings(resp.company_name, resp.sms_template, resp.include_form_link)
                        showEditor = false
                        Log.d("show local:",companyName.trim()+"|"+templateToSave+"|"+includeLink)
                        Log.d("show diff:",resp.company_name+"|"+resp.sms_template+"|"+resp.include_form_link)
                        Log.d("show state:",state.companyName+"|"+state.smsTemplate+"|"+state.includeFormLinkInSms)

                    }
                },
                enabled = isChanged,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text("Edit SMS") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    if (!isPaid) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Upgrade required", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Free plan cannot edit SMS content. You can still set Company Name and toggle the form link.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = onUpgrade) { Text("Upgrade") }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Read-only on free tier
                    OutlinedTextField(
                        value = editingTemplate,
                        onValueChange = { if (isPaid) editingTemplate = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6,
                        readOnly = !isPaid,
                        label = { Text("SMS Template") }
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { if (isPaid) editingTemplate += " $PH_COMPANY" },
                            label = { Text("Insert Company") }
                        )
                        AssistChip(
                            onClick = {
                                if (isPaid) {
                                    editingTemplate += " $PH_FORM_LINK"
                                }
                            },
                            label = { Text("Insert Form Link") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEditor = false }) { Text("Done") }
            }
        )
    }
}
