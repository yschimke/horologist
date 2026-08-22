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
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.GradientType
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineCap
import com.google.android.horologist.remotecompose.lottie.format.graphicelement.styles.LineJoin
import com.google.android.horologist.remotecompose.lottie.renderer.properties.Point
import com.google.android.horologist.remotecompose.lottie.renderer.properties.RemoteGradientValue

internal interface RemoteStyle {
  fun getPaint(): RemotePaint
}

@SuppressLint("RestrictedApi")
internal class RemoteFill(val fillColor: RemoteColor) : RemoteStyle {
  override fun getPaint(): RemotePaint {
    return RemotePaint { this.color = fillColor }
  }
}

@SuppressLint("RestrictedApi")
internal class RemoteStroke(
  val strokeColor: RemoteColor,
  val strokeWidth: RemoteFloat,
  val opacity: RemoteFloat,
  val lineCap: LineCap = LineCap.Round,
  val lineJoin: LineJoin = LineJoin.Round,
  val miterLimit: RemoteFloat? = null,
) : RemoteStyle {
  override fun getPaint(): RemotePaint {
    return RemotePaint {
      this.color = strokeColor.copy(alpha = opacity / 100f)
      this.style = PaintingStyle.Stroke
      this.strokeWidth = this@RemoteStroke.strokeWidth
      this.strokeCap =
        when (lineCap) {
          LineCap.Butt -> StrokeCap.Butt
          LineCap.Round -> StrokeCap.Round
          LineCap.Square -> StrokeCap.Square
        }
      this.strokeJoin =
        when (lineJoin) {
          LineJoin.Miter -> StrokeJoin.Miter
          LineJoin.Round -> StrokeJoin.Round
          LineJoin.Bevel -> StrokeJoin.Bevel
        }
    }
  }
}

internal class RemoteGradientFill(
  val gradient: RemoteGradientValue,
  val startPoint: Point,
  val endPoint: Point,
  val gradientType: GradientType,
  val opacity: RemoteFloat,
) : RemoteStyle {
  override fun getPaint(): RemotePaint {
    return RemotePaint()
  }
}

internal class RemoteGradientStroke(
  val gradient: RemoteGradientValue,
  val startPoint: Point,
  val endPoint: Point,
  val gradientType: GradientType,
  val opacity: RemoteFloat,
  val strokeWidth: RemoteFloat,
) : RemoteStyle {
  override fun getPaint(): RemotePaint {
    return RemotePaint()
  }
}

internal class NoopStyle() : RemoteStyle {
  override fun getPaint(): RemotePaint {
    return RemotePaint()
  }
}
