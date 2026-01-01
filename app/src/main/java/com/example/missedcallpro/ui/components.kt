package com.example.missedcallpro.ui

import android.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Text("←") }
                    }
                },
                actions = actions
            )
        },
        content = content
    )
}

@Composable
fun QuotaRow(
    label: String,
    used: Int,
    limit: Int,
    onClick: (() -> Unit)? = null
) {
    val rowMod = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(rowMod)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text("$used / $limit used", style = MaterialTheme.typography.bodyMedium)
        }
        if (onClick != null) Text(">", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun FormEditRow(
    label: String,
    onClick: (() -> Unit)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(Modifier.clickable { onClick() })
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(">", style = MaterialTheme.typography.titleLarge)
    }
}