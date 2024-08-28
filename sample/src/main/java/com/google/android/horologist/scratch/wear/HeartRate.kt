/*
 * Copyright 2024 The Android Open Source Project
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
package com.google.android.horologist.scratch.wear

import android.content.Context
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import com.google.android.horologist.tiles.render.SingleTileLayoutRenderer

class HeartRate(context: Context) : SingleTileLayoutRenderer<Unit, Unit>(context) {

    fun simpleLayout(
        context: Context,
        deviceParameters: DeviceParameters,
        heartRateBpm: Int,
        clickable: Clickable
    ) = PrimaryLayout.Builder(deviceParameters)
        .setPrimaryLabelTextContent(
            Text.Builder(context, "Now")
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setColor(ColorBuilders.argb(GoldenTilesColors.White))
                .build()
        )
        .setContent(
            Text.Builder(context, heartRateBpm.toString())
                .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                .setColor(ColorBuilders.argb(GoldenTilesColors.White))
                .build()

        )
        .setSecondaryLabelTextContent(
            Text.Builder(context, "bpm")
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .setColor(ColorBuilders.argb(GoldenTilesColors.LightGray))
                .build()
        )
        .setPrimaryChipContent(
            CompactChip.Builder(context, "Measure", clickable, deviceParameters)
                .setChipColors(
                    ChipColors(
                        /*backgroundColor=*/
                        ColorBuilders.argb(GoldenTilesColors.LightRed),
                        /*contentColor=*/
                        ColorBuilders.argb(GoldenTilesColors.Black)
                    )
                )
                .build()
        )
        .build()

    override fun renderTile(
        state: Unit,
        deviceParameters: DeviceParameters
    ): LayoutElementBuilders.LayoutElement = simpleLayout(
        context, deviceParameters,
        heartRateBpm = 80,
        clickable = emptyClickable
    )
}
