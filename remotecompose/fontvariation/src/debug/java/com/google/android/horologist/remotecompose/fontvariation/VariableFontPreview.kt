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
import android.content.Context
import androidx.annotation.RawRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteTime
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.abs
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Reads a variable font from a raw resource. */
fun Context.variableFont(@RawRes resId: Int): VariableFont =
  resources.openRawResource(resId).use { VariableFont.parse(it.readBytes()) }

/**
 * Captures a [RemoteVariableFontText] document whose axis is the named float `"axis"`, and plays it
 * with that float set to [value]. One document serves every value: only the player's float changes,
 * never the document or a font.
 */
@SuppressLint("RestrictedApi")
@Composable
fun VariableFontTextPreview(
  text: String,
  axis: String,
  value: Float,
  modifier: Modifier = Modifier,
  @RawRes fontResId: Int = R.raw.google_sans_flex_wght_rond,
  fontSize: Dp = 32.dp,
  color: Color = Color.White,
  location: Map<String, Float> = emptyMap(),
  documentWidth: Int = 400,
  documentHeight: Int = 100,
  clock: RemoteClock = RemoteClock.SYSTEM,
) {
  val context = LocalContext.current
  val font = remember(fontResId) { context.variableFont(fontResId) }
  // The document starts at the first value too, so a frame drawn before the player's float is set
  // already shows it. Later values only reach the player.
  val initial = remember { value }
  val doc =
    rememberRemoteDocument(clock = clock) {
      val axisValue = rememberNamedRemoteFloat("axis") { initial.rf }
      RemoteVariableFontText(
        text = text,
        font = font,
        axis = axis,
        value = axisValue,
        fontSize = fontSize.asRdp(),
        color = color.rc,
        location = location,
      )
    }
  doc.value?.let { document ->
    RemoteDocumentPlayer(
      document = document,
      modifier = modifier,
      documentWidth = documentWidth,
      documentHeight = documentHeight,
      update = { player -> player.setUserLocalFloat("axis", value) },
    )
  }
}

/**
 * Captures a [RemoteVariableFontExpressionText] document with one named float per axis in [values],
 * named `axis.<tag>`, and plays it with those floats set to [values]. As with
 * [VariableFontTextPreview], one document serves every value.
 */
@SuppressLint("RestrictedApi")
@Composable
fun VariableFontExpressionTextPreview(
  text: String,
  values: Map<String, Float>,
  modifier: Modifier = Modifier,
  @RawRes fontResId: Int = R.raw.google_sans_flex_wght_rond,
  fontSize: Dp = 32.dp,
  color: Color = Color.White,
  location: Map<String, Float> = emptyMap(),
  documentWidth: Int = 400,
  documentHeight: Int = 100,
  clock: RemoteClock = RemoteClock.SYSTEM,
) {
  val context = LocalContext.current
  val font = remember(fontResId) { context.variableFont(fontResId) }
  val initial = remember { values }
  val doc =
    rememberRemoteDocument(clock = clock) {
      val axes = initial.mapValues { (tag, v) -> rememberNamedRemoteFloat("axis.$tag") { v.rf } }
      RemoteVariableFontExpressionText(
        text = text,
        font = font,
        axes = axes,
        fontSize = fontSize.asRdp(),
        color = color.rc,
        location = location,
      )
    }
  doc.value?.let { document ->
    RemoteDocumentPlayer(
      document = document,
      modifier = modifier,
      documentWidth = documentWidth,
      documentHeight = documentHeight,
      update = { player -> values.forEach { (tag, v) -> player.setUserLocalFloat("axis.$tag", v) } },
    )
  }
}

/** [VariableFontTextPreview] with the axis swept from [from] to [to] and back, forever. */
@Composable
fun VariableFontTextAnimatedPreview(
  text: String,
  axis: String,
  from: Float,
  to: Float,
  modifier: Modifier = Modifier,
  durationMillis: Int = 2000,
  fontSize: Dp = 32.dp,
  location: Map<String, Float> = emptyMap(),
) {
  val transition = rememberInfiniteTransition(label = "FontAxis")
  val value by
    transition.animateFloat(
      initialValue = from,
      targetValue = to,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = durationMillis, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "Axis",
    )
  VariableFontTextPreview(
    text = text,
    axis = axis,
    value = value,
    modifier = modifier,
    fontSize = fontSize,
    location = location,
  )
}

/**
 * [VariableFontExpressionTextPreview] with every axis swept together from [from] to [to] and back,
 * forever: one document, several axes moving at once.
 */
@Composable
fun VariableFontExpressionTextAnimatedPreview(
  text: String,
  from: Map<String, Float>,
  to: Map<String, Float>,
  modifier: Modifier = Modifier,
  @RawRes fontResId: Int = R.raw.google_sans_flex_wght_rond,
  durationMillis: Int = 2000,
  fontSize: Dp = 32.dp,
) {
  val transition = rememberInfiniteTransition(label = "FontAxes")
  val t by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = durationMillis, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "Axes",
    )
  VariableFontExpressionTextPreview(
    text = text,
    values = from.mapValues { (tag, a) -> a + (to.getValue(tag) - a) * t },
    modifier = modifier,
    fontResId = fontResId,
    fontSize = fontSize,
  )
}

/**
 * A value that sweeps from [from] to [to] and back every [periodSeconds], driven by the document's
 * own clock: the player animates it with no host input and no named float.
 */
fun sweep(from: Float, to: Float, periodSeconds: Float): RemoteFloat {
  val phase = (RemoteTime().ContinuousSec() % periodSeconds) / periodSeconds
  val triangle = -abs(phase * 2f - 1f) + 1f
  return triangle * (to - from) + from
}

/** The value of [sweep] at [seconds]. */
fun sweepAt(from: Float, to: Float, periodSeconds: Float, seconds: Float): Float {
  val phase = (seconds % periodSeconds) / periodSeconds
  return from + (to - from) * (1f - kotlin.math.abs(phase * 2f - 1f))
}

/**
 * A [RemoteVariableFontExpressionText] document whose [axes] are built inside the document, for
 * example with [sweep], so it animates on the player's own clock: no named floats, no host input.
 */
@SuppressLint("RestrictedApi")
@Composable
fun VariableFontSelfAnimatedPreview(
  text: String,
  axes: () -> Map<String, RemoteFloat>,
  modifier: Modifier = Modifier,
  @RawRes fontResId: Int = R.raw.google_sans_flex_wght_rond,
  fontSize: Dp = 32.dp,
  color: Color = Color.White,
  documentWidth: Int = 400,
  documentHeight: Int = 100,
  clock: RemoteClock = RemoteClock.SYSTEM,
) {
  val context = LocalContext.current
  val font = remember(fontResId) { context.variableFont(fontResId) }
  val doc =
    rememberRemoteDocument(clock = clock) {
      RemoteVariableFontExpressionText(
        text = text,
        font = font,
        axes = axes(),
        fontSize = fontSize.asRdp(),
        color = color.rc,
      )
    }
  doc.value?.let { document ->
    RemoteDocumentPlayer(
      document = document,
      modifier = modifier,
      documentWidth = documentWidth,
      documentHeight = documentHeight,
    )
  }
}
