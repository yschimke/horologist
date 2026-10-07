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

/** One variation axis of a [VariableFont], in user (design) units, from its `fvar` table. */
public data class FontAxis(
  public val tag: String,
  public val minValue: Float,
  public val defaultValue: Float,
  public val maxValue: Float,
)

/**
 * A TrueType variable font (`glyf` + `gvar`), read directly from its bytes.
 *
 * This is a deliberately small reader: just enough of `cmap`, `hmtx`, `glyf`, `fvar`, `avar` and
 * `gvar` to produce glyph outlines and advances at any point in the font's design space. It exists
 * so that a Remote Compose document can carry *pre-instanced* outlines and animate between them
 * with path tweens, rather than asking the player to re-instance the font on every frame.
 *
 * Because every outline is generated from the font's own point list, the outlines of one glyph at
 * two different locations always have exactly the same structure, which is what a path tween needs.
 * CFF2 (PostScript-outline) variable fonts are not supported.
 */
public class VariableFont private constructor(private val data: FontBytes) {
  private val tables: Map<String, Int> = readTableDirectory(data)

  /** The font's units per em, the coordinate space of its outlines. */
  public val unitsPerEm: Int = data.u16(table("head") + 18)

  private val indexToLocFormat = data.i16(table("head") + 50)
  private val numGlyphs = data.u16(table("maxp") + 4)
  private val numberOfHMetrics = data.u16(table("hhea") + 34)

  /** Typographic ascender, in font units. */
  public val ascender: Int = data.i16(table("hhea") + 4)

  /** Typographic descender (negative below the baseline), in font units. */
  public val descender: Int = data.i16(table("hhea") + 6)

  /** The variation axes, in `fvar` order. */
  public val axes: List<FontAxis> = readAxes()

  private val avarMaps: List<List<Pair<Float, Float>>>? = readAvar()
  private val gvar: Gvar? = tables["gvar"]?.let { Gvar(data, it, axes.size) }
  private val cmap: Map<Int, Int> = readCmap()

  /** Returns the glyph id for [codePoint], or 0 (`.notdef`) when the font has none. */
  public fun glyphId(codePoint: Int): Int = cmap[codePoint] ?: 0

  /**
   * Normalizes a user-space [location] (axis tag to value) to the font's normalized coordinates,
   * applying `avar`. Axes missing from [location] take their default.
   */
  internal fun normalize(location: Map<String, Float>): FloatArray =
    FloatArray(axes.size) { i ->
      val axis = axes[i]
      val v = (location[axis.tag] ?: axis.defaultValue).coerceIn(axis.minValue, axis.maxValue)
      val n =
        when {
          v < axis.defaultValue -> (v - axis.defaultValue) / (axis.defaultValue - axis.minValue)
          v > axis.defaultValue -> (v - axis.defaultValue) / (axis.maxValue - axis.defaultValue)
          else -> 0f
        }
      avarMaps?.getOrNull(i)?.let { piecewiseLinear(it, n) } ?: n
    }

  /** Maps a normalized coordinate on [axisIndex] back to user space, inverting `avar`. */
  internal fun denormalize(axisIndex: Int, normalized: Float): Float {
    val axis = axes[axisIndex]
    val n =
      avarMaps?.getOrNull(axisIndex)?.let { map ->
        piecewiseLinear(map.map { (from, to) -> to to from }, normalized)
      } ?: normalized
    return if (n < 0) axis.defaultValue + n * (axis.defaultValue - axis.minValue)
    else axis.defaultValue + n * (axis.maxValue - axis.defaultValue)
  }

  /**
   * Every normalized coordinate of axis [axisIndex] at which an outline or advance of [glyphIds]
   * changes slope: the corners of each `gvar` tuple, the axis extremes and default, and the `avar`
   * mapping points. Between two consecutive breakpoints the outlines are exactly linear in the axis
   * value, which is what makes a chain of path tweens reproduce the font exactly.
   */
  internal fun normalizedBreakpoints(axisIndex: Int, glyphIds: Collection<Int>): Set<Float> {
    val points = sortedSetOf(-1f, 0f, 1f)
    avarMaps?.getOrNull(axisIndex)?.forEach { (_, to) -> points.add(to) }
    val gvar = gvar ?: return points
    for (glyph in closure(glyphIds)) {
      for (tuple in gvar.tuples(glyph)) {
        val peak = tuple.peak[axisIndex]
        if (peak == 0f) continue
        points.add(tuple.start[axisIndex])
        points.add(peak)
        points.add(tuple.end[axisIndex])
      }
    }
    return points
  }

  /** [glyphIds] plus every glyph they reference as components. */
  private fun closure(glyphIds: Collection<Int>): Set<Int> {
    val out = linkedSetOf<Int>()
    val pending = ArrayDeque(glyphIds)
    while (pending.isNotEmpty()) {
      val g = pending.removeFirst()
      if (out.add(g)) {
        (glyph(g) as? Glyf.Composite)?.components?.forEach { pending.add(it.glyphId) }
      }
    }
    return out
  }

  /** The outline and advance of [glyphId] at the normalized [coords]. */
  internal fun outline(glyphId: Int, coords: FloatArray): GlyphOutline {
    val glyph = glyph(glyphId)
    val (advance, lsb) = hMetrics(glyphId)
    return when (glyph) {
      is Glyf.Simple -> {
        val n = glyph.xs.size
        val xs = FloatArray(n + PHANTOM_POINTS)
        val ys = FloatArray(n + PHANTOM_POINTS)
        for (i in 0 until n) {
          xs[i] = glyph.xs[i].toFloat()
          ys[i] = glyph.ys[i].toFloat()
        }
        // Phantom points: their x deltas carry the advance-width variation.
        xs[n] = (glyph.xMin - lsb).toFloat()
        xs[n + 1] = xs[n] + advance
        gvar?.apply(glyphId, coords, xs, ys, glyph.endPts)
        GlyphOutline(contours = glyph.contours(xs, ys), advance = xs[n + 1] - xs[n])
      }
      is Glyf.Composite -> {
        val count = glyph.components.size
        val xs = FloatArray(count + PHANTOM_POINTS)
        val ys = FloatArray(count + PHANTOM_POINTS)
        glyph.components.forEachIndexed { i, c ->
          xs[i] = c.dx.toFloat()
          ys[i] = c.dy.toFloat()
        }
        xs[count] = (glyph.xMin - lsb).toFloat()
        xs[count + 1] = xs[count] + advance
        gvar?.apply(glyphId, coords, xs, ys, endPts = null)
        val contours =
          glyph.components.flatMapIndexed { i, c ->
            outline(c.glyphId, coords).contours.map { contour ->
              contour.transformed(c.xx, c.xy, c.yx, c.yy, xs[i], ys[i])
            }
          }
        GlyphOutline(contours, xs[count + 1] - xs[count])
      }
      null -> {
        val xs = FloatArray(PHANTOM_POINTS)
        val ys = FloatArray(PHANTOM_POINTS)
        xs[1] = advance.toFloat()
        gvar?.apply(glyphId, coords, xs, ys, endPts = IntArray(0))
        GlyphOutline(emptyList(), xs[1] - xs[0])
      }
    }
  }

  private fun hMetrics(glyphId: Int): Pair<Int, Int> {
    val hmtx = table("hmtx")
    val metric = minOf(glyphId, numberOfHMetrics - 1)
    val advance = data.u16(hmtx + metric * 4)
    val lsb =
      if (glyphId < numberOfHMetrics) data.i16(hmtx + glyphId * 4 + 2)
      else data.i16(hmtx + numberOfHMetrics * 4 + (glyphId - numberOfHMetrics) * 2)
    return advance to lsb
  }

  private val glyphCache = HashMap<Int, Glyf?>()

  private fun glyph(glyphId: Int): Glyf? =
    glyphCache.getOrPut(glyphId) {
      require(glyphId in 0 until numGlyphs) { "glyph $glyphId out of range" }
      val loca = table("loca")
      val (start, end) =
        if (indexToLocFormat == 0) {
          data.u16(loca + glyphId * 2) * 2 to data.u16(loca + glyphId * 2 + 2) * 2
        } else {
          data.u32(loca + glyphId * 4).toInt() to data.u32(loca + glyphId * 4 + 4).toInt()
        }
      if (end <= start) null else Glyf.read(data, table("glyf") + start)
    }

  private fun table(tag: String): Int =
    tables[tag] ?: throw IllegalArgumentException("font has no '$tag' table")

  private fun readAxes(): List<FontAxis> {
    val fvar = tables["fvar"] ?: return emptyList()
    val axesOffset = data.u16(fvar + 4)
    val count = data.u16(fvar + 8)
    val size = data.u16(fvar + 10)
    return List(count) { i ->
      val p = fvar + axesOffset + i * size
      FontAxis(
        tag = data.tag(p),
        minValue = data.fixed(p + 4),
        defaultValue = data.fixed(p + 8),
        maxValue = data.fixed(p + 12),
      )
    }
  }

  private fun readAvar(): List<List<Pair<Float, Float>>>? {
    val avar = tables["avar"] ?: return null
    // majorVersion, minorVersion, reserved, axisCount, then one segment map per axis.
    var p = avar + 8
    return List(data.u16(avar + 6)) {
      val count = data.u16(p)
      p += 2
      List(count) { (data.f2dot14(p) to data.f2dot14(p + 2)).also { p += 4 } }
    }
  }

  private fun readCmap(): Map<Int, Int> {
    val cmap = table("cmap")
    val count = data.u16(cmap + 2)
    val subtables =
      (0 until count).map { i ->
        val r = cmap + 4 + i * 8
        Triple(data.u16(r), data.u16(r + 2), cmap + data.u32(r + 4).toInt())
      }
    fun find(platform: Int, encoding: Int, format: Int) = subtables.firstOrNull {
      it.first == platform && it.second == encoding && data.u16(it.third) == format
    }
    val chosen =
      find(3, 10, 12)
        ?: find(0, 4, 12)
        ?: find(3, 1, 4)
        ?: find(0, 3, 4)
        ?: subtables.firstOrNull { data.u16(it.third) == 4 || data.u16(it.third) == 12 }
        ?: return emptyMap()
    val p = chosen.third
    val out = HashMap<Int, Int>()
    if (data.u16(p) == 12) {
      val groups = data.u32(p + 12).toInt()
      for (g in 0 until groups) {
        val r = p + 16 + g * 12
        val startChar = data.u32(r).toInt()
        val endChar = data.u32(r + 4).toInt()
        val startGlyph = data.u32(r + 8).toInt()
        for (c in startChar..endChar) out[c] = startGlyph + (c - startChar)
      }
    } else {
      val segX2 = data.u16(p + 6)
      val ends = p + 14
      val starts = ends + segX2 + 2
      val deltas = starts + segX2
      val rangeOffsets = deltas + segX2
      for (s in 0 until segX2 / 2) {
        val end = data.u16(ends + s * 2)
        val start = data.u16(starts + s * 2)
        val delta = data.i16(deltas + s * 2)
        val rangeOffsetAt = rangeOffsets + s * 2
        val rangeOffset = data.u16(rangeOffsetAt)
        for (c in start..end) {
          if (c == 0xFFFF) continue
          val glyph =
            if (rangeOffset == 0) (c + delta) and 0xFFFF
            else {
              val g = data.u16(rangeOffsetAt + rangeOffset + (c - start) * 2)
              if (g == 0) 0 else (g + delta) and 0xFFFF
            }
          if (glyph != 0) out[c] = glyph
        }
      }
    }
    return out
  }

  public companion object {
    private const val PHANTOM_POINTS = 4

    /** Reads a variable TrueType font from [bytes] (a `.ttf`). */
    public fun parse(bytes: ByteArray): VariableFont = VariableFont(FontBytes(bytes))

    private fun readTableDirectory(data: FontBytes): Map<String, Int> {
      val count = data.u16(4)
      return (0 until count).associate { i ->
        val r = 12 + i * 16
        data.tag(r) to data.u32(r + 8).toInt()
      }
    }

    private fun piecewiseLinear(map: List<Pair<Float, Float>>, v: Float): Float {
      if (map.isEmpty()) return v
      val sorted = map.sortedBy { it.first }
      if (v <= sorted.first().first) return sorted.first().second + (v - sorted.first().first)
      if (v >= sorted.last().first) return sorted.last().second + (v - sorted.last().first)
      for (i in 0 until sorted.size - 1) {
        val (x0, y0) = sorted[i]
        val (x1, y1) = sorted[i + 1]
        if (v in x0..x1) return if (x1 == x0) y0 else y0 + (v - x0) * (y1 - y0) / (x1 - x0)
      }
      return v
    }
  }
}

/** A glyph outline: closed contours of TrueType points, plus the glyph's advance. */
internal class GlyphOutline(val contours: List<Contour>, val advance: Float)

/** One closed contour: point coordinates and whether each point is on the curve. */
internal class Contour(val xs: FloatArray, val ys: FloatArray, val onCurve: BooleanArray) {
  val size: Int
    get() = xs.size

  fun transformed(xx: Float, xy: Float, yx: Float, yy: Float, dx: Float, dy: Float): Contour =
    Contour(
      FloatArray(size) { i -> xs[i] * xx + ys[i] * yx + dx },
      FloatArray(size) { i -> xs[i] * xy + ys[i] * yy + dy },
      onCurve,
    )
}
