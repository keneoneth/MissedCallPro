package com.example.missedcallpro.data.remote

import com.example.missedcallpro.data.FilterNumberCreate
import com.example.missedcallpro.data.FilterNumberOut
import com.example.missedcallpro.data.FilterRuleUpdate
import com.example.missedcallpro.data.GoogleConfirmReq
import com.example.missedcallpro.data.GoogleConfirmResp
import com.example.missedcallpro.data.GoogleRestoreReq
import com.example.missedcallpro.data.GoogleRestoreResp
import com.example.missedcallpro.data.PlanDto
import com.example.missedcallpro.data.SubscriptionDto
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

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

    @POST("subscriptions/google/confirm")
    suspend fun confirmGoogleSubscription(@Body req: GoogleConfirmReq): GoogleConfirmResp

    @POST("subscriptions/google/restore")
    suspend fun restoreGoogleSubscription(@Body req: GoogleRestoreReq): GoogleRestoreResp

    // country allow/block rules
    @GET("filter-rule")
    suspend fun getFilterRule(): FilterRuleUpdate

    @PUT("filter-rule")
    suspend fun putFilterRule(@Body body: FilterRuleUpdate): FilterRuleUpdate

    @GET("filter-numbers")
    suspend fun listFilterNumbers(): List<FilterNumberOut>

    @POST("filter-numbers")
    suspend fun addFilterNumber(@Body body: FilterNumberCreate): FilterNumberOut

    @DELETE("filter-numbers/{id}")
    suspend fun deleteFilterNumber(@Path("id") id: Long): Map<String, Any>
}
