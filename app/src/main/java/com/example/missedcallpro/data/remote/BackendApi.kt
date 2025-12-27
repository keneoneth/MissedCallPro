package com.example.missedcallpro.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST
@JsonClass(generateAdapter = true)
data class BootstrapResponse(
    val username: String,
    val email: String,
)

@JsonClass(generateAdapter = true)
data class LogoutResponse(
    val ok: Boolean,
)
@JsonClass(generateAdapter = true)
data class DeleteAccountResponse(
    val ok: Boolean,
)
interface BackendApi {
    @POST("auth/bootstrap")
    suspend fun bootstrap(): BootstrapResponse

    @POST("auth/logout")
    suspend fun logout(): LogoutResponse

    @POST("auth/deleteAccount")
    suspend fun deleteAccount(): DeleteAccountResponse
}
