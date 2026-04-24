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

package com.google.android.horologist.mediasample.ui.debug

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import com.google.android.horologist.mediasample.R
import com.google.android.horologist.mediasample.ui.common.MediaScreenScaffold
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds

@Composable
fun AudioDebugScreen(
    audioDebugScreenViewModel: AudioDebugScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by audioDebugScreenViewModel.uiState.collectAsStateWithLifecycle()

    AudioDebugScreen(state = toDebugState(uiState), modifier = modifier)
}

data class AudioDebugState(
    val format: String,
    val trackOffloaded: String,
    val offloadSupported: String,
    val sleepingForOffload: String,
    val offloadScheduled: String,
    val offloadPercent: String,
    val events: List<Event>,
) {
    data class Event(val time: Long, val message: String)
}

private fun toDebugState(uiState: com.google.android.horologist.mediasample.ui.debug.AudioDebugScreenViewModel.UiState?): AudioDebugState {
    val status = uiState?.audioOffloadStatus
    val format = status?.format?.run { "$sampleMimeType $sampleRate" }.orEmpty()
    val trackOffloaded = status?.trackOffloadDescription() ?: "N/A"
    val times = status?.updateToNow()
    val enabled = times?.run { formatDuration(enabled) }.orEmpty()
    val disabled = times?.run { formatDuration(disabled) }.orEmpty()

    return AudioDebugState(
        format = format,
        trackOffloaded = trackOffloaded,
        offloadSupported = uiState?.formatSupported?.toString().orEmpty(),
        sleepingForOffload = status?.sleepingForOffload?.toString().orEmpty(),
        offloadScheduled = status?.offloadSchedulingEnabled.toString().orEmpty(),
        offloadPercent = (times?.percent ?: "") + "($enabled/$disabled)",
        events = status?.errors.orEmpty().reversed().map {
            AudioDebugState.Event(it.time, it.message)
        },
    )
}

@Composable
fun AudioDebugScreen(
    state: AudioDebugState,
    modifier: Modifier = Modifier,
) {
    val transformationSpec = rememberTransformationSpec()
    val columnState = rememberTransformingLazyColumnState()
    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.ListHeader,
        last = ColumnItemType.Button,
    )

    MediaScreenScaffold(
        scrollState = columnState,
        modifier = modifier,
        contentPadding = contentPadding,
    ) { padding ->
        TransformingLazyColumn(state = columnState, contentPadding = padding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding)
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(text = stringResource(id = R.string.sample_audio_debug))
                }
            }
            item { DebugText(stringResource(id = R.string.sample_debug_format, state.format), transformationSpec) }
            item { DebugText(stringResource(id = R.string.sample_track_offloaded, state.trackOffloaded), transformationSpec) }
            item { DebugText(stringResource(id = R.string.sample_offload_supported, state.offloadSupported), transformationSpec) }
            item { DebugText(stringResource(id = R.string.sample_debug_offload_sleeping, state.sleepingForOffload), transformationSpec) }
            item { DebugText(stringResource(id = R.string.sample_debug_offload_scheduled, state.offloadScheduled), transformationSpec) }
            item { DebugText(stringResource(id = R.string.sample_debug_offload_percent, state.offloadPercent), transformationSpec) }
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(text = stringResource(id = R.string.sample_audio_debug_events))
                }
            }
            items(state.events, key = { it.time }) { event ->
                val message = remember(event.time) {
                    val time = Instant.ofEpochMilli(event.time).atZone(ZoneId.systemDefault())
                        .toLocalTime()
                    "$time ${event.message}"
                }
                Text(
                    text = message,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                )
            }
        }
    }
}

@Composable
private fun androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope.DebugText(
    text: String,
    spec: androidx.wear.compose.material3.lazy.TransformationSpec,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .transformedHeight(this, spec),
    )
}

fun formatDuration(millis: Long): String {
    return (millis / 1000).seconds.toString()
}
