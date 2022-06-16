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
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement
import androidx.wear.tiles.ModifiersBuilders.Clickable
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.material.Button
import androidx.wear.tiles.material.ButtonColors
import androidx.wear.tiles.material.ChipColors
import androidx.wear.tiles.material.Colors
import androidx.wear.tiles.material.CompactChip
import androidx.wear.tiles.material.Text
import androidx.wear.tiles.material.Typography
import androidx.wear.tiles.material.layouts.MultiButtonLayout
import androidx.wear.tiles.material.layouts.PrimaryLayout
import com.google.android.horologist.compose.tools.ExperimentalHorologistComposeToolsApi
import com.google.android.horologist.compose.tools.LayoutElementPreview
import com.google.android.horologist.compose.tools.TileLayoutPreview
import com.google.android.horologist.compose.tools.WearPreviewDevices
import com.google.android.horologist.compose.tools.WearPreviewFontSizes
import com.google.android.horologist.fitness.FitnessActivitiesTileRenderer.Companion.addActivityIcons
import com.google.android.horologist.sample.R
import com.google.android.horologist.tile.FullWidthPreview
import com.google.android.horologist.tiles.ExperimentalHorologistTilesApi
import com.google.android.horologist.tiles.images.drawableResToImageResource
import com.google.android.horologist.tiles.render.SingleTileLayoutRenderer

class FitnessActivitiesTileRenderer(context: Context) :
    SingleTileLayoutRenderer<FitnessActivitiesTileRenderer.TileState, Unit>(context) {
    override fun createTheme(): Colors = FitnessTheme

    override fun renderTile(
        state: TileState,
        deviceParameters: DeviceParameters
    ): LayoutElement {
        return PrimaryLayout.Builder(deviceParameters)
            .setPrimaryLabelTextContent(title())
            .setContent(startActivityButtons(DummyClickable))
            .setPrimaryChipContent(moreChip(DummyClickable, deviceParameters))
            .build()
    }

    internal fun title() = Text.Builder(context, "Start Activity")
        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
        .setColor(argb(theme.onSurface))
        .build()

    internal fun startActivityButtons(clickable: Clickable) =
        MultiButtonLayout.Builder()
            .addButtonContent(iconButton(clickable, NordicIcon))
            .addButtonContent(iconButton(clickable, CycleIcon))
            .addButtonContent(iconButton(clickable, SittingIcon))
            .build()

    internal fun moreChip(
        clickable: Clickable,
        deviceParameters: DeviceParameters
    ) = CompactChip.Builder(context, "More", clickable, deviceParameters)
        .setChipColors(ChipColors.secondaryChipColors(theme))
        .build()

    internal fun iconButton(clickable: Clickable, icon: String) =
        Button.Builder(context, clickable)
            .setIconContent(icon)
            .setButtonColors(ButtonColors.primaryButtonColors(theme))
            .build()

    override fun Resources.Builder.produceRequestedResources(
        resourceResults: Unit,
        deviceParameters: DeviceParameters,
        resourceIds: MutableList<String>
    ) {
        addActivityIcons()
    }

    data class TileState(val count: Int)

    companion object {
        internal fun Resources.Builder.addActivityIcons() {
            addIdToImageMapping(NordicIcon, drawableResToImageResource(R.drawable.ic_nordic))
            addIdToImageMapping(CycleIcon, drawableResToImageResource(R.drawable.ic_cycle))
            addIdToImageMapping(SittingIcon, drawableResToImageResource(R.drawable.ic_sitting))
        }

        const val NordicIcon = "ic_nordic.xml"
        const val CycleIcon = "ic_cycle.xml"
        const val SittingIcon = "ic_sitting.xml"
    }
}

public val DummyClickable: Clickable = Clickable.Builder()
    .setId("click")
    .build()

@OptIn(ExperimentalHorologistComposeToolsApi::class, ExperimentalHorologistTilesApi::class)
@WearPreviewDevices
@WearPreviewFontSizes
@Composable
fun FitnessActivitiesTilePreview() {
    val context = LocalContext.current

    val tileState = remember { FitnessActivitiesTileRenderer.TileState(0) }

    val renderer = remember {
        FitnessActivitiesTileRenderer(context)
    }

    TileLayoutPreview(
        tileState,
        Unit,
        renderer
    )
}

@FullWidthPreview
@Composable
fun FitnessActivitiesActivitiesPreview() {
    FitnessActivitiesComponentDisplay(tileResourcesFn = {
        addActivityIcons()
    }) {
        this.startActivityButtons(DummyClickable)
    }
}

@Composable
fun FitnessActivitiesComponentDisplay(
    tileResourcesFn: Resources.Builder.() -> Unit = {},
    block: FitnessActivitiesTileRenderer.() -> LayoutElement,
) {
    val context = LocalContext.current

    val renderer = remember {
        FitnessActivitiesTileRenderer(context)
    }

    LayoutElementPreview(element = renderer.block()) {
        tileResourcesFn()
    }
}
