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
import androidx.compose.remote.creation.compose.state.mad
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Draws one line of [text] in a variable [font] with any number of its axes driven by
 * [RemoteFloat]s, without the player ever loading or re-instancing a font.
 *
 * Changing a font-variation setting normally means building a new typeface instance and laying the
 * text out again, which is expensive per frame. Instead the document carries the font's own
 * variation model: every outline coordinate is a float expression of the axis values — the default
 * position plus each variation region's delta weighted by that region's scalar, exactly as the font
 * defines it. The player re-evaluates those expressions when an axis changes and draws a single
 * path whose points are the results, so the outline is the font's own at every value, not an
 * approximation, and the document needs no font.
 *
 * When one axis is animated and its variation regions do not overlap (as when they meet only at its
 * default), the same outline is drawn instead as a path tween between a few key outlines, with no
 * expression per coordinate: exact, and usually smaller and faster to play.
 *
 * The axes can be any [RemoteFloat]: named floats a host sets, animations, or expressions of the
 * document's own clock. The document grows with the number of distinct coordinates and variation
 * regions in [text], and fastest with the number of axes animated together, since regions that span
 * several axes multiply; animate only the axes that move and fix the rest with [location].
 *
 * This works the outline out from [font] each time a document is made. To do that ahead of time,
 * and leave the font out of the app, make a [VariableTextOutline] at build time and draw it with
 * the other overload.
 *
 * The trade-off is that the text is fixed at creation time and drawn as a path: glyphs are placed
 * by their advances and the font's pair kerning, with no ligatures or complex-script shaping, no
 * font fallback for characters [font] lacks, and no accessible text unless the caller adds a
 * content description.
 *
 * @param text The single line of text to draw.
 * @param font The variable font to take outlines from.
 * @param axes The animated axes, by tag, with their values in the axis' user units. Values are
 *   clamped to the axis' range on the player.
 * @param fontSize The font size.
 * @param modifier Modifier for the canvas; the text's own width and height are applied after it.
 * @param color The fill color.
 * @param location Values for the axes not in [axes], held fixed; missing axes take their defaults.
 * @param kerningLocation Where in the design space the font's `GPOS` pair kerning is taken. Kerning
 *   is a constant per pair: it does not follow the animated axes.
 */
@Composable
@RemoteComposable
public fun RemoteVariableFontText(
  text: String,
  font: VariableFont,
  axes: Map<String, RemoteFloat>,
  fontSize: RemoteDp,
  modifier: RemoteModifier = RemoteModifier,
  color: RemoteColor = Color.Black.rc,
  location: Map<String, Float> = emptyMap(),
  kerningLocation: Map<String, Float> = location,
) {
  VariableFontText(text, font, axes, fontSize, modifier, color, location, kerningLocation)
}

/** [RemoteVariableFontText], with the key outlines allowed or not. */
@Composable
@RemoteComposable
internal fun VariableFontText(
  text: String,
  font: VariableFont,
  axes: Map<String, RemoteFloat>,
  fontSize: RemoteDp,
  modifier: RemoteModifier = RemoteModifier,
  color: RemoteColor = Color.Black.rc,
  location: Map<String, Float> = emptyMap(),
  kerningLocation: Map<String, Float> = location,
  allowKeys: Boolean = true,
) {
  val tags = axes.keys.toList()
  val outline =
    remember(text, font, tags, location, kerningLocation, allowKeys) {
      OutlineCache.get(OutlineKey(font, text, tags, location, kerningLocation, allowKeys)) {
        font.outline(text, tags, location, kerningLocation, null, 0f, allowKeys)
      }
    }
  RemoteVariableFontText(outline, axes, fontSize, modifier, color)
}

private data class OutlineKey(
  val font: VariableFont,
  val text: String,
  val axes: List<String>,
  val location: Map<String, Float>,
  val kerningLocation: Map<String, Float>,
  val allowKeys: Boolean,
)

/**
 * The outlines of recent texts. Working one out from the font is most of the cost of making a
 * document, and a widget or server often makes the same text's document again and again, each time
 * in a new composition where `remember` starts afresh.
 */
private object OutlineCache {
  private const val SIZE = 32
  private val entries =
    object : LinkedHashMap<OutlineKey, VariableTextOutline>(SIZE, 0.75f, true) {
      override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<OutlineKey, VariableTextOutline>
      ) = size > SIZE
    }

  fun get(key: OutlineKey, make: () -> VariableTextOutline): VariableTextOutline =
    synchronized(entries) { entries[key] }
      ?: make().also { synchronized(entries) { entries[key] = it } }
}

/**
 * Draws [outline], made by [VariableFont.outline] now or ahead of time and perhaps read back with
 * [VariableTextOutline.decode], with its axes driven by [axes]. Nothing about the font is worked
 * out here, so making the document is quick, and the font itself is not needed.
 *
 * @param outline The text, its font and its animated axes, worked out.
 * @param axes A value for each of [VariableTextOutline.axes], by tag, in the axis' user units.
 * @param fontSize The font size.
 * @param modifier Modifier for the canvas; the text's own width and height are applied after it.
 * @param color The fill color.
 */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
public fun RemoteVariableFontText(
  outline: VariableTextOutline,
  axes: Map<String, RemoteFloat>,
  fontSize: RemoteDp,
  modifier: RemoteModifier = RemoteModifier,
  color: RemoteColor = Color.Black.rc,
) {
  require(axes.keys == outline.axes.toSet()) { "axes ${axes.keys} must be ${outline.axes}" }
  val em = 1f / outline.unitsPerEm
  val width = fontSize * (outline.width * em)
  val height = fontSize * ((outline.ascender - outline.descender) * em)

  RemoteCanvas(modifier = modifier.width(width).height(height)) {
    val values = outline.axes.map { axes.getValue(it) }
    val tents = outline.tents.map { it.remote(values[it.axis]).createReference() }
    val paint = RemotePaint { this.color = color }
    val scale = fontSize.toPx() * em
    remoteCanvas.save()
    remoteCanvas.translate(0f.rf, scale * outline.ascender.toFloat())
    remoteCanvas.scale(scale, -scale)
    when (val body = outline.body) {
      is VariableTextOutline.Keys -> {
        // At most one tent is above zero: tween from the default outline towards its key. The
        // first tent's tween also draws the default outline, when none is.
        val base = outline.path(body.base) { it }
        val keys = body.keys.map { key -> outline.path(key) { it } }
        tents.forEachIndexed { i, scalar ->
          val draw = { drawTweenPath(base, keys[i], scalar, 0f.rf, 1f.rf, paint) }
          val others = tents.filterIndexed { j, _ -> j != i }
          when {
            i > 0 -> remoteCanvas.drawConditionally(scalar.isGreaterThan(0f.rf), draw)
            others.isEmpty() -> draw()
            else ->
              remoteCanvas.drawConditionally(
                others.reduce { a, b -> a + b }.isLessThanOrEqualTo(0f.rf),
                draw,
              )
          }
        }
      }
      is VariableTextOutline.Forms -> {
        val state = remoteComposeCreationState
        val regions =
          body.regions.map { r ->
            if (r.size == 1) tents[r[0]]
            else r.map { tents[it] }.reduce { a, b -> a * b }.createReference()
          }
        val forms =
          body.constants.indices.map { f ->
            val terms = body.termStart[f] until body.termStart[f + 1]
            // Each multiply-add holds two more values on the stack until the chain unwinds, so a
            // long chain is written in parts that fit an expression's 32 tokens.
            terms.chunked(TERMS_PER_EXPRESSION).fold(body.constants[f].rf) { sum, part ->
              part
                .fold(sum) { s, t -> mad(regions[body.regionOf[t]], body.coefficients[t].rf, s) }
                .createReference()
            }
          }
        val ids = forms.map { it.getFloatIdForCreationState(state) }
        val coordinates =
          FloatArray(body.slots.size) {
            if (body.slots[it] < 0) body.literals[it] else ids[body.slots[it]]
          }
        drawPath(outline.path(coordinates) { it }, paint)
      }
    }
    remoteCanvas.restore()
  }
}

/** The path of [verbs][VariableTextOutline.verbs] through [coordinates], mapped by [value]. */
@SuppressLint("RestrictedApi")
private inline fun VariableTextOutline.path(
  coordinates: FloatArray,
  value: (Float) -> Float,
): RemotePath {
  val path = RemotePath()
  var i = 0
  for (verb in verbs) {
    when (verb.toInt()) {
      MOVE -> path.moveTo(value(coordinates[i]), value(coordinates[i + 1]))
      LINE -> path.lineTo(value(coordinates[i]), value(coordinates[i + 1]))
      QUAD ->
        path.quadTo(
          value(coordinates[i]),
          value(coordinates[i + 1]),
          value(coordinates[i + 2]),
          value(coordinates[i + 3]),
        )
      else -> path.close()
    }
    i +=
      when (verb.toInt()) {
        MOVE,
        LINE -> 2
        QUAD -> 4
        else -> 0
      }
  }
  return path
}

/** This tent on the player, as clamped ramps of its axis' [value]. */
@SuppressLint("RestrictedApi")
internal fun VariableTextOutline.TentRamps.remote(value: RemoteFloat): RemoteFloat =
  knots.indices.fold(y0.rf) { sum, i -> sum + clamp(value - knots[i], 0f, widths[i]) * slopes[i] }

/** Three tokens a term and one for the sum so far: 31 of the 32 an expression may have. */
internal const val TERMS_PER_EXPRESSION = 10

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
