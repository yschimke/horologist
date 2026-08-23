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
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.Ellipse
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.ShapeDirection
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.geometry.shapeDirection
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animatePosition
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateVector

/** Evaluates a Lottie [Ellipse] into a [RemoteLottiePath]. */
@SuppressLint("RestrictedApi")
internal fun ellipse(el: Ellipse, animationSettings: LottieSettings): RemoteLottiePath? {
  if (el.hidden?.constantValue == true) return null

  val pos = animatePosition(el.position, animationSettings)
  val size = animateVector(el.size, animationSettings)
  val width = size.getOrElse(0) { 0f.rf }
  val height = size.getOrElse(1) { 0f.rf }
  val halfWidth = width / 2f
  val halfHeight = height / 2f

  val cpW = halfWidth * 0.55228f
  val cpH = halfHeight * 0.55228f

  val vertices: List<List<RemoteFloat>>
  val inTangents: List<List<RemoteFloat>>
  val outTangents: List<List<RemoteFloat>>

  if (el.shapeDirection == ShapeDirection.Reversed) {
    rcPath.moveTo(posX, posY - halfHeight)
    rcPath.cubicTo(
      posX - cpW,
      posY - halfHeight,
      posX - halfWidth,
      posY - cpH,
      posX - halfWidth,
      posY,
    )
    rcPath.cubicTo(
      posX - halfWidth,
      posY + cpH,
      posX - cpW,
      posY + halfHeight,
      posX,
      posY + halfHeight,
    )
    rcPath.cubicTo(
      posX + cpW,
      posY + halfHeight,
      posX + halfWidth,
      posY + cpH,
      posX + halfWidth,
      posY,
    )
    rcPath.cubicTo(
      posX + halfWidth,
      posY - cpH,
      posX + cpW,
      posY - halfHeight,
      posX,
      posY - halfHeight,
    )
    rcPath.close()
  } else {
    rcPath.moveTo(posX, posY - halfHeight)
    rcPath.cubicTo(
      posX + cpW,
      posY - halfHeight,
      posX + halfWidth,
      posY - cpH,
      posX + halfWidth,
      posY,
    )
    rcPath.cubicTo(
      posX + halfWidth,
      posY + cpH,
      posX + cpW,
      posY + halfHeight,
      posX,
      posY + halfHeight,
    )
    rcPath.cubicTo(
      posX - cpW,
      posY + halfHeight,
      posX - halfWidth,
      posY + cpH,
      posX - halfWidth,
      posY,
    )
    rcPath.cubicTo(
      posX - halfWidth,
      posY - cpH,
      posX - cpW,
      posY - halfHeight,
      posX,
      posY - halfHeight,
    )
    rcPath.close()
  }

  return RemoteLottiePath(rcPath)
}
