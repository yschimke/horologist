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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.google.android.horologist.scratch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsFootball
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.transformed
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MotionScheme
import androidx.wear.compose.material3.OutlinedIconButton
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import com.google.android.horologist.compose.layout.AppScaffold
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults.padding
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.material.Title

class ScratchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    AppScaffold(modifier = Modifier.background(Color.Black)) {
        val columnState = rememberResponsiveColumnState(
            contentPadding = padding(
                first = ScalingLazyColumnDefaults.ItemType.Text,
                last = ScalingLazyColumnDefaults.ItemType.MultiButton
            )
        )
        ScreenScaffold(scrollState = columnState) {
            ScalingLazyColumn(columnState) {
                item {
                    Title("Boring and Sad Buttons")
                }
                item {
                    Buttons()
                }
                item {
                    Title("Animated Buttons")
                }
                item {
                    AnimatedButtons()
                }
                item {
                    Title("Not exactly subtle Buttons")
                }
                item {
                    CircleStarButton()
                }
                item {
                    Title("We are going to need some zip ties")
                }
                item {
                    PlayPauseButton()
                }
            }
        }
    }
}

@Composable
private fun Buttons() {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = IconButtonDefaults.shape
    ButtonRow(shape, interactionSource)
}

@Composable
private fun AnimatedButtons() {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = IconButtonDefaults.animatedShape(interactionSource)
    ButtonRow(shape, interactionSource)
}

@Composable
private fun CircleStarButton() {
    val interactionSource = remember { MutableInteractionSource() }
    val morph = Morph(RoundedPolygon.circle(5).normalized(), RoundedPolygon.star(5).normalized())
    val shape = animatedMorphedShape(
        interactionSource,
        morph = morph
    )
    ButtonRow(shape, interactionSource)
}

@Composable
private fun PlayPauseButton() {
    val interactionSource = remember { MutableInteractionSource() }

    val infiniteTransition = rememberInfiniteTransition(label = "Play Progress")
    val playProgress = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Path animation"
    )

    val playing = remember { mutableStateOf(false) }
    val playingAnimation = animateFloatAsState(if (playing.value) 1f else 0f)

    val pausedShape = RoundedPolygon.rectangle(1f, 1f, rounding = CornerRounding(0.1f)).normalized()
    val playingShape = RoundedPolygon.star(
        numVerticesPerRadius = 6,
        1f,
        0.25f,
        rounding = CornerRounding(1f * 0.32f),
        innerRounding = CornerRounding(1f * 0.32f),
        centerX = 0f,
        centerY = 0f
    ).normalized()
    val morph = remember { Morph(pausedShape, playingShape) }
    val shape = remember { AnimatedMorphShape(morph) { playingAnimation.value } }

    val progressMorph = remember {
        Morph(pausedShape.transformed(android.graphics.Matrix().apply {
            this.setScale(120f, 120f)
        }), playingShape.transformed(android.graphics.Matrix().apply {
            this.setScale(120f, 120f)
        }))
    }
    Box {
        FilledTonalIconButton(
            onClick = { playing.value = !playing.value },
            shape = shape,
            interactionSource = interactionSource,
            modifier = Modifier.size(60.dp)
        ) {
            Icon(Icons.Rounded.SportsSoccer, "")
        }
        Progress(60.dp, progressMorph, { playingAnimation.value }) { playProgress.value }
    }
}


@Composable
private fun Progress(
    buttonSize: Dp,
    shapeMorph: Morph,
    shapeProgress: () -> Float,
    progress: () -> Float
) {
    val pathMeasure = remember { PathMeasure() }
    Canvas(modifier = Modifier.size(buttonSize)) {
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

@Composable
private fun ButtonRow(
    shape: Shape,
    interactionSource: MutableInteractionSource
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.padding(bottom = 12.dp)) {
        OutlinedIconButton(onClick = {}, shape = shape, interactionSource = interactionSource) {
            Icon(Icons.Rounded.SportsFootball, "")
        }
        FilledTonalIconButton(onClick = {}, shape = shape, interactionSource = interactionSource) {
            Icon(Icons.Rounded.SportsSoccer, "")
        }
        TextButton(
            onClick = {},
            shape = shape,
            interactionSource = interactionSource,
            colors = TextButtonDefaults.filledTonalTextButtonColors()
        ) {
            Text("لندن")
        }
    }
}

@WearPreviewLargeRound
@Composable
fun WearAppPreview() {
    WearApp()
}

@Composable
fun animatedMorphedShape(
    interactionSource: InteractionSource,
    morph: Morph,
    onPressAnimationSpec: FiniteAnimationSpec<Float> = MotionScheme.bouncyFastSpec(),
    onReleaseAnimationSpec: FiniteAnimationSpec<Float> = MotionScheme.flatDefaultSpec(),
): Shape {
    val pressed = interactionSource.collectIsPressedAsState()

    val transition = updateTransition(pressed.value, label = "Pressed State")
    val progress: State<Float> =
        transition.animateFloat(
            label = "Pressed",
            transitionSpec = {
                when {
                    false isTransitioningTo true -> onPressAnimationSpec
                    else -> onReleaseAnimationSpec
                }
            }
        ) { pressedTarget ->
            if (pressedTarget) 1f else 0f
        }

    return remember { AnimatedMorphShape(morph) { progress.value } }
}

@Stable
class AnimatedMorphShape(
    val morph: Morph,
    private val progress: () -> Float
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path =
            morph.toPath(progress()).asComposePath().apply {
                transform(Matrix().apply { scale(size.width, size.height) })
            }

        return Outline.Generic(path)
    }
}
