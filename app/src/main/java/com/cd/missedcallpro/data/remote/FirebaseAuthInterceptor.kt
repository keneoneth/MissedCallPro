package com.cd.missedcallpro.data.remote

import com.cd.missedcallpro.data.auth.FirebaseTokenProvider
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class FirebaseAuthInterceptor(
    private val tokenProvider: FirebaseTokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            tokenProvider.getIdToken(forceRefresh = false)
        }

        val req = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(req)
    }
}
