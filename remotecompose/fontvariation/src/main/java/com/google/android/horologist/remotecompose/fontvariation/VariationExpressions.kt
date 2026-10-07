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
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rf

/**
 * One axis' factor of a region scalar: rises from 0 at [start] to 1 at [peak] and falls back to 0
 * at [end], in normalized coordinates.
 */
internal data class Tent(val axis: Int, val start: Float, val peak: Float, val end: Float) {
  fun evaluate(n: Float): Float =
    when {
      n == peak -> 1f
      n <= start || end <= n -> 0f
      n < peak -> (n - start) / (peak - start)
      else -> (end - n) / (end - peak)
    }
}

/**
 * The factors of a region scalar that can vary. Axes whose peak is 0, and malformed ones, are
 * always 1 and are left out, exactly as [TupleRegion.scalar] skips them.
 */
internal fun TupleRegion.tents(): List<Tent> =
  peak.indices.mapNotNull { i ->
    val s = start[i]
    val p = peak[i]
    val e = end[i]
    if (p == 0f || s > p || p > e || (s < 0f && e > 0f)) null else Tent(i, s, p, e)
  }

/**
 * A [LinearForm] with the axes that are not animated folded in at their fixed values: what is left
 * is a constant plus, per distinct product of animated [Tent]s, a coefficient.
 */
internal data class AnimatedForm(val constant: Float, val terms: Map<List<Tent>, Float>) {
  val isConstant: Boolean
    get() = terms.isEmpty()

  fun evaluate(coords: FloatArray): Float =
    terms.entries.fold(constant) { sum, (tents, k) ->
      sum + k * tents.fold(1f) { p, t -> p * t.evaluate(coords[t.axis]) }
    }
}

/**
 * Specializes the forms of a font to a set of [animated] axes, holding every other axis at its
 * normalized value in [fixed].
 */
internal class AxisSpecialization(private val animated: Set<Int>, private val fixed: FloatArray) {
  private val regions = HashMap<TupleRegion, Pair<Float, List<Tent>>>()

  fun specialize(form: LinearForm): AnimatedForm {
    var constant = form.constant
    val terms = LinkedHashMap<List<Tent>, Float>()
    for ((region, k) in form.terms) {
      val (factor, tents) =
        regions.getOrPut(region) {
          val (moving, still) = region.tents().partition { it.axis in animated }
          still.fold(1f) { p, t -> p * t.evaluate(fixed[t.axis]) } to moving
        }
      if (factor == 0f) continue
      if (tents.isEmpty()) {
        constant += k * factor
      } else {
        val v = (terms[tents] ?: 0f) + k * factor
        if (v == 0f) terms.remove(tents) else terms[tents] = v
      }
    }
    return AnimatedForm(constant, terms)
  }
}

/**
 * Builds the Remote Compose float expressions that evaluate a font's variation model on the player.
 * Each axis is normalized once, each distinct product of tents is written once, and each distinct
 * coordinate once; the [RemoteFloat]s returned refer to them rather than repeat them.
 *
 * @param axes The animated axes, by index, with their user-space values.
 */
@SuppressLint("RestrictedApi")
internal class RemoteVariationModel(
  private val font: VariableFont,
  private val axes: Map<Int, RemoteFloat>,
) {
  private val normalized = HashMap<Int, RemoteFloat>()
  private val scalars = HashMap<List<Tent>, RemoteFloat>()
  private val forms = HashMap<AnimatedForm, RemoteFloat>()

  /** The value of [form] on the player. */
  fun float(form: AnimatedForm): RemoteFloat =
    if (form.isConstant) form.constant.rf
    else
      forms.getOrPut(form) {
        form.terms.entries
          .fold(form.constant.rf) { sum, (tents, k) -> sum + scalar(tents) * k }
          .createReference()
      }

  private fun scalar(tents: List<Tent>): RemoteFloat =
    scalars.getOrPut(tents) { tents.map { tent(it) }.reduce { p, t -> p * t }.createReference() }

  private fun tent(t: Tent): RemoteFloat {
    val n = normalized(t.axis)
    // A side with no width is a step; a slope steep enough to cross a whole 1/16384 F2DOT14 step
    // is indistinguishable from one.
    val rise =
      if (t.peak > t.start) (n - t.start) / (t.peak - t.start) else (n - t.start) * STEP + 1f
    val fall = if (t.end > t.peak) (-n + t.end) / (t.end - t.peak) else (-n + t.end) * STEP + 1f
    return max(min(rise, fall), 0f)
  }

  /**
   * The normalized coordinate of an axis: the font's own user-to-normalized mapping, `avar`
   * included, which is piecewise linear with corners at the axis' minimum, default, maximum and
   * `avar` inputs. Written as a sum of clamped ramps, one per segment.
   */
  private fun normalized(axis: Int): RemoteFloat =
    normalized.getOrPut(axis) {
      val info = font.axes[axis]
      val value = axes.getValue(axis)
      val knots =
        (listOf(info.minValue, info.defaultValue, info.maxValue) + font.avarBreakpoints(axis))
          .filter { it in info.minValue..info.maxValue }
          .distinct()
          .sorted()
      val ys = knots.map { font.normalize(mapOf(info.tag to it))[axis] }
      var sum: RemoteFloat = ys.first().rf
      for (k in 0 until knots.size - 1) {
        val width = knots[k + 1] - knots[k]
        val rise = ys[k + 1] - ys[k]
        if (rise == 0f) continue
        sum += clamp(value - knots[k], 0f, width) * (rise / width)
      }
      sum.createReference()
    }

  private companion object {
    const val STEP = 1e6f
  }
}
