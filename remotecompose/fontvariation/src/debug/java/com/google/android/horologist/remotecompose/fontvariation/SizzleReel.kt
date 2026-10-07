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
import android.icu.text.DecimalFormat
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteTime
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.cos
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.sin
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import kotlin.math.PI

/**
 * A sizzle reel for a round watch, after the Wear OS Material 3 Expressive guide's "Rich color" and
 * "Variable fonts" sections: Roboto Flex everywhere, three accent colors, and every axis moved by
 * the document's own clock. Nothing is set from the host, and no font is loaded or instanced on the
 * player: each scene is one document whose outlines are expressions of the time.
 */
internal object SizzlePalette {
  val surface = Color(0xFF000000)
  val primary = Color(0xFFA9C7FF)
  val primaryContainer = Color(0xFF214A86)
  val secondary = Color(0xFFFFB1D0)
  val secondaryContainer = Color(0xFF6B2449)
  val tertiary = Color(0xFFFFB4A8)
  val stop = Color(0xFFFF5545)
  val onStop = Color(0xFF410001)
  val outline = Color(0xFF8E9099)
}

/** A sine between [from] and [to] with [period] seconds, shifted by [phase] periods. */
@SuppressLint("RestrictedApi")
internal fun wave(from: Float, to: Float, period: Float, phase: Float = 0f): RemoteFloat {
  val t = RemoteTime().ContinuousSec()
  val s = sin((t / period + phase) * (2f * PI.toFloat()))
  return s * ((to - from) / 2f) + (from + to) / 2f
}

/** Scene 1: the font itself, weight and width breathing against each other. */
@SuppressLint("RestrictedApi")
@Composable
internal fun SizzleFlexScene(robotoFlex: VariableFont) {
  RemoteBox(
    RemoteModifier.fillMaxSize().background(SizzlePalette.surface.rc),
    contentAlignment = RemoteAlignment.Center,
  ) {
    Pulses(SizzlePalette.primaryContainer, SizzlePalette.secondaryContainer)
    RemoteColumn(
      horizontalAlignment = RemoteAlignment.CenterHorizontally,
      verticalArrangement = RemoteArrangement.Center,
    ) {
      RemoteVariableFontText(
        text = "Roboto",
        font = robotoFlex,
        axes = mapOf("wght" to wave(250f, 900f, 3f, 0.5f)),
        fontSize = 22.rdp,
        color = SizzlePalette.secondary.rc,
      )
      RemoteVariableFontText(
        text = "Flex",
        font = robotoFlex,
        axes = mapOf("wght" to wave(100f, 1000f, 3f), "wdth" to wave(151f, 25f, 3f)),
        fontSize = 68.rdp,
        color = SizzlePalette.primary.rc,
      )
    }
  }
}

/** Scene 2: one word, a weight wave rolling through it, letter by letter, in three accents. */
@SuppressLint("RestrictedApi")
@Composable
internal fun SizzleWaveScene(robotoFlex: VariableFont) {
  val accents = listOf(SizzlePalette.primary, SizzlePalette.secondary, SizzlePalette.tertiary)
  RemoteBox(
    RemoteModifier.fillMaxSize().background(SizzlePalette.surface.rc),
    contentAlignment = RemoteAlignment.Center,
  ) {
    RemoteColumn(
      horizontalAlignment = RemoteAlignment.CenterHorizontally,
      verticalArrangement = RemoteArrangement.spacedBy(2.rdp, RemoteAlignment.CenterVertically),
    ) {
      RemoteRow {
        "Expressive"
          .forEachIndexed { i, c ->
            RemoteVariableFontText(
              text = c.toString(),
              font = robotoFlex,
              axes = mapOf("wght" to wave(150f, 1000f, 1.6f, -i * 0.08f)),
              fontSize = 30.rdp,
              color = accents[i % 3].rc,
            )
          }
      }
      RemoteRow {
        "Wear OS"
          .forEachIndexed { i, c ->
            RemoteVariableFontText(
              text = c.toString(),
              font = robotoFlex,
              axes =
                mapOf(
                  "slnt" to wave(0f, -10f, 1.6f, -i * 0.1f),
                  "wdth" to wave(60f, 151f, 1.6f, -i * 0.1f),
                ),
              fontSize = 30.rdp,
              location = mapOf("wght" to 700f),
              color = SizzlePalette.outline.rc,
            )
          }
      }
    }
  }
}

/**
 * Scene 3: the guide's stopwatch. The time is a `RemoteString` the document builds from its own
 * clock; every second lands heavy and relaxes, and the stop button is the third accent, red.
 */
@SuppressLint("RestrictedApi")
@Composable
internal fun SizzleStopwatchScene(robotoFlex: VariableFont) {
  val t = RemoteTime().ContinuousSec()
  val tenths = (t * 10f) - (t * 10f) % 1f
  val whole = tenths / 10f - (tenths / 10f) % 1f
  val beat = t % 1f
  val text =
    twoDigits((whole - whole % 60f) / 60f) +
      ":" +
      twoDigits(whole % 60f) +
      "." +
      tenth(tenths % 10f)
  RemoteBox(
    RemoteModifier.fillMaxSize().background(SizzlePalette.surface.rc),
    contentAlignment = RemoteAlignment.Center,
  ) {
    Orbit()
    RemoteColumn(
      horizontalAlignment = RemoteAlignment.CenterHorizontally,
      verticalArrangement = RemoteArrangement.spacedBy(10.rdp, RemoteAlignment.CenterVertically),
    ) {
      RemoteVariableFontText(
        text = text,
        characters = "0123456789:.",
        maxLength = 7,
        font = robotoFlex,
        axes = mapOf("wght" to (beat * beat - beat * 2f + 1f) * 800f + 200f),
        fontSize = 40.rdp,
        color = SizzlePalette.primary.rc,
        location = mapOf("wdth" to 100f),
      )
      RemoteBox(
        RemoteModifier.clip(RemoteRoundedCornerShape(50))
          .background(SizzlePalette.stop.rc)
          .padding(18.rdp, 6.rdp),
        contentAlignment = RemoteAlignment.Center,
      ) {
        RemoteVariableFontText(
          text = "Stop",
          font = robotoFlex,
          axes = mapOf("wdth" to wave(80f, 140f, 1f), "wght" to wave(600f, 900f, 1f)),
          fontSize = 20.rdp,
          color = SizzlePalette.onStop.rc,
        )
      }
    }
  }
}

/** Scene 4: roundness, the one axis Roboto Flex lacks, from Google Sans Flex. */
@SuppressLint("RestrictedApi")
@Composable
internal fun SizzleRoundScene(googleSansFlex: VariableFont) {
  RemoteBox(
    RemoteModifier.fillMaxSize().background(SizzlePalette.surface.rc),
    contentAlignment = RemoteAlignment.Center,
  ) {
    Pulses(SizzlePalette.secondaryContainer, SizzlePalette.primaryContainer)
    RemoteColumn(horizontalAlignment = RemoteAlignment.CenterHorizontally) {
      RemoteVariableFontText(
        text = "Round",
        font = googleSansFlex,
        axes = mapOf("ROND" to wave(0f, 100f, 2f), "wght" to wave(300f, 900f, 4f)),
        fontSize = 46.rdp,
        color = SizzlePalette.secondary.rc,
      )
      RemoteVariableFontText(
        text = "and soft",
        font = googleSansFlex,
        axes = mapOf("ROND" to wave(100f, 0f, 2f)),
        fontSize = 24.rdp,
        location = mapOf("wght" to 500f),
        color = SizzlePalette.tertiary.rc,
      )
    }
  }
}

/** Scene 5: the point of it all, every line on its own axes. */
@SuppressLint("RestrictedApi")
@Composable
internal fun SizzleOutroScene(robotoFlex: VariableFont) {
  RemoteBox(
    RemoteModifier.fillMaxSize().background(SizzlePalette.surface.rc),
    contentAlignment = RemoteAlignment.Center,
  ) {
    RemoteColumn(horizontalAlignment = RemoteAlignment.CenterHorizontally) {
      RemoteVariableFontText(
        text = "Font",
        font = robotoFlex,
        axes = mapOf("wght" to wave(100f, 1000f, 2f)),
        fontSize = 40.rdp,
        color = SizzlePalette.primary.rc,
      )
      RemoteVariableFontText(
        text = "reload",
        font = robotoFlex,
        axes = mapOf("slnt" to wave(0f, -10f, 2f, 0.25f), "wdth" to wave(50f, 151f, 2f, 0.25f)),
        fontSize = 40.rdp,
        location = mapOf("wght" to 600f),
        color = SizzlePalette.secondary.rc,
      )
      RemoteVariableFontText(
        text = "free",
        font = robotoFlex,
        axes = mapOf("wght" to wave(1000f, 300f, 2f, 0.5f), "GRAD" to wave(-200f, 150f, 2f, 0.5f)),
        fontSize = 40.rdp,
        color = SizzlePalette.tertiary.rc,
      )
      RemoteVariableFontText(
        text = "Remote Compose",
        font = robotoFlex,
        axes = mapOf("wdth" to wave(80f, 120f, 4f)),
        fontSize = 14.rdp,
        location = mapOf("wght" to 500f),
        color = SizzlePalette.outline.rc,
      )
    }
  }
}

/** Two soft discs swelling out of phase behind a scene. */
@SuppressLint("RestrictedApi")
@Composable
private fun Pulses(first: Color, second: Color) {
  RemoteCanvas(RemoteModifier.fillMaxSize()) {
    val r = min(width, height) / 2f
    drawCircle(RemotePaint { color = second.rc }, r * wave(0.55f, 0.95f, 3f, 0.5f), center)
    drawCircle(RemotePaint { color = first.rc }, r * wave(0.35f, 0.8f, 3f), center)
  }
}

/** A track around the bezel with a dot running a lap every second. */
@SuppressLint("RestrictedApi")
@Composable
private fun Orbit() {
  RemoteCanvas(RemoteModifier.fillMaxSize()) {
    val r = min(width, height) / 2f - 16f
    val track = RemotePaint {
      color = SizzlePalette.primaryContainer.rc
      style = PaintingStyle.Stroke
      strokeWidth = 6f.rf
    }
    drawCircle(track, r, center)
    val a = (RemoteTime().ContinuousSec() % 1f) * (2f * PI.toFloat())
    drawCircle(
      RemotePaint { color = SizzlePalette.secondary.rc },
      9f.rf,
      RemoteOffset(center.x + sin(a) * r, center.y - cos(a) * r),
    )
  }
}

@SuppressLint("RestrictedApi")
private fun twoDigits(value: RemoteFloat): RemoteString {
  val digit = DecimalFormat("0")
  val ones = value % 10f
  return ((value - ones) / 10f).toRemoteString(digit) + ones.toRemoteString(digit)
}

@SuppressLint("RestrictedApi")
private fun tenth(value: RemoteFloat): RemoteString = value.toRemoteString(DecimalFormat("0"))
