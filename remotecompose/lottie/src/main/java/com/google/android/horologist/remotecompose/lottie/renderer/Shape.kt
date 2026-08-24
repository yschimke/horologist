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
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import com.google.android.horologist.remotecompose.lottie.LocalAnimationSettings
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.GraphicElement
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Ellipse
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.GeometryShape
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Path
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.PolyStar
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Rectangle
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Group
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.Fill
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateColor
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateGradient
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.ellipse
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.path
import com.google.android.horologist.remotecompose.lottie.renderer.shapes.rectangle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

internal data class StyledShapes(val shapes: List<RemoteShape>, val style: RemoteStyle)

/** Renders a list of Lottie Shapes to the RemoteCanvas. */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
internal fun RenderShapes(
  shapes: List<GraphicElement>,
  transformStack: List<Transform>,
  matteContext: MatteContext? = null,
  layerVisibility: RemoteFloat = 1f.rf,
) {
  val animationSettings = LocalAnimationSettings.current
  val shapeGroups = gatherShapes(shapes, animationSettings)

  // Aspect-ratio scaling and centering is applied once, at the top level, by the
  // drawWithContent modifier in LottieAnimation - shapes draw in raw Lottie coordinates here.
  RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
    if (matteContext != null) {
      remoteCanvas.save()
      applyMatteClip(matteContext, animationSettings, remoteCanvas)
    }

    val layerOpacity =
      (transformStack.lastOrNull()?.opacity?.let { animateScalar(it, animationSettings) / 100f }
        ?: 1f.rf) * layerVisibility

    for (shapeGroup in shapeGroups) {
      val paint = shapeGroup.style.getPaint(layerOpacity)

      for (transform in transformStack) {
        remoteCanvas.save()
        transform(transform, null, animationSettings, remoteCanvas)
      }

      usePaint(paint) {
        for (shape in shapeGroup.shapes) {
          shape.draw(this, remoteCanvas, layerOpacity)
        }
      }

      for (transform in transformStack) {
        remoteCanvas.restore()
      }
    }

    if (matteContext != null) {
      remoteCanvas.restore()
    }
  }
}

@SuppressLint("RestrictedApi")
private fun gatherShapes(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
  parentTrimPath: TrimPath? = null,
): List<StyledShapes> {
  val shapeGroups = mutableListOf<StyledShapes>()
  var currentGeometries = mutableListOf<RemoteShape>()
  var currentGroups = mutableListOf<Group>()
  val activeTrimPath: TrimPath? =
    shapes.filterIsInstance<TrimPath>().firstOrNull { it.hidden != true } ?: parentTrimPath
  var hasEmittedStyle = false

  for (shape in shapes) {
    when (shape) {
      is TrimPath -> {
        // Handled via activeTrimPath
      }
      is GeometryShape -> {
        if (hasEmittedStyle) {
          currentGeometries = mutableListOf()
          currentGroups = mutableListOf()
          hasEmittedStyle = false
        }
        val remoteShape =
          when (shape) {
            is Path -> evaluatePath(shape, animationSettings, activeTrimPath)
            is Rectangle -> evaluateRectangle(shape, animationSettings)
            is Ellipse -> evaluateEllipse(shape, animationSettings)
            is PolyStar -> evaluatePolyStar(shape, animationSettings)
          }
        currentGeometries.addIfNotNull(remoteShape)
      }
      is Group -> {
        if (hasEmittedStyle) {
          currentGeometries = mutableListOf()
          currentGroups = mutableListOf()
          hasEmittedStyle = false
        }
        val groupShape = group(shape, animationSettings, activeTrimPath)
        if (groupShape != null) {
          shapeGroups.add(StyledShapes(listOf(groupShape), NoopStyle()))
        }
        currentGroups.add(shape)
      }
      is Fill -> {
        if (shape.hidden != true) {
          val fill = fill(shape, animationSettings)
          val shapesToStyle = mutableListOf<RemoteShape>()
          if (currentGeometries.isNotEmpty()) {
            shapesToStyle.addAll(currentGeometries)
          }
          for (group in currentGroups) {
            shapesToStyle.addAll(evaluateGroupGeometries(group, animationSettings, activeTrimPath))
          }
          if (shapesToStyle.isNotEmpty()) {
            shapeGroups.add(StyledShapes(shapesToStyle, fill))
          }
          hasEmittedStyle = true
        }
      }
      is Transform -> {} // No-op - handled groups
      else -> {}
    }
  }

  // In Lottie, elements at higher array indices are at the bottom of the stack and drawn first;
  // elements at lower array indices are at the top of the stack and drawn last.
  return shapeGroups.reversed()
}

private fun group(group: Group, animationSettings: LottieSettings): RemoteGroup? {
  if (group.hidden?.constantValue == true) {
    return null
  }

  val transform = group.shapes.filterIsInstance<Transform>().firstOrNull()
  val contentShapes = group.shapes.filter { it !is Transform }
  val styledShapes = gatherShapes(contentShapes, animationSettings, parentTrimPath)
  if (styledShapes.isEmpty()) {
    return null
  }
  return RemoteGroup(styledShapes, animationSettings, transform)
}

@SuppressLint("RestrictedApi")
private fun polyStar(star: PolyStar, animationSettings: LottieSettings): RemoteLottiePath? {
  if (star.hidden?.constantValue == true) return null

  val pos = animatePosition(star.position, animationSettings)
  val posX = pos.x.constantValueOrNull ?: 0f
  val posY = pos.y.constantValueOrNull ?: 0f

  val points = animateScalar(star.points, animationSettings).constantValueOrNull ?: 0f
  val rotation = animateScalar(star.rotation, animationSettings).constantValueOrNull ?: 0f
  val outerRadius = animateScalar(star.outerRadius, animationSettings).constantValueOrNull ?: 0f
  val outerRoundedness =
    (animateScalar(star.outerRoundness, animationSettings).constantValueOrNull ?: 0f) / 100f

  val rcPath =
    when (star.starType) {
      PolyStarType.Star -> {
        val innerRadius =
          star.innerRadius?.let { animateScalar(it, animationSettings).constantValueOrNull } ?: 0f
        val innerRoundedness =
          (star.innerRoundness?.let { animateScalar(it, animationSettings).constantValueOrNull }
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

private fun fill(fill: Fill, animationSettings: LottieSettings): RemoteFill {
  return RemoteFill(animateColor(fill.color, animationSettings))
}

@SuppressLint("RestrictedApi")
private fun stroke(stroke: Stroke, animationSettings: LottieSettings): RemoteStroke {
  val strokeColor = animateColor(stroke.color, animationSettings)
  val strokeWidth = animateScalar(stroke.strokeWidth, animationSettings)
  val opacity = animateScalar(stroke.opacity, animationSettings)
  val miterLimit = stroke.miterLimit?.let { animateScalar(it, animationSettings) }
  return RemoteStroke(
    strokeColor = strokeColor,
    strokeWidth = strokeWidth,
    opacity = opacity,
    lineCap = stroke.lineCap,
    lineJoin = stroke.lineJoin,
    miterLimit = miterLimit,
  )
}

@SuppressLint("RestrictedApi")
private fun gradientFill(
  fill: GradientFill,
  animationSettings: LottieSettings,
): RemoteGradientFill {
  val startPoint = animatePosition(fill.startPoint, animationSettings)
  val endPoint = animatePosition(fill.endPoint, animationSettings)
  val gradient = animateGradient(fill.colors, animationSettings)
  val opacity = animateScalar(fill.opacity, animationSettings)
  return RemoteGradientFill(
    gradient = gradient,
    startPoint = startPoint,
    endPoint = endPoint,
    gradientType = fill.gradientType,
    opacity = opacity,
  )
}

@SuppressLint("RestrictedApi")
private fun gradientStroke(
  stroke: GradientStroke,
  animationSettings: LottieSettings,
): RemoteGradientStroke {
  val startPoint = animatePosition(stroke.startPoint, animationSettings)
  val endPoint = animatePosition(stroke.endPoint, animationSettings)
  val gradient = animateGradient(stroke.colors, animationSettings)
  val opacity = animateScalar(stroke.opacity, animationSettings)
  val strokeWidth = animateScalar(stroke.strokeWidth, animationSettings)
  return RemoteGradientStroke(
    gradient = gradient,
    startPoint = startPoint,
    endPoint = endPoint,
    gradientType = stroke.gradientType,
    opacity = opacity,
    strokeWidth = strokeWidth,
  )
}

private fun MutableList<RemoteShape>.addIfNotNull(shape: RemoteShape?) {
  if (shape != null) {
    this.add(shape)
  }
}

@SuppressLint("RestrictedApi")
private fun applyMatteClip(
  matteContext: MatteContext,
  animationSettings: LottieSettings,
  canvas: RemoteCanvas,
) {
  val matteLayer = matteContext.matteLayer
  if (matteLayer !is ShapeLayer || matteLayer.hidden == true) return

  val layerTransforms =
    if (matteLayer.transform != null) {
      matteContext.matteTransforms + matteLayer.transform
    } else {
      matteContext.matteTransforms
    }

  for (transform in layerTransforms) {
    transform(transform, null, animationSettings, canvas)
  }

  clipShapes(matteLayer.shapes, animationSettings, canvas)

  for (transform in layerTransforms.reversed()) {
    inverseTransform(transform, animationSettings, canvas)
  }
}

@SuppressLint("RestrictedApi")
private fun clipShapes(
  shapes: List<GraphicElement>,
  animationSettings: LottieSettings,
  canvas: RemoteCanvas,
) {
  for (shape in shapes) {
    if (shape.hidden == true) continue
    when (shape) {
      is Rectangle -> {
        val lottiePath = evaluateRectangle(shape, animationSettings)
        if (lottiePath != null) {
          val rcPath = buildRemotePathFromBezier(lottiePath.path)
          canvas.clipPath(rcPath)
        }
      }
      is Path -> {
        val lottiePath = evaluatePath(shape, animationSettings, null)
        if (lottiePath != null) {
          val rcPath = buildRemotePathFromBezier(lottiePath.path)
          canvas.clipPath(rcPath)
        }
      }
      is Ellipse -> {
        val lottiePath = evaluateEllipse(shape, animationSettings)
        if (lottiePath != null) {
          val rcPath = buildRemotePathFromBezier(lottiePath.path)
          canvas.clipPath(rcPath)
        }
      }
      is PolyStar -> {
        val lottiePath = evaluatePolyStar(shape, animationSettings)
        if (lottiePath != null) {
          val rcPath = buildRemotePathFromBezier(lottiePath.path)
          canvas.clipPath(rcPath)
        }
      }
      is Group -> {
        val groupTransform = shape.shapes.filterIsInstance<Transform>().firstOrNull()
        if (groupTransform != null) {
          transform(groupTransform, null, animationSettings, canvas)
          clipShapes(shape.shapes.filter { it !is Transform }, animationSettings, canvas)
          inverseTransform(groupTransform, animationSettings, canvas)
        } else {
          clipShapes(shape.shapes, animationSettings, canvas)
        }
      }
      else -> {}
    }
  }
}

@SuppressLint("RestrictedApi")
internal fun buildRemotePathFromBezier(path: List<RemoteBezierValue>): RemotePath {
  val rcPath = RemotePath()
  rcPath.reset()
  if (path.isEmpty()) return rcPath
  for (subpath in path) {
    val vertices = subpath.vertices
    val inTangents = subpath.inTangents
    val outTangents = subpath.outTangents

    if (vertices.isEmpty()) continue

    val startX = vertices[0].getOrElse(0) { 0f.rf }.constantValueOrNull ?: 0f
    val startY = vertices[0].getOrElse(1) { 0f.rf }.constantValueOrNull ?: 0f
    rcPath.moveTo(startX, startY)

    val maxIndex = if (subpath.closed) vertices.size else vertices.size - 1
    for (i in 0 until maxIndex) {
      val p0 = vertices[i]
      val lastIndex = if (i == vertices.size - 1 && subpath.closed) 0 else i + 1
      val p4 = vertices[lastIndex]
      val inTangent = inTangents.getOrNull(lastIndex)
      val outTangent = outTangents.getOrNull(i)

      val p0x = p0.getOrElse(0) { 0f.rf }.constantValueOrNull ?: 0f
      val p0y = p0.getOrElse(1) { 0f.rf }.constantValueOrNull ?: 0f
      val p4x = p4.getOrElse(0) { 0f.rf }.constantValueOrNull ?: 0f
      val p4y = p4.getOrElse(1) { 0f.rf }.constantValueOrNull ?: 0f

      val inTangentX = inTangent?.getOrElse(0) { 0f.rf }?.constantValueOrNull ?: 0f
      val inTangentY = inTangent?.getOrElse(1) { 0f.rf }?.constantValueOrNull ?: 0f
      val outTangentX = outTangent?.getOrElse(0) { 0f.rf }?.constantValueOrNull ?: 0f
      val outTangentY = outTangent?.getOrElse(1) { 0f.rf }?.constantValueOrNull ?: 0f

      val p1x = p0x + outTangentX
      val p1y = p0y + outTangentY
      val p2x = p4x + inTangentX
      val p2y = p4y + inTangentY

      rcPath.cubicTo(p1x, p1y, p2x, p2y, p4x, p4y)
    }

    if (subpath.closed) {
      rcPath.close()
    }
  }
  return rcPath
}
