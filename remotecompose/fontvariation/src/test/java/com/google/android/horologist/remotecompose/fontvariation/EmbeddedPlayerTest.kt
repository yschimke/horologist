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

@file:OptIn(ExperimentalRemotePlayerApi::class)

package com.google.android.horologist.remotecompose.fontvariation

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.fontvariation.generated.HamburgWght
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every way this module draws, played by the embedded Compose player (`RcPlayer`) and by the View
 * player, at several axis values: the two must draw the same, to within antialiasing. `RcPlayer`
 * takes no named floats from its host, so each value gets its own documents, with the axis an
 * expression rather than a literal so the drawing still follows it on the player.
 * `ClockDrivenAxesTest` covers values that change after load.
 */
@Config(sdk = [35], qualifiers = "w600dp-h900dp-xhdpi")
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EmbeddedPlayerTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private val values = mutableStateMapOf<String, Float>()

  @After
  fun disableEmbeddedPlayer() {
    RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = false
  }

  @Test fun keyOutlines() = compare(listOf("wght")) { a -> text(a) }

  @Test fun expressionsSeveralAxes() = compare(listOf("wght", "slnt")) { a -> text(a) }

  @Test
  fun expressionsOverlappingRegions() =
    compare(listOf("wght")) { a ->
      RemoteVariableFontText(TEXT, googleSansFlex.font, a, SIZE.dp.asRdp(), color = Color.White.rc)
    }

  @Test
  fun keyframeTween() =
    compare(listOf("wght")) { a ->
      RemoteVariableFontTweenText(
        TEXT,
        robotoFlex.font,
        "wght",
        a.getValue("wght"),
        SIZE.dp.asRdp(),
        color = Color.White.rc,
      )
    }

  @Test
  fun generatedOutline() =
    compare(listOf("wght")) { a ->
      HamburgWght(a.getValue("wght"), SIZE.dp.asRdp(), color = Color.White.rc)
    }

  /** The 1.0.0-alpha18 embedded player does not evaluate `BitmapTextMeasure`; alpha21 does. */
  @Ignore("draws nothing in remote-player-compose 1.0.0-alpha18; fixed in alpha21")
  @Test
  fun remoteString() =
    compare(listOf("wght")) { a ->
      RemoteVariableFontText(
        RemoteString("Ham") + RemoteString("burg"),
        characters = "Hambur g",
        maxLength = 8,
        font = robotoFlex.font,
        axes = a,
        fontSize = SIZE.dp.asRdp(),
        color = Color.White.rc,
      )
    }

  /** A path tween drawn by id and tweened again, which the key outlines would need for two axes. */
  @Ignore("draws nothing in remote-player-compose 1.0.0-alpha18; fixed in alpha21")
  @SuppressLint("RestrictedApi")
  @Test
  fun tweenOfATween() =
    compare(listOf("wght")) { a ->
      RemoteCanvas(RemoteModifier.size(80.rdp, 32.rdp)) {
        val w = remoteComposeCreationState.document
        val t = (a.getValue("wght") / 1000f).getFloatIdForCreationState(remoteComposeCreationState)
        fun box(width: Float) =
          RemotePath().apply {
            moveTo(10f, 10f)
            lineTo(10f + width, 10f)
            lineTo(10f + width, 50f)
            lineTo(10f, 50f)
            close()
          }
        remoteCanvas.internalCanvas.usePaint(RemotePaint { color = Color.White.rc })
        remoteCanvas.internalCanvas.flush()
        val inner = w.pathTween(w.addPathData(box(10f)), w.addPathData(box(50f)), t)
        w.drawPath(w.pathTween(inner, w.addPathData(box(130f)), t))
      }
    }

  @Composable
  private fun text(axes: Map<String, RemoteFloat>) =
    RemoteVariableFontText(TEXT, robotoFlex.font, axes, SIZE.dp.asRdp(), color = Color.White.rc)

  private fun compare(axes: List<String>, content: @Composable (Map<String, RemoteFloat>) -> Unit) {
    RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true
    val locations =
      listOf(
        mapOf("wght" to 400f, "slnt" to 0f),
        mapOf("wght" to 100f, "slnt" to -10f),
        mapOf("wght" to 1000f, "slnt" to -4f),
        mapOf("wght" to 650f, "slnt" to -7f),
      )
    locations.first().forEach { (k, v) -> values[k] = v }
    composeRule.setContent {
      Column(Modifier.background(Color.Black)) {
        Player("view", embedded = false, axes, content)
        Player("embedded", embedded = true, axes, content)
      }
    }
    val report = StringBuilder()
    for (location in locations) {
      location.forEach { (k, v) -> values[k] = v }
      val (view, embedded) = settled()
      val (max, count) = diff(view, embedded)
      report.appendLine(
        "  ${location.filterKeys { it in axes }}: view ink ${ink(view)}, embedded ink " +
          "${ink(embedded)}, $count px differ, max $max"
      )
      assertWithMessage("$report").that(ink(view)).isGreaterThan(0L)
      assertWithMessage("embedded player differs from the View player\n$report")
        .that(max)
        .isAtMost(MAX_DIFF)
    }
    println(report)
  }

  @SuppressLint("RestrictedApi")
  @Composable
  private fun Player(
    tag: String,
    embedded: Boolean,
    axes: List<String>,
    content: @Composable (Map<String, RemoteFloat>) -> Unit,
  ) {
    val current = axes.associateWith { values.getValue(it) }
    key(current) {
      val doc = rememberRemoteDocument { content(current.mapValues { (_, v) -> v.rf * 1f }) }
      Box(Modifier.size(WIDTH.dp, HEIGHT.dp).testTag(tag)) {
        doc.value?.let {
          if (embedded) RcPlayer(it, Modifier.size(WIDTH.dp, HEIGHT.dp))
          else RemoteDocumentPlayer(it, WIDTH, HEIGHT, Modifier.size(WIDTH.dp, HEIGHT.dp))
        }
      }
    }
  }

  /** Both players' frames once two captures in a row agree. */
  private fun settled(): Pair<Bitmap, Bitmap> {
    var last: Pair<Bitmap, Bitmap>? = null
    repeat(20) {
      composeRule.mainClock.advanceTimeByFrame()
      composeRule.waitForIdle()
      val now = capture("view") to capture("embedded")
      if (last != null && last!!.first.sameAs(now.first) && last!!.second.sameAs(now.second)) {
        return now
      }
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

  private fun ink(b: Bitmap): Long =
    (0 until b.height).sumOf { y ->
      (0 until b.width).sumOf { x -> (b.getPixel(x, y) and 0xff).toLong() }
    }

  private companion object {
    val robotoFlex = testFonts[1]
    const val TEXT = "Hamburg"
    const val SIZE = 22
    const val WIDTH = 160
    const val HEIGHT = 32
    const val MAX_DIFF = 72
  }
}
