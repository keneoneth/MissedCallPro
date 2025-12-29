package com.example.missedcallpro.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.missedcallpro.App
import com.example.missedcallpro.data.FilterStore

@Composable
fun FilterListRoute(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val api = app.container.api
    val store = FilterStore(api)

    FilterListScreen(
        store = store,
        onBack = onBack
    )
}
