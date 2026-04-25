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

package com.google.android.horologist.mediasample.ui.settings

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import ee.schimke.composeai.preview.ScrollMode
import ee.schimke.composeai.preview.ScrollingPreview

private val defaultState = DeveloperOptionsState(
    showTimeTextInfo = false,
    podcastControls = true,
    loadItemsAtStartup = false,
    animated = true,
    debugOffload = false,
    writable = true,
    networkRequestActive = false,
    streamingMode = false,
)

@Composable
private fun Screen(state: DeveloperOptionsState = defaultState) {
    AppScaffold(timeText = { TimeText(timeSource = FixedPreviewTimeSource) }) {
        DeveloperOptionsScreen(
            state = state,
            onNetworkRequestToggle = {},
            onAudioDebugClick = {},
            onSamplesClick = {},
            onShowTimeTextInfoChange = {},
            onDebugOffloadChange = {},
            onPodcastControlsChange = {},
            onLoadItemsAtStartupChange = {},
            onStreamingModeChange = {},
            onAnimatedChange = {},
            onForceStopClick = {},
            onShowTestDialogClick = {},
        )
    }
}

@WearPreviewSmallRound
@WearPreviewLargeRound
@Composable
fun DeveloperOptionsScreenPreview() {
    Screen()
}

@WearPreviewLargeRound
@ScrollingPreview(modes = [ScrollMode.LONG])
@Composable
fun DeveloperOptionsScreenLongPreview() {
    Screen()
}
