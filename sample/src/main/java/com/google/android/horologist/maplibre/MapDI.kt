/*
 * Copyright 2024 The Android Open Source Project
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

package com.google.android.horologist.maplibre

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.horologist.components.SampleApplication
import com.google.android.horologist.maplibre.PreferencesKeys.AllowLte
import com.google.android.horologist.maplibre.PreferencesKeys.ForceOffline
import com.google.android.horologist.networks.data.DataRequest
import com.google.android.horologist.networks.data.DataRequestRepository
import com.google.android.horologist.networks.data.NetworkInfo
import com.google.android.horologist.networks.data.NetworkType
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.status.NetworkRepository
import com.google.android.horologist.sample.BuildConfig.MAP_TILER_KEY
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.module.http.HttpRequestImpl
import com.mapbox.mapboxsdk.offline.OfflineManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

object MapDI {
    lateinit var networkOfflineFlow: StateFlow<Boolean>
    lateinit var networkRepository: NetworkRepository
    lateinit var dataRequestRepository: DataRequestRepository
    lateinit var mapOkHttpClient: OkHttpClient

    @SuppressLint("StaticFieldLeak")
    lateinit var offlineManager: OfflineManager

    val styleUrl =
        "https://api.maptiler.com/maps/uk-openzoomstack-night/style.json?key=$MAP_TILER_KEY"

    val angel = Point.fromLngLat(51.5355285, -0.1073767)

    val minZoom = 10.0
    val maxZoom = 18.0

    @OptIn(DelicateCoroutinesApi::class)
    fun init(applicationContext: SampleApplication) {
        Mapbox.getInstance(
            applicationContext,
            MAP_TILER_KEY,
            WellKnownTileServer.MapTiler
        )

        dataRequestRepository = applicationContext.dataRequestRepository
        networkRepository = applicationContext.networkRepository

        networkOfflineFlow =
            networkRepository.networkStatus.combine(applicationContext.preferences.data) { network, preferences ->
                if (preferences[ForceOffline] == true) {
                    println("setConnected(false) because offline forced")
                    true
                } else if (network.activeNetwork?.networkInfo?.type in listOf(
                        NetworkType.Wifi,
                        NetworkType.BT
                    )) {
                    println("setConnected(true) because on " + network.activeNetwork?.networkInfo?.type)
                    false
                } else {
                    val lteAllowed = preferences[AllowLte] == true
                    println("setConnected(available) because of LTE allowed lteAllowed on " + network.activeNetwork?.networkInfo?.type)
                    !lteAllowed
                }
            }.stateIn(GlobalScope, started = SharingStarted.WhileSubscribed(5_000), true)

        GlobalScope.launch {
            networkOfflineFlow.collect { offline ->
                Mapbox.setConnected(!offline)
            }
        }

        mapOkHttpClient = OkHttpClient.Builder().addInterceptor(
            HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BASIC)
                redactQueryParams("key")
            },
        )
            .eventListener(object : EventListener() {
                override fun callStart(call: Call) {
                    println("callStart ${call.request().url}")
                }

                override fun cacheConditionalHit(
                    call: Call,
                    cachedResponse: Response
                ) {
                    println("cacheConditionalHit ${call.request().url}")
                }

                override fun cacheHit(call: Call, response: Response) {
                    println("cacheHit ${call.request().url}")
                }

                override fun cacheMiss(call: Call) {
                    println("cacheMiss ${call.request().url}")
                }

                override fun requestBodyEnd(call: Call, byteCount: Long) {
                    dataRequestRepository.storeRequest(
                        DataRequest(
                            RequestType.ApiRequest,
                            NetworkInfo.Unknown(),
                            byteCount
                        )
                    )
                }

                override fun responseBodyEnd(call: Call, byteCount: Long) {
                    dataRequestRepository.storeRequest(
                        DataRequest(
                            RequestType.ApiRequest,
                            NetworkInfo.Unknown(),
                            byteCount
                        )
                    )
                }

                override fun requestHeadersEnd(call: Call, request: Request) {
                    dataRequestRepository.storeRequest(
                        DataRequest(
                            RequestType.ApiRequest,
                            NetworkInfo.Unknown(),
                            request.headers.byteCount()
                        )
                    )
                }

                override fun responseHeadersEnd(call: Call, response: Response) {
                    dataRequestRepository.storeRequest(
                        DataRequest(
                            RequestType.ApiRequest,
                            NetworkInfo.Unknown(),
                            response.headers.byteCount()
                        )
                    )
                }
            })
            .addNetworkInterceptor {
                val request = it.request()

                val forceCache =
                    request.url.encodedPath.startsWith("/fonts/") || request.url.encodedPath.startsWith(
                        "/maps/"
                    ) || request.url.pathSegments.last() in listOf(
                        "tiles.json",
                        "style.json"
                    )

                val response = it.proceed(request)

                if (forceCache) {
                    response.newBuilder()
                        .header("Cache-Control", "public, max-age=8640000")
                        .build()
                } else {
                    response
                }
            }
            .build()

        HttpRequestImpl.setOkHttpClient(mapOkHttpClient)
        HttpRequestImpl.enableLog(true)
        HttpRequestImpl.enablePrintRequestUrlOnFailure(true)

        offlineManager = OfflineManager.getInstance(applicationContext).apply {
            setMaximumAmbientCacheSize(100_000_000, null)
            setOfflineMapboxTileCountLimit(10_000)
        }
    }
}

private const val USER_PREFERENCES_NAME = "map_preferences"

val Context.preferences by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

object PreferencesKeys {
    val ForceOffline = booleanPreferencesKey("force_offline")
    val AllowLte = booleanPreferencesKey("allow_lte")
}