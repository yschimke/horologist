/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.horologist.auth.sample.shared

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import com.google.android.gms.auth.api.identity.Identity
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationAuthModule {

    @Singleton
    @Provides
    fun authorizationClient(
        @ApplicationContext context: Context,
    ) = Identity.getAuthorizationClient(context)

    @Singleton
    @Provides
    public fun servicesCoroutineScope(): CoroutineScope {
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e(
                "SampleApplication",
                "Uncaught exception thrown by a service: ${throwable.message}",
                throwable,
            )
        }
        return CoroutineScope(Dispatchers.IO + SupervisorJob() + coroutineExceptionHandler)
    }

    @Singleton
    @Provides
    fun cache(
        @ApplicationContext application: Context,
    ): Cache = Cache(
        application.cacheDir.resolve("HttpCache"),
        10_000_000,
    )

    @Singleton
    @Provides
    fun alwaysHttpsInterceptor(): Interceptor = Interceptor {
        var request = it.request()

        if (request.url.scheme == "http") {
            request = request.newBuilder().url(
                request.url.newBuilder().scheme("https").build(),
            ).build()
        }

        it.proceed(request)
    }

    @Singleton
    @Provides
    fun okhttpClient(
        cache: Cache,
        alwaysHttpsInterceptor: Interceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder().followSslRedirects(false)
            .addInterceptor(alwaysHttpsInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = Level.BODY
            })
//            .eventListenerFactory(LoggingEventListener.Factory())
            .cache(cache)
            .build()
    }

    @Singleton
    @Provides
    fun json(): Json {
        return Json {
            encodeDefaults = false
        }
    }

    @Singleton
    @Provides
    fun httpClient(
        okhttp: OkHttpClient,
        json: Json
    ): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                preconfigured = okhttp
            }
            install(ContentNegotiation) {
                json()
            }
        }
    }

    @Singleton
    @Provides
    fun passkeyAuthRepository(http: HttpClient): PasskeyAuthRepository {
        return PasskeyAuthRepository(http)
    }

    @Singleton
    @Provides
    fun moshi() = Moshi.Builder().build()
}
