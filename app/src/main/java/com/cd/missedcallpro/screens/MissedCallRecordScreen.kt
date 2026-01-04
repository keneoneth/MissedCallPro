package com.cd.missedcallpro.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cd.missedcallpro.App
import com.cd.missedcallpro.data.MissedCallItem
import com.cd.missedcallpro.ui.ScreenScaffold
import retrofit2.HttpException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private fun formatIsoTime(iso: String): String {
    return try {
        val odt = OffsetDateTime.parse(iso)
        odt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    } catch (_: Exception) {
        iso
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissedCallRecordScreen(
    onBack: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as App
    val api = app.container.api

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<MissedCallItem>>(emptyList()) }
    var reloadKey by remember { mutableIntStateOf(0) }

    suspend fun load() {
        loading = true
        error = null
        try {
            val resp = api.listMissedCalls(limit = 100)
            items = resp.items
        } catch (e: HttpException) {
            val msg = "HTTP ${e.code()} loading missed calls"
            Log.e("MissedCallRecordScreen", msg, e)
            error = msg
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown error loading missed calls"
            Log.e("MissedCallRecordScreen", msg, e)
            error = msg
        } finally {
            loading = false
        }
    }

    LaunchedEffect(reloadKey) { load() }

    ScreenScaffold(
        title = "Missed Call Records",
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
                Button(
                    onClick = { /* trigger reload */reloadKey++ },
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text("Retry")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Caller", modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.labelMedium)
                Text("Time", modifier = Modifier.weight(1.6f), style = MaterialTheme.typography.labelMedium)
                Text("SMS", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelMedium)
            }


            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items) { it ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(it.caller, modifier = Modifier.weight(1.4f))
                            Text(formatIsoTime(it.occurred_at), modifier = Modifier.weight(1.6f))
                            Text(it.sms_status, modifier = Modifier.weight(0.8f))
                        }

                        val note = it.sms_note?.trim().orEmpty()
                        if (note.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}