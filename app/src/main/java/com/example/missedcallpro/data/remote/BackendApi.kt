package com.example.missedcallpro.data.remote

import com.example.missedcallpro.data.PlanDto
import com.example.missedcallpro.data.SubscriptionDto
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
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

    @GET("/plans")
    suspend fun getPlans(): List<PlanDto>

    @GET("/subscriptions/me")
    suspend fun getSubscription(): SubscriptionDto
}
