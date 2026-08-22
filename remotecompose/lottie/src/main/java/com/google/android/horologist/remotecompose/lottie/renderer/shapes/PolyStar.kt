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

package com.google.android.horologist.remotecompose.lottie.renderer.shapes

import android.annotation.SuppressLint
import androidx.compose.remote.creation.RemotePath
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStarType
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteCompiledPath
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

// Note: We deliberately do not use `androidx.graphics.shapes.RoundedPolygon` here because:
// 1. Lottie defines its own exact Bézier tangent calculation and rounding constants (0.47829 for
//    stars, 0.25 for polygons) matching Bodymovin/After Effects and lottie-android, whereas
//    RoundedPolygon cuts corner arcs with a different circular/smoothed curvature profile.
// 2. Lottie polystars support fractional points (e.g. 5.5 points) smoothly morphing the last
//    vertex, while RoundedPolygon requires an integer vertex count.
// 3. Lottie shapes support explicit path direction (e.g. counter-clockwise d=3) affecting fill
//    winding rules.
// 4. Writing directly into RemotePath avoids intermediate allocations and preserves 1:1 visual
//    parity.

/** Evaluates a Lottie [PolyStar] parametric shape into a [RemoteCompiledPath]. */
@SuppressLint("RestrictedApi")
internal fun evaluatePolyStar(
  star: PolyStar,
  animationSettings: LottieSettings,
): RemoteCompiledPath? {
  if (star.hidden == true) return null

  val pos = animatePosition(star.position, animationSettings)
  val posX = pos.x.constantValueOrNull ?: 0f
  val posY = pos.y.constantValueOrNull ?: 0f

  val points = animateScalar(star.points, animationSettings).constantValueOrNull ?: 0f
  val rotation = animateScalar(star.rotation, animationSettings).constantValueOrNull ?: 0f
  val outerRadius = animateScalar(star.outerRadius, animationSettings).constantValueOrNull ?: 0f
  val outerRoundedness =
    (animateScalar(star.outerRoundedness, animationSettings).constantValueOrNull ?: 0f) / 100f

  val rcPath =
    when (star.starType) {
      PolyStarType.Star -> {
        val innerRadius =
          star.innerRadius?.let { animateScalar(it, animationSettings).constantValueOrNull } ?: 0f
        val innerRoundedness =
          (star.innerRoundedness?.let { animateScalar(it, animationSettings).constantValueOrNull }
            ?: 0f) / 100f
        createStarPath(
          points = points,
          positionX = posX,
          positionY = posY,
          rotation = rotation,
          innerRadius = innerRadius,
          outerRadius = outerRadius,
          innerRoundedness = innerRoundedness,
          outerRoundedness = outerRoundedness,
        )
      }
      PolyStarType.Polygon -> {
        createPolygonPath(
          points = points,
          positionX = posX,
          positionY = posY,
          rotation = rotation,
          radius = outerRadius,
          roundedness = outerRoundedness,
        )
      }
    }

  return RemoteCompiledPath(rcPath)
}

@SuppressLint("RestrictedApi")
private fun createStarPath(
  points: Float,
  positionX: Float,
  positionY: Float,
  rotation: Float,
  innerRadius: Float,
  outerRadius: Float,
  innerRoundedness: Float,
  outerRoundedness: Float,
): RemotePath {
  val path = RemotePath()
  path.reset()

  var currentAngle = Math.toRadians((rotation - 90.0)).toFloat()
  val anglePerPoint = (2.0 * PI / points).toFloat()
  val halfAnglePerPoint = anglePerPoint / 2.0f
  val partialPointAmount = points - points.toInt()

  var x: Float
  var y: Float
  var previousX: Float
  var previousY: Float
  var partialPointRadius = 0f

  if (partialPointAmount != 0f) {
    partialPointRadius = innerRadius + partialPointAmount * (outerRadius - innerRadius)
    x = (partialPointRadius * cos(currentAngle.toDouble())).toFloat()
    y = (partialPointRadius * sin(currentAngle.toDouble())).toFloat()
    path.moveTo(x + positionX, y + positionY)
    currentAngle += anglePerPoint * partialPointAmount / 2f
  } else {
    x = (outerRadius * cos(currentAngle.toDouble())).toFloat()
    y = (outerRadius * sin(currentAngle.toDouble())).toFloat()
    path.moveTo(x + positionX, y + positionY)
    currentAngle += halfAnglePerPoint
  }

  var longSegment = false
  val numPoints = ceil(points.toDouble()).toInt() * 2
  for (i in 0 until numPoints) {
    var radius = if (longSegment) outerRadius else innerRadius
    var dTheta = halfAnglePerPoint
    if (partialPointRadius != 0f && i == numPoints - 2) {
      dTheta = anglePerPoint * partialPointAmount / 2f
    }
    if (partialPointRadius != 0f && i == numPoints - 1) {
      radius = partialPointRadius
    }
    previousX = x
    previousY = y
    x = (radius * cos(currentAngle.toDouble())).toFloat()
    y = (radius * sin(currentAngle.toDouble())).toFloat()

    if (innerRoundedness == 0f && outerRoundedness == 0f) {
      path.lineTo(x + positionX, y + positionY)
    } else {
      val cp1Theta = (atan2(previousY.toDouble(), previousX.toDouble()) - PI / 2.0).toFloat()
      val cp1Dx = cos(cp1Theta.toDouble()).toFloat()
      val cp1Dy = sin(cp1Theta.toDouble()).toFloat()

      val cp2Theta = (atan2(y.toDouble(), x.toDouble()) - PI / 2.0).toFloat()
      val cp2Dx = cos(cp2Theta.toDouble()).toFloat()
      val cp2Dy = sin(cp2Theta.toDouble()).toFloat()

      val cp1Roundedness = if (longSegment) innerRoundedness else outerRoundedness
      val cp2Roundedness = if (longSegment) outerRoundedness else innerRoundedness
      val cp1Radius = if (longSegment) innerRadius else outerRadius
      val cp2Radius = if (longSegment) outerRadius else innerRadius

      var cp1x = cp1Radius * cp1Roundedness * 0.47829f * cp1Dx
      var cp1y = cp1Radius * cp1Roundedness * 0.47829f * cp1Dy
      var cp2x = cp2Radius * cp2Roundedness * 0.47829f * cp2Dx
      var cp2y = cp2Radius * cp2Roundedness * 0.47829f * cp2Dy
      if (partialPointAmount != 0f) {
        if (i == 0) {
          cp1x *= partialPointAmount
          cp1y *= partialPointAmount
        } else if (i == numPoints - 1) {
          cp2x *= partialPointAmount
          cp2y *= partialPointAmount
        }
      }

      path.cubicTo(
        previousX - cp1x + positionX,
        previousY - cp1y + positionY,
        x + cp2x + positionX,
        y + cp2y + positionY,
        x + positionX,
        y + positionY,
      )
    }

    currentAngle += dTheta
    longSegment = !longSegment
  }

  path.close()
  return path
}

@SuppressLint("RestrictedApi")
private fun createPolygonPath(
  points: Float,
  positionX: Float,
  positionY: Float,
  rotation: Float,
  radius: Float,
  roundedness: Float,
): RemotePath {
  val path = RemotePath()
  path.reset()

  val pts = floor(points.toDouble()).toInt()
  var currentAngle = Math.toRadians((rotation - 90.0)).toFloat()
  val anglePerPoint = (2.0 * PI / pts).toFloat()

  var x = (radius * cos(currentAngle.toDouble())).toFloat()
  var y = (radius * sin(currentAngle.toDouble())).toFloat()
  path.moveTo(x + positionX, y + positionY)
  currentAngle += anglePerPoint

  var previousX: Float
  var previousY: Float
  val numPoints = ceil(points.toDouble()).toInt()
  for (i in 0 until numPoints) {
    previousX = x
    previousY = y
    x = (radius * cos(currentAngle.toDouble())).toFloat()
    y = (radius * sin(currentAngle.toDouble())).toFloat()

    if (roundedness != 0f) {
      val cp1Theta = (atan2(previousY.toDouble(), previousX.toDouble()) - PI / 2.0).toFloat()
      val cp1Dx = cos(cp1Theta.toDouble()).toFloat()
      val cp1Dy = sin(cp1Theta.toDouble()).toFloat()

      val cp2Theta = (atan2(y.toDouble(), x.toDouble()) - PI / 2.0).toFloat()
      val cp2Dx = cos(cp2Theta.toDouble()).toFloat()
      val cp2Dy = sin(cp2Theta.toDouble()).toFloat()

      val cp1x = radius * roundedness * 0.25f * cp1Dx
      val cp1y = radius * roundedness * 0.25f * cp1Dy
      val cp2x = radius * roundedness * 0.25f * cp2Dx
      val cp2y = radius * roundedness * 0.25f * cp2Dy

      path.cubicTo(
        previousX - cp1x + positionX,
        previousY - cp1y + positionY,
        x + cp2x + positionX,
        y + cp2y + positionY,
        x + positionX,
        y + positionY,
      )
    } else {
      if (i == numPoints - 1) {
        continue
      }
      path.lineTo(x + positionX, y + positionY)
    }

    currentAngle += anglePerPoint
  }

  path.close()
  return path
}
