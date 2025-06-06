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

package com.google.android.horologist.mediasample.sc
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.android.horologist.mediasample.sc.model.Waveform

data class WaveformColours(
    val wave: Color = Color(0xFF868686),
    val wavePlayed: Color = Color(0xFFFF5100),
    val wavePaused: Color = Color(0xFF666666),
    val wavePlayedPaused: Color = Color(0xFFAA5533),
    val reflection: Color = Color(0xFF999999),
    val reflectionActive: Color = Color(0xFFFFC0A0),
)

// TODO optimise this, shouldn't need all the work on each draw
// how to react to size?
@Composable
fun WaveformChart(
    modifier: Modifier,
    waveform: Waveform,
    waveformColour: WaveformColours,
    completionPercent: Float,
    playing: Boolean,
) {
    val barWidth = 7
    val reflectionScale = 0.3f
    val waveScale = 1.0f - reflectionScale
    Canvas(modifier = modifier) {
        val width = this.size.width
        val height = this.size.height
        val chunks = width / barWidth
        val chunkSize = (waveform.width / chunks).toInt()
        val completedChunks = chunks * completionPercent.coerceIn(0f, 1f)
        val chunkList = waveform.samples.chunked(chunkSize).map { it.average() }
        val midpointY = height * waveScale
        chunkList.forEachIndexed { i, rawY ->
            val wavePlayingColor =
                if (i < completedChunks) waveformColour.wavePlayed else waveformColour.wave
            val wavePausedColor =
                if (i < completedChunks) waveformColour.wavePlayedPaused else waveformColour.wavePaused
            val waveColor = if (playing) wavePlayingColor else wavePausedColor
            val reflectionColor =
                if (i < completedChunks) waveformColour.reflectionActive else waveformColour.reflection
            val x = i.toFloat() * barWidth
            val lineX = x + barWidth / 2
            val unscaledY = height * (rawY.toFloat() / waveform.height)
            val waveY = unscaledY * waveScale
            val reflectionY = unscaledY * reflectionScale
            // draw wave
            this.drawLine(
                color = waveColor,
                strokeWidth = barWidth - 1f,
                start = Offset(lineX, midpointY),
                end = Offset(lineX, midpointY - waveY),
            )
            // draw reflection
            this.drawLine(
                color = reflectionColor,
                strokeWidth = barWidth - 1f,
                start = Offset(lineX, midpointY),
                end = Offset(lineX, midpointY + reflectionY),
            )
        }
    }
}
