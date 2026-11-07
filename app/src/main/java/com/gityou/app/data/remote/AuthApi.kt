package com.gityou.app.data.remote

import com.gityou.app.data.remote.dto.AccessTokenResponse
import com.gityou.app.data.remote.dto.DeviceCodeResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * GitHub OAuth endpoints (hosted on github.com, not api.github.com).
 * Both calls must answer with JSON, hence the Accept header.
 */
interface AuthApi {

    @POST("login/device/code")
    @Headers("Accept: application/json")
    @FormUrlEncoded
    suspend fun deviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String,
    ): DeviceCodeResponse

    @POST("login/oauth/access_token")
    @Headers("Accept: application/json")
    @FormUrlEncoded
    suspend fun accessToken(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String,
    ): AccessTokenResponse
}
