package com.example.missedcallpro.auth

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import com.example.missedcallpro.R
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class GoogleAuthClient(
    private val context: Context
) {
    private val auth by lazy { Firebase.auth }
    private val credentialManager by lazy { CredentialManager.create(context) }

    suspend fun signInWithGoogle(): FirebaseUser? {
        return try {
            // 1. Prepare the Google ID Request
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // 2. Fetch the Credential (Suspends here until user picks an account)
            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            // 3. Extract and Authenticate
            handleCredential(result.credential)
        } catch (e: GetCredentialException) {
            Log.e("AuthRepo", "Credential Manager error", e)
            null
        } catch (e: Exception) {
            Log.e("AuthRepo", "Firebase Auth error", e)
            null
        }
    }

    private suspend fun handleCredential(credential: Credential): FirebaseUser? {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

            // Sign in to Firebase and return the user
            return auth.signInWithCredential(authCredential).await().user
        }
        return null
    }

    private suspend fun signOutUser() {
        // 1. Firebase Sign Out (Synchronous)
        auth.signOut()

        // 2. Clear Credential Manager (Asynchronous/Suspending)
        try {
            val clearRequest = ClearCredentialStateRequest()
            // clearCredentialState is a suspend function provided by the library
            credentialManager.clearCredentialState(clearRequest)

        } catch (e: ClearCredentialException) {
            Log.e(TAG, "Couldn't clear user credentials: ${e.localizedMessage}")
        }
    }

    fun isSignedIn(): Boolean = auth.currentUser != null
}