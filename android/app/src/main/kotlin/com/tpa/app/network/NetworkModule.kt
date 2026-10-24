package com.tpa.app.network

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tpa.app.BuildConfig
import com.tpa.app.network.TpaApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

object NetworkModule {
    @Composable
    fun provideApi(): TpaApi {
        val context = LocalContext.current
        val baseUrl = BuildConfig.BASE_URL
        
        return remember(context, baseUrl) {
            // Server payloads carry extra fields (symbol/asOf/provenance/v, etc.) and may
            // send nulls for optional values; be lenient so real responses deserialize.
            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                explicitNulls = false
            }
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(TpaApi::class.java)
        }
    }
}
