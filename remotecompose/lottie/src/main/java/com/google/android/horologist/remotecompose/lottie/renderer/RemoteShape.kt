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
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.grouping.Transform

@SuppressLint("RestrictedApi")
internal interface RemoteShape {
  fun draw(drawScope: RemoteDrawScope, canvas: RemoteCanvas)
}

@SuppressLint("RestrictedApi")
internal class RemoteCompiledPath(val path: RemotePath) : RemoteShape {
  override fun draw(drawScope: RemoteDrawScope, canvas: RemoteCanvas) {
    canvas.drawPath(path)
  }
}

@SuppressLint("RestrictedApi")
internal class RemoteLottiePath(val path: List<RemoteBezierValue>) : RemoteShape {
  override fun draw(drawScope: RemoteDrawScope, canvas: RemoteCanvas) {
    if (path.isEmpty()) return
    val rcPath = RemotePath()
    rcPath.reset()

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

    canvas.drawPath(rcPath)
  }
}

@SuppressLint("RestrictedApi")
internal class RemoteGroup(
  val childShapes: List<StyledShapes>,
  val animationSettings: LottieSettings,
  val transform: Transform?,
) : RemoteShape {
  override fun draw(drawScope: RemoteDrawScope, canvas: RemoteCanvas) {
    for (shapeGroup in childShapes) {
      val paint = shapeGroup.style.getPaint()
      canvas.save()

      if (transform != null) {
        transform(transform, paint, animationSettings, canvas)
      }

      drawScope.usePaint(paint) {
        for (shape in shapeGroup.shapes) {
          shape.draw(drawScope, canvas)
        }
      }

      canvas.restore()
    }
  }
}
