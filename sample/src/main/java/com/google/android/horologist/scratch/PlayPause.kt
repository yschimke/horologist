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

package com.google.android.horologist.scratch

import android.graphics.PointF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.PointTransformer
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath
import com.google.android.horologist.composables.MorphButton
import com.google.android.horologist.compose.material.ButtonSize

object PlayPause {
    val rectangle = listOf(
        PointF(6f, 5f),
        PointF(10f, 5f),
        PointF(10f, 19f),
        PointF(6f, 19f),
    ).flatten()

    private fun List<PointF>.flatten(): FloatArray {
        return this.flatMap { listOf(it.x, it.y) }.toFloatArray()
    }

    val leftPause =
        RoundedPolygon(
            vertices = rectangle.transformFrom(24f).offset(PointF(-0.25f, -0.25F)),
        )

    val rightPause =
        RoundedPolygon(
            vertices = rectangle.offset(offset = PointF(8f, 0f)).transformFrom(24f)
                .offset(PointF(-0.25f, -0.25F))
        )

    val bottomPlay =
        RoundedPolygon(
            vertices = listOf(
                PointF(8f, 19f),
                PointF(8f, 12f),
                PointF(19f, 12f),
            ).flatten().transformFrom(24f).offset(PointF(-0.25f, -0.25F)),
        )

    val topPlay =
        RoundedPolygon(
            vertices = listOf(
                PointF(8f, 12f),
                PointF(8f, 5f),
                PointF(19f, 12f),
            ).flatten().transformFrom(24f).offset(PointF(-0.25f, -0.25F)),
        )

    private fun FloatArray.offset(offset: PointF): FloatArray = FloatArray(this.size) { this[it] + if (it % 2 == 0) offset.x else offset.y }

    private fun FloatArray.transformFrom(fl: Float): FloatArray = FloatArray(this.size) { this[it] / fl }

    fun iconMorphs(size: Float): List<Morph> {
        val scaleTransformer = scaleTransformer(size)
        return listOf(
            Morph(
                leftPause.transformed(scaleTransformer),
                bottomPlay.transformed(scaleTransformer)
            ),
            Morph(rightPause.transformed(scaleTransformer), topPlay.transformed(scaleTransformer))
        )
    }

    val circle =
        RoundedPolygon.circle(
            radius = 1f / 2,
            centerX = 1f / 2,
            centerY = 1f / 2
        )

    val square =
        RoundedPolygon.rectangle(
            width = 1f,
            height = 1f,
            centerX = 1f / 2,
            centerY = 1f / 2,
//            rounding = CornerRounding(32f)
        )

    fun scaleTransformer(mult: Float) = PointTransformer { x, y ->
        TransformResult(
            x * mult,
            y * mult
        )
    }

    fun shapeMorph(size: Float): Morph {
        val scaleTransformer = scaleTransformer(size)
        return Morph(
            circle.transformed(scaleTransformer),
            square.transformed(scaleTransformer),
        )
    }
}

@Composable
public fun PlayPauseButton(
    modifier: Modifier = Modifier,
    playing: () -> Boolean,
    progress: () -> Float,
    onClick: () -> Unit,
    buttonSize: ButtonSize = ButtonSize.Large
) {
    val sizePx = with(LocalDensity.current) {
        buttonSize.tapTargetSize.toPx()
    }

    val shapeMorph = remember(sizePx) { PlayPause.shapeMorph(sizePx) }
    val iconsMorphs = remember(sizePx) { PlayPause.iconMorphs(sizePx) }

    val shapeProgress =
        animateFloatAsState(targetValue = if (playing()) 1f else 0f, label = "Button Shape")

    MorphButton(
        modifier = modifier,
        shapeMorph = shapeMorph,
        shapeProgress = { shapeProgress.value },
        contentDescription = "Morph",
        buttonSize = buttonSize,
        onClick = onClick
    ) {
        Canvas(modifier = Modifier.size(buttonSize.iconSize)) {
            val value = shapeProgress.value
            iconsMorphs.forEach { morph ->
                drawPath(morph.toPath(value).asComposePath(), Color.White)
            }
        }
        Progress(
            buttonSize = buttonSize,
            shapeMorph = shapeMorph,
            shapeProgress = { shapeProgress.value },
            progress = { progress() })
    }
}

@Composable
private fun Progress(
    buttonSize: ButtonSize,
    shapeMorph: Morph,
    shapeProgress: () -> Float,
    progress: () -> Float
) {
    val pathMeasure = remember { PathMeasure() }
    Canvas(modifier = Modifier.size(buttonSize.tapTargetSize)) {
        val progress1 = progress()
        val shapeProgress1 = shapeProgress()

        val shape = shapeMorph.toPath(shapeProgress1).asComposePath()

        // TODO cache this
        val completedPath = Path()
        pathMeasure.setPath(shape, false)
        val length = pathMeasure.length
        pathMeasure.getSegment(0f, progress1 * length, completedPath)

        drawPath(completedPath, color = Color.Red, style = Stroke(5f))
    }
}
