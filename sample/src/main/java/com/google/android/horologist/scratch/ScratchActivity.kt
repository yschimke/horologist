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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.IconToggleButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
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
            val mode = remember { mutableStateOf(ScrollMode.Spacer) }
            WearApp(mode = mode.value, onChange = { mode.value = ScrollMode.entries.toList()[(mode.value.ordinal + 1) % 3] })
        }
    }
}

enum class ScrollMode {
    Offset, Padding, Spacer
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

//        println(if (scrollList) "list" else "offset")

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
    -1 to Icons.Default.SpeakerPhone,
//    4 to Icons.Default.NightlightRound,
//    5 to Icons.Default.FlashlightOn,
//    6 to Icons.Default.Place,
//    7 to Icons.Default.DoDisturbOn,
//    8 to Icons.Default.Bluetooth,
//    9 to Icons.Default.Home,
//    10 to Icons.Default.Battery3Bar,
//    11 to Icons.Default.AirplanemodeActive,
)

@Composable
fun WearApp(mode: ScrollMode, onChange: () -> Unit = {}) {
    val buttonStates = remember { mutableStateMapOf<Int, Boolean>() }

    val columnState = rememberTransformingLazyColumnState()
    val screenOffset =
        if (mode == ScrollMode.Offset) rememberScreenOffsetScrollableState(columnState) else RotaryScrollableDefaults.behavior(
            columnState
        )

    val contentPadding = when (mode) {
        ScrollMode.Offset -> rememberResponsiveColumnPadding(
            first = ColumnItemType.ButtonRow, last = ColumnItemType.ButtonRow
        )

        ScrollMode.Padding -> {
            val normal = rememberResponsiveColumnPadding(
                first = ColumnItemType.ButtonRow, last = ColumnItemType.ButtonRow
            )
            val layoutDirection = LocalLayoutDirection.current
            PaddingValues(
                start = normal.calculateLeftPadding(layoutDirection),
                top = normal.calculateTopPadding(),
                end = normal.calculateRightPadding(layoutDirection),
                bottom = LocalConfiguration.current.screenHeightDp.dp - 20.dp
            )
        }

        ScrollMode.Spacer -> rememberResponsiveColumnPadding(
            first = ColumnItemType.ButtonRow, last = ColumnItemType.ButtonRow
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
                    if (mode == ScrollMode.Offset) {
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
                            if (mode == ScrollMode.Offset) {
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
                        if (row == 0 && column == 0) {
                            TextButton(
                                onClick = onChange,
                                colors = TextButtonDefaults.outlinedTextButtonColors(),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                            ) {
                                Text("$mode", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                            }
                            return@repeat
                        }

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

                                Row {
                                    val s = "Nrml %.2f\n%.2f".format(
                                        scrollProgress?.topOffsetFraction ?: 0f,
                                        scrollProgress?.bottomOffsetFraction ?: 0f
                                    )
                                    Text(
                                        text = s,
                                        style = MaterialTheme.typography.bodyExtraSmall.copy(
                                            fontSize = 8.sp
                                        ),
                                        textAlign = TextAlign.Center,
                                        maxLines = 3, modifier = Modifier.weight(1f)
                                    )

                                    if (mode == ScrollMode.Offset) {
                                        val offSetProgress =
                                            scrollProgress?.withOffset((screenOffset as ScreenOffsetScrollableState).screenOffsetPercent)
                                        val s2 = "Ofst %.2f\n%.2f".format(
                                            offSetProgress?.topOffsetFraction ?: 0f,
                                            offSetProgress?.bottomOffsetFraction ?: 0f
                                        )
                                        Text(
                                            text = s2,
                                            style = MaterialTheme.typography.bodyExtraSmall.copy(
                                                fontSize = 8.sp
                                            ),
                                            textAlign = TextAlign.Center,
                                            maxLines = 3, modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(
                    modifier = Modifier
                        .height(LocalConfiguration.current.screenHeightDp.dp)
                        .background(Color.DarkGray)
                )
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
