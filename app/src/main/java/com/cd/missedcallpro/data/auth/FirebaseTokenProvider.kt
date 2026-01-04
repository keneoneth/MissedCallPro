package com.cd.missedcallpro.data.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class FirebaseTokenProvider(
    private val auth: FirebaseAuth
) {
    suspend fun getIdToken(forceRefresh: Boolean = false): String {
        val user = auth.currentUser ?: error("Not logged in")
        val result = user.getIdToken(forceRefresh).await()
        return result.token ?: error("Firebase returned null token")
    }
}
