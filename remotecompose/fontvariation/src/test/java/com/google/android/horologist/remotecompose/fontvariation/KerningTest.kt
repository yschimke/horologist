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

import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import kotlin.math.abs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The `GPOS` reader's pair kerning against the platform's own shaping of the same font at the same
 * location: `measure("AV") - measure("A") - measure("V")` at a text size of one em.
 */
@Config(sdk = [35])
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KerningTest {

  @Test
  fun kerningMatchesThePlatform() {
    val report = StringBuilder()
    var kerned = 0
    for (testFont in testFonts) {
      val font = testFont.font
      val file = File("src/debug/res/raw/${testFont.resource}.ttf")
      val locations =
        listOf(
          emptyMap(),
          testFont.axes.associateWith { tag -> font.axes.first { it.tag == tag }.maxValue },
        ) +
          listOf(testFont.axes.associateWith { tag -> font.axes.first { it.tag == tag }.minValue })
      for (location in locations) {
        val paint =
          Paint().apply {
            typeface =
              Typeface.Builder(file)
                .setFontVariationSettings(location.entries.joinToString { (t, v) -> "'$t' $v" })
                .build()
            textSize = font.unitsPerEm.toFloat()
          }
        var worst = 0f
        for (a in PAIRS) {
          val (x, y) = a[0] to a[1]
          val platform = paint.measureText(a) - paint.measureText("$x") - paint.measureText("$y")
          val ours = font.kerning(font.glyphId(x.code), font.glyphId(y.code), location)
          if (abs(platform) > 0.5f) kerned++
          worst = maxOf(worst, abs(platform - ours))
          assertWithMessage("$testFont '$a' at $location").that(ours).isWithin(1f).of(platform)
        }
        report.appendLine("$testFont $location worst=$worst")
      }
    }
    println(report)
    // The pairs are chosen to kern in most fonts; a reader that found no kerning would pass
    // trivially.
    assertWithMessage("kerned pairs seen").that(kerned).isGreaterThan(20)
  }

  private companion object {
    val PAIRS =
      listOf(
        "AV",
        "VA",
        "To",
        "Ty",
        "LT",
        "Yo",
        "AW",
        "P.",
        "r.",
        "F,",
        "11",
        "17",
        "77",
        "T:",
        "y.",
        "Av",
      )
  }
}
