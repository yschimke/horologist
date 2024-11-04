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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import com.google.android.horologist.compose.rotaryinput.accumulatedBehavior
import com.google.android.horologist.maplibre.MapDI.maxZoom
import com.google.android.horologist.maplibre.MapDI.minZoom
import com.google.android.horologist.maplibre.MapDI.styleUrl
import com.maplibre.compose.MapView
import com.maplibre.compose.camera.CameraState
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.rememberSaveableMapControls
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.maplibre.compose.settings.AttributionSettings
import com.maplibre.compose.settings.CompassSettings
import com.maplibre.compose.settings.LogoSettings
import com.maplibre.compose.settings.MapControlPosition
import com.maplibre.compose.settings.MapControls

@Composable
fun MapScreen(onSettingsClick: () -> Unit) {
    val mapViewCamera = rememberSaveableMapViewCamera(
        initialCamera = MapViewCamera(
            state = CameraState.Centered(
                51.5355285, -0.1073767, zoom = 15.0
            )
        )
    )

    val initialMapControls = MapControls(
        attribution = AttributionSettings.initWithPosition(
            position = MapControlPosition.BottomCenter()
        ), compass = CompassSettings.initWithPosition(
        enabled = true, position = MapControlPosition.Center()
    ), logo = LogoSettings(enabled = false)
    )

    val mapControls = rememberSaveableMapControls(
        initialMapControls = initialMapControls
    )

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotaryScrollable(
                    mapZoomBehaviour(mapViewCamera),
                    focusRequester = rememberActiveFocusRequester()
                )
        ) {
            MapView(
                modifier = Modifier.fillMaxSize(),
                styleUrl = styleUrl,
                camera = mapViewCamera,
                mapControls = mapControls,
            )
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.CenterEnd),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color.DarkGray,
                    containerColor = Color.DarkGray.copy(alpha = 0.2f)
                ),
                shapes = IconButtonDefaults.animatedShapes()
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}

@Composable
private fun mapZoomBehaviour(mapViewCamera: MutableState<MapViewCamera>) =
    accumulatedBehavior {
        val state = mapViewCamera.value.state as CameraState.Centered

        val newZoom = (state.zoom + if (it > 0f) {
            1f
        } else {
            -1f
        }).coerceIn(minZoom, maxZoom)

        mapViewCamera.value = mapViewCamera.value.copy(state = state.copy(zoom = newZoom))
    }