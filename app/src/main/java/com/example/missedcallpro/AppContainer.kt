package com.example.missedcallpro

import com.example.missedcallpro.data.auth.FirebaseTokenProvider
import com.example.missedcallpro.data.remote.ApiFactory
import com.google.firebase.auth.FirebaseAuth

class AppContainer {
    private val tokenProvider = FirebaseTokenProvider(FirebaseAuth.getInstance())
    val api = ApiFactory.create(BuildConfig.BACKEND_URL, tokenProvider)
}
