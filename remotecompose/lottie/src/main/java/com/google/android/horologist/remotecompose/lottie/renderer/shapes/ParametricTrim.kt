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
import com.google.android.horologist.remotecompose.lottie.LottieSettings
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.modifiers.TrimPath
import com.google.android.horologist.remotecompose.lottie.format.properties.StaticBezierProperty
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.RemoteLottiePath
import com.google.android.horologist.remotecompose.lottie.renderer.RemotePathTrim
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteBezierValue
import com.google.android.horologist.remotecompose.lottie.renderer.properties.animateScalar

/** Retains live coordinates; only constant geometry may use the existing precomputed trim path. */
@SuppressLint("RestrictedApi")
internal fun trimParametricPath(
  path: RemoteBezierValue,
  trim: TrimPath?,
  settings: LottieSettings,
): RemoteLottiePath {
  if (trim == null || trim.hidden?.constantValue == true) return RemoteLottiePath(listOf(path))
  val coordinates = path.vertices + path.inTangents + path.outTangents
  if (coordinates.all { point -> point.all { it.constantValueOrNull != null } }) {
    val constant =
      BezierValue(
        closed = path.closed,
        vertices = path.vertices.map { point -> point.map { it.constantValue } },
        inTangents = path.inTangents.map { point -> point.map { it.constantValue } },
        outTangents = path.outTangents.map { point -> point.map { it.constantValue } },
      )
    return RemoteLottiePath(
      evaluatePathGeometry(StaticBezierProperty(value = constant), trim, null, settings)
    )
  }
  return RemoteLottiePath(
    listOf(path),
    trim =
      RemotePathTrim(
        animateScalar(trim.start, settings) / 100f,
        animateScalar(trim.end, settings) / 100f,
        animateScalar(trim.offset, settings) / 360f,
      ),
  )
}
