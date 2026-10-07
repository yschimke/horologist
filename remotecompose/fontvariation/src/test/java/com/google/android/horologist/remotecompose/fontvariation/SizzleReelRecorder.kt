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
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.roundToInt
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Records the sizzle reel's scenes ([SizzleFlexScene] and the rest) frame by frame on the View
 * player, stepping the document's clock, into PNGs under `$SIZZLE_OUT/<scene>/`. Skipped unless
 * `SIZZLE_OUT` is set; `sizzle-reel.sh` in this module runs it and joins the frames into a video.
 */
@Config(sdk = [35], qualifiers = "w227dp-h227dp-xhdpi")
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SizzleReelRecorder {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private val clock = SettableClock()
  private var scene by mutableStateOf(0)

  @Test
  fun record() {
    val out = System.getenv("SIZZLE_OUT")
    assumeTrue("SIZZLE_OUT not set", out != null)
    val robotoFlex = testFonts.first { it.name == "Roboto Flex" }.font
    val sansFlex = googleSansFlex.font
    val scenes: List<Pair<String, @Composable () -> Unit>> =
      listOf(
        "1-flex" to { SizzleFlexScene(robotoFlex) },
        "2-wave" to { SizzleWaveScene(robotoFlex) },
        "3-stopwatch" to { SizzleStopwatchScene(robotoFlex) },
        "4-round" to { SizzleRoundScene(sansFlex) },
        "5-outro" to { SizzleOutroScene(robotoFlex) },
      )
    val seconds = System.getenv("SIZZLE_SECONDS")?.toFloat() ?: 4f
    val fps = System.getenv("SIZZLE_FPS")?.toInt() ?: 30
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent { key(scene) { Player { scenes[scene].second() } } }
    for ((index, named) in scenes.withIndex()) {
      val dir = File(out, named.first).apply { mkdirs() }
      clock.seconds = 0f
      scene = index
      repeat(4) {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
      }
      for (frame in 0 until (seconds * fps).roundToInt()) {
        clock.seconds = frame / fps.toFloat()
        repeat(2) {
          composeRule.mainClock.advanceTimeByFrame()
          composeRule.waitForIdle()
        }
        File(dir, "frame_%04d.png".format(frame)).outputStream().use {
          round(capture()).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
      }
    }
  }

  @SuppressLint("RestrictedApi")
  @Composable
  private fun Player(content: @Composable () -> Unit) {
    val doc = rememberRemoteDocument(clock = clock) { content() }
    Box(Modifier.size(SIZE.dp).testTag("player")) {
      doc.value?.let { RemoteDocumentPlayer(it, SIZE, SIZE, Modifier.size(SIZE.dp)) }
    }
  }

  private fun capture(): Bitmap {
    val root = composeRule.activity.window.decorView
    val whole = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
    composeRule.runOnUiThread { root.draw(Canvas(whole)) }
    val b = composeRule.onNodeWithTag("player").fetchSemanticsNode().boundsInWindow
    return Bitmap.createBitmap(
      whole,
      b.left.roundToInt(),
      b.top.roundToInt(),
      b.width.roundToInt(),
      b.height.roundToInt(),
    )
  }

  /** The frame as a round watch shows it. */
  private fun round(frame: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawCircle(frame.width / 2f, frame.height / 2f, frame.width / 2f, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(frame, 0f, 0f, paint)
    return result
  }

  /** A clock held at [seconds] past 10:00:00 UTC, so the time in the hour is [seconds]. */
  private class SettableClock : RemoteClock {
    var seconds = 0f

    private fun instant() =
      Instant.parse("2026-01-01T10:00:00Z").plusNanos((seconds * 1e9).toLong())

    override fun millis(): Long = instant().toEpochMilli()

    override fun nanoTime(): Long = (seconds * 1e9).toLong()

    override fun getZoneId(): String = "UTC"

    override fun snapshot(millis: Long?): RemoteClock.TimeSnapshot =
      SystemClock(Clock.fixed(instant(), ZoneOffset.UTC)).snapshot(millis)
  }

  private companion object {
    const val SIZE = 227
  }
}
