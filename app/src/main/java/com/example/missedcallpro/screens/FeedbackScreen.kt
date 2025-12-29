package com.example.missedcallpro.screens

// ui/feedback/FeedbackScreen.kt
import android.app.Activity
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.missedcallpro.App
import com.example.missedcallpro.BuildConfig
import com.example.missedcallpro.data.FeedbackViewModel
import com.example.missedcallpro.data.FeedbackViewModelFactory
import com.example.missedcallpro.data.PaymentViewModel
import com.example.missedcallpro.data.PaymentViewModelFactory
import com.example.missedcallpro.ui.ScreenScaffold
import java.util.Locale

@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as App
    val context = LocalContext.current

    val factory = remember {
        FeedbackViewModelFactory(
            appVersion = BuildConfig.VERSION_NAME,
            deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ),
            locale = Locale.getDefault().toLanguageTag(),
            api = app.container.api
        )
    }

    val viewModel: FeedbackViewModel = viewModel(factory = factory)

    val state by viewModel.state.collectAsState()

    ScreenScaffold(
        title = "Feedback", onBack = onBack
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Tell us what to improve")

            OutlinedTextField(
                value = state.text,
                onValueChange = viewModel::onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                placeholder = { Text("Write your feedback here…") },
                supportingText = {
                    Text("${state.text.length}/${viewModel.maxChars}")
                },
                isError = state.error != null,
                enabled = !state.isSending
            )

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::send,
                enabled = !state.isSending && state.text.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSending) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Sending…")
                } else {
                    Text("Send")
                }
            }

            if (state.sent) {
                Text("Message already sent.")
            }
        }
    }
}
