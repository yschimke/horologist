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

/** A line of text laid out at one location in the design space, in font units (y up). */
internal class TextOutline(val contours: List<Contour>, val advance: Float)

/**
 * Lays [text] out on one line at [location] using nominal glyph advances.
 *
 * This is `cmap` + advance layout only: no kerning, ligatures or complex-script shaping. The point
 * is that the result is structurally identical at every location — the same glyphs, contours and
 * points in the same order — so two layouts can be tweened point by point.
 */
internal fun VariableFont.layout(text: String, location: Map<String, Float>): TextOutline {
  val coords = normalize(location)
  var x = 0f
  val contours = mutableListOf<Contour>()
  for (glyph in glyphIds(text)) {
    val outline = outline(glyph, coords)
    outline.contours.mapTo(contours) { it.transformed(1f, 0f, 0f, 1f, x, 0f) }
    x += outline.advance
  }
  return TextOutline(contours, x)
}

internal fun VariableFont.glyphIds(text: String): List<Int> {
  val ids = mutableListOf<Int>()
  var i = 0
  while (i < text.length) {
    val cp = text.codePointAt(i)
    ids += glyphId(cp)
    i += Character.charCount(cp)
  }
  return ids
}

/**
 * The [axis] values at which [text] must be sampled so that linear interpolation between
 * consecutive samples reproduces the font exactly, with every other axis held at [location].
 *
 * Variable-font deltas are piecewise linear in normalized coordinates, with corners only where a
 * variation region starts, peaks or ends (and where `avar` bends the mapping). Sampling at those
 * corners — only the ones that affect the glyphs in [text] — gives an exact keyframe set, usually
 * far smaller than sampling at a fixed step: two for Google Sans Flex `ROND`.
 *
 * @param range restricts the keyframes to the part of the axis that will actually be animated.
 */
public fun VariableFont.axisKeyframes(
  text: String,
  axis: String,
  range: ClosedFloatingPointRange<Float>? = null,
): List<Float> {
  val index = axes.indexOfFirst { it.tag == axis }
  require(index >= 0) { "font has no '$axis' axis; it has ${axes.map { it.tag }}" }
  val fontAxis = axes[index]
  val lo = (range?.start ?: fontAxis.minValue).coerceIn(fontAxis.minValue, fontAxis.maxValue)
  val hi = (range?.endInclusive ?: fontAxis.maxValue).coerceIn(fontAxis.minValue, fontAxis.maxValue)
  val values =
    normalizedBreakpoints(index, glyphIds(text).toSet())
      .map { denormalize(index, it) }
      .filter { it > lo && it < hi }
  return (listOf(lo) + values + listOf(hi)).distinct().sorted()
}

/** Receives a glyph outline as path commands. */
internal interface PathSink {
  fun moveTo(x: Float, y: Float)

  fun lineTo(x: Float, y: Float)

  fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float)

  fun close()
}

/**
 * Emits [this] outline as TrueType quadratic path commands.
 *
 * The command sequence depends only on which points are on or off the curve, never on where they
 * are, so outlines of the same text at different locations always produce the same commands —
 * including degenerate segments, which a platform path would be free to drop.
 */
internal fun TextOutline.emit(sink: PathSink) {
  for (c in contours) {
    val n = c.size
    if (n == 0) continue
    val first = (0 until n).firstOrNull { c.onCurve[it] }
    if (first == null) {
      // All off-curve: every on-curve point is implied, starting between the last and first.
      sink.moveTo((c.xs[n - 1] + c.xs[0]) / 2, (c.ys[n - 1] + c.ys[0]) / 2)
      for (i in 0 until n) {
        val j = (i + 1) % n
        sink.quadTo(c.xs[i], c.ys[i], (c.xs[i] + c.xs[j]) / 2, (c.ys[i] + c.ys[j]) / 2)
      }
    } else {
      sink.moveTo(c.xs[first], c.ys[first])
      var pending = -1
      for (k in 1..n) {
        val i = (first + k) % n
        if (c.onCurve[i]) {
          if (pending >= 0) sink.quadTo(c.xs[pending], c.ys[pending], c.xs[i], c.ys[i])
          else sink.lineTo(c.xs[i], c.ys[i])
          pending = -1
        } else {
          if (pending >= 0) {
            sink.quadTo(
              c.xs[pending],
              c.ys[pending],
              (c.xs[pending] + c.xs[i]) / 2,
              (c.ys[pending] + c.ys[i]) / 2,
            )
          }
          pending = i
        }
      }
    }
    sink.close()
  }
}
