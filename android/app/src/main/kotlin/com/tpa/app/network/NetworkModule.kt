package com.tpa.app.network

import com.tpa.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

object NetworkModule {
    /** Single Retrofit instance per process, owned by [com.tpa.app.AppContainer]. */
    fun createApi(baseUrl: String = BuildConfig.BASE_URL): TpaApi {
        // Server payloads carry extra fields and optional nulls; stay lenient.
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TpaApi::class.java)
    }
}
