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
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.layout.RemoteTime
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.schimke.composeai.preview.AnimatedPreview

@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@Composable
fun HelloRoundness0Preview() {
  VariableFontTweenTextPreview("Hello", "ROND", 0f, Modifier.size(200.dp, 50.dp))
}

@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@Composable
fun HelloRoundness100Preview() {
  VariableFontTweenTextPreview("Hello", "ROND", 100f, Modifier.size(200.dp, 50.dp))
}

@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@AnimatedPreview(durationMs = 2000, frameIntervalMs = 100, showCurves = false)
@Composable
fun HelloRoundnessAnimatedPreview() {
  VariableFontTweenTextAnimatedPreview("Hello", "ROND", 0f, 100f, Modifier.size(200.dp, 50.dp))
}

@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@AnimatedPreview(durationMs = 2000, frameIntervalMs = 100, showCurves = false)
@Composable
fun HelloWeightAnimatedPreview() {
  VariableFontTweenTextAnimatedPreview("Hello", "wght", 100f, 1000f, Modifier.size(200.dp, 50.dp))
}

@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@AnimatedPreview(durationMs = 2000, frameIntervalMs = 100, showCurves = false)
@Composable
fun HelloWeightAndRoundnessExpressionAnimatedPreview() {
  VariableFontTextAnimatedPreview(
    "Hello",
    from = mapOf("wght" to 100f, "ROND" to 0f),
    to = mapOf("wght" to 1000f, "ROND" to 100f),
    modifier = Modifier.size(200.dp, 50.dp),
  )
}

@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@Composable
fun RobotoFlexSlantPreview() {
  VariableFontTweenTextPreview(
    "Hello",
    "slnt",
    -10f,
    Modifier.size(200.dp, 50.dp),
    fontResId = R.raw.roboto_flex,
  )
}

@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@AnimatedPreview(durationMs = 2000, frameIntervalMs = 100, showCurves = false)
@Composable
fun RobotoFlexSlantWidthExpressionAnimatedPreview() {
  VariableFontTextAnimatedPreview(
    "Hello",
    from = mapOf("slnt" to 0f, "wdth" to 25f),
    to = mapOf("slnt" to -10f, "wdth" to 151f),
    modifier = Modifier.size(200.dp, 50.dp),
    fontResId = R.raw.roboto_flex,
  )
}

/** Weight and slant on their own periods, animated by the document's clock alone. */
@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@AnimatedPreview(durationMs = 6000, frameIntervalMs = 200, showCurves = false)
@Composable
fun RobotoFlexWeightAndSlantSelfAnimatedPreview() {
  VariableFontSelfAnimatedPreview(
    "Hello",
    axes = { mapOf("wght" to sweep(100f, 1000f, 4f), "slnt" to sweep(0f, -10f, 6f)) },
    modifier = Modifier.size(200.dp, 50.dp),
    fontResId = R.raw.roboto_flex,
  )
}

/** [value], a whole number from 0 to 99, as two digits. */
@SuppressLint("RestrictedApi")
private fun twoDigits(value: RemoteFloat): RemoteString {
  val digit = DecimalFormat("0")
  val ones = value % 10f
  return ((value - ones) / 10f).toRemoteString(digit) + ones.toRemoteString(digit)
}

/**
 * A clock whose text is a [RemoteString] built in the document from its own time, drawn in Roboto
 * Flex with the weight sweeping: the digits change and the weight moves with no host input.
 */
@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@AnimatedPreview(durationMs = 4000, frameIntervalMs = 250, showCurves = false)
@SuppressLint("RestrictedApi")
@Composable
fun RemoteStringClockPreview() {
  val context = LocalContext.current
  val font = remember { context.variableFont(R.raw.roboto_flex) }
  SelfAnimatedDocumentPreview(Modifier.size(200.dp, 50.dp)) {
    val time = RemoteTime()
    val text = twoDigits(time.Minutes() % 60f) + ":" + twoDigits(time.Seconds() % 60f)
    RemoteVariableFontText(
      text = text,
      characters = "0123456789:",
      maxLength = 5,
      font = font,
      axes = mapOf("wght" to sweep(100f, 1000f, 4f)),
      fontSize = 32.dp.asRdp(),
      color = Color.White.rc,
    )
  }
}
