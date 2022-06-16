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

@file:OptIn(ExperimentalHorologistComposeToolsApi::class, ExperimentalHorologistTilesApi::class)

package com.google.android.horologist.fitness

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.wear.tiles.ColorBuilders.argb
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.tiles.ModifiersBuilders.Clickable
import androidx.wear.tiles.material.Chip
import androidx.wear.tiles.material.ChipColors
import androidx.wear.tiles.material.Colors
import androidx.wear.tiles.material.CompactChip
import androidx.wear.tiles.material.Text
import androidx.wear.tiles.material.Typography
import androidx.wear.tiles.material.layouts.PrimaryLayout
import com.google.android.horologist.compose.tools.ExperimentalHorologistComposeToolsApi
import com.google.android.horologist.compose.tools.TileLayoutPreview
import com.google.android.horologist.compose.tools.WearPreviewDevices
import com.google.android.horologist.compose.tools.WearPreviewFontSizes
import com.google.android.horologist.tiles.ExperimentalHorologistTilesApi
import com.google.android.horologist.tiles.render.SingleTileLayoutRenderer

class FitnessActivityTileRenderer(context: Context) :
    SingleTileLayoutRenderer<FitnessActivityTileRenderer.TileState, Unit>(context) {
    override fun createTheme(): Colors = FitnessTheme

    override fun renderTile(
        state: TileState,
        deviceParameters: DeviceParameters
    ): LayoutElementBuilders.LayoutElement {
        return PrimaryLayout.Builder(deviceParameters)
            .setPrimaryLabelTextContent(title(state))
            .setContent(startRunChip(DummyClickable, deviceParameters, state))
            .setPrimaryChipContent(moreChip(DummyClickable, deviceParameters))
            .build()
    }

    internal fun title(state: TileState) =
        Text.Builder(context, "${state.distance} · ${state.time}")
            .setTypography(Typography.TYPOGRAPHY_CAPTION1)
            .setColor(argb(theme.onPrimary))
            .build()

    internal fun startRunChip(
        clickable: Clickable,
        deviceParameters: DeviceParameters,
        state: TileState
    ) = Chip.Builder(context, clickable, deviceParameters)
        .setPrimaryLabelContent(state.startAction)
        .setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)
        .setChipColors(ChipColors.primaryChipColors(theme))
        .build()

    internal fun moreChip(
        clickable: Clickable,
        deviceParameters: DeviceParameters
    ) = CompactChip.Builder(context, "More", clickable, deviceParameters)
        .setChipColors(ChipColors.secondaryChipColors(theme))
        .build()

    data class TileState(val distance: String, val time: String, val startAction: String)
}

@OptIn(ExperimentalHorologistComposeToolsApi::class, ExperimentalHorologistTilesApi::class)
@WearPreviewDevices
@WearPreviewFontSizes
@Composable
fun FitnessActivityTilePreview() {
    val context = LocalContext.current

    val tileState = remember {
        FitnessActivityTileRenderer.TileState(
            distance = "12km",
            time = "2 days ago",
            startAction = "Start run"
        )
    }

    val renderer = remember {
        FitnessActivityTileRenderer(context)
    }

    TileLayoutPreview(
        tileState,
        Unit,
        renderer
    )
}
