package com.example.missedcallpro.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.missedcallpro.App
import com.example.missedcallpro.data.AppState
import com.example.missedcallpro.data.Defaults
import com.example.missedcallpro.data.SmsSettingsDto
import com.example.missedcallpro.ui.ScreenScaffold
import kotlinx.coroutines.launch
import retrofit2.HttpException

private const val PH_COMPANY = "{{COMPANY}}"
private const val PH_FORM_LINK = "{{FORM_LINK}}"

// NOTE: We keep the token in the template always.
// When includeLink is OFF, we exclude its contribution from the character count.
// We do NOT remove it from the stored template, so free users can re-enable later.

fun computeRenderedSmsLength(
    template: String,
    companyName: String,
    includeLink: Boolean
): Int {
    var s = template

    // Replace company token with actual company name
    s = s.replace(PH_COMPANY, companyName.trim())

    // Link token contribution:
    // - If ON: count as reserved fixed length
    // - If OFF: count as 0 chars but keep token in template (so we replace with "")
    s = if (includeLink) {
        s.replace(PH_FORM_LINK, "x".repeat(SmsLimits.FORM_LINK_RESERVED_CHARS))
    } else {
        s.replace(PH_FORM_LINK, "")
    }

    // Normalize whitespace similarly to backend
    s = s.replace(Regex("\\s+"), " ").trim()

    return s.length
}

fun smsMaxAllowedChars(includeLink: Boolean): Int {
    return SmsLimits.SMS_TOTAL_CHARS
}

@Composable
fun TemplateScreen(
    title: String,
    state: AppState,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    saveSmsSettings: (company: String, template: String, includeLink: Boolean) -> Unit
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as App
    val api = app.container.api
    val isPaid = state.plan.can_edit_templates
    val OPT_OUT_SUFFIX = Defaults.OPT_OUT_SUFFIX

    var showEditor by remember { mutableStateOf(false) }

    // Local edit buffers
    var companyName by remember(state.companyName) { mutableStateOf(state.companyName) }
    var includeLink by remember(state.includeFormLinkInSms) { mutableStateOf(state.includeFormLinkInSms) }
    var editingTemplate by remember(state.smsTemplate) { mutableStateOf(state.smsTemplate) }

    val companyTrimmed = companyName.trim()
    val companyMissing = companyTrimmed.isBlank()

    // Company length limit
    val companyMax = SmsLimits.COMPANY_NAME_MAX_CHARS
    val companyTooLong = companyTrimmed.length > companyMax

    // Warnings about the link token presence (template stays unchanged)
    val tokenPresent = editingTemplate.contains(PH_FORM_LINK)
    val needsRemovedFormLinkWarning = !includeLink && tokenPresent
    val needsAddFormLinkWarning = includeLink && !tokenPresent

    // Rendered-length enforcement (company + optional reserved link counted)
    val renderedLen = remember(companyTrimmed, includeLink, editingTemplate) {
        computeRenderedSmsLength(editingTemplate, companyTrimmed, includeLink)
    }
    val maxLen = SmsLimits.SMS_TOTAL_CHARS
    val overLimit = renderedLen > maxLen

    val trimmedTemplateEnd = editingTemplate.trimEnd()
    val missingOptOutSuffix = !trimmedTemplateEnd.endsWith(OPT_OUT_SUFFIX)

    val isChanged = companyTrimmed != state.companyName ||
            includeLink != state.includeFormLinkInSms ||
            editingTemplate != state.smsTemplate

    fun onToggleIncludeLink(newValue: Boolean) {
        // Do NOT edit the template. Only switch the flag.
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
                        onValueChange = { newValue ->
                            // Hard cap at 80 chars (keeps app + backend consistent)
                            companyName = if (newValue.length <= companyMax) newValue else newValue.take(companyMax)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = companyTooLong,
                        supportingText = { Text("${companyTrimmed.length}/$companyMax") },
                        placeholder = { Text("e.g., ABC Plumbing Inc.") }
                    )

                    if (companyTooLong) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Company Name must be at most $companyMax characters.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Include Active Form Link", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "If OFF, we won’t insert the form link into SMS (and it won’t count toward the limit).",
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
                            "Form link is disabled, but your SMS template contains $PH_FORM_LINK. The token will be ignored (not inserted) when sending.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (needsAddFormLinkWarning) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Form link is enabled, but your template does not include $PH_FORM_LINK. Add it if you want the link to appear.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (missingOptOutSuffix) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Your SMS template must end with: \"$OPT_OUT_SUFFIX\"",
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

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Rendered length: $renderedLen / $maxLen",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (overLimit) {
                        Text(
                            text = "Too long. Shorten the SMS (company + optional link budget are included).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val scope = rememberCoroutineScope()
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val templateToSave = if (isPaid) editingTemplate else state.smsTemplate

                            val resp = api.putSmsSettings(
                                SmsSettingsDto(
                                    company_name = companyTrimmed,
                                    sms_template = templateToSave,
                                    include_form_link = includeLink
                                )
                            )

                            saveSmsSettings(resp.company_name, resp.sms_template, resp.include_form_link)
                            showEditor = false

                        } catch (e: HttpException) {
                            Toast.makeText(ctx, "Save rejected.", Toast.LENGTH_SHORT).show()
                            Log.e("TemplateScreen", "HTTP ${e.code()}", e)
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "Save failed.", Toast.LENGTH_SHORT).show()
                            Log.e("TemplateScreen", "Save failed", e)
                        }
                    }
                },
                // Save blocked if over SMS limit or company name too long
                enabled = isChanged && !overLimit && !companyTooLong && !missingOptOutSuffix,
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()), // Still recommended so chips don't vanish
                        verticalAlignment = Alignment.Top,            // Changed from CenterVertically
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { if (isPaid) editingTemplate += " $PH_COMPANY" },
                            label = { Text("Insert Company") }
                        )
                        AssistChip(
                            onClick = {
                                if (isPaid) {
                                    // Avoid duplicates
                                    if (!editingTemplate.contains(PH_FORM_LINK)) {
                                        editingTemplate += " $PH_FORM_LINK"
                                    }
                                }
                            },
                            label = { Text("Insert Form Link") }
                        )
                        AssistChip(
                            onClick = {
                                if (isPaid) {
                                    val t = editingTemplate.trimEnd()
                                    if (!t.endsWith(OPT_OUT_SUFFIX)) {
                                        editingTemplate = t.trimEnd() + (if (t.endsWith(".")) " " else " ") + OPT_OUT_SUFFIX
                                    }
                                }
                            },
                            label = { Text("Append STOP footer") }
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