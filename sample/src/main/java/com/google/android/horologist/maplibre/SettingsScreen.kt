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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.scrollTransform
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults.ItemType
import com.google.android.horologist.maplibre.MapDI.angel
import com.google.android.horologist.maplibre.MapDI.styleUrl
import com.google.android.horologist.maplibre.PreferencesKeys.AllowLte
import com.google.android.horologist.maplibre.PreferencesKeys.ForceOffline
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.offline.OfflineGeometryRegionDefinition
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineRegion
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val contentPadding =
        ScalingLazyColumnDefaults.padding(first = ItemType.Text, last = ItemType.Chip)
    val scrollState = rememberTransformingLazyColumnState()

    val context = LocalContext.current
    val preferencesDataStore = context.preferences

    val preferences by preferencesDataStore.data.collectAsStateWithLifecycle(null)
    val coroutineScope = rememberCoroutineScope()

    val offlineRegionsState = remember { mutableStateOf<List<OfflineRegion>?>(null) }

    LaunchedEffect(Unit) {
        MapDI.offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onError(error: String) {
                println("Error while listing offline region: $error")
            }

            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                offlineRegionsState.value = offlineRegions?.toList().orEmpty()
            }
        })
    }

    ScreenScaffold(
        scrollState = scrollState
    ) {
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            contentPadding = contentPadding()
        ) {
            item {
                ListHeader {
                    Text("Settings")
                }
            }
            item {
                SwitchButton(
                    checked = preferences?.get(AllowLte) == true,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            preferencesDataStore.edit {
                                it[AllowLte] = checked
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .scrollTransform(this),
                    enabled = preferences != null
                ) {
                    Text("Allow LTE")
                }
            }
            item {
                SwitchButton(
                    checked = preferences?.get(ForceOffline) == true,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            preferencesDataStore.edit {
                                println("forceOffline $checked")
                                it[ForceOffline] = checked
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .scrollTransform(this),
                    enabled = preferences != null
                ) {
                    Text("Offline Mode")
                }
            }
            item {
                Button(
                    onClick = {
                        MapDI.offlineManager.clearAmbientCache(null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .scrollTransform(this)
                ) {
                    Text("Clear Cache")
                }
            }
            item {
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .scrollTransform(this)
                ) {
                    Text("Licenses")
                }
            }
            item {
                ListHeader {
                    Text("Offline Regions")
                }
            }
            val offlineRegions = offlineRegionsState.value
            if (offlineRegions != null) {
                items(offlineRegions) {
                    Text("${it.id} ${it.definition.bounds?.center}")
                }
            } else {
                item {
                    Text("Loading...")
                }
            }
            item {
                Button(
                    onClick = {
                        MapDI.offlineManager.createOfflineRegion(
                            OfflineGeometryRegionDefinition(
                                styleUrl,
                                Polygon.fromLngLats(
                                    listOf(
                                        listOf(
                                            fromLngLat(
                                                angel.longitude() - 1.0,
                                                angel.latitude() - 1.0
                                            ),
                                            fromLngLat(
                                                angel.longitude() + 1.0,
                                                angel.latitude() - 1.0
                                            ),
                                            fromLngLat(
                                                angel.longitude() + 1.0,
                                                angel.latitude() + 1.0
                                            ),
                                            fromLngLat(
                                                angel.longitude() - 1.0,
                                                angel.latitude() + 1.0
                                            ),
                                            fromLngLat(
                                                angel.longitude() - 1.0,
                                                angel.latitude() - 1.0
                                            ),
                                        )
                                    )
                                ),
                                12.0,
                                20.0,
                                1.0f,
                                false
                            ),
                            ByteArray(0),
                            object : OfflineManager.CreateOfflineRegionCallback {
                                override fun onCreate(offlineRegion: OfflineRegion) {
                                    offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                                }

                                override fun onError(error: String) {
                                    println("Error while creating offline region: $error")
                                }
                            })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .scrollTransform(this)
                ) {
                    Text("Cache Angel Offline")
                }
            }
        }
    }
}