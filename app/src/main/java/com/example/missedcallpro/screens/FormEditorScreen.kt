package com.example.missedcallpro.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.missedcallpro.App
import com.example.missedcallpro.data.FieldTypes
import com.example.missedcallpro.data.FormFieldDto
import com.example.missedcallpro.data.FormUpdateRequestDto
import com.example.missedcallpro.data.McOptionDto
import com.example.missedcallpro.data.McOptionsEditor
import com.example.missedcallpro.data.FormStore
import com.example.missedcallpro.ui.ScreenScaffold
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormEditorScreen(
    store: FormStore,
    onUpgrade: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val api = app.container.api
    val scope = rememberCoroutineScope()
    val state by store.state.collectAsState()

    // MC bottom sheet
    var editingMcFieldIndex by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load from backend once
    LaunchedEffect(Unit) {
        store.setLoading()
        try {
            val resp = api.getForm()
            store.setForm(resp.form, resp.is_locked) // baseline from server
        } catch (e: Exception) {
            Log.e("FormEditorScreen", "Load failed", e)
            store.setError(e.message ?: "Failed to load form")
        }
    }

    // Server/baseline form (never directly edited)
    val serverForm = state.form

    // Local editable form (this is what UI edits)
    var localForm by remember(serverForm) { mutableStateOf(serverForm) }

    // If backend says locked, we prevent edits
    val locked = false //state.isLocked

    // Dirty check (only enable Save when different from server baseline)
    val isDirty = localForm != null && serverForm != null && localForm != serverForm
    val canSave = localForm != null && !state.isLoading && isDirty && !locked


    // Optional: intercept back to discard edits (simple: just go back)
    fun handleBack() {
        // If you want a confirm dialog later, hook it here.
        onBack()
    }

    ScreenScaffold(
        title = "Form Editor",
        onBack = { handleBack() },
        actions = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val toSave = localForm ?: return@TextButton
                    scope.launch {
                        store.setLoading()
                        try {
                            val resp = api.updateForm(FormUpdateRequestDto(toSave))
                            store.setForm(resp.form, resp.is_locked)
                            // localForm will auto-reset because serverForm changes (remember(serverForm))
                        } catch (e: Exception) {
                            Log.e("FormEditorScreen", "Save failed", e)
                            store.setError(e.message ?: "Failed to save form")
                        }
                    }
                }
            ) { Text("Save") }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (locked) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Upgrade required", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Free tier: Form is locked. Upgrade to customize.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = onUpgrade) { Text("Upgrade") }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            // safeguards localForm must not be null
            if (localForm == null) {
                if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Loading...", Modifier.padding(16.dp))
                return@Column
            }

            // Unsaved changes hint
            if (isDirty) {
                Text(
                    "Unsaved changes",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            val form = localForm!!

            // Title editor
            OutlinedTextField(
                value = form.title,
                onValueChange = { newTitle ->
                    if (locked) return@OutlinedTextField
                    localForm = form.copy(title = newTitle)
                },
                enabled = !locked,
                label = { Text("Form Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(form.fields) { idx, field ->
                    FieldRow(
                        field = field,
                        locked = locked,
                        onChange = { updated ->
                            val newFields = form.fields.toMutableList()
                            newFields[idx] = updated
                            localForm = form.copy(fields = newFields)
                        },
                        onDelete = {
                            if (locked) return@FieldRow
                            val newFields = form.fields.toMutableList()
                            newFields.removeAt(idx)
                            localForm = form.copy(fields = newFields)
                        },
                        onEditMcOptions = {
                            if (locked) return@FieldRow
                            editingMcFieldIndex = idx
                            scope.launch { sheetState.show() }
                        }
                    )
                }
            }

            if (!locked) {
                Button(
                    onClick = {
                        val newField = FormFieldDto(
                            id = "field_${UUID.randomUUID()}",
                            label = "New question",
                            type = "text",
                            required = false
                        )
                        localForm = form.copy(fields = form.fields + newField)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("+ Add field")
                }
            }
        }
    }

    // MC options bottom sheet
    val mcIndex = editingMcFieldIndex
    val formForSheet = localForm
    if (mcIndex != null && formForSheet != null) {
        val field = formForSheet.fields.getOrNull(mcIndex)
        if (field != null && field.type == "mc") {
            ModalBottomSheet(
                onDismissRequest = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        editingMcFieldIndex = null
                    }
                },
                sheetState = sheetState
            ) {
                McOptionsEditor(
                    initial = field,
                    onDone = { updatedField ->
                        val latest = localForm ?: return@McOptionsEditor
                        val newFields = latest.fields.toMutableList()
                        newFields[mcIndex] = updatedField
                        localForm = latest.copy(fields = newFields)

                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            editingMcFieldIndex = null
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldRow(
    field: FormFieldDto,
    locked: Boolean,
    onChange: (FormFieldDto) -> Unit,
    onDelete: () -> Unit,
    onEditMcOptions: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = field.label,
                onValueChange = { if (!locked) onChange(field.copy(label = it)) },
                enabled = !locked,
                label = { Text("Question") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Type dropdown
            var expanded by remember { mutableStateOf(false) }
            val currentTypeLabel = FieldTypes.firstOrNull { it.code == field.type }?.label ?: field.type

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (!locked) expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = currentTypeLabel,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !locked,
                    label = { Text("Answer type") },
                    trailingIcon = {
                        // Standard dropdown arrow icon
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    FieldTypes.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.label) },
                            onClick = {
                                expanded = false
                                if (locked) return@DropdownMenuItem

                                val updated = when (t.code) {
                                    "mc" -> field.copy(
                                        type = "mc",
                                        options = field.options ?: listOf(
                                            McOptionDto(id = "opt1", label = "Option 1"),
                                            McOptionDto(id = "opt2", label = "Option 2")
                                        ),
                                        max_choices = field.max_choices ?: 1
                                    )
                                    else -> field.copy(
                                        type = t.code,
                                        options = null,
                                        max_choices = null
                                    )
                                }
                                onChange(updated)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Switch(
                        checked = field.required,
                        onCheckedChange = { if (!locked) onChange(field.copy(required = it)) },
                        enabled = !locked
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Required")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (field.type == "mc") {
                        OutlinedButton(
                            onClick = onEditMcOptions,
                            enabled = !locked
                        ) { Text("Edit options") }
                    }
                    IconButton(onClick = onDelete, enabled = !locked) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
            }
        }
    }
}
