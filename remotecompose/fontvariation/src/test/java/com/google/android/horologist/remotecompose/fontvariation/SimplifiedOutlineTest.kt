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
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
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
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * An outline simplified for the pixel size it is drawn at, against the exact one: what each
 * tolerance saves and how many pixels it changes, at each axis' extremes, default and random
 * values.
 */
@Config(sdk = [35], qualifiers = "w600dp-h900dp-xhdpi")
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SimplifiedOutlineTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private val values = mutableStateMapOf<String, Float>()
  private val documents = mutableStateMapOf<String, CoreDocument>()

  @Test fun robotoFlexWeight22() = compare(testFonts[1], listOf("wght"), 22)

  @Test fun robotoFlexWeightSlant22() = compare(testFonts[1], listOf("wght", "slnt"), 22)

  @Test fun robotoFlexWeightSlant12() = compare(testFonts[1], listOf("wght", "slnt"), 12)

  @Test fun googleSansFlex22() = compare(googleSansFlex, listOf("wght", "ROND"), 22)

  private fun compare(testFont: TestFont, axes: List<String>, sizeDp: Int) {
    val font = testFont.font
    val pixelSize = sizeDp * DENSITY
    val outlines =
      linkedMapOf("exact" to font.outline(TEXT, axes)) +
        TOLERANCES.associate { t ->
          "1/${(1 / t).toInt()} px" to
            font.outline(TEXT, axes, pixelSize = pixelSize, tolerancePixels = t)
        }
    val info = axes.map { tag -> font.axes.first { it.tag == tag } }
    val random = Random(axes.hashCode())
    val locations =
      listOf(
        info.associate { it.tag to it.defaultValue },
        info.associate { it.tag to it.minValue },
        info.associate { it.tag to it.maxValue },
      ) +
        List(3) {
          info.associate {
            it.tag to it.minValue + (it.maxValue - it.minValue) * random.nextFloat()
          }
        }
    locations.first().forEach { (k, v) -> values[k] = v }
    composeRule.setContent {
      Column(Modifier.background(Color.Black)) {
        outlines.forEach { (tag, outline) ->
          Player(tag, axes) { a ->
            RemoteVariableFontText(outline, a, sizeDp.dp.asRdp(), color = Color.White.rc)
          }
        }
      }
    }
    composeRule.waitUntil(20_000) { documents.size == outlines.size }
    val worst = outlines.keys.associateWith { 0 to 0 }.toMutableMap()
    for (location in locations) {
      location.forEach { (k, v) -> values[k] = v }
      val frames = settled(outlines.keys)
      outlines.keys.drop(1).forEach { tag ->
        val (max, count) = diff(frames.getValue(tag), frames.getValue("exact"))
        val (m, c) = worst.getValue(tag)
        worst[tag] = maxOf(m, max) to maxOf(c, count)
      }
    }
    val report = StringBuilder("$testFont $axes at $sizeDp dp ($pixelSize px)\n")
    outlines.forEach { (tag, outline) ->
      val (max, count) = worst.getValue(tag)
      report.appendLine(
        "  %-9s %s  %4d verbs  worst: %3d px differ, max %3d"
          .format(tag, DocumentStats.of(documents.getValue(tag)), outline.verbs.size, count, max)
      )
    }
    println(report)
    val (max, _) = worst.getValue("1/${(1 / DEFAULT_TOLERANCE_PIXELS).toInt()} px")
    assertWithMessage("$report").that(max).isAtMost(MAX_DIFF)
  }

  @SuppressLint("RestrictedApi")
  @Composable
  private fun Player(
    tag: String,
    axes: List<String>,
    content: @Composable (Map<String, RemoteFloat>) -> Unit,
  ) {
    val initial = values.toMap()
    val doc = rememberRemoteDocument {
      content(
        axes.associateWith { a -> rememberNamedRemoteFloat("axis.$a") { initial.getValue(a).rf } }
      )
    }
    doc.value?.let { d -> LaunchedEffect(d) { documents[tag] = d } }
    Box(Modifier.size(WIDTH.dp, HEIGHT.dp).testTag(tag)) {
      doc.value?.let {
        RemoteDocumentPlayer(
          it,
          documentWidth = WIDTH,
          documentHeight = HEIGHT,
          modifier = Modifier.size(WIDTH.dp, HEIGHT.dp),
          update = { player ->
            axes.forEach { a -> player.setUserLocalFloat("axis.$a", values.getValue(a)) }
          },
        )
      }
    }
  }

  /** Every player's frame once two captures in a row agree. */
  private fun settled(tags: Collection<String>): Map<String, Bitmap> {
    var last: Map<String, Bitmap>? = null
    repeat(10) {
      composeRule.waitForIdle()
      val now = tags.associateWith(::capture)
      if (last != null && tags.all { last!!.getValue(it).sameAs(now.getValue(it)) }) return now
      last = now
    }
    return last!!
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

  private fun diff(a: Bitmap, b: Bitmap): Pair<Int, Int> {
    var max = 0
    var count = 0
    for (y in 0 until a.height) {
      for (x in 0 until a.width) {
        val d = abs((a.getPixel(x, y) and 0xff) - (b.getPixel(x, y) and 0xff))
        if (d > 0) count++
        max = maxOf(max, d)
      }
    }
    return max to count
  }

  private companion object {
    const val TEXT = "Hamburgefonstiv 0123"
    const val DENSITY = 2f
    const val WIDTH = 300
    const val HEIGHT = 32
    val TOLERANCES = listOf(1f / 16, 1f / 8, 1f / 4, 1f / 2)

    /**
     * Any change to an edge can flip a pixel by about a quarter, as antialiasing quantizes
     * coverage; a sixteenth of a pixel never does more.
     */
    const val MAX_DIFF = 72
  }
}
