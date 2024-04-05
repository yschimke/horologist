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

package com.google.android.horologist.scratch.wear

import android.view.Surface
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.AndroidExternalSurfaceZOrder
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.google.android.horologist.compose.pager.PagerScreen
import com.google.android.horologist.compose.tools.TileLayoutPreview
import com.google.android.horologist.tiles.render.TileLayoutRenderer
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

    PagerScreen(state = pagerState) { rawPage ->
        val page = ((rawPage - watchfaceOffset) % (tileCount + 1))
        if (page == 0) {
            WatchfaceScreen(surfaceRef)
        } else {
            val tileRenderer = tiles[page - 1]
            TileScreen(tileRenderer)
        }
    }

    val surface = surfaceRef.value
    val paint = remember {
        android.graphics.Paint().apply {
            this.textSize = 28f
        }
    }
    LaunchedEffect(surface) {
        surface?.let { surface ->
            while (true) {
                withFrameMillis {
                    surface.lockHardwareCanvas().apply {
                        drawColor(Color.Blue.toArgb())

                        drawText(
                            LocalTime.now().toString(),
                            this.width / 3f,
                            this.height / 2f,
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

@Composable
fun WatchfaceScreen(surfaceRef: MutableState<Surface?>) {
    AndroidExternalSurface(
        modifier = Modifier
            .fillMaxSize(),
        zOrder = AndroidExternalSurfaceZOrder.Behind
    ) {
        onSurface { surface, _, _ ->
            surfaceRef.value = surface

            surface.onDestroyed {
                surfaceRef.value = null
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun TileScreen(tileRenderer: TileLayoutRenderer<Unit, Unit>) {
    TileLayoutPreview(Unit, Unit, tileRenderer)
}