package com.cd.missedcallpro

import com.cd.missedcallpro.data.auth.FirebaseTokenProvider
import com.cd.missedcallpro.data.remote.ApiFactory
import com.google.firebase.auth.FirebaseAuth

class AppContainer {
    private val tokenProvider = FirebaseTokenProvider(FirebaseAuth.getInstance())
    val api = ApiFactory.create(BuildConfig.BACKEND_URL, tokenProvider)
}
