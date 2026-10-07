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
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.compose.embedded.RcPlayer
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
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Axes animated by the document's own clock, with no named floats and no host input, played by the
 * View player (`RemoteDocumentPlayer`) and the embedded Compose player (`RcPlayer`).
 *
 * Each live frame is compared with a reference: a fresh document with that instant's axis values as
 * constants, played by the View player. The two players keep time differently:
 * - the View player reads the document's [RemoteClock], so it must match the reference at the
 *   clock's time exactly;
 * - the embedded player counts Compose frame time from its own first frame and paces its frames, so
 *   it trails the frame clock by a few frames. It must match the reference at one of the instants
 *   up to [LAG_FRAMES] frames earlier, and keep moving.
 */
@Config(sdk = [35], qualifiers = "w400dp-h900dp-xhdpi")
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ClockDrivenAxesTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private val clock = SettableClock()
  private var seconds by mutableStateOf(0f)

  @After
  fun disableEmbeddedPlayer() {
    RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = false
  }

  /** Approach 2: weight and slant together, each on its own period. */
  @Test
  fun expressionPathWeightAndSlant() {
    val font = testFonts[1].font
    follow(
      live = {
        RemoteVariableFontText(
          TEXT,
          font,
          mapOf("wght" to sweep(100f, 1000f, 4f), "slnt" to sweep(0f, -10f, 6f)),
          SIZE.dp.asRdp(),
          color = Color.White.rc,
        )
      },
      reference = { s ->
        RemoteVariableFontText(
          TEXT,
          font,
          mapOf(
            "wght" to constant(sweepAt(100f, 1000f, 4f, s)),
            "slnt" to constant(sweepAt(0f, -10f, 6f, s)),
          ),
          SIZE.dp.asRdp(),
          color = Color.White.rc,
        )
      },
    )
  }

  /** One axis whose regions meet at its default: drawn as a tween between key outlines. */
  @Test
  fun tweenGridWeight() {
    val font = testFonts[1].font
    follow(
      live = {
        RemoteVariableFontText(
          TEXT,
          font,
          mapOf("wght" to sweep(100f, 1000f, 4f)),
          SIZE.dp.asRdp(),
          color = Color.White.rc,
        )
      },
      reference = { s ->
        RemoteVariableFontText(
          TEXT,
          font,
          mapOf("wght" to constant(sweepAt(100f, 1000f, 4f, s))),
          SIZE.dp.asRdp(),
          color = Color.White.rc,
        )
      },
    )
  }

  /** Approach 1: the path tween on one axis. */
  @Test
  fun pathTweenWeight() {
    val font = testFonts[1].font
    follow(
      live = {
        RemoteVariableFontTweenText(
          TEXT,
          font,
          "wght",
          sweep(100f, 1000f, 4f),
          SIZE.dp.asRdp(),
          color = Color.White.rc,
        )
      },
      reference = { s ->
        RemoteVariableFontTweenText(
          TEXT,
          font,
          "wght",
          constant(sweepAt(100f, 1000f, 4f, s)),
          SIZE.dp.asRdp(),
          color = Color.White.rc,
        )
      },
    )
  }

  private fun follow(live: @Composable () -> Unit, reference: @Composable (Float) -> Unit) {
    RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      Column(Modifier.background(Color.Black)) {
        Player("view", embedded = false) { live() }
        Player("embedded", embedded = true) { live() }
        for (lag in 0..LAG_FRAMES) {
          key(seconds, lag) {
            Player("reference$lag", embedded = false) {
              reference(seconds - lag * FRAME_MS / 1000f)
            }
          }
        }
      }
    }
    val report = StringBuilder()
    var previous: Bitmap? = null
    for (frames in listOf(32L, 96L, 128L, 208L, 320L, 352L)) {
      val target = frames * FRAME_MS
      composeRule.mainClock.advanceTimeBy(target - 3 * FRAME_MS - composeRule.mainClock.currentTime)
      clock.seconds = target / 1000f
      seconds = target / 1000f
      repeat(3) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
      }
      val view = difference(capture("view"), capture("reference0"))
      val embedded = capture("embedded")
      val lags = (0..LAG_FRAMES).map { difference(embedded, capture("reference$it")) }
      report.appendLine(
        "t=${target / 1000f}s view=$view embedded=" +
          lags.withIndex().joinToString { (lag, d) -> "-$lag:%.3f".format(d) }
      )
      assertWithMessage("view player at ${target}ms\n$report").that(view).isLessThan(TOLERANCE)
      assertWithMessage("embedded player near ${target}ms\n$report")
        .that(lags.min())
        .isLessThan(TOLERANCE)
      previous?.let {
        assertWithMessage("embedded player moved\n$report")
          .that(difference(it, embedded))
          .isGreaterThan(MOVED)
      }
      previous = embedded
    }
    println(report)
    assertThat(previous).isNotNull()
  }

  /** A constant the player treats like any other float expression. */
  private fun constant(value: Float): RemoteFloat = value.rf * 1f

  @SuppressLint("RestrictedApi")
  @Composable
  private fun Player(tag: String, embedded: Boolean, content: @Composable () -> Unit) {
    val doc = rememberRemoteDocument(clock = clock) { content() }
    Box(Modifier.size(WIDTH.dp, HEIGHT.dp).testTag(tag)) {
      doc.value?.let {
        if (embedded) RcPlayer(it, Modifier.size(WIDTH.dp, HEIGHT.dp))
        else RemoteDocumentPlayer(it, WIDTH, HEIGHT, Modifier.size(WIDTH.dp, HEIGHT.dp))
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

  /** Summed luminance difference over summed ink: 0 when identical, 1 when no ink is shared. */
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

  /** A clock held at [seconds] past 10:10:00 UTC. */
  private class SettableClock : RemoteClock {
    var seconds = 0f

    private fun instant() =
      Instant.parse("2026-01-01T10:10:00Z").plusNanos((seconds * 1e9).toLong())

    override fun millis(): Long = instant().toEpochMilli()

    override fun nanoTime(): Long = (seconds * 1e9).toLong()

    override fun getZoneId(): String = "UTC"

    override fun snapshot(millis: Long?): RemoteClock.TimeSnapshot =
      SystemClock(Clock.fixed(instant(), ZoneOffset.UTC)).snapshot(millis)
  }

  private companion object {
    const val TEXT = "Hamburg"
    const val SIZE = 22
    const val WIDTH = 160
    const val HEIGHT = 32
    const val FRAME_MS = 16L
    const val LAG_FRAMES = 8
    const val TOLERANCE = 0.02f
    const val MOVED = 0.05f
  }
}
