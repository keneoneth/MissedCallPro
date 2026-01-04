package com.cd.missedcallpro.data.remote

import com.squareup.moshi.Moshi
import com.cd.missedcallpro.data.auth.FirebaseTokenProvider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
object ApiFactory {
    fun create(
        baseUrl: String,
        tokenProvider: FirebaseTokenProvider
    ): BackendApi {

        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val okHttp = OkHttpClient.Builder()
            .addInterceptor(FirebaseAuthInterceptor(tokenProvider))
            .addInterceptor(logger)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(BackendApi::class.java)
    }
}
