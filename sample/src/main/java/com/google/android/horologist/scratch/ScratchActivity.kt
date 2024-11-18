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

package com.google.android.horologist.scratch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DoDisturbOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastRoundToInt
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableBehavior
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.IconToggleButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextToggleButton
import androidx.wear.compose.material3.TextToggleButtonDefaults
import androidx.wear.compose.material3.lazy.scrollTransform
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import kotlinx.coroutines.CoroutineScope

class ScratchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearApp(useOffsets = true)
        }
    }
}

class ScreenOffsetScrollableState(
    val columnState: TransformingLazyColumnState,
    val columnRotaryBehavior: RotaryScrollableBehavior,
) : RotaryScrollableBehavior {
    val screenOffsetPx = mutableFloatStateOf(0f)
    val screenOffsetPercent = derivedStateOf { screenOffsetPx.floatValue / 454f }

    override suspend fun CoroutineScope.performScroll(
        timestampMillis: Long, delta: Float, inputDeviceId: Int, orientation: Orientation
    ) {
        val scrollList = if (screenOffsetPx.floatValue < 0f) {
            false
        } else if (delta > 0f) {
            columnState.canScrollForward
        } else {
            true
        }

//        println("${if (scrollList) "list" else "offset"} $delta csf: ${columnState.canScrollForward} offset: ${screenOffsetPx.floatValue}")

        println(if (scrollList) "list" else "offset")

        if (scrollList) {
            with(columnRotaryBehavior) {
                performScroll(timestampMillis, delta, inputDeviceId, orientation)
            }
        } else {
            screenOffsetPx.floatValue = (screenOffsetPx.floatValue - delta / 2f).coerceIn(
                -454f, 0f
            )
        }
    }
}

@Composable
fun rememberScreenOffsetScrollableState(columnState: TransformingLazyColumnState): ScreenOffsetScrollableState {
    val columnRotaryBehavior = RotaryScrollableDefaults.behavior(columnState)
    return remember {
        ScreenOffsetScrollableState(columnState, columnRotaryBehavior)
    }
}

val icons = mapOf(
    3 to Icons.Default.SpeakerPhone,
    4 to Icons.Default.NightlightRound,
    5 to Icons.Default.FlashlightOn,
    6 to Icons.Default.Place,
    7 to Icons.Default.DoDisturbOn,
    8 to Icons.Default.Bluetooth,
    9 to Icons.Default.Home,
    10 to Icons.Default.Battery3Bar,
    11 to Icons.Default.AirplanemodeActive,
)

@Composable
fun WearApp(useOffsets: Boolean) {
    val buttonStates = remember { mutableStateMapOf<Int, Boolean>() }

    val columnState = rememberTransformingLazyColumnState()
    val screenOffset =
        if (useOffsets) rememberScreenOffsetScrollableState(columnState) else RotaryScrollableDefaults.behavior(columnState)

    val contentPadding = if (useOffsets) rememberResponsiveColumnPadding(
        first = ColumnItemType.ButtonRow, last = ColumnItemType.ButtonRow
    ) else {
        val normal = rememberResponsiveColumnPadding(
            first = ColumnItemType.ButtonRow, last = ColumnItemType.ButtonRow
        )
        val layoutDirection = LocalLayoutDirection.current
        PaddingValues(
            start = normal.calculateLeftPadding(layoutDirection),
            top = normal.calculateTopPadding(),
            end = normal.calculateRightPadding(layoutDirection),
            bottom = LocalConfiguration.current.screenHeightDp.dp
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .rotaryScrollable(
                screenOffset, focusRequester = rememberActiveFocusRequester()
            )
    ) {
        TransformingLazyColumn(
            state = columnState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (useOffsets) {
                        translationY =
                            (screenOffset as ScreenOffsetScrollableState).screenOffsetPx.floatValue
                    }
                }
                .border(1.dp, Color.DarkGray, CircleShape),
            contentPadding = contentPadding,
            userScrollEnabled = false,
            rotaryScrollableBehavior = null,
        ) {
            items(4) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .run {
                            if (useOffsets) {
                                scrollTransformWithOffset(
                                    this@items,
                                    offset = (screenOffset as ScreenOffsetScrollableState).screenOffsetPercent
                                )
                            } else {
                                scrollTransform(this@items)
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(
                        10.dp, alignment = CenterHorizontally
                    )
                ) {
                    repeat(3) { column ->
                        val index = row * 3 + column
                        val buttonState = buttonStates.getOrPut(index) { false }
                        val icon = icons[index]

                        if (icon != null) {
                            IconToggleButton(
                                onCheckedChange = { checked ->
                                    buttonStates[index] = checked
                                },
                                checked = buttonState,
                                shapes = IconToggleButtonDefaults.variantAnimatedShapes()
                            ) {
                                Icon(imageVector = icon, contentDescription = null)
                            }
                        } else {
                            TextToggleButton(
                                onCheckedChange = { checked ->
                                    buttonStates[index] = checked
                                },
                                checked = buttonState,
                                shapes = TextToggleButtonDefaults.variantAnimatedShapes()
                            ) {
                                val scrollProgress =
                                    columnState.layoutInfo.visibleItems.fastFirstOrNull { it.index == row }?.scrollProgress
                                val s = "%.2f\n%.2f".format(
                                    scrollProgress?.topOffsetFraction ?: 0f,
                                    scrollProgress?.bottomOffsetFraction ?: 0f
                                )
                                Text(
                                    text = s,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.scrollTransformWithOffset(
    scope: TransformingLazyColumnItemScope,
    offset: State<Float>,
): Modifier = with(scope) {
    var minMorphingHeight by remember { mutableStateOf<Float?>(null) }
    val spec =
        remember { LazyColumnScrollTransformBehavior { minMorphingHeight } }

    this@scrollTransformWithOffset then
            TargetMorphingHeightConsumerModifierElement {
                minMorphingHeight = it?.toFloat()
            }
                .transformedHeight { height, scrollProgress ->
                    with(spec) {
                        scrollProgress.withOffset(offset).placementHeight(height.toFloat())
                            .fastRoundToInt()
                    }
                }
                .graphicsLayer { contentTransformation(spec) { scrollProgress?.withOffset(offset) } }
}

fun TransformingLazyColumnItemScrollProgress.withOffset(offset: State<Float>): TransformingLazyColumnItemScrollProgress {
    return TransformingLazyColumnItemScrollProgress(
        topOffsetFraction = topOffsetFraction + offset.value,
        bottomOffsetFraction = bottomOffsetFraction + offset.value
    )
}