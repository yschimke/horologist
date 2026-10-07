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
 * A coordinate of a variable font, as the font itself defines it: a default value plus, for each
 * variation region, a delta weighted by that region's scalar.
 *
 * `value = constant + Σ coefficient(region) × region.scalar(coords)`
 *
 * Every outline point and advance of a `glyf`/`gvar`/`HVAR` font has exactly this shape (IUP and
 * composite transforms are linear in the deltas), so it can be evaluated anywhere in the design
 * space — on the device by [evaluate], or on a Remote Compose player as a float expression.
 */
internal class LinearForm(val constant: Float, val terms: Map<TupleRegion, Float> = emptyMap()) {
  val isConstant: Boolean
    get() = terms.isEmpty()

  operator fun plus(other: LinearForm): LinearForm =
    LinearForm(constant + other.constant, merge(terms, other.terms, 1f))

  operator fun minus(other: LinearForm): LinearForm =
    LinearForm(constant - other.constant, merge(terms, other.terms, -1f))

  operator fun times(k: Float): LinearForm =
    if (k == 0f) ZERO else LinearForm(constant * k, terms.mapValues { it.value * k })

  /** The value at normalized design-space [coords]. */
  fun evaluate(coords: FloatArray): Float =
    terms.entries.fold(constant) { sum, (region, k) -> sum + k * region.scalar(coords) }

  companion object {
    val ZERO: LinearForm = LinearForm(0f)

    fun of(value: Float): LinearForm = if (value == 0f) ZERO else LinearForm(value)

    /** The midpoint of [a] and [b]: an implied on-curve point. */
    fun mid(a: LinearForm, b: LinearForm): LinearForm = (a + b) * 0.5f

    private fun merge(
      a: Map<TupleRegion, Float>,
      b: Map<TupleRegion, Float>,
      sign: Float,
    ): Map<TupleRegion, Float> {
      if (b.isEmpty()) return a
      val out = LinkedHashMap(a)
      for ((region, k) in b) {
        val v = (out[region] ?: 0f) + sign * k
        if (v == 0f) out.remove(region) else out[region] = v
      }
      return out
    }
  }
}

/** A contour whose point coordinates are [LinearForm]s. */
internal class VariedContour(
  val xs: List<LinearForm>,
  val ys: List<LinearForm>,
  val onCurve: BooleanArray,
) {
  val size: Int
    get() = xs.size

  fun transformed(
    xx: Float,
    xy: Float,
    yx: Float,
    yy: Float,
    dx: LinearForm,
    dy: LinearForm,
  ): VariedContour =
    VariedContour(
      List(size) { i -> xs[i] * xx + ys[i] * yx + dx },
      List(size) { i -> xs[i] * xy + ys[i] * yy + dy },
      onCurve,
    )

  /** The plain contour at normalized [coords]. */
  fun evaluate(coords: FloatArray): Contour =
    Contour(
      FloatArray(size) { xs[it].evaluate(coords) },
      FloatArray(size) { ys[it].evaluate(coords) },
      onCurve,
    )
}

/** A glyph or line of text whose geometry is [LinearForm]s. */
internal class VariedOutline(val contours: List<VariedContour>, val advance: LinearForm)
