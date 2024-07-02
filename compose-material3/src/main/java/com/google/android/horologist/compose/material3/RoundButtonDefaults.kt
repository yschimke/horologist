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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.google.android.horologist.compose.material3

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath
import androidx.wear.compose.material3.tokens.ShapeTokens

object RoundButtonDefaults {

    // TODO similar shapes library for Wear
    val Off = RoundedPolygon.circle().normalized()

    val On = RoundedPolygon.rectangle(rounding = CornerRounding(radius = 2f / 3f)).normalized()

    val Pressed = RoundedPolygon.rectangle()

    val circleSquareMorph: Morph by lazy { Morph(Off, On) }

    fun Morph.toShape(progress: () -> Float) = object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val path = toPath(progress()).asComposePath().apply {
                transform(Matrix().apply {
                    scale(size.width, size.height)
                })
            }

            return Outline.Generic(path)
        }
    }

    val Off2 = ShapeTokens.CornerFull

    val On2 = RoundedCornerShape(50 * 2f / 3f)

    val Pressed2 = ShapeTokens.CornerSmall

    fun circleSquareShape(progress: () -> Float): Shape {
        return object : Shape {
            override fun createOutline(
                size: Size,
                layoutDirection: LayoutDirection,
                density: Density
            ): Outline {
                val cornerSize = androidx.compose.ui.util.lerp(
                    size.width / 2,
                    On2.topEnd.toPx(size, density),
                    progress()
                )

                return RoundedCornerShape(cornerSize).createOutline(size, layoutDirection, density)
            }
        }
    }

    val ExtraLarge = 72.dp
    val Large = 60.dp
    val Standard = 52.dp
    val Small = 48.dp
}