package com.example.missedcallpro.data.remote

import com.example.missedcallpro.data.DeviceMissedCallRequest
import com.example.missedcallpro.data.DeviceMissedCallResponse
import com.example.missedcallpro.data.FeedbackCreate
import com.example.missedcallpro.data.FeedbackResponse
import com.example.missedcallpro.data.FilterNumberCreate
import com.example.missedcallpro.data.FilterNumberOut
import com.example.missedcallpro.data.FilterRuleUpdate
import com.example.missedcallpro.data.FormResponseDto
import com.example.missedcallpro.data.FormUpdateRequestDto
import com.example.missedcallpro.data.GoogleConfirmReq
import com.example.missedcallpro.data.GoogleConfirmResp
import com.example.missedcallpro.data.GoogleRestoreReq
import com.example.missedcallpro.data.GoogleRestoreResp
import com.example.missedcallpro.data.MissedCallsResponse
import com.example.missedcallpro.data.PlanDto
import com.example.missedcallpro.data.SmsSettingsDto
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
    @POST("/auth/bootstrap")
    suspend fun bootstrap(): BootstrapResponse

    @POST("/auth/logout")
    suspend fun logout(): LogoutResponse

    @POST("/auth/deleteAccount")
    suspend fun deleteAccount(): DeleteAccountResponse

    @GET("/plans")
    suspend fun getPlans(): List<PlanDto>

    @GET("/subscriptions/me")
    suspend fun getSubscription(): SubscriptionDto

    @POST("/subscriptions/google/confirm")
    suspend fun confirmGoogleSubscription(@Body req: GoogleConfirmReq): GoogleConfirmResp

    @POST("/subscriptions/google/restore")
    suspend fun restoreGoogleSubscription(@Body req: GoogleRestoreReq): GoogleRestoreResp

    // country allow/block rules
    @GET("/filter-rule")
    suspend fun getFilterRule(): FilterRuleUpdate

    @PUT("/filter-rule")
    suspend fun putFilterRule(@Body body: FilterRuleUpdate): FilterRuleUpdate

    @GET("/filter-numbers")
    suspend fun listFilterNumbers(): List<FilterNumberOut>

    @POST("/filter-numbers")
    suspend fun addFilterNumber(@Body body: FilterNumberCreate): FilterNumberOut

    @DELETE("/filter-numbers/{id}")
    suspend fun deleteFilterNumber(@Path("id") id: Long): Map<String, Any>

    @POST("/feedback")
    suspend fun submitFeedback(@Body body: FeedbackCreate): FeedbackResponse

    @POST("/device/missed-call")
    suspend fun reportDeviceMissedCall(
        @Body body: DeviceMissedCallRequest
    ): DeviceMissedCallResponse

    @GET("/settings/sms")
    suspend fun getSmsSettings(): SmsSettingsDto

    @PUT("/settings/sms")
    suspend fun putSmsSettings(@Body body: SmsSettingsDto): SmsSettingsDto

    @GET("/form")
    suspend fun getForm(): FormResponseDto

    @PUT("/form")
    suspend fun updateForm(@Body req: FormUpdateRequestDto): FormResponseDto

    @GET("/device/missed-calls")
    suspend fun listMissedCalls(@Query("limit") limit: Int = 100): MissedCallsResponse
}
