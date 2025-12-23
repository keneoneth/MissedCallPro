package com.example.missedcallpro.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.missedcallpro.ui.ScreenScaffold

@Composable
fun LandingScreen(
    onGoogleLogin: () -> Unit
) {
    ScreenScaffold(title = "MissedCallPro") { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Follow up on missed calls fast.", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text("Login to manage your plan, quotas, and templates.")
            Spacer(Modifier.height(24.dp))

            Button(onClick = onGoogleLogin, modifier = Modifier.fillMaxWidth()) {
                Text("Login with Google")
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Note: Google login is stubbed for now. Next step is wiring real Google Sign-In.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}