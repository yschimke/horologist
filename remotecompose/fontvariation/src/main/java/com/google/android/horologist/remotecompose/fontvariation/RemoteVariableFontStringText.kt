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
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteBitmapFont
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.abs
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.roundToInt

/**
 * Draws one line of a [RemoteString] [text], which may change on the player, in a variable [font]
 * with its [axes] driven by [RemoteFloat]s, without the player ever loading a font.
 *
 * The text is limited to a known set of [characters] — digits and punctuation for a clock, ASCII
 * for a label — and to at most [maxLength] of them. Each character's outline is in the document
 * once, as expressions of the axes (as for the `String` overload), and the player picks one for
 * each position from the text it has:
 * - it reads the character at each position by measuring that one-character substring with a hidden
 *   bitmap font whose glyph for each of [characters] is as wide as its index;
 * - it places each glyph after the advances of those before it, also expressions of the axes, plus
 *   the font's pair kerning, which a second hidden bitmap font carries in its kerning table so the
 *   player applies it while measuring.
 *
 * Characters outside [characters], and any beyond [maxLength], are not drawn. Kerning is the font's
 * own `GPOS` pair kerning, taken at [kerningLocation]: it follows the text but not the axes. The
 * box is as wide as [maxLength] of the widest character over the axes' whole range, so the layout
 * never moves as the text or axes change; the text starts at its left edge.
 *
 * The document holds one conditional draw per position and character, so it grows with [maxLength]
 * × [characters].
 *
 * @param text The text to draw; it may change on the player.
 * @param characters Every character [text] may contain.
 * @param maxLength The most characters [text] may have.
 * @param font The variable font to take outlines from.
 * @param axes The animated axes, by tag, with their values in the axis' user units.
 * @param fontSize The font size.
 * @param modifier Modifier for the canvas; the text's own width and height are applied after it.
 * @param color The fill color.
 * @param location Values for the axes not in [axes], held fixed; missing axes take their defaults.
 * @param kerningLocation Where in the design space the kerning is taken.
 */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
public fun RemoteVariableFontText(
  text: RemoteString,
  characters: String,
  maxLength: Int,
  font: VariableFont,
  axes: Map<String, RemoteFloat>,
  fontSize: RemoteDp,
  modifier: RemoteModifier = RemoteModifier,
  color: RemoteColor = Color.Black.rc,
  location: Map<String, Float> = emptyMap(),
  kerningLocation: Map<String, Float> = location,
) {
  val indices = axes.keys.associateWith { tag -> font.axes.indexOfFirst { it.tag == tag } }
  require(indices.values.none { it < 0 }) {
    "${indices.filterValues { it < 0 }.keys} not among ${font.axes.map { it.tag }}"
  }
  val set =
    remember(characters) {
      characters.codePoints().toArray().distinct().map { String(Character.toChars(it)) }
    }
  val glyphs = remember(set, font) { set.map { font.variedLayout(it) } }
  val animated = indices.values.toSet()
  val specialization =
    remember(font, animated, location) { AxisSpecialization(animated, font.normalize(location)) }
  val fonts = remember(set, font, kerningLocation) { lookupFonts(set, font, kerningLocation) }
  val widest =
    remember(set, font, animated, location) {
      set.indices.maxOf { c ->
        font.maxAdvance(set[c], glyphs[c].advance, animated, font.normalize(location))
      }
    }
  val em = 1f / font.unitsPerEm
  val width = fontSize * ((maxLength * widest + (maxLength - 1) * fonts.widestKern) * em)
  val height = fontSize * ((font.ascender - font.descender) * em)
  val animatedAxes = indices.entries.associate { (tag, i) -> i to axes.getValue(tag) }

  RemoteCanvas(modifier = modifier.width(width).height(height)) {
    val state = remoteComposeCreationState
    val writer = state.document
    val model = RemoteVariationModel(font, animatedAxes)
    val id = { value: RemoteFloat -> value.getFloatIdForCreationState(state) }
    val coordinate = { form: LinearForm ->
      val value = specialization.specialize(form)
      if (value.isConstant) value.constant else id(model.float(value))
    }
    // The glyph layer is written straight to the document: RemoteCanvas writes a path's data again
    // each time it draws it, and every glyph is drawn at every position. The canvas' recorded
    // operations (the paint) are flushed first so everything stays in order.
    val canvas = remoteCanvas.internalCanvas
    canvas.usePaint(RemotePaint { this.color = color })
    canvas.flush()
    val scale = fontSize.toPx() * em
    writer.save()
    writer.translate(0f, id(scale * font.ascender.toFloat()))
    writer.scale(id(scale), id(scale * -1f))
    val paths = glyphs.map { glyph ->
      writer.addPathData(RemotePath().also { glyph.emit(RemotePathSink(it, coordinate)) })
    }
    val advances = glyphs.map { model.float(specialization.specialize(it.advance)) }

    val length = text.length
    var pen: RemoteFloat = 0f.rf
    var known: RemoteFloat = 0f.rf
    for (i in 0 until maxLength) {
      val start = min(length, i.ri)
      val end = min(length, (i + 1).ri)
      // The index of the character at i in the set, or -1 for none (past the end, or not in it).
      val index =
        (fonts.index.measureWidth(text.substring(start, end), 0f.rf) - 1f).createReference()
      // Exactly one term is 1 when the index is c; all are 0 for no character.
      val match = set.indices.map { c -> max(-abs(index - c.toFloat()) + 1f, 0f) }
      known = (known + match.fold(0f.rf) { sum, m -> sum + m }).createReference()
      // All the kerning up to this glyph: the text through it, measured with every glyph one unit
      // wide, less one unit for each glyph the set has. The pair this glyph ends is included.
      val kern = fonts.kerning.measureWidth(text.substring(0.ri, end), 0f.rf) - known
      val x = id(pen + kern)
      val indexId = id(index)
      for (c in set.indices) {
        writer.conditionalOperations(ConditionalOperations.TYPE_EQ, indexId, c.toFloat())
        writer.save()
        writer.translate(x, 0f)
        writer.drawPath(paths[c])
        writer.restore()
        writer.endConditionalOperations()
      }
      pen =
        (pen + set.indices.fold(0f.rf) { sum, c -> sum + match[c] * advances[c] }).createReference()
    }
    writer.restore()
  }
}

/** The two hidden bitmap fonts: one that reads a character's index, one that carries kerning. */
@SuppressLint("RestrictedApi")
internal class LookupFonts(
  val index: RemoteBitmapFont,
  val kerning: RemoteBitmapFont,
  val widestKern: Float,
)

/** Builds the [LookupFonts] for [set], with [font]'s pair kerning at [location]. */
@SuppressLint("RestrictedApi")
internal fun lookupFonts(
  set: List<String>,
  font: VariableFont,
  location: Map<String, Float>,
): LookupFonts {
  val pixel = ImageBitmap(1, 1)
  // A glyph is marginLeft + width + marginRight wide: index + 1 here, so 0 means no character.
  val index =
    RemoteBitmapFont(
      set.mapIndexed { i, c -> RemoteBitmapFont.Glyph(c, pixel, 0, 0, i.toShort(), 0, 1, 1) },
      emptyMap(),
    )
  val ids = set.map { font.glyphId(it.codePointAt(0)) }
  val pairs = HashMap<String, Short>()
  for (a in set.indices) {
    for (b in set.indices) {
      val k = font.kerning(ids[a], ids[b], location).roundToInt()
      if (k != 0)
        pairs[set[a] + set[b]] =
          k.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
  }
  val kerning =
    RemoteBitmapFont(set.map { c -> RemoteBitmapFont.Glyph(c, pixel, 0, 0, 0, 0, 1, 1) }, pairs)
  return LookupFonts(index, kerning, maxOf(0f, pairs.values.maxOfOrNull { it.toFloat() } ?: 0f))
}

/** Emits outline commands into a [RemotePath], each coordinate a literal or an expression id. */
@SuppressLint("RestrictedApi")
internal class RemotePathSink(
  private val path: RemotePath,
  private val coordinate: (LinearForm) -> Float,
) : PathSink<LinearForm> {
  override fun moveTo(x: LinearForm, y: LinearForm) = path.moveTo(coordinate(x), coordinate(y))

  override fun lineTo(x: LinearForm, y: LinearForm) = path.lineTo(coordinate(x), coordinate(y))

  override fun quadTo(x1: LinearForm, y1: LinearForm, x2: LinearForm, y2: LinearForm) =
    path.quadTo(coordinate(x1), coordinate(y1), coordinate(x2), coordinate(y2))

  override fun close() = path.close()
}
