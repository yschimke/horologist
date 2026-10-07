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

import kotlin.math.abs
import kotlin.math.hypot

/**
 * One line of text in a variable font, worked out for a set of animated axes: everything
 * [RemoteVariableFontText] needs to draw it, with the font no longer involved.
 *
 * Make one with [VariableFont.outline], at run time or ahead of it: [encode] turns it into a string
 * that a build step can write into source, and [decode] reads it back without parsing a font or
 * laying anything out, so a document that draws it is quick to create and the app need not ship the
 * font at all.
 *
 * It holds the outline in one of two exact forms. When one axis is animated and its variation
 * regions do not overlap, a few whole key outlines, tweened between on the player. Otherwise each
 * coordinate as `constant + Σ coefficient × region scalar`, evaluated on the player.
 */
public class VariableTextOutline
internal constructor(
  /** The animated axes' tags, in the order [tents] refer to them. */
  public val axes: List<String>,
  internal val unitsPerEm: Int,
  internal val ascender: Int,
  internal val descender: Int,
  /** The widest advance anywhere the axes move, in font units: the box never reflows. */
  internal val width: Float,
  internal val tents: List<TentRamps>,
  internal val verbs: ByteArray,
  internal val body: Body,
) {
  /**
   * A tent as clamped ramps of its axis' user value: `y0 + Σ slope × clamp(v - knot, 0, width)`.
   */
  internal class TentRamps(
    val axis: Int,
    val y0: Float,
    val knots: FloatArray,
    val widths: FloatArray,
    val slopes: FloatArray,
  ) {
    fun evaluate(v: Float): Float =
      knots.indices.fold(y0) { s, i -> s + slopes[i] * (v - knots[i]).coerceIn(0f, widths[i]) }
  }

  internal sealed interface Body

  /** The default outline and, per tent, the outline with that tent at its peak. */
  internal class Keys(val base: FloatArray, val keys: List<FloatArray>) : Body

  /**
   * Each coordinate in path order is [literals] when [slots] is -1, else form `slots[i]`. Form `f`
   * is `constants[f] + Σ coefficients[t] × region(regionOf[t])` for `t` in `termStart[f] until
   * termStart[f + 1]`; a region is the product of its tents.
   */
  internal class Forms(
    val regions: List<IntArray>,
    val constants: FloatArray,
    val termStart: IntArray,
    val regionOf: IntArray,
    val coefficients: FloatArray,
    val slots: IntArray,
    val literals: FloatArray,
  ) : Body

  /** The coordinates in path order at user axis [values] (indexed like [axes]): a check. */
  internal fun evaluate(values: FloatArray): FloatArray {
    val scalars = FloatArray(tents.size) { tents[it].evaluate(values[tents[it].axis]) }
    return when (val b = body) {
      is Keys ->
        FloatArray(b.base.size) { i ->
          b.keys.indices.fold(b.base[i]) { s, t -> s + scalars[t] * (b.keys[t][i] - b.base[i]) }
        }
      is Forms -> {
        val regions = b.regions.map { r -> r.fold(1f) { p, t -> p * scalars[t] } }
        val forms =
          FloatArray(b.constants.size) { f ->
            (b.termStart[f] until b.termStart[f + 1]).fold(b.constants[f]) { s, t ->
              s + b.coefficients[t] * regions[b.regionOf[t]]
            }
          }
        FloatArray(b.slots.size) { i -> if (b.slots[i] < 0) b.literals[i] else forms[b.slots[i]] }
      }
    }
  }

  /** This outline as a string [decode] reads back exactly. */
  public fun encode(): String =
    Packer()
      .apply {
        int(FORMAT)
        int(axes.size)
        axes.forEach { tag -> tag.forEach { int(it.code) } }
        int(unitsPerEm)
        int(ascender)
        int(descender)
        float(width)
        int(tents.size)
        for (t in tents) {
          int(t.axis)
          float(t.y0)
          int(t.knots.size)
          t.knots.forEach(::float)
          t.widths.forEach(::float)
          t.slopes.forEach(::float)
        }
        int(verbs.size)
        verbs.forEach { int(it.toInt()) }
        when (val b = body) {
          is Keys -> {
            int(0)
            int(b.base.size)
            path(b.base, null)
            b.keys.forEach { path(it, b.base) }
          }
          is Forms -> {
            int(1)
            int(b.regions.size)
            b.regions.forEach { r ->
              int(r.size)
              r.forEach(::int)
            }
            int(b.constants.size)
            b.constants.forEach(::float)
            b.termStart.forEach(::int)
            b.regionOf.forEach(::int)
            b.coefficients.forEach(::float)
            int(b.slots.size)
            for (i in b.slots.indices) {
              int(b.slots[i] + 1)
              if (b.slots[i] < 0) float(b.literals[i])
            }
          }
        }
      }
      .toString()

  public companion object {
    private const val FORMAT = 1

    /** Reads an outline [encode] wrote; [parts] are concatenated, so a long one can be split. */
    public fun decode(vararg parts: String): VariableTextOutline {
      val r = Unpacker(parts.joinToString(""))
      check(r.int() == FORMAT) { "unknown outline format" }
      val axes = List(r.int()) { String(CharArray(4) { r.int().toChar() }) }
      val unitsPerEm = r.int()
      val ascender = r.int()
      val descender = r.int()
      val width = r.float()
      val tents =
        List(r.int()) {
          val axis = r.int()
          val y0 = r.float()
          val n = r.int()
          TentRamps(axis, y0, r.floats(n), r.floats(n), r.floats(n))
        }
      val verbs = ByteArray(r.int()) { r.int().toByte() }
      val body =
        if (r.int() == 0) {
          val size = r.int()
          val base = r.path(size, null)
          Keys(base, List(tents.size) { r.path(size, base) })
        } else {
          val regions = List(r.int()) { IntArray(r.int()) { r.int() } }
          val forms = r.int()
          val constants = r.floats(forms)
          val termStart = IntArray(forms + 1) { r.int() }
          val terms = termStart[forms]
          val regionOf = IntArray(terms) { r.int() }
          val coefficients = r.floats(terms)
          val n = r.int()
          val literals = FloatArray(n)
          val slots =
            IntArray(n) { i -> (r.int() - 1).also { if (it < 0) literals[i] = r.float() } }
          Forms(regions, constants, termStart, regionOf, coefficients, slots, literals)
        }
      return VariableTextOutline(axes, unitsPerEm, ascender, descender, width, tents, verbs, body)
    }
  }
}

/**
 * [text] worked out for animating [axes] (tags), the other axes held at [location] and kerning
 * taken at [kerningLocation]: what [RemoteVariableFontText] draws, without the font.
 *
 * With [pixelSize], the font size in pixels the text will be drawn at, the outline is also
 * simplified wherever that cannot move an edge by more than [tolerancePixels]: variation terms too
 * small to matter are dropped, curves that are flat at every axis value become lines, and points on
 * a straight line at every axis value go. At a larger size those changes could show.
 */
public fun VariableFont.outline(
  text: String,
  axes: List<String>,
  location: Map<String, Float> = emptyMap(),
  kerningLocation: Map<String, Float> = location,
  pixelSize: Float? = null,
  tolerancePixels: Float = DEFAULT_TOLERANCE_PIXELS,
): VariableTextOutline =
  outline(text, axes, location, kerningLocation, pixelSize, tolerancePixels, true)

/** The default for [outline]'s `tolerancePixels`: a sixteenth of a pixel. */
public const val DEFAULT_TOLERANCE_PIXELS: Float = 1f / 16

internal fun VariableFont.outline(
  text: String,
  axes: List<String>,
  location: Map<String, Float>,
  kerningLocation: Map<String, Float>,
  pixelSize: Float?,
  tolerancePixels: Float,
  allowKeys: Boolean,
): VariableTextOutline {
  val indices = axes.map { tag -> this.axes.indexOfFirst { it.tag == tag } }
  require(indices.none { it < 0 }) {
    "${axes.filterIndexed { i, _ -> indices[i] < 0 }} not among ${this.axes.map { it.tag }}"
  }
  val varied = variedLayout(text, kerningLocation)
  val fixed = normalize(location)
  val specialization = AxisSpecialization(indices.toSet(), fixed)
  val verbs = mutableListOf<Int>()
  val coords = mutableListOf<AnimatedForm>()
  varied.emit(
    object : PathSink<LinearForm> {
      override fun moveTo(x: LinearForm, y: LinearForm) = add(MOVE, x, y)

      override fun lineTo(x: LinearForm, y: LinearForm) = add(LINE, x, y)

      override fun quadTo(x1: LinearForm, y1: LinearForm, x2: LinearForm, y2: LinearForm) =
        add(QUAD, x1, y1, x2, y2)

      override fun close() {
        verbs += CLOSE
      }

      private fun add(verb: Int, vararg forms: LinearForm) {
        verbs += verb
        forms.forEach { coords += specialization.specialize(it) }
      }
    }
  )
  var commands = OutlineCommands(verbs, coords)
  if (pixelSize != null) {
    val tolerance = tolerancePixels * unitsPerEm / pixelSize
    commands = commands.pruneTerms(tolerance / 2).removePoints(tolerance / 2)
  }

  val tents = commands.coords.flatMap { it.terms.keys }.flatten().distinct()
  val ramps = tents.map { t ->
    val r = tentRamps(t)
    VariableTextOutline.TentRamps(
      indices.indexOf(t.axis),
      r.first,
      r.second.map { it.first }.toFloatArray(),
      r.second.map { it.second }.toFloatArray(),
      r.second.map { it.third }.toFloatArray(),
    )
  }
  val width = maxAdvance(text, varied.advance, indices.toSet(), fixed)
  val body = (if (allowKeys) commands.keys(tents) else null) ?: commands.forms(tents)
  return VariableTextOutline(
    axes,
    unitsPerEm,
    ascender,
    descender,
    width,
    ramps,
    ByteArray(commands.verbs.size) { commands.verbs[it].toByte() },
    body,
  )
}

internal const val MOVE = 0
internal const val LINE = 1
internal const val QUAD = 2
internal const val CLOSE = 3

private fun coordinatesOf(verb: Int) =
  when (verb) {
    MOVE,
    LINE -> 2
    QUAD -> 4
    else -> 0
  }

/** Path verbs and their coordinates, in order. */
private class OutlineCommands(val verbs: List<Int>, val coords: List<AnimatedForm>) {
  /** Drops each coordinate's smallest terms while together they cannot exceed [budget]. */
  fun pruneTerms(budget: Float): OutlineCommands =
    OutlineCommands(
      verbs,
      coords.map { form ->
        var spent = 0f
        val kept = LinkedHashMap(form.terms)
        for ((region, k) in form.terms.entries.sortedBy { abs(it.value) }) {
          if (spent + abs(k) > budget) break
          spent += abs(k)
          kept.remove(region)
        }
        AnimatedForm(form.constant, kept)
      },
    )

  /**
   * Turns quadratics that stay within [budget] of their chord into lines, then removes on-curve
   * points that stay within [budget] of the line through their neighbours, at every axis value.
   * Each test bounds the deviation over all values by its constant part plus every term at full
   * strength, since every region scalar is between 0 and 1.
   */
  fun removePoints(budget: Float): OutlineCommands {
    data class Command(val verb: Int, val c: List<AnimatedForm>)

    val commands = mutableListOf<Command>()
    var i = 0
    for (v in verbs) {
      val n = coordinatesOf(v)
      commands += Command(v, coords.subList(i, i + n))
      i += n
    }
    // Quadratic to line: the curve's furthest point from its chord is half the control point's
    // distance from the chord's midpoint.
    var previous: Pair<AnimatedForm, AnimatedForm>? = null
    val flattened = commands.map { c ->
      val out =
        if (c.verb == QUAD && previous != null) {
          val (px, py) = previous!!
          val dx = c.c[0] - (px + c.c[2]) * 0.5f
          val dy = c.c[1] - (py + c.c[3]) * 0.5f
          if (bound(dx, dy) / 2 <= budget) Command(LINE, c.c.subList(2, 4)) else c
        } else c
      if (c.verb != CLOSE) previous = c.c[c.c.size - 2] to c.c[c.c.size - 1]
      out
    }
    // Collinear points: B in A→B→C (both lines) goes when B stays near the segment A→C. The
    // point after a removal is kept, so deviations do not add up.
    val result = mutableListOf<Command>()
    var k = 0
    while (k < flattened.size) {
      val c = flattened[k]
      val next = flattened.getOrNull(k + 1)
      val prev = result.lastOrNull()
      if (c.verb == LINE && next?.verb == LINE && prev != null && prev.verb != CLOSE) {
        val ax = prev.c[prev.c.size - 2]
        val ay = prev.c[prev.c.size - 1]
        // Any point on A→C bounds B's distance from it; take B's projection at the default.
        val ex = next.c[0].constant - ax.constant
        val ey = next.c[1].constant - ay.constant
        val length2 = ex * ex + ey * ey
        val t =
          if (length2 == 0f) 0f
          else
            (((c.c[0].constant - ax.constant) * ex + (c.c[1].constant - ay.constant) * ey) /
                length2)
              .coerceIn(0f, 1f)
        val dx = c.c[0] - (ax * (1 - t) + next.c[0] * t)
        val dy = c.c[1] - (ay * (1 - t) + next.c[1] * t)
        if (bound(dx, dy) <= budget) {
          result += next
          k += 2
          continue
        }
      }
      result += c
      k++
    }
    return OutlineCommands(result.map { it.verb }, result.flatMap { it.c })
  }

  /**
   * The key outlines, when one axis moves the outline, its tents never overlap, and keys are
   * smaller.
   */
  fun keys(tents: List<Tent>): VariableTextOutline.Keys? {
    if (tents.isEmpty() || tents.map { it.axis }.distinct().size != 1) return null
    if (coords.any { form -> form.terms.keys.any { it.size != 1 } }) return null
    val disjoint =
      tents.indices.all { i ->
        (i + 1 until tents.size).all { j ->
          tents[i].end <= tents[j].start || tents[j].end <= tents[i].start
        }
      }
    if (!disjoint) return null
    // Bytes on the wire, roughly: a path float is 4; an expression is about 10 plus 4 per token,
    // and one per distinct moving coordinate has a token for its constant and 3 per term.
    val pathFloats = verbs.sumOf { 1L + coordinatesOf(it) + if (it == LINE || it == QUAD) 2 else 0 }
    val keyBytes = (tents.size + 1) * pathFloats * 4
    val expressionBytes =
      pathFloats * 4 +
        coords.filterNot { it.isConstant }.distinct().sumOf { 10L + 4 * (1 + 3 * it.terms.size) }
    if (keyBytes > expressionBytes) return null
    val base = FloatArray(coords.size) { coords[it].constant }
    return VariableTextOutline.Keys(
      base,
      tents.map { t ->
        FloatArray(coords.size) { coords[it].constant + (coords[it].terms[listOf(t)] ?: 0f) }
      },
    )
  }

  fun forms(tents: List<Tent>): VariableTextOutline.Forms {
    val regions = coords.flatMap { it.terms.keys }.distinct()
    val regionIndex = regions.withIndex().associate { (i, r) -> r to i }
    val forms = coords.filterNot { it.isConstant }.distinct()
    val formIndex = forms.withIndex().associate { (i, f) -> f to i }
    val termStart = IntArray(forms.size + 1)
    val regionOf = mutableListOf<Int>()
    val coefficients = mutableListOf<Float>()
    forms.forEachIndexed { f, form ->
      termStart[f] = regionOf.size
      form.terms.forEach { (region, k) ->
        regionOf += regionIndex.getValue(region)
        coefficients += k
      }
    }
    termStart[forms.size] = regionOf.size
    return VariableTextOutline.Forms(
      regions.map { r -> IntArray(r.size) { tents.indexOf(r[it]) } },
      FloatArray(forms.size) { forms[it].constant },
      termStart,
      regionOf.toIntArray(),
      coefficients.toFloatArray(),
      IntArray(coords.size) { if (coords[it].isConstant) -1 else formIndex.getValue(coords[it]) },
      FloatArray(coords.size) { if (coords[it].isConstant) coords[it].constant else 0f },
    )
  }
}

private operator fun AnimatedForm.plus(o: AnimatedForm): AnimatedForm = combine(o, 1f)

private operator fun AnimatedForm.minus(o: AnimatedForm): AnimatedForm = combine(o, -1f)

private operator fun AnimatedForm.times(k: Float): AnimatedForm =
  AnimatedForm(constant * k, terms.mapValues { it.value * k })

private fun AnimatedForm.combine(o: AnimatedForm, sign: Float): AnimatedForm {
  val terms = LinkedHashMap(terms)
  o.terms.forEach { (r, k) -> terms[r] = (terms[r] ?: 0f) + sign * k }
  return AnimatedForm(constant + sign * o.constant, terms)
}

/** An upper bound on the length of (dx, dy) anywhere the region scalars can be, each in [0, 1]. */
private fun bound(dx: AnimatedForm, dy: AnimatedForm): Float {
  fun extent(f: AnimatedForm) =
    abs(f.constant) + f.terms.values.sumOf { abs(it).toDouble() }.toFloat()
  return hypot(extent(dx), extent(dy))
}

/**
 * A value per character for most of an outline: a whole number (zigzag) is one character from
 * U+0022, or three after U+0020 when it is large; any other float is its bits in three characters
 * after U+0021. In a path, whole numbers are differences from the predicted value and a float's
 * bits are the value itself.
 */
private class Packer {
  private val out = StringBuilder()

  fun int(n: Int) {
    val z = (n shl 1) xor (n shr 31)
    if (z < SMALL) {
      out.append((FIRST + z).toChar())
    } else {
      out.append(BIG.toChar())
      out.append((0x100 + (z ushr 30)).toChar())
      out.append((0x100 + ((z ushr 15) and 0x7fff)).toChar())
      out.append((0x100 + (z and 0x7fff)).toChar())
    }
  }

  fun float(f: Float) {
    val n = f.toInt()
    if (n.toFloat() == f && !(f == 0f && 1f / f < 0f)) int(n) else bits(f)
  }

  private fun bits(f: Float) {
    val bits = f.toRawBits()
    out.append(BITS.toChar())
    out.append((0x100 + (bits ushr 22)).toChar())
    out.append((0x100 + ((bits ushr 11) and 0x7ff)).toChar())
    out.append((0x100 + (bits and 0x7ff)).toChar())
  }

  /** A path's coordinates, each the difference from [base]'s or from the last x or y when exact. */
  fun path(values: FloatArray, base: FloatArray?) {
    val last = FloatArray(2)
    values.forEachIndexed { i, v ->
      val predicted = base?.get(i) ?: last[i % 2]
      val d = (v - predicted).toInt()
      if (predicted + d == v && !(v == 0f && 1f / v < 0f)) int(d) else bits(v)
      last[i % 2] = v
    }
  }

  override fun toString(): String = out.toString()
}

private class Unpacker(private val s: String) {
  private var i = 0

  fun int(): Int {
    val c = s[i++].code
    val z =
      when (c) {
        BIG -> {
          var z = 0
          repeat(3) { z = (z shl 15) or (s[i++].code - 0x100) }
          z
        }
        BITS -> error("expected a whole number at ${i - 1}")
        else -> c - FIRST
      }
    return (z ushr 1) xor -(z and 1)
  }

  fun float(): Float = if (s[i].code == BITS) bits() else int().toFloat()

  private fun bits(): Float {
    i++
    var bits = 0
    repeat(3) { bits = (bits shl 11) or (s[i++].code - 0x100) }
    return Float.fromBits(bits)
  }

  fun floats(n: Int) = FloatArray(n) { float() }

  fun path(n: Int, base: FloatArray?): FloatArray {
    val last = FloatArray(2)
    return FloatArray(n) { k ->
      val v = if (s[i].code == BITS) bits() else int() + (base?.get(k) ?: last[k % 2])
      v.also { last[k % 2] = it }
    }
  }
}

private const val BIG = 0x20
private const val BITS = 0x21
private const val FIRST = 0x22

/** Whole numbers below this are one character, staying clear of the surrogates at U+D800. */
private const val SMALL = 0xD000 - FIRST
