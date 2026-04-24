/*
 * Copyright 2026 The Android Open Source Project
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

package com.google.android.horologist.mediasample.ui.debug

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import com.google.android.horologist.mediasample.ui.settings.FixedPreviewTimeSource
import ee.schimke.composeai.preview.ScrollMode
import ee.schimke.composeai.preview.ScrollingPreview

private val state = AudioDebugState(
    format = "audio/mpeg 44100",
    trackOffloaded = "N/A",
    offloadSupported = "true",
    sleepingForOffload = "false",
    offloadScheduled = "true",
    offloadPercent = "72%(3m/1m)",
    events = listOf(
        AudioDebugState.Event(1, "Offload started"),
        AudioDebugState.Event(2, "Buffer underrun"),
        AudioDebugState.Event(3, "Offload disabled — output device changed"),
    ),
)

@Composable
private fun Screen() {
    AppScaffold(timeText = { TimeText(timeSource = FixedPreviewTimeSource) }) {
        AudioDebugScreen(state = state)
    }
}

@WearPreviewSmallRound
@WearPreviewLargeRound
@Composable
fun AudioDebugScreenPreview() {
    Screen()
}

@WearPreviewLargeRound
@ScrollingPreview(modes = [ScrollMode.LONG])
@Composable
fun AudioDebugScreenLongPreview() {
    Screen()
}
