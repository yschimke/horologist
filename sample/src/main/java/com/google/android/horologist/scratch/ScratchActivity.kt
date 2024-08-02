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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults.scalingParams
import androidx.wear.compose.foundation.lazy.ScalingParams
import androidx.wear.compose.material.LocalContentAlpha
import androidx.wear.compose.material.LocalContentColor
import androidx.wear.compose.material.LocalTextStyle
import androidx.wear.compose.material.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import com.google.android.horologist.compose.layout.AppScaffold
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults.ItemType
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults.padding
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.material.ListHeaderDefaults.firstItemPadding
import com.google.android.horologist.compose.material.ResponsiveListHeader

class ScratchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    AppScaffold(modifier = Modifier.background(Color.Black)) {
        val columnState = rememberResponsiveColumnState(
            contentPadding = padding(
                first = ItemType.Text,
                last = ItemType.Text
            )
        ).copy(
            scalingParams = scalingParams(
                edgeScale = 1f,
                edgeAlpha = 1f,
            ),
        )
        ScreenScaffold(scrollState = columnState) {
            ScalingLazyColumn(columnState = columnState) {
                item {
                    ResponsiveListHeader(
                        contentColor = Color.White,
                        contentPadding = firstItemPadding()
                    ) {
                        val color = Color.Unspecified
                        val style = LocalTextStyle.current
                        val localContentColor =
                            LocalContentColor.current.copy(alpha = LocalContentAlpha.current)

                        val textColor = color.takeOrElse {
                            style.color.takeOrElse {
                                localContentColor
                            }
                        }

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = "Header ${textColor.hex}"
                        )
                    }
                }
                item {
                    ResponsiveListHeader(
                        contentColor = Color.White,
                        contentPadding = firstItemPadding()
                    ) {
                        val color = Color.Unspecified
                        val style = LocalTextStyle.current
                        val localContentColor =
                            LocalContentColor.current.copy(alpha = LocalContentAlpha.current)

                        val textColor = color.takeOrElse {
                            style.color.takeOrElse {
                                localContentColor
                            }
                        }

                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            text = "Header ${textColor.hex}"
                        )
                    }
                }
                item {
                    ResponsiveListHeader(
                        contentColor = Color.White,
                        contentPadding = firstItemPadding()
                    ) {
                        val style = LocalTextStyle.current
                        BasicText(
                            modifier = Modifier.fillMaxWidth(),
                            text = "style ${style.color.hex}",
                            color = { style.color },
                        )
                    }
                }
                item {
                    ResponsiveListHeader(
                        contentColor = Color.White,
                        contentPadding = firstItemPadding()
                    ) {
                        val localContentColor =
                            LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                        BasicText(
                            modifier = Modifier.fillMaxWidth(),
                            text = "content color ${localContentColor.hex}",
                            color = { localContentColor }
                        )
                    }
                }
                item {
                    ResponsiveListHeader(
                        contentColor = Color.White,
                        contentPadding = firstItemPadding()
                    ) {
                        BasicText(
                            modifier = Modifier.fillMaxWidth(),
                            text = "White ${Color.White.hex}",
                            color = { Color.White }
                        )
                    }
                }
            }
        }
    }
}

val Color.hex
    get() = String.format("#%06X", 0xFFFFFF and toArgb())

@WearPreviewLargeRound
@Composable
fun WearAppPreview() {
    WearApp()
}

public fun ScalingLazyColumnState.copy(scalingParams: ScalingParams): ScalingLazyColumnState = ScalingLazyColumnState(
    initialScrollPosition = initialScrollPosition,
    timeTextHomeOffset = timeTextHomeOffset,
    autoCentering = autoCentering,
    anchorType = anchorType,
    contentPadding = contentPadding,
    rotaryMode = rotaryMode,
    reverseLayout = reverseLayout,
    verticalArrangement = verticalArrangement,
    horizontalAlignment = horizontalAlignment,
    userScrollEnabled = userScrollEnabled,
    scalingParams = scalingParams,
)
