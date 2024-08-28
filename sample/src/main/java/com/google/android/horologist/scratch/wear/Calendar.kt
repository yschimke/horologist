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
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import com.google.android.horologist.tiles.render.SingleTileLayoutRenderer

class Calendar(context: Context) : SingleTileLayoutRenderer<Unit, Unit>(context) {

    fun layout(
        context: Context,
        deviceParameters: DeviceParameters,
        eventTime: String,
        eventName: String,
        eventLocation: String,
        clickable: Clickable
    ) = PrimaryLayout.Builder(deviceParameters)
        .setPrimaryLabelTextContent(
            Text.Builder(context, eventTime)
                .setColor(ColorBuilders.argb(GoldenTilesColors.LightBlue))
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .build()
        )
        .setContent(
            Text.Builder(context, eventName)
                .setMaxLines(3)
                .setColor(ColorBuilders.argb(GoldenTilesColors.White))
                .setTypography(Typography.TYPOGRAPHY_BODY1)
                .build()
        )
        .setSecondaryLabelTextContent(
            Text.Builder(context, eventLocation)
                .setColor(ColorBuilders.argb(GoldenTilesColors.Gray))
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .build()
        )
        .setPrimaryChipContent(
            CompactChip.Builder(context, "Agenda", clickable, deviceParameters).build()
        )
        .build()

    override fun renderTile(
        state: Unit,
        deviceParameters: DeviceParameters
    ): LayoutElementBuilders.LayoutElement = layout(
        context, deviceParameters,
        eventTime = "6:30-7:30 PM",
        eventName = "Morning Pilates with Christina Lloyd",
        eventLocation = "216 Market Street",
        clickable = emptyClickable
    )
}

val emptyClickable = ModifiersBuilders.Clickable.Builder()
    .setOnClick(ActionBuilders.LoadAction.Builder().build())
    .setId("")
    .build()
