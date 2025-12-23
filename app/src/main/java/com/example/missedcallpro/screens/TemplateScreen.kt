package com.example.missedcallpro.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.missedcallpro.data.AppState
import com.example.missedcallpro.data.TemplateType
import com.example.missedcallpro.ui.ScreenScaffold

@Composable
fun TemplateScreen(
    title: String,
    state: AppState,
    type: TemplateType,
    onBack: () -> Unit,
    onEditBlocked: () -> Unit,
    onSave: (String) -> Unit
) {
    val isPaid = state.plan.isPaid
    val templateText = when (type) {
        TemplateType.SMS -> state.smsTemplate
        TemplateType.EMAIL -> state.emailTemplate
    }

    var showEditor by remember { mutableStateOf(false) }
    var editingText by remember(templateText) { mutableStateOf(templateText) }

    ScreenScaffold(title = title, onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Current Template", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    Text(templateText, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (!isPaid) onEditBlocked() else {
                        editingText = templateText
                        showEditor = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit")
            }

            Spacer(Modifier.height(10.dp))
            Text(
                if (isPaid) "You can edit this template." else "Free plan uses default templates. Upgrade to edit.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text("Edit Template") },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6
                )
            },
            confirmButton = {
                Button(onClick = {
                    onSave(editingText)
                    showEditor = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) { Text("Cancel") }
            }
        )
    }
}
