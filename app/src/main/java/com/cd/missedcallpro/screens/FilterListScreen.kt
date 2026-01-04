package com.cd.missedcallpro.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cd.missedcallpro.data.FilterStore
import com.cd.missedcallpro.data.FilterUiState
import com.cd.missedcallpro.ui.ScreenScaffold
import kotlinx.coroutines.launch

@Composable
fun FilterListScreen(
    store: FilterStore,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf(FilterUiState()) }
    var err by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    // dialogs
    var showAddCallingCode by remember { mutableStateOf(false) }
    var showAddBlockedNumber by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            loading = true
            err = null
            try {
                state = store.load()
            } catch (e: Exception) {
                err = e.message ?: "Failed to load filter settings"
            } finally {
                loading = false
            }
        }
    }

    fun saveModeAndCodes(newMode: String, newCodes: List<String>) {
        scope.launch {
            err = null
            try {

                store.saveModeAndCodes(newMode, newCodes)

                state = store.load()
            } catch (e: Exception) {
                err = e.message ?: "Failed to save"
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    ScreenScaffold(
        title = "Filter List",
        onBack = onBack
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            item {
                if (err != null) Text(err!!, color = MaterialTheme.colorScheme.error)
            }

            // ====== Country filter (single mode) ======
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Country filter", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))

                        FilterModeSelector(
                            mode = state.mode,
                            onChange = { newMode ->

                                saveModeAndCodes(newMode, state.callingCodes)
                            }
                        )

                        if (state.mode != "none") {
                            Spacer(Modifier.height(12.dp))

                            if (state.callingCodes.isEmpty()) {
                                Text("No calling codes yet.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                CallingCodeChipList(
                                    codes = state.callingCodes,
                                    onRemove = { code ->
                                        val newCodes = state.callingCodes.filterNot { it == code }
                                        saveModeAndCodes(state.mode, newCodes)
                                    }
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showAddCallingCode = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Add calling code")
                            }
                        }
                    }
                }
            }

            // ====== Blocked numbers ======
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Blocked Numbers", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showAddBlockedNumber = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add blocked number")
                        }

                        Spacer(Modifier.height(12.dp))

                        if (state.blockedNumbers.isEmpty()) {
                            Text("No blocked numbers yet.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.blockedNumbers.forEach { row ->
                                    Card(Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                row.e164_number,
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        err = null
                                                        try {
                                                            store.deleteBlockedNumber(row.id)
                                                            state = store.load()
                                                        } catch (e: Exception) {
                                                            err = e.message ?: "Delete failed"
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }

        // dialogs
        if (showAddCallingCode) {
            AddCallingCodeDialog(
                title = "Add calling code",
                onDismiss = { showAddCallingCode = false },
                onSave = { raw ->
                    val code = raw.trim().removePrefix("+")
                    if (code.isNotEmpty() && code.all { it.isDigit() }) {
                        val newCodes = (state.callingCodes + code).distinct().sorted()
                        saveModeAndCodes(state.mode, newCodes)
                    }
                    showAddCallingCode = false
                }
            )
        }

        if (showAddBlockedNumber) {
            AddBlockedNumberDialog(
                onDismiss = { showAddBlockedNumber = false },
                onSave = { e164 ->
                    scope.launch {
                        err = null
                        try {
                            store.addBlockedNumber(e164.trim())
                            showAddBlockedNumber = false
                            state = store.load()
                        } catch (e: Exception) {
                            err = e.message ?: "Add failed"
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FilterModeSelector(
    mode: String,
    onChange: (String) -> Unit
) {
    // mode: "none" | "allow" | "block"
    val options = listOf(
        "none" to "Off",
        "allow" to "Allow only these countries",
        "block" to "Block these countries"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RadioButton(selected = mode == value, onClick = { onChange(value) })
                Text(label, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun CallingCodeChipList(
    codes: List<String>,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val rows = codes.sorted().map { "+$it" }.chunked(2)
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { display ->
                    AssistChip(
                        onClick = { /* no-op */ },
                        label = { Text(display) },
                        trailingIcon = {
                            IconButton(onClick = { onRemove(display.removePrefix("+")) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AddCallingCodeDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Enter calling code like +1, +852, +81")
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Calling code") },
                    placeholder = { Text("+1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(input) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddBlockedNumberDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add blocked number") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tip: include country code. Example: +14165551234")
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Phone number") },
                    placeholder = { Text("+14165551234") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = input.trim()
                if (v.startsWith("+") && v.drop(1).any { it.isDigit() }) onSave(v)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
