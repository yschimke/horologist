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
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * One document per approach, captured once with its axis float at 0; afterwards only the player's
 * float changes. Both must follow it, within a keyframe segment as well as across segments, and
 * agree with each other at every value.
 */
@Config(sdk = [35], qualifiers = "w400dp-h400dp-xhdpi")
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiveUpdateTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private var value by mutableStateOf(0f)

  @Test
  fun weight() {
    // Google Sans Flex wght keyframes include 400 and 700: 450 to 650 stays in one segment.
    val ink = follow(googleSansFlex, "wght", listOf(400f, 450f, 650f, 1000f, 500f))
    assertThat(ink[1]).isGreaterThan(ink[0])
    assertThat(ink[2]).isGreaterThan(ink[1])
    assertThat(ink[3]).isGreaterThan(ink[2])
    assertThat(ink[4]).isLessThan(ink[3])
  }

  @Test
  fun grade() {
    // Roboto Flex GRAD keyframes are -200, 0 and 150; 0 to 150 is one segment.
    val ink = follow(testFonts[1], "GRAD", listOf(150f, -200f, 150f, 0f, 100f))
    assertThat(ink[0]).isGreaterThan(ink[3])
    assertThat(ink[1]).isLessThan(ink[3])
    assertThat(ink[2]).isEqualTo(ink[0])
    assertThat(ink[4]).isGreaterThan(ink[3])
  }

  private fun follow(testFont: TestFont, axis: String, values: List<Float>): List<Long> {
    val font = testFont.font
    composeRule.setContent {
      Column(Modifier.background(Color.Black)) {
        Player("tween", axis) { v ->
          RemoteVariableFontText(TEXT, font, axis, v, SIZE.dp.asRdp(), color = Color.White.rc)
        }
        Player("expression", axis) { v ->
          RemoteVariableFontExpressionText(
            TEXT,
            font,
            mapOf(axis to v),
            SIZE.dp.asRdp(),
            color = Color.White.rc,
          )
        }
      }
    }
    return values.map { v ->
      value = v
      composeRule.waitForIdle()
      val tween = ink("tween")
      val expression = ink("expression")
      assertWithMessage("$testFont $axis=$v").that(expression).isEqualTo(tween)
      tween
    }
  }

  @SuppressLint("RestrictedApi")
  @Composable
  private fun Player(
    tag: String,
    axis: String,
    content: @Composable (androidx.compose.remote.creation.compose.state.RemoteFloat) -> Unit,
  ) {
    val doc = rememberRemoteDocument { content(rememberNamedRemoteFloat("axis") { 0f.rf }) }
    Box(Modifier.size(WIDTH.dp, HEIGHT.dp).testTag(tag)) {
      doc.value?.let {
        RemoteDocumentPlayer(
          it,
          documentWidth = WIDTH,
          documentHeight = HEIGHT,
          modifier = Modifier.size(WIDTH.dp, HEIGHT.dp),
          update = { player -> player.setUserLocalFloat("axis", value) },
        )
      }
    }
  }

  /** The summed luminance of the node tagged [tag], drawn afresh in software. */
  private fun ink(tag: String): Long {
    val root = composeRule.activity.window.decorView
    val whole = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
    composeRule.runOnUiThread { root.draw(Canvas(whole)) }
    val b = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
    var sum = 0L
    for (y in b.top.roundToInt() until b.bottom.roundToInt()) {
      for (x in b.left.roundToInt() until b.right.roundToInt()) sum += whole.getPixel(x, y) and 0xff
    }
    return sum
  }

  private companion object {
    const val TEXT = "Hamburg"
    const val SIZE = 22
    const val WIDTH = 140
    const val HEIGHT = 32
  }
}
