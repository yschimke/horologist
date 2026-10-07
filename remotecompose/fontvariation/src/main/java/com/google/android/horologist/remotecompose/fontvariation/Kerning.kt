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

/**
 * Pair kerning from a font's `GPOS` table: the x-advance adjustments of the `kern` feature's pair
 * positioning lookups (formats 1 and 2, directly or through extension lookups), with their
 * variation deltas from the `GDEF` item variation store.
 *
 * Only the first glyph's x-advance is read, which is what horizontal kerning uses. Pairs the first
 * matching subtable of a lookup does not cover fall through to its next subtable, and later lookups
 * add to earlier ones, as a shaper applies them.
 */
internal class Gpos(
  private val data: FontBytes,
  private val offset: Int,
  private val variations: ItemVariationStore?,
) {
  /** Each `kern` lookup's pair positioning subtables, in lookup order. */
  private val lookups: List<List<Int>> = run {
    val featureList = offset + data.u16(offset + 6)
    val lookupList = offset + data.u16(offset + 8)
    val lookups = sortedSetOf<Int>()
    for (f in 0 until data.u16(featureList)) {
      val record = featureList + 2 + f * 6
      if (data.tag(record) != "kern") continue
      val feature = featureList + data.u16(record + 4)
      for (l in 0 until data.u16(feature + 2)) lookups += data.u16(feature + 4 + l * 2)
    }
    lookups.map { index ->
      val lookup = lookupList + data.u16(lookupList + 2 + index * 2)
      val type = data.u16(lookup)
      List(data.u16(lookup + 4)) { s -> lookup + data.u16(lookup + 6 + s * 2) }
        .mapNotNull { sub ->
          when {
            type == PAIR -> sub
            type == EXTENSION && data.u16(sub + 2) == PAIR -> sub + data.u32(sub + 4).toInt()
            else -> null
          }
        }
    }
  }

  /** The kerning between [first] and [second] at normalized [coords], in font units. */
  fun kerning(first: Int, second: Int, coords: FloatArray): Float {
    var total = 0f
    for (lookup in lookups) {
      // The first subtable that covers the pair applies; the rest of the lookup is skipped.
      val value = lookup.firstNotNullOfOrNull { pairValue(it, first, second) } ?: continue
      total += value(coords)
    }
    return total
  }

  /**
   * One subtable's adjustment for the pair, or null when the subtable does not cover [first].
   * Format 2 covers every second glyph once it covers the first (class 0 included).
   */
  private fun pairValue(sub: Int, first: Int, second: Int): ((FloatArray) -> Float)? {
    val coverageIndex = coverage(sub + data.u16(sub + 2), first) ?: return null
    val format1 = data.u16(sub + 4)
    val format2 = data.u16(sub + 6)
    val size1 = valueRecordSize(format1)
    val size2 = valueRecordSize(format2)
    return when (data.u16(sub)) {
      1 -> {
        val pairSet = sub + data.u16(sub + 10 + coverageIndex * 2)
        val record = 2 + size1 + size2
        var lo = 0
        var hi = data.u16(pairSet) - 1
        while (lo <= hi) {
          val mid = (lo + hi) ushr 1
          val p = pairSet + 2 + mid * record
          val glyph = data.u16(p)
          when {
            glyph < second -> lo = mid + 1
            glyph > second -> hi = mid - 1
            else -> return xAdvance(p + 2, format1, pairSet)
          }
        }
        null
      }
      2 -> {
        val class1 = classOf(sub + data.u16(sub + 8), first)
        val class2 = classOf(sub + data.u16(sub + 10), second)
        val class2Count = data.u16(sub + 14)
        val p = sub + 16 + (class1 * class2Count + class2) * (size1 + size2)
        xAdvance(p, format1, sub)
      }
      else -> null
    }
  }

  /** The x-advance of the value record at [p], with its device delta resolved against [base]. */
  private fun xAdvance(p: Int, format: Int, base: Int): (FloatArray) -> Float {
    if (format and X_ADVANCE == 0) return { 0f }
    var q = p
    if (format and X_PLACEMENT != 0) q += 2
    if (format and Y_PLACEMENT != 0) q += 2
    val advance = data.i16(q).toFloat()
    // The device offsets follow the four values; x-advance's is the third.
    var d = p + 2 * Integer.bitCount(format and 0xF)
    if (format and X_PLACEMENT_DEVICE != 0) d += 2
    if (format and Y_PLACEMENT_DEVICE != 0) d += 2
    val device = if (format and X_ADVANCE_DEVICE != 0) data.u16(d) else 0
    val store = variations
    if (device == 0 || store == null || data.u16(base + device + 4) != VARIATION_INDEX) {
      return { advance }
    }
    val outer = data.u16(base + device)
    val inner = data.u16(base + device + 2)
    return { coords -> advance + store.delta(outer, inner, coords) }
  }

  private fun valueRecordSize(format: Int): Int = 2 * Integer.bitCount(format and 0xFF)

  private fun coverage(table: Int, glyph: Int): Int? {
    when (data.u16(table)) {
      1 -> {
        var lo = 0
        var hi = data.u16(table + 2) - 1
        while (lo <= hi) {
          val mid = (lo + hi) ushr 1
          val g = data.u16(table + 4 + mid * 2)
          when {
            g < glyph -> lo = mid + 1
            g > glyph -> hi = mid - 1
            else -> return mid
          }
        }
        return null
      }
      2 ->
        return (0 until data.u16(table + 2))
          .map { table + 4 + it * 6 }
          .firstOrNull { glyph in data.u16(it)..data.u16(it + 2) }
          ?.let { data.u16(it + 4) + glyph - data.u16(it) }
      else -> return null
    }
  }

  private fun classOf(table: Int, glyph: Int): Int =
    when (data.u16(table)) {
      1 -> {
        val start = data.u16(table + 2)
        if (glyph - start in 0 until data.u16(table + 4)) data.u16(table + 6 + (glyph - start) * 2)
        else 0
      }
      2 ->
        (0 until data.u16(table + 2))
          .map { table + 4 + it * 6 }
          .firstOrNull { glyph in data.u16(it)..data.u16(it + 2) }
          ?.let { data.u16(it + 4) } ?: 0
      else -> 0
    }

  private companion object {
    const val PAIR = 2
    const val EXTENSION = 9
    const val X_PLACEMENT = 0x0001
    const val Y_PLACEMENT = 0x0002
    const val X_ADVANCE = 0x0004
    const val X_PLACEMENT_DEVICE = 0x0010
    const val Y_PLACEMENT_DEVICE = 0x0020
    const val X_ADVANCE_DEVICE = 0x0040
    const val VARIATION_INDEX = 0x8000
  }
}
