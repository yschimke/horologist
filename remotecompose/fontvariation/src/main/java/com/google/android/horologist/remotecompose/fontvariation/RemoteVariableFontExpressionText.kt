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
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Draws one line of [text] in a variable [font] with any number of its axes driven by
 * [RemoteFloat]s, by evaluating the font's own variation model on the player.
 *
 * Where [RemoteVariableFontText] carries pre-instanced outlines and tweens between them, this
 * carries the model itself: every outline coordinate is a float expression of the axis values — the
 * default position plus each variation region's delta weighted by that region's scalar, exactly as
 * the font defines it. The player re-evaluates those expressions when an axis changes and draws a
 * single path whose points are the results. It never loads or re-instances a font. The axes can be
 * any [RemoteFloat]: named floats a host sets, animations, or expressions of the document's clock.
 *
 * Compared with the tween:
 * - several axes can move at once and independently, and the axis values need no keyframes;
 * - the document grows with the number of distinct coordinates and regions in [text], not with the
 *   number of keyframes, and each axis change costs one evaluation of those expressions.
 *
 * The same limits apply: the text is fixed at creation time, with nominal advances and no shaping
 * or fallback, and has no accessible text unless the caller adds a content description.
 *
 * @param text The single line of text to draw.
 * @param font The variable font to take outlines from.
 * @param axes The animated axes, by tag, with their values in the axis' user units. Values are
 *   clamped to the axis' range on the player.
 * @param fontSize The font size.
 * @param modifier Modifier for the canvas; the text's own width and height are applied after it.
 * @param color The fill color.
 * @param location Values for the axes not in [axes], held fixed; missing axes take their defaults.
 */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
public fun RemoteVariableFontExpressionText(
  text: String,
  font: VariableFont,
  axes: Map<String, RemoteFloat>,
  fontSize: RemoteDp,
  modifier: RemoteModifier = RemoteModifier,
  color: RemoteColor = Color.Black.rc,
  location: Map<String, Float> = emptyMap(),
) {
  val indices = axes.keys.associateWith { tag -> font.axes.indexOfFirst { it.tag == tag } }
  require(indices.values.none { it < 0 }) {
    "${indices.filterValues { it < 0 }.keys} not among ${font.axes.map { it.tag }}"
  }
  val outline = remember(text, font) { font.variedLayout(text) }
  val specialization =
    remember(font, indices.values.toSet(), location) {
      AxisSpecialization(indices.values.toSet(), font.normalize(location))
    }
  val animatedAxes = indices.entries.associate { (tag, i) -> i to axes.getValue(tag) }
  val maxAdvance =
    remember(text, font, indices.values.toSet(), location) {
      font.maxAdvance(text, outline.advance, indices.values.toSet(), font.normalize(location))
    }
  val em = 1f / font.unitsPerEm
  val width = fontSize * (maxAdvance * em)
  val height = fontSize * ((font.ascender - font.descender) * em)

  RemoteCanvas(modifier = modifier.width(width).height(height)) {
    val state = remoteComposeCreationState
    val model = RemoteVariationModel(font, animatedAxes)
    // Each coordinate is either a literal or the NaN-boxed id of its expression; the player
    // resolves the ids whenever the expressions change.
    val coordinate = { form: LinearForm ->
      val animated = specialization.specialize(form)
      if (animated.isConstant) animated.constant
      else model.float(animated).getFloatIdForCreationState(state)
    }
    val path = RemotePath()
    outline.emit(
      object : PathSink<LinearForm> {
        override fun moveTo(x: LinearForm, y: LinearForm) =
          path.moveTo(coordinate(x), coordinate(y))

        override fun lineTo(x: LinearForm, y: LinearForm) =
          path.lineTo(coordinate(x), coordinate(y))

        override fun quadTo(x1: LinearForm, y1: LinearForm, x2: LinearForm, y2: LinearForm) =
          path.quadTo(coordinate(x1), coordinate(y1), coordinate(x2), coordinate(y2))

        override fun close() = path.close()
      }
    )
    val scale = fontSize.toPx() * em
    remoteCanvas.save()
    remoteCanvas.translate(0f.rf, scale * font.ascender.toFloat())
    remoteCanvas.scale(scale, -scale)
    drawPath(path, RemotePaint { this.color = color })
    remoteCanvas.restore()
  }
}

/**
 * The widest [advance] of [text] anywhere the [animated] axes can move, the others held at [fixed].
 *
 * The box is fixed rather than following the axes, so animating them never reflows the layout. (A
 * layout modifier driven by the axes would also stop the canvas' expressions from following them on
 * the AndroidX player.) The advance is linear in each axis between the corners of the regions that
 * vary it, so its maximum is at one of the grid points those corners make.
 */
internal fun VariableFont.maxAdvance(
  text: String,
  advance: LinearForm,
  animated: Set<Int>,
  fixed: FloatArray,
): Float {
  val glyphs = glyphIds(text).toSet()
  var points = listOf(fixed)
  for (axis in animated) {
    val values = normalizedBreakpoints(axis, glyphs)
    points = points.flatMap { p -> values.map { v -> p.copyOf().also { it[axis] = v } } }
  }
  return points.maxOf { advance.evaluate(it) }
}
