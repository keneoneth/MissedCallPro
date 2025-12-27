package com.example.missedcallpro.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST
@JsonClass(generateAdapter = true)
data class BootstrapResponse(
    val username: String,
    val access_token: String,
    val refresh_token: String
)

@JsonClass(generateAdapter = true)
data class LogoutResponse(
    val ok: Boolean,
)
@JsonClass(generateAdapter = true)
data class LogoutRequest(val refresh_token: String)
interface BackendApi {
    @POST("auth/bootstrap")
    suspend fun bootstrap(): BootstrapResponse

    @POST("auth/logout")
    suspend fun logout(@Body req: LogoutRequest): LogoutResponse

}
