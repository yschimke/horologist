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
import androidx.compose.remote.creation.compose.state.mad
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
 * Each tent is written once, as clamped ramps of its axis' user value, each distinct product of
 * tents once, and each distinct coordinate once, as a chain of multiply-adds; the [RemoteFloat]s
 * returned refer to them rather than repeat them.
 *
 * @param axes The animated axes, by index, with their user-space values.
 */
@SuppressLint("RestrictedApi")
internal class RemoteVariationModel(
  private val font: VariableFont,
  private val axes: Map<Int, RemoteFloat>,
) {
  private val tents = HashMap<Tent, RemoteFloat>()
  private val scalars = HashMap<List<Tent>, RemoteFloat>()
  private val forms = HashMap<AnimatedForm, RemoteFloat>()

  /**
   * The value of [form] on the player. Each multiply-add holds two more values on the stack until
   * the chain unwinds, so a long chain is written in parts that fit an expression's 32 tokens.
   */
  fun float(form: AnimatedForm): RemoteFloat =
    if (form.isConstant) form.constant.rf
    else
      forms.getOrPut(form) {
        form.terms.entries.chunked(TERMS_PER_EXPRESSION).fold(form.constant.rf) { sum, terms ->
          terms.fold(sum) { s, (region, k) -> mad(scalar(region), k.rf, s) }.createReference()
        }
      }

  private fun scalar(region: List<Tent>): RemoteFloat =
    scalars.getOrPut(region) {
      if (region.size == 1) tent(region.single())
      else region.map(::tent).reduce { p, t -> p * t }.createReference()
    }

  private fun tent(t: Tent): RemoteFloat =
    tents.getOrPut(t) { font.tentOf(t, axes.getValue(t.axis)).createReference() }

  private companion object {
    /** Three tokens a term and one for the sum so far: 31 of the 32 an expression may have. */
    const val TERMS_PER_EXPRESSION = 10
  }
}
