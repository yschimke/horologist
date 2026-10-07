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

package com.google.android.horologist.remotecompose.fontvariation

import android.annotation.SuppressLint
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Draws one line of [text] in a variable [font] with its [axis] driven by a [RemoteFloat], without
 * the player ever loading or re-instancing a font.
 *
 * Changing a font-variation setting normally means building a new typeface instance and laying the
 * text out again, which is expensive per frame. Instead, the text's outlines are computed here, at
 * document creation time, at each of the axis' [keyframes][axisKeyframes]; the document carries
 * those paths and the player only interpolates between the two that bracket [value]. Because
 * variable-font outlines are piecewise linear in the axis between those keyframes, the result is
 * the font's own outline at every value, not an approximation, and the document needs no font.
 *
 * The trade-off is that the text is fixed at creation time and drawn as a path: there is no
 * kerning, ligature or complex-script shaping (glyphs are placed by their nominal advances), no
 * font fallback for characters [font] lacks, and no accessible text unless the caller adds a
 * content description.
 *
 * @param text The single line of text to draw.
 * @param font The variable font to take outlines from.
 * @param axis The tag of the axis to animate, for example `"ROND"` or `"wght"`.
 * @param value The axis value, in the axis' user units; clamped to [axisRange].
 * @param fontSize The font size.
 * @param modifier Modifier for the canvas; the text's own width and height are applied after it.
 * @param color The fill color.
 * @param location Values for the other axes, held fixed; missing axes take their defaults.
 * @param axisRange The part of the axis [value] moves through. Keyframes outside it are not
 *   emitted, so a narrower range means a smaller document.
 */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
public fun RemoteVariableFontText(
  text: String,
  font: VariableFont,
  axis: String,
  value: RemoteFloat,
  fontSize: RemoteDp,
  modifier: RemoteModifier = RemoteModifier,
  color: RemoteColor = Color.Black.rc,
  location: Map<String, Float> = emptyMap(),
  axisRange: ClosedFloatingPointRange<Float>? = null,
) {
  val frames =
    remember(text, font, axis, location, axisRange) {
      variableTextKeyframes(font, text, axis, location, axisRange)
    }
  val em = 1f / font.unitsPerEm
  val width = fontSize * (frames.maxOf { it.advance } * em)
  val height = fontSize * ((font.ascender - font.descender) * em)

  RemoteCanvas(modifier = modifier.width(width).height(height)) {
    val paint = RemotePaint { this.color = color }
    val scale = fontSize.toPx() * em
    remoteCanvas.save()
    remoteCanvas.translate(0f.rf, scale * font.ascender.toFloat())
    remoteCanvas.scale(scale, -scale)
    if (frames.size == 1) {
      drawPath(frames[0].path, paint)
    } else {
      val v = clamp(value, frames.first().value, frames.last().value)
      for (i in 0 until frames.size - 1) {
        val from = frames[i]
        val to = frames[i + 1]
        val tween = clamp((v - from.value) / (to.value - from.value), 0f, 1f)
        val draw = { drawTweenPath(from.path, to.path, tween, paint = paint) }
        // Exactly one segment draws: the first below its upper keyframe, the last from its
        // lower one, and each middle one over [from, to).
        when {
          frames.size == 2 -> draw()
          i == 0 -> remoteCanvas.drawConditionally(v.isLessThan(to.value.rf), draw)
          i == frames.size - 2 ->
            remoteCanvas.drawConditionally(v.isGreaterThanOrEqualTo(from.value.rf), draw)
          else ->
            remoteCanvas.drawConditionally(
              v.isGreaterThanOrEqualTo(from.value.rf) and v.isLessThan(to.value.rf),
              draw,
            )
        }
      }
    }
    remoteCanvas.restore()
  }
}

/** The outline of a line of text at one value of the animated axis. */
@SuppressLint("RestrictedApi")
internal class VariableTextFrame(val value: Float, val path: RemotePath, val advance: Float)

@SuppressLint("RestrictedApi")
internal fun variableTextKeyframes(
  font: VariableFont,
  text: String,
  axis: String,
  location: Map<String, Float>,
  axisRange: ClosedFloatingPointRange<Float>?,
): List<VariableTextFrame> =
  font.axisKeyframes(text, axis, axisRange).map { v ->
    val outline = font.layout(text, location + (axis to v))
    val path = RemotePath()
    outline.emit(
      object : PathSink<Float> {
        override fun moveTo(x: Float, y: Float) = path.moveTo(x, y)

        override fun lineTo(x: Float, y: Float) = path.lineTo(x, y)

        override fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) =
          path.quadTo(x1, y1, x2, y2)

        override fun close() = path.close()
      }
    )
    VariableTextFrame(v, path, outline.advance)
  }
