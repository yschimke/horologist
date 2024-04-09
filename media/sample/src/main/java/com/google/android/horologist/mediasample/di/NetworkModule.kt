/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.android.horologist.mediasample.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.http.HttpEngine
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Looper
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.google.android.horologist.mediasample.BuildConfig
import com.google.android.horologist.mediasample.data.api.KtorUampService
import com.google.android.horologist.mediasample.data.api.UampService
import com.google.android.horologist.mediasample.data.api.UampService.Companion.BASE_URL
import com.google.android.horologist.mediasample.data.api.WearArtworkUampService
import com.google.android.horologist.mediasample.ui.AppConfig
import com.google.android.horologist.networks.data.DataRequestRepository
import com.google.android.horologist.networks.data.InMemoryDataRequestRepository
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.highbandwidth.HighBandwidthNetworkMediator
import com.google.android.horologist.networks.highbandwidth.StandardHighBandwidthNetworkMediator
import com.google.android.horologist.networks.logging.NetworkStatusLogger
import com.google.android.horologist.networks.okhttp.DeferredCallFactory
import com.google.android.horologist.networks.okhttp.NetworkAwareCallFactory
import com.google.android.horologist.networks.okhttp.NetworkSelectingCallFactory
import com.google.android.horologist.networks.okhttp.impl.NetworkLoggingEventListenerFactory
import com.google.android.horologist.networks.request.NetworkRequester
import com.google.android.horologist.networks.request.NetworkRequesterImpl
import com.google.android.horologist.networks.rules.NetworkingRulesEngine
import com.google.android.horologist.networks.status.NetworkRepository
import com.google.android.horologist.networks.status.NetworkRepositoryImpl
import com.google.common.collect.ImmutableSet
import com.hypercubetools.ktor.moshi.moshi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.LoggingEventListener
import java.io.File
import java.util.Optional
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun networkRepository(
        @ApplicationContext application: Context,
        @ForApplicationScope coroutineScope: CoroutineScope,
    ): NetworkRepository = NetworkRepositoryImpl.fromContext(
        application,
        coroutineScope,
    )

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
        println("okhttpClient")
        check(Looper.getMainLooper().thread != Thread.currentThread())

        return OkHttpClient.Builder().followSslRedirects(false)
            .addInterceptor(alwaysHttpsInterceptor)
            .eventListenerFactory(LoggingEventListener.Factory()).cache(cache).build()
    }

    @Provides
    fun networkLogger(): NetworkStatusLogger = NetworkStatusLogger.Logging

    @Singleton
    @Provides
    fun dataRequestRepository(): DataRequestRepository =
        InMemoryDataRequestRepository()

    @Provides
    fun connectivityManager(
        @ApplicationContext application: Context,
    ): ConnectivityManager =
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    fun wifiManager(
        @ApplicationContext application: Context,
    ): WifiManager = application.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @Singleton
    @Provides
    fun aggregatingHighBandwidthRequester(
        networkLogger: NetworkStatusLogger,
        networkRequester: NetworkRequester,
        @ForApplicationScope coroutineScope: CoroutineScope,
    ) = StandardHighBandwidthNetworkMediator(
        networkLogger,
        networkRequester,
        coroutineScope,
        3.seconds,
    )

    @Singleton
    @Provides
    fun highBandwidthRequester(
        highBandwidthNetworkMediator: StandardHighBandwidthNetworkMediator,
    ): HighBandwidthNetworkMediator = highBandwidthNetworkMediator

    @Singleton
    @Provides
    fun networkRequester(
        connectivityManager: ConnectivityManager,
    ): NetworkRequester = NetworkRequesterImpl(
        connectivityManager,
    )

    @Singleton
    @Provides
    fun networkAwareCallFactory(
        appConfig: AppConfig,
        okhttpClient: dagger.Lazy<OkHttpClient>,
        networkingRulesEngine: dagger.Lazy<NetworkingRulesEngine>,
        highBandwidthNetworkMediator: dagger.Lazy<HighBandwidthNetworkMediator>,
        dataRequestRepository: DataRequestRepository,
        networkRepository: NetworkRepository,
        @ForApplicationScope coroutineScope: CoroutineScope,
        logger: NetworkStatusLogger,
    ): Call.Factory {
        println("networkAwareCallFactory")
        val client = okhttpClient.get()
        return DeferredCallFactory {
            if (appConfig.strictNetworking != null) {
                NetworkSelectingCallFactory(
                    networkingRulesEngine.get(),
                    highBandwidthNetworkMediator.get(),
                    networkRepository,
                    dataRequestRepository,
                    client,
                    coroutineScope,
                    logger = logger,
                )
            } else {
                client.newBuilder()
                    .eventListenerFactory(
                        NetworkLoggingEventListenerFactory(
                            logger,
                            networkRepository,
                            client.eventListenerFactory,
                            dataRequestRepository,
                        ),
                    )
                    .build()
            }
        }
    }

    @Singleton
    @Provides
    fun moshi() = Moshi.Builder().build()

    @Singleton
    @Provides
    fun httpClient(
        okHttpClient: dagger.Lazy<OkHttpClient>,
        moshi: Moshi
    ): HttpClient {
        println("httpClient")
        return HttpClient(OkHttp) {
            engine {
                preconfigured
            }

            defaultRequest {
                url(BASE_URL)
            }

            install(ContentNegotiation) {
                moshi(moshi)
            }
        }
    }

    @Singleton
    @Provides
    fun uampService(
        client: dagger.Lazy<HttpClient>,
    ): UampService = WearArtworkUampService(
        KtorUampService(client),
    )

    @Singleton
    @Provides
    fun imageLoader(
        @ApplicationContext application: Context,
        @CacheDir cacheDir: File,
        callFactory: dagger.Lazy<Call.Factory>,
    ): ImageLoader {
        println("imageLoader")
        return ImageLoader.Builder(application)
            .crossfade(false)
            .components {
                add(SvgDecoder.Factory())
            }
            .respectCacheHeaders(false).diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .callFactory {
                NetworkAwareCallFactory(
                    callFactory.get(),
                    defaultRequestType = RequestType.ImageRequest,
                )
            }.apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }.build()
    }
    val flow = MutableStateFlow<ImmutableSet<Int>>(ImmutableSet.of())

    fun update(newValue: Int) {
        flow.update {
            ImmutableSet.builderWithExpectedSize<Int>(it.size + 1)
                .addAll(it)
                .add(newValue)
                .build()
        }
    }

    @Singleton
    @Provides
    fun httpEngine(
        @ApplicationContext application: Context,
    ): Optional<HttpEngine> {
        return if (Build.VERSION.SDK_INT >= 34) {
            val httpEngine = HttpEngine.Builder(application)
                .setEnableBrotli(true)
                .addQuicHint("media.githubusercontent.com", 443, 443)
                .build()
            Optional.of(httpEngine)
        } else {
            Optional.empty()
        }
    }
}
