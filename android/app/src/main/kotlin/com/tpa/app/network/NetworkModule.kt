package com.tpa.app.network

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.tpa.app.BuildConfig
import com.tpa.app.network.TpaApi
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.KotlinSerializationConverterFactory
import retrofit2.create

object NetworkModule {
    @Composable
    fun provideApi(): TpaApi {
        val context = LocalContext.current
        val baseUrl = BuildConfig.BASE_URL
        
        return remember(context, baseUrl) {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(KotlinSerializationConverterFactory.create())
                .build()
                .create(TpaApi::class.java)
        }
    }
}