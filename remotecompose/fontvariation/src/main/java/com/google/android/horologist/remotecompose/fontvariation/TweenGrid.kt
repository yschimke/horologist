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
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.clamp
import androidx.compose.remote.creation.compose.state.rf

/**
 * An outline animated on one axis drawn as path tweens between key outlines, with no coordinate
 * expressions at all.
 *
 * It applies when no two of the tents the axis' regions use overlap, as when its regions meet only
 * at its default. Then at most one tent's scalar is non-zero, and the outline is the default
 * outline tweened towards that tent's key (the outline with the tent at its peak) by the scalar:
 * exactly the font's outline, drawn by one `drawTweenPath`, chosen by a condition on the scalars.
 *
 * Each frame only tweens two paths, but every key is a whole outline, so with many tents the keys
 * outweigh the expressions they replace; [of] returns a grid only when its document is the smaller.
 * Several axes would need tweens of tweens, which the embedded player does not draw.
 */
internal class TweenGrid
private constructor(
  val tents: List<Tent>,
  private val commands: List<Pair<Int, List<AnimatedForm>>>,
) {
  /** The outline with [tent] at its peak, or the default outline when it is null. */
  @SuppressLint("RestrictedApi")
  fun key(tent: Tent?): RemotePath {
    val value = { form: AnimatedForm ->
      form.terms.entries.fold(form.constant) { sum, (region, k) ->
        if (region == listOfNotNull(tent)) sum + k else sum
      }
    }
    val path = RemotePath()
    for ((verb, forms) in commands) {
      val v = forms.map(value)
      when (verb) {
        MOVE -> path.moveTo(v[0], v[1])
        LINE -> path.lineTo(v[0], v[1])
        QUAD -> path.quadTo(v[0], v[1], v[2], v[3])
        else -> path.close()
      }
    }
    return path
  }

  /**
   * Writes the keys and draws the outline with the current paint. [scalar] gives the id of a tent's
   * scalar on the player, and [anyTent] the id of a value above zero when some tent is.
   */
  @SuppressLint("RestrictedApi")
  fun draw(writer: RemoteComposeWriter, scalar: (Tent) -> Float, anyTent: Float) {
    val base = writer.addPathData(key(null))
    val keys = tents.map { writer.addPathData(key(it)) }
    writer.conditionalOperations(ConditionalOperations.TYPE_LTE, anyTent, 0f)
    writer.drawPath(base)
    writer.endConditionalOperations()
    tents.forEachIndexed { i, tent ->
      writer.conditionalOperations(ConditionalOperations.TYPE_GT, scalar(tent), 0f)
      writer.drawTweenPath(base, keys[i], scalar(tent), 0f, 1f)
      writer.endConditionalOperations()
    }
  }

  companion object {
    private const val MOVE = 0
    private const val LINE = 1
    private const val QUAD = 2
    private const val CLOSE = 3

    /**
     * The grid for [outline] with [animated] axes, or null when more than one axis is animated, two
     * of its tents overlap, or the keys would outweigh an expression per coordinate.
     */
    fun of(
      outline: VariedOutline,
      specialization: AxisSpecialization,
      animated: List<Int>,
    ): TweenGrid? {
      val commands = mutableListOf<Pair<Int, List<AnimatedForm>>>()
      outline.emit(
        object : PathSink<LinearForm> {
          override fun moveTo(x: LinearForm, y: LinearForm) {
            commands += MOVE to listOf(x, y).map(specialization::specialize)
          }

          override fun lineTo(x: LinearForm, y: LinearForm) {
            commands += LINE to listOf(x, y).map(specialization::specialize)
          }

          override fun quadTo(x1: LinearForm, y1: LinearForm, x2: LinearForm, y2: LinearForm) {
            commands += QUAD to listOf(x1, y1, x2, y2).map(specialization::specialize)
          }

          override fun close() {
            commands += CLOSE to emptyList()
          }
        }
      )
      if (animated.size != 1) return null
      val regions = commands.flatMap { it.second }.flatMap { it.terms.keys }.distinct()
      if (regions.any { it.size != 1 }) return null
      val tents = regions.map { it.single() }
      if (tents.isEmpty()) return null
      val disjoint =
        tents.indices.all { i ->
          (i + 1 until tents.size).all { j ->
            tents[i].end <= tents[j].start || tents[j].end <= tents[i].start
          }
        }
      if (!disjoint) return null
      // Bytes on the wire, roughly: a path float is 4; an expression is about 10 plus 4 per token,
      // and one per distinct moving coordinate has a token for its constant and 4 per term.
      val pathFloats = commands.sumOf { (verb, _) ->
        when (verb) {
          MOVE -> 3
          LINE -> 5
          QUAD -> 7
          else -> 1
        }.toLong()
      }
      val keys = tents.size + 1L
      val grid = keys * pathFloats * 4
      val expressions =
        pathFloats * 4 +
          commands
            .flatMap { it.second }
            .filterNot { it.isConstant }
            .distinct()
            .sumOf { 10L + 4 * (1 + 4 * it.terms.size) }
      return if (grid <= expressions) TweenGrid(tents, commands) else null
    }
  }
}

/**
 * A tent's value as a function of its axis' user value [value], written as clamped ramps: it is
 * piecewise linear, with corners where the font's user-to-normalized mapping (`avar` included) has
 * them and where the tent does.
 */
@SuppressLint("RestrictedApi")
internal fun VariableFont.tentOf(tent: Tent, value: RemoteFloat): RemoteFloat {
  val info = axes[tent.axis]
  val f = { v: Float -> tent.evaluate(normalize(mapOf(info.tag to v))[tent.axis]) }
  val knots =
    (listOf(info.minValue, info.maxValue) +
        avarBreakpoints(tent.axis) +
        listOf(tent.start, tent.peak, tent.end).map { denormalize(tent.axis, it) })
      .filter { it in info.minValue..info.maxValue }
      .distinct()
      .sorted()
  val ys = knots.map(f)
  var sum: RemoteFloat = ys.first().rf
  for (k in 0 until knots.size - 1) {
    val width = knots[k + 1] - knots[k]
    val rise = ys[k + 1] - ys[k]
    if (rise != 0f) sum += clamp(value - knots[k], 0f, width) * (rise / width)
  }
  return sum
}
