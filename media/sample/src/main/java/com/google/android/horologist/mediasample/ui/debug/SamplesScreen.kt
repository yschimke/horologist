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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import com.google.android.horologist.media.ui.navigation.MediaNavController.navigateToPlayer
import com.google.android.horologist.mediasample.R
import com.google.android.horologist.mediasample.ui.common.MediaScreenScaffold

@Composable
fun SamplesScreen(
    samplesScreenViewModel: SamplesScreenViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val uiState by samplesScreenViewModel.uiState.collectAsStateWithLifecycle()

    SamplesScreen(
        samples = uiState.samples.map { SampleItem(it.id, it.name) },
        onSampleClick = { id ->
            samplesScreenViewModel.playSamples(id)
            navController.navigateToPlayer()
        },
        modifier = modifier,
    )
}

data class SampleItem(val id: Int, val name: String)

@Composable
fun SamplesScreen(
    samples: List<SampleItem>,
    onSampleClick: (Int) -> Unit,
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
                    Text(text = stringResource(id = R.string.sample_samples))
                }
            }
            items(samples, key = { it.id }) { sample ->
                Button(
                    onClick = { onSampleClick(sample.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    label = { Text(sample.name) },
                )
            }
        }
    }
}
