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

/** Big-endian reads over a font file. */
internal class FontBytes(private val bytes: ByteArray) {
  fun u8(p: Int): Int = bytes[p].toInt() and 0xFF

  fun i8(p: Int): Int = bytes[p].toInt()

  fun u16(p: Int): Int = (u8(p) shl 8) or u8(p + 1)

  fun i16(p: Int): Int = u16(p).toShort().toInt()

  fun u32(p: Int): Long = (u16(p).toLong() shl 16) or u16(p + 2).toLong()

  fun i32(p: Int): Int = u32(p).toInt()

  fun fixed(p: Int): Float = i32(p) / 65536f

  fun f2dot14(p: Int): Float = i16(p) / 16384f

  fun tag(p: Int): String = String(CharArray(4) { (u8(p + it)).toChar() })
}

/** A `glyf` entry: either a simple outline or a list of transformed components. */
internal sealed class Glyf(val xMin: Int) {

  class Simple(
    xMin: Int,
    val endPts: IntArray,
    val xs: IntArray,
    val ys: IntArray,
    val onCurve: BooleanArray,
  ) : Glyf(xMin) {
    /** Splits (possibly varied) point coordinates back into contours. */
    fun contours(px: FloatArray, py: FloatArray): List<Contour> {
      var start = 0
      return endPts.map { end ->
        val range = start..end
        Contour(px.sliceArray(range), py.sliceArray(range), onCurve.sliceArray(range)).also {
          start = end + 1
        }
      }
    }
  }

  class Component(
    val glyphId: Int,
    val dx: Int,
    val dy: Int,
    val xx: Float,
    val xy: Float,
    val yx: Float,
    val yy: Float,
  )

  class Composite(xMin: Int, val components: List<Component>) : Glyf(xMin)

  companion object {
    private const val ON_CURVE = 0x01
    private const val X_SHORT = 0x02
    private const val Y_SHORT = 0x04
    private const val REPEAT = 0x08
    private const val X_SAME_OR_POSITIVE = 0x10
    private const val Y_SAME_OR_POSITIVE = 0x20

    private const val ARG_1_AND_2_ARE_WORDS = 0x0001
    private const val ARGS_ARE_XY_VALUES = 0x0002
    private const val WE_HAVE_A_SCALE = 0x0008
    private const val MORE_COMPONENTS = 0x0020
    private const val WE_HAVE_AN_X_AND_Y_SCALE = 0x0040
    private const val WE_HAVE_A_TWO_BY_TWO = 0x0080

    fun read(data: FontBytes, offset: Int): Glyf {
      val numberOfContours = data.i16(offset)
      val xMin = data.i16(offset + 2)
      return if (numberOfContours >= 0) readSimple(data, offset, numberOfContours, xMin)
      else readComposite(data, offset, xMin)
    }

    private fun readSimple(data: FontBytes, offset: Int, contours: Int, xMin: Int): Simple {
      var p = offset + 10
      val endPts = IntArray(contours) { data.u16(p).also { p += 2 } }
      val numPoints = if (contours == 0) 0 else endPts.last() + 1
      p += 2 + data.u16(p) // instructions
      val flags = IntArray(numPoints)
      var i = 0
      while (i < numPoints) {
        val flag = data.u8(p++)
        flags[i++] = flag
        if (flag and REPEAT != 0) {
          repeat(data.u8(p++)) { flags[i++] = flag }
        }
      }
      fun coords(short: Int, same: Int): IntArray {
        var value = 0
        return IntArray(numPoints) { k ->
          val flag = flags[k]
          value +=
            when {
              flag and short != 0 -> data.u8(p++).let { if (flag and same != 0) it else -it }
              flag and same != 0 -> 0
              else -> data.i16(p).also { p += 2 }
            }
          value
        }
      }
      val xs = coords(X_SHORT, X_SAME_OR_POSITIVE)
      val ys = coords(Y_SHORT, Y_SAME_OR_POSITIVE)
      return Simple(xMin, endPts, xs, ys, BooleanArray(numPoints) { flags[it] and ON_CURVE != 0 })
    }

    private fun readComposite(data: FontBytes, offset: Int, xMin: Int): Composite {
      var p = offset + 10
      val components = mutableListOf<Component>()
      do {
        val flags = data.u16(p)
        val glyphId = data.u16(p + 2)
        p += 4
        val dx: Int
        val dy: Int
        if (flags and ARG_1_AND_2_ARE_WORDS != 0) {
          dx = data.i16(p)
          dy = data.i16(p + 2)
          p += 4
        } else {
          dx = data.i8(p)
          dy = data.i8(p + 1)
          p += 2
        }
        // Point-matched placement (anchoring a component point to a parent point) needs the
        // component's points resolved first; variable fonts place components by offset, so it is
        // an explicit, documented limitation rather than a silent misplacement.
        require(flags and ARGS_ARE_XY_VALUES != 0) {
          "composite glyph positions component $glyphId by point matching, which this reader " +
            "does not support"
        }
        var xx = 1f
        var xy = 0f
        var yx = 0f
        var yy = 1f
        when {
          flags and WE_HAVE_A_SCALE != 0 -> {
            xx = data.f2dot14(p)
            yy = xx
            p += 2
          }
          flags and WE_HAVE_AN_X_AND_Y_SCALE != 0 -> {
            xx = data.f2dot14(p)
            yy = data.f2dot14(p + 2)
            p += 4
          }
          flags and WE_HAVE_A_TWO_BY_TWO != 0 -> {
            xx = data.f2dot14(p)
            xy = data.f2dot14(p + 2)
            yx = data.f2dot14(p + 4)
            yy = data.f2dot14(p + 6)
            p += 8
          }
        }
        components += Component(glyphId, dx, dy, xx, xy, yx, yy)
      } while (flags and MORE_COMPONENTS != 0)
      return Composite(xMin, components)
    }
  }
}

/** One tuple variation's region, as normalized start/peak/end per axis. */
internal class TupleRegion(val start: FloatArray, val peak: FloatArray, val end: FloatArray) {
  /** The weight of this region at normalized [coords] (OpenType "scalar"). */
  fun scalar(coords: FloatArray): Float {
    var scalar = 1f
    for (i in peak.indices) {
      val lower = start[i]
      val peak = peak[i]
      val upper = end[i]
      if (peak == 0f) continue
      if (lower > peak || peak > upper) continue
      if (lower < 0f && upper > 0f) continue
      val v = coords[i]
      if (v == peak) continue
      if (v <= lower || upper <= v) return 0f
      scalar *= if (v < peak) (v - lower) / (peak - lower) else (upper - v) / (upper - peak)
    }
    return scalar
  }
}

/** The `gvar` table: per-glyph tuple variations of point coordinates. */
internal class Gvar(
  private val data: FontBytes,
  private val offset: Int,
  private val axisCount: Int,
) {
  private val sharedTuples: List<FloatArray>
  private val glyphCount = data.u16(offset + 12)
  private val longOffsets = data.u16(offset + 14) and 1 != 0
  private val dataStart = offset + data.u32(offset + 16).toInt()

  init {
    val count = data.u16(offset + 6)
    val sharedOffset = offset + data.u32(offset + 8).toInt()
    sharedTuples = List(count) { t -> readTuple(sharedOffset + t * axisCount * 2) }
  }

  private fun readTuple(p: Int) = FloatArray(axisCount) { data.f2dot14(p + it * 2) }

  private fun glyphData(glyphId: Int): IntRange? {
    if (glyphId >= glyphCount) return null
    val (start, end) =
      if (longOffsets) {
        data.u32(offset + 20 + glyphId * 4).toInt() to
          data.u32(offset + 20 + glyphId * 4 + 4).toInt()
      } else {
        data.u16(offset + 20 + glyphId * 2) * 2 to data.u16(offset + 20 + glyphId * 2 + 2) * 2
      }
    return if (end <= start) null else (dataStart + start) until (dataStart + end)
  }

  private class Tuple(val region: TupleRegion, val dataSize: Int, val privatePoints: Boolean)

  private fun headers(glyphId: Int): Triple<Int, List<Tuple>, Int>? {
    val range = glyphData(glyphId) ?: return null
    val base = range.first
    val countAndFlags = data.u16(base)
    val tupleCount = countAndFlags and 0x0FFF
    var p = base + 4
    val tuples =
      List(tupleCount) {
        val size = data.u16(p)
        val index = data.u16(p + 2)
        p += 4
        val peak =
          if (index and EMBEDDED_PEAK_TUPLE != 0) readTuple(p).also { p += axisCount * 2 }
          else sharedTuples[index and TUPLE_INDEX_MASK]
        val start: FloatArray
        val end: FloatArray
        if (index and INTERMEDIATE_REGION != 0) {
          start = readTuple(p)
          end = readTuple(p + axisCount * 2)
          p += axisCount * 4
        } else {
          start = FloatArray(axisCount) { minOf(peak[it], 0f) }
          end = FloatArray(axisCount) { maxOf(peak[it], 0f) }
        }
        Tuple(TupleRegion(start, peak, end), size, index and PRIVATE_POINT_NUMBERS != 0)
      }
    return Triple(base + data.u16(base + 2), tuples, countAndFlags)
  }

  /** Every region that varies [glyphId]. */
  fun tuples(glyphId: Int): List<TupleRegion> =
    headers(glyphId)?.second?.map { it.region } ?: emptyList()

  /**
   * Adds the variation deltas of [glyphId] at [coords] to the points [xs]/[ys] in place.
   *
   * [endPts] are the contour ends of a simple glyph, used to interpolate the deltas of points a
   * tuple does not mention (IUP); for a composite glyph pass null, and unmentioned points get no
   * delta.
   */
  fun apply(glyphId: Int, coords: FloatArray, xs: FloatArray, ys: FloatArray, endPts: IntArray?) {
    val (serialized, tuples, countAndFlags) = headers(glyphId) ?: return
    val numPoints = xs.size
    val origX = xs.copyOf()
    val origY = ys.copyOf()
    var p = serialized
    var sharedPoints: IntArray? = null
    if (countAndFlags and SHARED_POINT_NUMBERS != 0) {
      val (points, next) = readPoints(p, numPoints)
      sharedPoints = points
      p = next
    }
    for (tuple in tuples) {
      val tupleEnd = p + tuple.dataSize
      val scalar = tuple.region.scalar(coords)
      if (scalar == 0f) {
        p = tupleEnd
        continue
      }
      var q = p
      val points =
        if (tuple.privatePoints) readPoints(q, numPoints).also { q = it.second }.first
        else sharedPoints ?: IntArray(numPoints) { it }
      val (dx, afterX) = readDeltas(q, points.size)
      val (dy, _) = readDeltas(afterX, points.size)
      p = tupleEnd

      val deltaX = FloatArray(numPoints)
      val deltaY = FloatArray(numPoints)
      val touched = BooleanArray(numPoints)
      points.forEachIndexed { k, point ->
        if (point < numPoints) {
          deltaX[point] += dx[k].toFloat()
          deltaY[point] += dy[k].toFloat()
          touched[point] = true
        }
      }
      if (endPts != null && points.size < numPoints) {
        interpolateUntouched(endPts, origX, origY, deltaX, deltaY, touched)
      }
      for (i in 0 until numPoints) {
        xs[i] += deltaX[i] * scalar
        ys[i] += deltaY[i] * scalar
      }
    }
  }

  /** Packed point numbers: returns the points and the offset after them. */
  private fun readPoints(start: Int, numPoints: Int): Pair<IntArray, Int> {
    var p = start
    var count = data.u8(p++)
    if (count == 0) return IntArray(numPoints) { it } to p
    if (count and 0x80 != 0) count = ((count and 0x7F) shl 8) or data.u8(p++)
    val points = IntArray(count)
    var i = 0
    var last = 0
    while (i < count) {
      val control = data.u8(p++)
      val runLength = (control and 0x7F) + 1
      val words = control and 0x80 != 0
      repeat(minOf(runLength, count - i)) {
        last += if (words) data.u16(p).also { p += 2 } else data.u8(p++)
        points[i++] = last
      }
    }
    return points to p
  }

  /** Packed deltas: returns [count] deltas and the offset after them. */
  private fun readDeltas(start: Int, count: Int): Pair<IntArray, Int> {
    var p = start
    val deltas = IntArray(count)
    var i = 0
    while (i < count) {
      val control = data.u8(p++)
      val runLength = (control and 0x3F) + 1
      repeat(minOf(runLength, count - i)) {
        deltas[i++] =
          when (control and 0xC0) {
            DELTAS_ARE_ZERO -> 0
            DELTAS_ARE_WORDS -> data.i16(p).also { p += 2 }
            DELTAS_ARE_LONGS -> data.i32(p).also { p += 4 }
            else -> data.i8(p++)
          }
      }
    }
    return deltas to p
  }

  companion object {
    private const val SHARED_POINT_NUMBERS = 0x8000
    private const val EMBEDDED_PEAK_TUPLE = 0x8000
    private const val INTERMEDIATE_REGION = 0x4000
    private const val PRIVATE_POINT_NUMBERS = 0x2000
    private const val TUPLE_INDEX_MASK = 0x0FFF
    private const val DELTAS_ARE_ZERO = 0x80
    private const val DELTAS_ARE_WORDS = 0x40
    private const val DELTAS_ARE_LONGS = 0xC0

    /**
     * Inferred deltas for the points a tuple leaves out (OpenType "IUP"): each untouched point
     * between two touched ones takes a delta interpolated from them by its original coordinate, per
     * axis, and contours with no touched points stay put. Phantom points are never interpolated.
     */
    fun interpolateUntouched(
      endPts: IntArray,
      origX: FloatArray,
      origY: FloatArray,
      deltaX: FloatArray,
      deltaY: FloatArray,
      touched: BooleanArray,
    ) {
      var start = 0
      for (end in endPts) {
        val refs = (start..end).filter { touched[it] }
        if (refs.isNotEmpty() && refs.size < end - start + 1) {
          for (r in refs.indices) {
            val a = refs[r]
            val b = refs[(r + 1) % refs.size]
            var i = if (a == end) start else a + 1
            while (i != b) {
              deltaX[i] = iup(origX[i], origX[a], origX[b], deltaX[a], deltaX[b])
              deltaY[i] = iup(origY[i], origY[a], origY[b], deltaY[a], deltaY[b])
              i = if (i == end) start else i + 1
            }
          }
        }
        start = end + 1
      }
    }

    private fun iup(x: Float, x1In: Float, x2In: Float, d1In: Float, d2In: Float): Float {
      if (x1In == x2In) return if (d1In == d2In) d1In else 0f
      val (x1, x2, d1, d2) =
        if (x1In < x2In) listOf(x1In, x2In, d1In, d2In) else listOf(x2In, x1In, d2In, d1In)
      return when {
        x <= x1 -> d1
        x >= x2 -> d2
        else -> d1 + (x - x1) * (d2 - d1) / (x2 - x1)
      }
    }
  }
}

/**
 * The `HVAR` table: advance-width variation, which a font may keep here rather than (or as well as)
 * in the `gvar` phantom points. When it is present it is the authority for advances.
 */
internal class Hvar(private val data: FontBytes, private val offset: Int) {
  private val store = offset + data.u32(offset + 4).toInt()
  private val advanceMap = data.u32(offset + 8).toInt().takeIf { it != 0 }?.let { offset + it }

  private val regions: List<TupleRegion> = run {
    val list = store + data.u32(store + 2).toInt()
    val axisCount = data.u16(list)
    List(data.u16(list + 2)) { r ->
      val p = list + 4 + r * axisCount * 6
      TupleRegion(
        FloatArray(axisCount) { data.f2dot14(p + it * 6) },
        FloatArray(axisCount) { data.f2dot14(p + it * 6 + 2) },
        FloatArray(axisCount) { data.f2dot14(p + it * 6 + 4) },
      )
    }
  }

  /** The (outer, inner) item a glyph's advance delta lives at. */
  private fun item(glyphId: Int): Pair<Int, Int> {
    val map = advanceMap ?: return 0 to glyphId
    val format = data.u8(map)
    val entryFormat = data.u8(map + 1)
    val count = if (format == 0) data.u16(map + 2) else data.u32(map + 2).toInt()
    val entries = map + if (format == 0) 4 else 6
    val size = ((entryFormat shr 4) and 0x3) + 1
    val innerBits = (entryFormat and 0xF) + 1
    val index = minOf(glyphId, count - 1)
    var entry = 0
    for (b in 0 until size) entry = (entry shl 8) or data.u8(entries + index * size + b)
    return (entry ushr innerBits) to (entry and ((1 shl innerBits) - 1))
  }

  /** The region indexes and deltas of the item behind [glyphId]'s advance. */
  private fun deltas(glyphId: Int): List<Pair<Int, Int>> {
    val (outer, inner) = item(glyphId)
    if (outer >= data.u16(store + 6)) return emptyList()
    val itemData = store + data.u32(store + 8 + outer * 4).toInt()
    val itemCount = data.u16(itemData)
    if (inner >= itemCount) return emptyList()
    val wordCountField = data.u16(itemData + 2)
    val longWords = wordCountField and 0x8000 != 0
    val wordCount = wordCountField and 0x7FFF
    val regionCount = data.u16(itemData + 4)
    val regionIndexes = IntArray(regionCount) { data.u16(itemData + 6 + it * 2) }
    val wordSize = if (longWords) 4 else 2
    val byteSize = if (longWords) 2 else 1
    val rowSize = wordCount * wordSize + (regionCount - wordCount) * byteSize
    var p = itemData + 6 + regionCount * 2 + inner * rowSize
    return List(regionCount) { r ->
      val delta =
        if (r < wordCount) {
          (if (longWords) data.i32(p) else data.i16(p)).also { p += wordSize }
        } else {
          (if (longWords) data.i16(p) else data.i8(p)).also { p += byteSize }
        }
      regionIndexes[r] to delta
    }
  }

  /** The advance-width delta of [glyphId] at normalized [coords], in font units. */
  fun advanceDelta(glyphId: Int, coords: FloatArray): Float =
    deltas(glyphId).fold(0f) { sum, (region, delta) ->
      sum + regions[region].scalar(coords) * delta
    }

  /** The regions that vary [glyphId]'s advance. */
  fun regions(glyphId: Int): List<TupleRegion> =
    deltas(glyphId).filter { it.second != 0 }.map { regions[it.first] }
}
