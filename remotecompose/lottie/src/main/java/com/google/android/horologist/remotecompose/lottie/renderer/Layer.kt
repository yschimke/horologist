/*
 * Copyright 2026 The Android Open Source Project
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

package com.google.android.horologist.remotecompose.lottie.renderer

import android.annotation.SuppressLint
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.horologist.remotecompose.lottie.LocalAnimationSettings
import com.google.android.horologist.remotecompose.lottie.format.Layer

/** A Layer in the Lottie composition */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal fun Layer(
  layer: Layer,
  childrenMap: Map<Int?, List<Layer>>,
) {
  if (layer.hidden == true) {
    return
  }

  val animationSettings = LocalAnimationSettings.current
  val transform = layer.transform
  val shapes = (layer as? Layer.ShapeLayer)?.shapes
  val shapeGroups =
    remember(shapes, animationSettings) {
      if (shapes != null) gatherShapes(shapes, animationSettings) else emptyList()
    }

  val animatedTransform =
    remember(transform, animationSettings) {
      if (transform != null) {
        val rotation = animateScalar(transform.rotation, animationSettings)
        val translation = animatePosition(transform.positionTranslation, animationSettings)
        val opacity = animateScalar(transform.opacity, animationSettings)
        val anchorPoint = animatePosition(transform.anchorPoint, animationSettings)
        val scale = animateVector(transform.scale, animationSettings)
        val scaleX = scale[0] / 100f
        val scaleY = scale[1] / 100f
        AnimatedLayerTransform(translation, rotation, scaleX, scaleY, anchorPoint, opacity)
      } else {
        null
      }
    }

  val layerModifier =
    RemoteModifier.drawWithContent {
      if (animatedTransform != null) {
        remoteCanvas.save()
        remoteCanvas.translate(animatedTransform.translation.x, animatedTransform.translation.y)
        remoteCanvas.rotate(animatedTransform.rotation)
        remoteCanvas.scale(animatedTransform.scaleX, animatedTransform.scaleY)
        remoteCanvas.translate(-animatedTransform.anchorPoint.x, -animatedTransform.anchorPoint.y)
      }

      if (shapeGroups.isNotEmpty()) {
        drawShapes(shapeGroups)
      }

      drawContent()

      if (animatedTransform != null) {
        remoteCanvas.restore()
      }
    }

  RemoteBox(modifier = RemoteModifier.fillMaxSize().then(layerModifier)) {
    val children = childrenMap[layer.index] ?: emptyList()
    for (child in children) {
      Layer(child, childrenMap)
    }
  }
}

internal data class AnimatedLayerTransform(
  val translation: Point,
  val rotation: RemoteFloat,
  val scaleX: RemoteFloat,
  val scaleY: RemoteFloat,
  val anchorPoint: Point,
  val opacity: RemoteFloat,
)
