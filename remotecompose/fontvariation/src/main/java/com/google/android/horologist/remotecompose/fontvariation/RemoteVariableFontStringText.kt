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
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
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
 * The glyphs of a variable font for a known set of characters, prepared once and shared by every
 * [RemoteVariableFontText] in a document that draws a [RemoteString] with them: a clock's time and
 * date, a counter and its label.
 *
 * Holds each character's outline and advance as expressions of the [axes], the hidden bitmap fonts
 * that read the text and carry the kerning, and the widest advance. The first text that draws with
 * it writes the outlines and expressions into the document; the others refer to them, so each
 * further text costs only its own positions.
 *
 * Build it with [rememberVariableFontGlyphs] inside the document being captured; it belongs to that
 * one document.
 */
public class VariableFontGlyphs
internal constructor(
  internal val font: VariableFont,
  internal val set: List<String>,
  private val outlines: List<VariedOutline>,
  private val specialization: AxisSpecialization,
  private val axes: Map<Int, RemoteFloat>,
  internal val fonts: LookupFonts,
  internal val widest: Float,
) {
  /** What the first text wrote into a document, for the others to refer to. */
  internal class Written(val paths: List<Int>, val advances: List<RemoteFloat>)

  private var writtenTo: Any? = null
  private var written: Written? = null

  /**
   * Writes the outlines and their expressions into [state]'s document the first time, and returns
   * them every time. The caller must be writing directly to the document, in order.
   */
  @SuppressLint("RestrictedApi")
  internal fun write(state: RemoteComposeCreationState): Written {
    val writer = state.document
    written
      ?.takeIf { writtenTo === writer }
      ?.let {
        return it
      }
    val model = RemoteVariationModel(font, axes)
    val coordinate = { form: LinearForm ->
      val value = specialization.specialize(form)
      if (value.isConstant) value.constant else model.float(value).getFloatIdForCreationState(state)
    }
    val paths = outlines.map { glyph ->
      writer.addPathData(RemotePath().also { glyph.emit(RemotePathSink(it, coordinate)) })
    }
    val advances = outlines.map { model.float(specialization.specialize(it.advance)) }
    return Written(paths, advances).also {
      written = it
      writtenTo = writer
    }
  }
}

/**
 * Prepares [VariableFontGlyphs] for [characters] in [font], with its [axes] driven by
 * [RemoteFloat]s, to share between several [RemoteVariableFontText]s in this document.
 *
 * @param font The variable font to take outlines from.
 * @param characters Every character the texts may contain.
 * @param axes The animated axes, by tag, with their values in the axis' user units.
 * @param location Values for the axes not in [axes], held fixed; missing axes take their defaults.
 * @param kerningLocation Where in the design space the font's `GPOS` pair kerning is taken.
 */
@Composable
public fun rememberVariableFontGlyphs(
  font: VariableFont,
  characters: String,
  axes: Map<String, RemoteFloat>,
  location: Map<String, Float> = emptyMap(),
  kerningLocation: Map<String, Float> = location,
): VariableFontGlyphs =
  remember(font, characters, axes, location, kerningLocation) {
    variableFontGlyphs(font, characters, axes, location, kerningLocation)
  }

internal fun variableFontGlyphs(
  font: VariableFont,
  characters: String,
  axes: Map<String, RemoteFloat>,
  location: Map<String, Float>,
  kerningLocation: Map<String, Float>,
): VariableFontGlyphs {
  val indices = axes.keys.associateWith { tag -> font.axes.indexOfFirst { it.tag == tag } }
  require(indices.values.none { it < 0 }) {
    "${indices.filterValues { it < 0 }.keys} not among ${font.axes.map { it.tag }}"
  }
  val set = characters.codePoints().toArray().distinct().map { String(Character.toChars(it)) }
  val outlines = set.map { font.variedLayout(it) }
  val animated = indices.values.toSet()
  val fixed = font.normalize(location)
  return VariableFontGlyphs(
    font = font,
    set = set,
    outlines = outlines,
    specialization = AxisSpecialization(animated, fixed),
    axes = indices.entries.associate { (tag, i) -> i to axes.getValue(tag) },
    fonts = lookupFonts(set, font, kerningLocation),
    widest =
      set.indices.maxOf { c -> font.maxAdvance(set[c], outlines[c].advance, animated, fixed) },
  )
}

/**
 * Draws one line of a [RemoteString] [text], which may change on the player, in a variable [font]
 * with its [axes] driven by [RemoteFloat]s, without the player ever loading a font.
 *
 * The text is limited to a known set of [characters] — digits and punctuation for a clock, ASCII
 * for a label — and to at most [maxLength] of them. To draw several texts with the same font,
 * characters and axes, build their glyphs once with [rememberVariableFontGlyphs] and use the
 * overload that takes them: the outlines are then in the document once rather than once per text.
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
  RemoteVariableFontText(
    text = text,
    maxLength = maxLength,
    glyphs = rememberVariableFontGlyphs(font, characters, axes, location, kerningLocation),
    fontSize = fontSize,
    modifier = modifier,
    color = color,
  )
}

/**
 * Draws one line of a [RemoteString] [text], which may change on the player, with [glyphs] prepared
 * by [rememberVariableFontGlyphs], without the player ever loading a font.
 *
 * Each of the [glyphs]' characters has its outline in the document once, as expressions of the
 * axes, and the player picks one for each position from the text it has:
 * - it reads the character at each position by measuring that one-character substring with a hidden
 *   bitmap font whose glyph for each character is as wide as its index;
 * - it places each glyph after the advances of those before it, also expressions of the axes, plus
 *   the font's pair kerning, which a second hidden bitmap font carries in its kerning table so the
 *   player applies it while measuring.
 *
 * Characters the [glyphs] lack, and any beyond [maxLength], are not drawn. Kerning follows the text
 * but not the axes. The box is as wide as [maxLength] of the widest character over the axes' whole
 * range, so the layout never moves as the text or axes change; the text starts at its left edge.
 *
 * Each position holds one conditional draw per character, so the text grows with [maxLength] × the
 * number of characters, on top of the glyphs, which are shared.
 *
 * @param text The text to draw; it may change on the player.
 * @param maxLength The most characters [text] may have.
 * @param glyphs The glyphs to draw with, shared with the document's other texts.
 * @param fontSize The font size.
 * @param modifier Modifier for the canvas; the text's own width and height are applied after it.
 * @param color The fill color.
 */
@SuppressLint("RestrictedApi")
@Composable
@RemoteComposable
public fun RemoteVariableFontText(
  text: RemoteString,
  maxLength: Int,
  glyphs: VariableFontGlyphs,
  fontSize: RemoteDp,
  modifier: RemoteModifier = RemoteModifier,
  color: RemoteColor = Color.Black.rc,
) {
  val font = glyphs.font
  val set = glyphs.set
  val fonts = glyphs.fonts
  val em = 1f / font.unitsPerEm
  val width = fontSize * ((maxLength * glyphs.widest + (maxLength - 1) * fonts.widestKern) * em)
  val height = fontSize * ((font.ascender - font.descender) * em)

  RemoteCanvas(modifier = modifier.width(width).height(height)) {
    val state = remoteComposeCreationState
    val writer = state.document
    val id = { value: RemoteFloat -> value.getFloatIdForCreationState(state) }
    // The glyph layer is written straight to the document: RemoteCanvas writes a path's data again
    // each time it draws it, and every glyph is drawn at every position. The canvas' recorded
    // operations (the paint) are flushed first so everything stays in order.
    val canvas = remoteCanvas.internalCanvas
    canvas.usePaint(RemotePaint { this.color = color })
    canvas.flush()
    val written = glyphs.write(state)
    val scale = fontSize.toPx() * em
    writer.save()
    writer.translate(0f, id(scale * font.ascender.toFloat()))
    writer.scale(id(scale), id(scale * -1f))

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
        writer.drawPath(written.paths[c])
        writer.restore()
        writer.endConditionalOperations()
      }
      pen =
        (pen + set.indices.fold(0f.rf) { sum, c -> sum + match[c] * written.advances[c] })
          .createReference()
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
