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
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The tween grid draws exactly what an expression per coordinate draws, in the View player, at each
 * axis' extremes, default and random values, for every test font, to within antialiasing. Where it
 * does not apply (several axes, overlapping regions, or keys that would outweigh the expressions)
 * both documents are the expression one. Set `TWEEN_GRID_EXPORT` to a directory to also write each
 * document there.
 */
@Config(sdk = [35], qualifiers = "w600dp-h900dp-xhdpi")
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TweenGridTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private val values = mutableStateMapOf<String, Float>()
  private val documents = mutableStateMapOf<String, CoreDocument>()

  @Test fun robotoFlexWeight() = compare(robotoFlex, listOf("wght"), grid = true)

  @Test fun robotoFlexSlant() = compare(robotoFlex, listOf("slnt"), grid = true)

  @Test fun robotoFlexGrade() = compare(robotoFlex, listOf("GRAD"))

  /** Two axes would need tweens of tweens, which the embedded player does not draw. */
  @Test fun robotoFlexWeightSlant() = compare(robotoFlex, listOf("wght", "slnt"), grid = false)

  /** Google Sans Flex's weight regions overlap (it has intermediate masters). */
  @Test fun googleSansFlexWeight() = compare(googleSansFlex, listOf("wght"), grid = false)

  @Test fun googleSansFlexRoundness() = compare(googleSansFlex, listOf("ROND"))

  @Test fun recursiveCasual() = compare(testFonts[2], listOf("CASL"))

  @Test fun recursiveSeveral() = compare(testFonts[2], listOf("wght", "slnt", "CASL"))

  @Test fun frauncesSoft() = compare(testFonts[3], listOf("SOFT"))

  @Test fun notoSansWeight() = compare(testFonts[4], listOf("wght"))

  @Test fun interWeight() = compare(testFonts[5], listOf("wght"))

  /** [grid] when the case is known to use the grid, so a regression to expressions shows. */
  private fun compare(testFont: TestFont, axes: List<String>, grid: Boolean? = null) {
    val font = testFont.font
    val info = axes.map { tag -> font.axes.first { it.tag == tag } }
    val random = Random(axes.hashCode())
    val locations =
      listOf(
        info.associate { it.tag to it.defaultValue },
        info.associate { it.tag to it.minValue },
        info.associate { it.tag to it.maxValue },
      ) +
        List(4) {
          info.associate {
            it.tag to it.minValue + (it.maxValue - it.minValue) * random.nextFloat()
          }
        }
    locations.first().forEach { (k, v) -> values[k] = v }
    val variants = mapOf("grid" to true, "expressions" to false)
    composeRule.setContent {
      Column(Modifier.background(Color.Black)) {
        variants.forEach { (tag, allow) ->
          Player(tag, axes) { a ->
            VariableFontText(
              TEXT,
              font,
              a,
              SIZE.dp.asRdp(),
              color = Color.White.rc,
              allowTweenGrid = allow,
            )
          }
        }
      }
    }
    composeRule.waitUntil(20_000) { documents.size == variants.size }

    val stats = variants.keys.associateWith { DocumentStats.of(documents.getValue(it)) }
    val report = StringBuilder("$testFont $axes\n")
    stats.forEach { (tag, s) -> report.appendLine("  %-12s %s".format(tag, s)) }
    val usesGrid = stats.getValue("grid").expressions < stats.getValue("expressions").expressions
    report.appendLine("  grid ${if (usesGrid) "used" else "not applicable"}")
    grid?.let { assertWithMessage("$report").that(usesGrid).isEqualTo(it) }
    System.getenv("TWEEN_GRID_EXPORT")?.let { dir ->
      variants.keys.forEach { tag ->
        val buffer = documents.getValue(tag).buffer.buffer
        File(dir, "${testFont.resource}_${axes.joinToString("_")}_$tag.rc")
          .apply { parentFile!!.mkdirs() }
          .writeBytes(buffer.buffer.copyOf(buffer.size))
      }
    }

    for (location in locations) {
      location.forEach { (k, v) -> values[k] = v }
      val (expressions, grid) = settled()
      val (maxDiff, differing) = diff(grid, expressions)
      report.appendLine("  $location: $differing px differ, max $maxDiff")
      assertWithMessage("$report at $location").that(maxDiff).isAtMost(MAX_DIFF)
      assertThat(ink(expressions)).isGreaterThan(0)
    }
    println(report)
  }

  @SuppressLint("RestrictedApi")
  @Composable
  private fun Player(
    tag: String,
    axes: List<String>,
    content:
      @Composable
      (Map<String, androidx.compose.remote.creation.compose.state.RemoteFloat>) -> Unit,
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

  /** Both players' frames once two captures in a row agree, so neither is a frame behind. */
  private fun settled(): Pair<Bitmap, Bitmap> {
    var last: Pair<Bitmap, Bitmap>? = null
    repeat(10) {
      composeRule.waitForIdle()
      val now = capture("expressions") to capture("grid")
      if (last != null && last!!.first.sameAs(now.first) && last!!.second.sameAs(now.second)) {
        return now
      }
      last = now
    }
    return last!!
  }

  /** The largest channel difference and the number of pixels that differ at all. */
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

  private fun ink(b: Bitmap): Long =
    (0 until b.height).sumOf { y ->
      (0 until b.width).sumOf { x -> (b.getPixel(x, y) and 0xff).toLong() }
    }

  private companion object {
    val robotoFlex = testFonts[1]
    const val TEXT = "Hamburgefonstiv 0123"
    const val SIZE = 22
    const val WIDTH = 300
    const val HEIGHT = 32
    /** Antialiasing: a tween's lerp and an expression's sum round differently at edges. */
    const val MAX_DIFF = 16
  }
}
