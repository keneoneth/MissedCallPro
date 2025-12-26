package com.example.missedcallpro.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.POST
@JsonClass(generateAdapter = true)
data class BootstrapResponse(
    val user_id: String,
    val is_new: Boolean,
    val email: String?
)

interface BackendApi {
    @POST("auth/bootstrap")
    suspend fun bootstrap(): BootstrapResponse
}
