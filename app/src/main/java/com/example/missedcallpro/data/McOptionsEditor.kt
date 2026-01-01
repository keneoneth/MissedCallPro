package com.example.missedcallpro.data

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
fun McOptionsEditor(
    initial: FormFieldDto,
    onDone: (FormFieldDto) -> Unit
) {
    var options by remember {
        mutableStateOf(initial.options ?: emptyList())
    }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Multiple choice options", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp), // scrollable area
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(options) { idx, opt ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = opt.label,
                        onValueChange = { newLabel ->
                            val new = options.toMutableList()
                            new[idx] = opt.copy(label = newLabel)
                            options = new
                        },
                        label = { Text("Option ${idx + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val new = options.toMutableList()
                            new.removeAt(idx)
                            options = new
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete option")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                enabled = options.size < 10,
                onClick = {
                    val next = options.size + 1
                    options = options + McOptionDto(
                        id = "opt_${UUID.randomUUID()}",
                        label = "Option $next"
                    )
                }
            ) {
                Text("+ Add option (${options.size}/10)")
            }

            Spacer(Modifier.weight(1f))

            Button(
                enabled = options.isNotEmpty(),
                onClick = {
                    val cleaned = options
                        .map { it.copy(label = it.label.trim()) }
                        .filter { it.label.isNotEmpty() }
                        .take(10)

                    // Ensure at least 1 option
                    val finalOpts = if (cleaned.isEmpty()) {
                        listOf(McOptionDto(id = "opt1", label = "Option 1"))
                    } else cleaned

                    onDone(initial.copy(options = finalOpts, max_choices = initial.max_choices ?: 1))
                }
            ) { Text("Done") }
        }
    }
}
