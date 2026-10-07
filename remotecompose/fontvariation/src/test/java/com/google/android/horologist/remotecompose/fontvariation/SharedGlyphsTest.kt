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
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Two [RemoteString] texts sharing one [VariableFontGlyphs] draw exactly what two texts with their
 * own glyphs draw, follow their strings independently, and cost the glyphs only once.
 */
@Config(sdk = [35], qualifiers = "w400dp-h400dp-xhdpi")
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SharedGlyphsTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private var first by mutableStateOf("")
  private var second by mutableStateOf("")
  private val sizes = HashMap<String, Int>()

  @Test
  fun sharedGlyphsDrawTheSameAndCostOnce() {
    val font = testFonts[1].font
    composeRule.setContent {
      Column(Modifier.background(Color.Black)) {
        Player("shared") {
          val weight = 700f.rf * 1f
          val glyphs = rememberVariableFontGlyphs(font, CHARACTERS, mapOf("wght" to weight))
          RemoteColumn {
            RemoteVariableFontText(named("first"), 5, glyphs, SIZE.dp.asRdp(), color = WHITE)
            RemoteVariableFontText(named("second"), 5, glyphs, SIZE.dp.asRdp(), color = WHITE)
          }
        }
        Player("separate") {
          val weight = 700f.rf * 1f
          RemoteColumn {
            separate(named("first"), font, weight)
            separate(named("second"), font, weight)
          }
        }
      }
    }
    val report = StringBuilder()
    for ((a, b) in listOf("12:45" to "09:30", "7" to "23:59", "" to "10:01", "11:11" to "")) {
      first = a
      second = b
      composeRule.waitForIdle()
      val d = difference(capture("shared"), capture("separate"))
      report.appendLine("'$a' / '$b': difference=$d")
      assertWithMessage(report.toString()).that(d).isLessThan(0.001f)
    }
    report.appendLine("sizes: $sizes")
    println(report)
    // The second text costs only its positions: well under twice the first.
    assertThat(sizes.getValue("shared")).isLessThan(sizes.getValue("separate") - 4000)
  }

  @Composable
  private fun separate(text: RemoteString, font: VariableFont, weight: RemoteFloat) {
    RemoteVariableFontText(
      text,
      CHARACTERS,
      5,
      font,
      mapOf("wght" to weight),
      SIZE.dp.asRdp(),
      color = WHITE,
    )
  }

  private fun named(name: String) = RemoteString.createNamedRemoteString(name, "")

  @SuppressLint("RestrictedApi")
  @Composable
  private fun Player(tag: String, content: @Composable () -> Unit) {
    val doc = rememberRemoteDocument { content() }
    Box(Modifier.size(WIDTH.dp, HEIGHT.dp).testTag(tag)) {
      doc.value?.let { document: CoreDocument ->
        LaunchedEffect(document) { sizes[tag] = document.buffer.buffer.size }
        RemoteDocumentPlayer(
          document,
          WIDTH,
          HEIGHT,
          Modifier.size(WIDTH.dp, HEIGHT.dp),
          update = { p ->
            p.setUserLocalString("first", first)
            p.setUserLocalString("second", second)
          },
        )
      }
    }
  }

  private fun capture(tag: String): Bitmap {
    val root = composeRule.activity.window.decorView
    val whole = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
    composeRule.runOnUiThread { root.draw(Canvas(whole)) }
    val b = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
    return Bitmap.createBitmap(
      whole,
      b.left.roundToInt(),
      b.top.roundToInt(),
      b.width.roundToInt(),
      b.height.roundToInt(),
    )
  }

  private fun difference(a: Bitmap, b: Bitmap): Float {
    var diff = 0.0
    var ink = 0.0
    for (y in 0 until minOf(a.height, b.height)) {
      for (x in 0 until minOf(a.width, b.width)) {
        val la = (a.getPixel(x, y) and 0xff) / 255.0
        val lb = (b.getPixel(x, y) and 0xff) / 255.0
        diff += abs(la - lb)
        ink += maxOf(la, lb)
      }
    }
    return if (ink == 0.0) 0f else (diff / ink).toFloat()
  }

  private companion object {
    const val CHARACTERS = "0123456789:"
    const val SIZE = 24
    const val WIDTH = 200
    const val HEIGHT = 80
    val WHITE = Color.White.rc
  }
}
