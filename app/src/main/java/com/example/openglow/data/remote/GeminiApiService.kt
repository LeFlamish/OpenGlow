package com.example.openglow.data.remote

import com.example.openglow.data.remote.dto.GeminiGenerateContentRequest
import com.example.openglow.data.remote.dto.GeminiGenerateContentResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApiService {
    @Headers("Content-Type: application/json")
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiGenerateContentRequest,
    ): GeminiGenerateContentResponse
}
