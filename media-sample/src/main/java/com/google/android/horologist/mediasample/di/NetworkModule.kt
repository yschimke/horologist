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
import android.net.wifi.WifiManager
import android.os.StrictMode
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.google.android.horologist.mediasample.catalog.UampService
import com.google.android.horologist.networks.data.DataRequestRepository
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.logging.NetworkStatusLogger
import com.google.android.horologist.networks.okhttp.NetworkAwareCallFactory
import com.google.android.horologist.networks.okhttp.NetworkSelectingCallFactory
import com.google.android.horologist.networks.rules.NetworkingRules
import com.google.android.horologist.networks.rules.NetworkingRulesEngine
import com.google.android.horologist.networks.status.HighBandwidthRequester
import com.google.android.horologist.networks.status.NetworkRepository
import com.squareup.moshi.Moshi
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.LoggingEventListener
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Simple DI implementation - to be replaced by hilt.
 */
class NetworkModule(
    private val mediaApplicationModule: MediaApplicationModule
) {
    val networkRepository: NetworkRepository by lazy {
        NetworkRepository.fromContext(
            mediaApplicationModule.application,
            mediaApplicationModule.coroutineScope,
            networkLogger
        )
    }

    val alwaysHttpsInterceptor by lazy {
        Interceptor {
            var request = it.request()

            if (request.url.scheme == "http") {
                request = request.newBuilder().url(
                    request.url.newBuilder().scheme("https").build()
                ).build()
            }

            it.proceed(request)
        }
    }

    val cacheDir by lazy {
        StrictMode.allowThreadDiskWrites().resetAfter {
            mediaApplicationModule.application.cacheDir
        }
    }

    val cache by lazy {
        Cache(
            cacheDir.resolve("HttpCache"),
            10_000_000
        )
    }

    val okhttpClient by lazy {
        OkHttpClient.Builder()
            .followSslRedirects(false)
            .addInterceptor(alwaysHttpsInterceptor)
            .eventListenerFactory(LoggingEventListener.Factory())
            .cache(cache)
            .build()
    }

    private val networkingRules: NetworkingRules by lazy {
        mediaApplicationModule.appConfig.strictNetworking!!
    }

    val networkLogger by lazy {
//        NetworkStatusLogger.InMemory()
        NetworkStatusLogger.Logging
    }

    val dataRequestRepository by lazy {
        DataRequestRepository.InMemoryDataRequestRepository
    }

    val networkingRulesEngine by lazy {
        NetworkingRulesEngine(
            networkRepository = networkRepository,
            logger = networkLogger,
            networkingRules = networkingRules
        )
    }

    private val connectivityManager: ConnectivityManager by lazy {
        mediaApplicationModule.application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val wifiManager: WifiManager by lazy {
        mediaApplicationModule.application.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val highBandwidthRequester: HighBandwidthRequester by lazy {
        HighBandwidthRequester(
            connectivityManager, mediaApplicationModule.coroutineScope, networkLogger
        )
    }

    val networkAwareCallFactory: Call.Factory by lazy {
        if (mediaApplicationModule.appConfig.strictNetworking != null) {
            NetworkSelectingCallFactory(
                networkingRulesEngine, highBandwidthRequester, dataRequestRepository, okhttpClient
            )
        } else {
            okhttpClient
        }
    }

    val moshi by lazy {
        Moshi.Builder().build()
    }

    val retrofit by lazy {
        Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl("https://storage.googleapis.com/uamp/")
            .callFactory(NetworkAwareCallFactory(networkAwareCallFactory, RequestType.ApiRequest))
            .build()
    }

    val uampService by lazy {
        retrofit.create(UampService::class.java)
    }

    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(mediaApplicationModule.application)
            .crossfade(false)
            .components {
                add(SvgDecoder.Factory())
            }
            .respectCacheHeaders(false)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .callFactory {
                NetworkAwareCallFactory(
                    networkAwareCallFactory,
                    defaultRequestType = RequestType.ImageRequest
                )
            }
            .build()
    }

    // Confusingly the result of allowThreadDiskWrites is the old policy,
    // while allow* methods immediately apply the change.
    // So `this` is the policy before we overrode it.
    fun <R> StrictMode.ThreadPolicy.resetAfter(block: () -> R) = try {
        block()
    } finally {
        StrictMode.setThreadPolicy(this)
    }
}