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

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.FloatExpression
import androidx.compose.remote.core.operations.PathData
import androidx.compose.remote.core.operations.layout.Container

/** What a document spends its bytes and its per-frame evaluation on. */
internal data class DocumentStats(
  val bytes: Int,
  val operations: Int,
  val expressions: Int,
  val expressionTokens: Int,
  val pathFloats: Int,
) {
  override fun toString(): String =
    "%,7d B  %5d ops  %5d exprs  %6d tokens (%.1f/expr)  %6d path floats"
      .format(
        bytes,
        operations,
        expressions,
        expressionTokens,
        expressionTokens.toFloat() / expressions,
        pathFloats,
      )

  companion object {
    fun of(document: CoreDocument): DocumentStats {
      val ops = flatten(document.operations)
      val expressions = ops.filterIsInstance<FloatExpression>()
      return DocumentStats(
        bytes = document.buffer.buffer.size,
        operations = ops.size,
        expressions = expressions.size,
        expressionTokens = expressions.sumOf { it.mSrcValue.size },
        pathFloats = ops.filterIsInstance<PathData>().sumOf { pathFloats(it) },
      )
    }

    private fun pathFloats(op: PathData): Int =
      (PathData::class.java.getDeclaredField("mFloatPath").apply { isAccessible = true }.get(op)
          as FloatArray)
        .size

    private fun flatten(ops: List<Operation>): List<Operation> = ops.flatMap { op ->
      listOf(op) + if (op is Container) flatten(op.list) else emptyList()
    }
  }
}
