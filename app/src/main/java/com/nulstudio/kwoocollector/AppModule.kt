package com.nulstudio.kwoocollector

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.nulstudio.kwoocollector.net.ApiService
import com.nulstudio.kwoocollector.net.MockInterceptor
import com.nulstudio.kwoocollector.ui.LoginViewModel
import com.nulstudio.kwoocollector.ui.dashboard.DashboardViewModel
import com.nulstudio.kwoocollector.ui.dataexplorer.DataExplorerViewModel
import com.nulstudio.kwoocollector.ui.profile.ProfileViewModel
import com.nulstudio.kwoocollector.ui.splash.SplashViewModel
import com.nulstudio.kwoocollector.util.TokenManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val appModule = module {
    single { TokenManager(context = get()) }

    single {
        val tokenManager: TokenManager = get()

        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = runBlocking { tokenManager.tokenFlow.firstOrNull() }

                val requestBuilder = chain.request().newBuilder()
                if (!token.isNullOrBlank()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }

                chain.proceed(requestBuilder.build())
            }
            // .addInterceptor(MockInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    single<ApiService> {
        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()

        Retrofit.Builder()
            // .baseUrl("http://106.75.215.104:1081/")
            .baseUrl("http://192.168.137.1:1081/")
            .client(get())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }

    viewModel { SplashViewModel(apiService = get(), tokenManager = get()) }

    viewModel {
        LoginViewModel(
            apiService = get(),
            tokenManager = get(),
            context = androidContext()
        )
    }

    viewModel { DataExplorerViewModel(get()) }

    viewModel {
        ProfileViewModel(
            apiService = get(),
            tokenManager = get(),
            context = androidContext()
        )
    }

    viewModel { DashboardViewModel(apiService = get()) }
}
