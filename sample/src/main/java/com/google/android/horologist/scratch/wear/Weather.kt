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
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.ResourceBuilders.AndroidImageResourceByResId
import androidx.wear.protolayout.ResourceBuilders.ImageResource
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.MultiSlotLayout
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import com.google.android.horologist.tiles.render.SingleTileLayoutRenderer

class Weather(context: Context) : SingleTileLayoutRenderer<Unit, Unit>(context) {

    val SCATTERED_SHOWERS_ICON_ID = "weathericon"

    fun layout(
        context: Context,
        deviceParameters: DeviceParameters,
        location: String,
        weatherIconId: String,
        currentTemperature: String,
        lowTemperature: String,
        highTemperature: String,
        weatherSummary: String
    ) = PrimaryLayout.Builder(deviceParameters)
        .setPrimaryLabelTextContent(
            Text.Builder(context, location)
                .setColor(ColorBuilders.argb(GoldenTilesColors.Blue))
                .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                .build()
        )
        .setContent(
            MultiSlotLayout.Builder()
                .addSlotContent(
                    Image.Builder()
                        .setWidth(dp(32f))
                        .setHeight(dp(32f))
                        .setResourceId(weatherIconId)
                        .build()
                )
                .addSlotContent(
                    Text.Builder(context, currentTemperature)
                        .setTypography(Typography.TYPOGRAPHY_DISPLAY1)
                        .setColor(ColorBuilders.argb(GoldenTilesColors.White))
                        .build()
                )
                .addSlotContent(
                    Column.Builder()
                        .addContent(
                            Text.Builder(context, highTemperature)
                                .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                .setColor(ColorBuilders.argb(GoldenTilesColors.White))
                                .build()
                        )
                        .addContent(
                            Text.Builder(context, lowTemperature)
                                .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                .setColor(ColorBuilders.argb(GoldenTilesColors.Gray))
                                .build()
                        )
                        .build()
                )
                .build()
        )
        .setSecondaryLabelTextContent(
            Text.Builder(context, weatherSummary)
                .setColor(ColorBuilders.argb(GoldenTilesColors.White))
                .setTypography(Typography.TYPOGRAPHY_TITLE3)
                .build()
        )
        .build()

    override fun renderTile(
        state: Unit,
        deviceParameters: DeviceParameters
    ): LayoutElementBuilders.LayoutElement = layout(
        context, deviceParameters,
        location = "San Francisco",
        weatherIconId = SCATTERED_SHOWERS_ICON_ID,
        currentTemperature = "52°",
        lowTemperature = "48°",
        highTemperature = "64°",
        weatherSummary = "Showers"
    )

    override fun ResourceBuilders.Resources.Builder.produceRequestedResources(
        resourceState: Unit,
        deviceParameters: DeviceParameters,
        resourceIds: List<String>
    ) {
        this.addIdToImageMapping(
            SCATTERED_SHOWERS_ICON_ID, ImageResource.Builder()
            .setAndroidResourceByResId(
                AndroidImageResourceByResId.Builder()
                    .setResourceId(com.google.android.horologist.logo.R.drawable.horologist_logo)
                    .build()
            )
            .build()
        )
    }
}
