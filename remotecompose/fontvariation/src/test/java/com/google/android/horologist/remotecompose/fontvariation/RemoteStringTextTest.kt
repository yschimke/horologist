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
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * [RemoteVariableFontText] over a [RemoteString] that changes on the player, against the `String`
 * overload drawing the same text as a constant: one document follows every text, kerning included.
 */
@Config(sdk = [35], qualifiers = "w400dp-h400dp-xhdpi")
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RemoteStringTextTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private var text by mutableStateOf("")

  @Test
  fun clockDigits() {
    follow(googleSansFlex.font, DIGITS, 5, listOf("12:45", "09:30", "7", "", "23:59"))
  }

  @Test
  fun kernedLetters() {
    follow(testFonts[1].font, LETTERS, 8, listOf("AVATAR", "Tokyo", "WAVY", "LT"))
  }

  private fun follow(font: VariableFont, characters: String, maxLength: Int, texts: List<String>) {
    composeRule.setContent {
      Column(Modifier.background(Color.Black)) {
        Player("live") {
          RemoteVariableFontText(
            RemoteString.createNamedRemoteString("text", ""),
            characters,
            maxLength,
            font,
            mapOf("wght" to 700f.rf * 1f),
            SIZE.dp.asRdp(),
            color = Color.White.rc,
          )
        }
        key(text) {
          Player("reference") {
            RemoteVariableFontText(
              text,
              font,
              mapOf("wght" to 700f.rf * 1f),
              SIZE.dp.asRdp(),
              color = Color.White.rc,
            )
          }
        }
      }
    }
    val report = StringBuilder()
    val failures = mutableListOf<String>()
    for (t in texts) {
      text = t
      composeRule.waitForIdle()
      val live = capture("live")
      val reference = capture("reference")
      val d = difference(live, reference)
      report.appendLine("'$t' difference=$d")
      if (d > TOLERANCE) failures += t
      System.getenv("DUMP")?.let { dir ->
        for ((n, b) in listOf("live" to live, "ref" to reference)) {
          java.io.File(dir, "${t.ifEmpty { "_" }}_$n.png").outputStream().use {
            b.compress(Bitmap.CompressFormat.PNG, 100, it)
          }
        }
      }
    }
    println(report)
    assertWithMessage(report.toString()).that(failures).isEmpty()
  }

  @SuppressLint("RestrictedApi")
  @Composable
  private fun Player(tag: String, content: @Composable () -> Unit) {
    val doc = rememberRemoteDocument { content() }
    Box(Modifier.size(WIDTH.dp, HEIGHT.dp).testTag(tag)) {
      doc.value?.let {
        RemoteDocumentPlayer(
          it,
          WIDTH,
          HEIGHT,
          Modifier.size(WIDTH.dp, HEIGHT.dp),
          update = { p -> p.setUserLocalString("text", text) },
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

  private fun ink(b: Bitmap): Long {
    var s = 0L
    for (y in 0 until b.height) for (x in 0 until b.width) s += b.getPixel(x, y) and 0xff
    return s
  }

  private fun difference(a: Bitmap, b: Bitmap): Float {
    var diff = 0.0
    var ink = 0.0
    for (y in 0 until minOf(a.height, b.height)) for (x in 0 until minOf(a.width, b.width)) {
      val la = (a.getPixel(x, y) and 0xff) / 255.0
      val lb = (b.getPixel(x, y) and 0xff) / 255.0
      diff += abs(la - lb)
      ink += maxOf(la, lb)
    }
    return if (ink == 0.0) 0f else (diff / ink).toFloat()
  }

  private companion object {
    const val DIGITS = "0123456789:"
    const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    const val SIZE = 24
    const val WIDTH = 200
    const val HEIGHT = 36
    const val TOLERANCE = 0.01f
  }
}
