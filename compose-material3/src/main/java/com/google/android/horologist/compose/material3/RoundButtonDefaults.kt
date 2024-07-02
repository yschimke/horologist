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

package com.google.android.horologist.compose.material3

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

object RoundButtonDefaults {

    // TODO similar shapes library for Wear
    val circle = MaterialShapes.Circle

    val square = MaterialShapes.Square

    val circleSquareMorph: Morph by lazy { Morph(circle, square) }

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

    val ExtraLarge = 72.dp
    val Large = 60.dp
    val Standard = 52.dp
    val Small = 48.dp
}