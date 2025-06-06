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

package com.google.android.horologist.mediasample.sc
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import coil.compose.rememberImagePainter
import com.google.android.horologist.media.ui.state.model.TrackPositionUiModel
import com.google.android.horologist.mediasample.sc.model.Waveform

@Composable
fun PlayScreenWaveform(
    modifier: Modifier,
    currentMediaItem: MediaItem?,
    progress: TrackPositionUiModel.Actual?,
    playing: Boolean?,
) {
    val soundcloudViewModel = hiltViewModel<SoundcloudViewModel>()
    val waveformUrl =
        remember(currentMediaItem) { currentMediaItem?.mediaMetadata?.subtitle?.toString() }
    if (waveformUrl?.endsWith(".png") == true) {
        Image(
            painter = rememberImagePainter(waveformUrl),
            contentDescription = "Waveform",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        WaveformCanvas(waveformUrl, soundcloudViewModel, modifier, progress, playing)
    }
}

@Composable
private fun WaveformCanvas(
    waveformUrl: String?,
    soundcloudViewModel: SoundcloudViewModel,
    modifier: Modifier,
    progress: TrackPositionUiModel.Actual?,
    playing: Boolean?,
) {
    var waveform by remember { mutableStateOf<Waveform?>(null) }
    if (waveformUrl != null && !waveformUrl.endsWith(".png")) {
        LaunchedEffect(key1 = waveformUrl) {
            if (waveformUrl != null && waveformUrl.startsWith("https://")) {
                waveform = soundcloudViewModel.waveform(waveformUrl)
            }
        }
    }
    if (waveform != null) {
        WaveformChart(
            modifier = modifier,
            waveform = waveform!!,
            waveformColour = WaveformColours(),
            completionPercent = progress?.percent ?: 0f,
            playing = playing ?: false,
        )
    }
}
