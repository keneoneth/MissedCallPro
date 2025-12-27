package com.example.missedcallpro.screens

import retrofit2.HttpException
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.missedcallpro.App
import com.example.missedcallpro.auth.GoogleAuthClient
import com.example.missedcallpro.ui.ScreenScaffold
import kotlinx.coroutines.launch

@Composable
fun LandingScreen(
    onGoogleLogin:  (String, String) -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Use the AuthRepository (the class we created in the previous step)
    val authRepo = GoogleAuthClient(context)

    var err by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val app = LocalContext.current.applicationContext as App
    val api = app.container.api

    ScreenScaffold(title = "") { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // App name — bold & centered
            Text(
                text = "MissedCallPro",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Follow up on missed calls automatically",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(32.dp))

            if (loading) {
                CircularProgressIndicator()
            } else {
                Button(onClick = {
                    loading = true
                    err = null

                    scope.launch {
                        val user = authRepo.signInWithGoogle()

                        if (user != null) {
                            try {
                                val resp = api.bootstrap()
                                Log.d("logged resp",resp.toString())
                                onGoogleLogin(resp.username,resp.email)
                            } catch (e: HttpException) {
                                err = when (e.code()) {
                                    else -> "Server error (${e.code()})."
                                }
                            } catch (e: Exception) {
                                err = e.message ?: "Unknown error"
                            } finally {
                                loading = false
                            }
                        } else {
                            err = "Login failed or was cancelled"
                        }
                    }
                }) {
                    Text("Login with Google")
                }
            }

            err?.let {
                Text(text = it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }

        }
    }
}