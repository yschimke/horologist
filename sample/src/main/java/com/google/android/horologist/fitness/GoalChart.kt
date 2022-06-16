/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.android.horologist.fitness

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.google.android.horologist.compose.tools.WearLargeRoundDevicePreview
import androidx.compose.ui.graphics.Paint as ComposePaint

val textPaint = ComposePaint().apply {
    color = Color.White
}


val barPaint = ComposePaint().apply {
    color = Color(FitnessTheme.primary)
    strokeWidth = 7f
    strokeCap = StrokeCap.Round
}

@Composable
fun GoalChart(
    modifier: Modifier = Modifier,
    state: GoalState
) {
    Canvas(modifier = modifier) {
        goalChart(state, textPaint, barPaint)
    }
}

val days = "MTWTFSS".toList()

fun DrawScope.goalChart(state: GoalState, textPaint: ComposePaint, barPaint: ComposePaint) {
    val androidPaint = textPaint.asFrameworkPaint()
    val width = size.width
    val max = state.values.maxOrNull() ?: 10
    this.drawIntoCanvas {
        state.values.forEachIndexed { i, value ->
            val x = 50f + (width / 9) * i
            it.nativeCanvas.drawText(i.toString(), x, size.height * 0.9f, androidPaint)
            val height = 0.6f * (value.toFloat() / max) * size.height
            val bottom = size.height * 0.9f - 20f
            it.drawLine(
                Offset(x, bottom),
                Offset(x, bottom - height),
                barPaint
            )
        }
    }
}

data class GoalState(val values: List<Int>)

@WearLargeRoundDevicePreview
@Composable
fun GoalChartPreview() {
    val state = remember { GoalState(listOf(5, 0, 5, 4, 5, 2, 3)) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GoalChart(
            modifier = Modifier
                .size(160.dp, 100.dp)
                .border(1.dp, Color.White), state = state
        )
    }
}
