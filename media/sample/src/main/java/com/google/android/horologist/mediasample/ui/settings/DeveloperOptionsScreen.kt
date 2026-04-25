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

package com.google.android.horologist.mediasample.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import com.google.android.horologist.mediasample.R
import com.google.android.horologist.mediasample.ui.common.MediaScreenScaffold
import com.google.android.horologist.mediasample.ui.navigation.UampNavigationScreen

@Composable
fun DeveloperOptionsScreen(
    developerOptionsScreenViewModel: DeveloperOptionsScreenViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val uiState by developerOptionsScreenViewModel.uiState.collectAsStateWithLifecycle()

    DeveloperOptionsScreen(
        state = DeveloperOptionsState(
            showTimeTextInfo = uiState.showTimeTextInfo,
            podcastControls = uiState.podcastControls,
            loadItemsAtStartup = uiState.loadItemsAtStartup,
            animated = uiState.animated,
            debugOffload = uiState.debugOffload,
            writable = uiState.writable,
            networkRequestActive = uiState.networkRequest != null,
            streamingMode = uiState.streamingMode,
        ),
        modifier = modifier,
        onNetworkRequestToggle = { developerOptionsScreenViewModel.toggleNetworkRequest() },
        onAudioDebugClick = { navController.navigate(UampNavigationScreen.AudioDebug.route) },
        onSamplesClick = { navController.navigate(UampNavigationScreen.Samples.route) },
        onShowTimeTextInfoChange = developerOptionsScreenViewModel::setShowTimeTextInfo,
        onDebugOffloadChange = developerOptionsScreenViewModel::setDebugOffload,
        onPodcastControlsChange = developerOptionsScreenViewModel::setPodcastControls,
        onLoadItemsAtStartupChange = developerOptionsScreenViewModel::setLoadItemsAtStartup,
        onStreamingModeChange = developerOptionsScreenViewModel::setStreamingMode,
        onAnimatedChange = developerOptionsScreenViewModel::setAnimated,
        onForceStopClick = { developerOptionsScreenViewModel.forceStop() },
        onShowTestDialogClick = {
            developerOptionsScreenViewModel.showDialog(
                // stringResource inside the lambda would require a snapshot; pre-read below.
                it,
            )
        },
    )
}

data class DeveloperOptionsState(
    val showTimeTextInfo: Boolean = false,
    val podcastControls: Boolean = false,
    val loadItemsAtStartup: Boolean = false,
    val animated: Boolean = true,
    val debugOffload: Boolean = false,
    val writable: Boolean = false,
    val networkRequestActive: Boolean = false,
    val streamingMode: Boolean = false,
)

@Composable
fun DeveloperOptionsScreen(
    state: DeveloperOptionsState,
    onNetworkRequestToggle: () -> Unit,
    onAudioDebugClick: () -> Unit,
    onSamplesClick: () -> Unit,
    onShowTimeTextInfoChange: (Boolean) -> Unit,
    onDebugOffloadChange: (Boolean) -> Unit,
    onPodcastControlsChange: (Boolean) -> Unit,
    onLoadItemsAtStartupChange: (Boolean) -> Unit,
    onStreamingModeChange: (Boolean) -> Unit,
    onAnimatedChange: (Boolean) -> Unit,
    onForceStopClick: () -> Unit,
    onShowTestDialogClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transformationSpec = rememberTransformationSpec()
    val columnState = rememberTransformingLazyColumnState()
    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.ListHeader,
        last = ColumnItemType.Button,
    )
    val errorMessage = stringResource(id = R.string.sample_error)

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
                    Text(text = stringResource(id = R.string.sample_developer_options))
                }
            }
            item {
                DevCheckboxItem(
                    transformationSpec,
                    state.networkRequestActive,
                    stringResource(id = R.string.request_network),
                    state.writable,
                ) { onNetworkRequestToggle() }
            }
            item {
                DevActionButton(
                    transformationSpec,
                    stringResource(id = R.string.sample_audio_debug),
                    onAudioDebugClick,
                )
            }
            item {
                DevActionButton(
                    transformationSpec,
                    stringResource(id = R.string.sample_samples),
                    onSamplesClick,
                )
            }
            item {
                DevCheckboxItem(
                    transformationSpec,
                    state.showTimeTextInfo,
                    stringResource(id = R.string.show_time_text_info),
                    state.writable,
                    onShowTimeTextInfoChange,
                )
            }
            item {
                DevCheckboxItem(
                    transformationSpec,
                    state.debugOffload,
                    stringResource(id = R.string.debug_offload),
                    state.writable,
                    onDebugOffloadChange,
                )
            }
            item {
                DevCheckboxItem(
                    transformationSpec,
                    state.podcastControls,
                    stringResource(id = R.string.podcast_controls),
                    state.writable,
                    onPodcastControlsChange,
                )
            }
            item {
                DevCheckboxItem(
                    transformationSpec,
                    state.loadItemsAtStartup,
                    stringResource(id = R.string.load_items),
                    state.writable,
                    onLoadItemsAtStartupChange,
                )
            }
            item {
                DevCheckboxItem(
                    transformationSpec,
                    state.streamingMode,
                    stringResource(id = R.string.streaming_mode),
                    state.writable,
                    onStreamingModeChange,
                )
            }
            item {
                DevCheckboxItem(
                    transformationSpec,
                    state.animated,
                    stringResource(id = R.string.animated),
                    state.writable,
                    onAnimatedChange,
                )
            }
            item {
                DevActionButton(
                    transformationSpec,
                    stringResource(id = R.string.force_stop),
                    onForceStopClick,
                )
            }
            item {
                DevActionButton(
                    transformationSpec,
                    stringResource(id = R.string.show_test_dialog),
                ) { onShowTestDialogClick(errorMessage) }
            }
        }
    }
}

@Composable
private fun androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope.DevActionButton(
    spec: androidx.wear.compose.material3.lazy.TransformationSpec,
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
            .transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        colors = ButtonDefaults.filledTonalButtonColors(),
        label = { Text(text) },
    )
}

@Composable
private fun androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope.DevCheckboxItem(
    spec: androidx.wear.compose.material3.lazy.TransformationSpec,
    checked: Boolean,
    text: String,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    CheckboxButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
            .transformedHeight(this, spec),
        transformation = SurfaceTransformation(spec),
        label = { Text(text) },
    )
}
