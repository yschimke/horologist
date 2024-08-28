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

@file:OptIn(ExperimentalWearFoundationApi::class, ExperimentalHazeMaterialsApi::class)

package com.google.android.horologist.scratch.wear

import android.view.Surface
import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.HierarchicalFocusCoordinator
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableBehavior
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.Text
import com.google.android.horologist.compose.material.Button
import com.google.android.horologist.compose.pager.PagerScreen
import com.google.android.horologist.tiles.render.TileLayoutRenderer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.time.LocalTime


@Composable
fun WearApp() {
    val context = LocalContext.current

    val tiles = listOf<TileLayoutRenderer<Unit, Unit>>(
        Calendar(context),
        HeartRate(context),
        Weather(context)
    )
    val tileCount = tiles.size
    val middle = Int.MAX_VALUE / 2

    val watchfaceOffset = middle % (tileCount + 1)

    val pagerState = rememberPagerState(initialPage = middle) {
        Int.MAX_VALUE
    }

    val surfaceRef = remember {
        mutableStateOf<Surface?>(null)
    }

    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val verticalState = remember {
        QssBehaviour(screenHeightDp)
    }

    val hazeState = remember { HazeState() }
    val hazeStyle = HazeMaterials.thin()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .rotaryScrollable(verticalState, rememberActiveFocusRequester())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState, style = hazeStyle)
        ) {
            WatchfaceScreen(surfaceRef)

            HierarchicalFocusCoordinator(requiresFocus = { false }) {
                PagerScreen(
                    state = pagerState,
                ) { rawPage ->
                    val page = ((rawPage - watchfaceOffset) % (tileCount + 1))
                    if (page != 0) {
                        val tileRenderer = tiles[page - 1]
                        TileScreen(tileRenderer)
                    }
                }
            }
        }

        Qss(modifier = Modifier.fillMaxSize(), hazeState, verticalState)
    }

    val surface = surfaceRef.value
    val paint = remember {
        android.graphics.Paint().apply {
            this.textSize = 28f
        }
    }
    val circlePaint = remember {
        android.graphics.Paint().apply {
            this.color = Color.White.toArgb()
        }
    }
    LaunchedEffect(surface) {
        surface?.let { surface ->
            while (true) {
                withFrameMillis {
                    surface.lockHardwareCanvas().apply {
                        drawColor(Color.Blue.toArgb())

                        if ((System.currentTimeMillis() % 10_000) < 2_000) {
                            drawCircle(width / 2f, height / 2f, width / 4f, circlePaint)
                        }

                        drawText(
                            LocalTime.now().toString(),
                            width / 3f,
                            height / 2f,
                            paint
                        )

                        surface.unlockCanvasAndPost(this)
                    }
                }
                delay(100)
            }
        }
    }
}

class QssBehaviour(val screenHeightDp: Dp) : RotaryScrollableBehavior {
    private val state = mutableFloatStateOf(-0.4f)
    private val _isVisible = derivedStateOf { state.floatValue < 0f }

    override suspend fun CoroutineScope.performScroll(
        timestampMillis: Long,
        delta: Float,
        inputDeviceId: Int,
        orientation: Orientation
    ) {
        state.floatValue = (state.floatValue + (delta / 150f)).coerceIn(-1f, 1f)
    }

    fun qssOffset(): Float {
        return ((-1f - state.floatValue.coerceIn(-1f, 0f)) * screenHeightDp.value).also {
            println(it)
        }
    }

    val isVisible: Boolean
        get() = _isVisible.value
}

@Composable
fun WatchfaceScreen(surfaceRef: MutableState<Surface?>) {
    AndroidEmbeddedExternalSurface(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        onSurface { surface, _, _ ->
            surfaceRef.value = surface

            surface.onDestroyed {
                surfaceRef.value = null
            }
        }
    }
}

@Composable
fun TileScreen(tileRenderer: TileLayoutRenderer<Unit, Unit>) {
    TileLayoutPreview(Unit, Unit, tileRenderer)
}

@Composable
fun Qss(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    verticalState: QssBehaviour
) {
    if (verticalState.isVisible) {
        Column(modifier = modifier
            .graphicsLayer {
                this.translationY = verticalState.qssOffset() * this.density
            }
            .clip(CircleShape)
            .hazeChild(state = hazeState)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "41%")
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), horizontalArrangement = Arrangement.Center
            ) {
                Button(imageVector = Icons.Default.Bed, contentDescription = "", onClick = { })
                Button(imageVector = Icons.Default.Power, contentDescription = "", onClick = { })
                Button(imageVector = Icons.Default.Settings, contentDescription = "", onClick = { })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    imageVector = Icons.Default.DoNotDisturbOn,
                    contentDescription = "",
                    onClick = { })
                Button(imageVector = Icons.Default.Watch, contentDescription = "", onClick = { })
                Button(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "",
                    onClick = { })
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), horizontalArrangement = Arrangement.Center
            ) {
                // TODO Pager
                Text(text = "...")
            }
        }
    }
}
