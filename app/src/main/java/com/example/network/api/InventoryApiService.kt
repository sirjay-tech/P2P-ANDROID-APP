package com.example.network.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface InventoryApiService {

    @GET("exec") // Typical webapp macros deploy macro endpoint
    suspend fun fetchInventory(): Response<List<InventoryDto>>

    @POST("exec")
    suspend fun submitTransaction(@Body transaction: InventoryDto): Response<InventoryDto>

    @JsonClass(generateAdapter = true)
    data class InventoryDto(
        @Json(name = "id") val id: Int?,
        @Json(name = "coin") val coin: String?,
        @Json(name = "type") val type: String?, // "BUY" or "SELL"
        @Json(name = "quantity") val quantity: Double?,
        @Json(name = "price") val price: Double?,
        @Json(name = "totalCost") val totalCost: Double?,
        @Json(name = "wallet") val wallet: String?,
        @Json(name = "date") val date: String?,
        @Json(name = "notes") val notes: String?
    )

    companion object {
        private const val DEFAULT_BASE_URL = "https://script.google.com/macros/s/placeholder-deployment/"

        fun create(customUrl: String? = null): InventoryApiService {
            val url = if (!customUrl.isNullOrEmpty()) {
                if (customUrl.endsWith("/")) customUrl else "$customUrl/"
            } else {
                DEFAULT_BASE_URL
            }

            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logger)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            return retrofit.create(InventoryApiService::class.java)
        }
    }
}
