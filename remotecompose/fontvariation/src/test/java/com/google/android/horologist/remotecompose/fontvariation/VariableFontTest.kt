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

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import kotlin.math.abs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

internal val testFont: VariableFont by lazy {
  VariableFont.parse(File("src/debug/res/raw/google_sans_flex_wght_rond.ttf").readBytes())
}

/** Checks the Kotlin reader against outlines `fontTools` produced for the same font. */
class VariableFontTest {

  @Test
  fun axes() {
    assertThat(testFont.axes)
      .containsExactly(FontAxis("wght", 1f, 400f, 1000f), FontAxis("ROND", 0f, 0f, 100f))
      .inOrder()
    assertThat(testFont.unitsPerEm).isEqualTo(2000)
  }

  @Test
  fun outlinesMatchFontTools() {
    val golden =
      Json.parseToJsonElement(
          javaClass.classLoader!!.getResource("google_sans_flex_golden.json")!!.readText()
        )
        .jsonObject
    var checkedPoints = 0
    for (case in golden.getValue("cases").jsonArray) {
      val location =
        case.jsonObject.getValue("location").jsonObject.mapValues { it.value.jsonPrimitive.float }
      val coords = testFont.normalize(location)
      for ((char, expected) in case.jsonObject.getValue("glyphs").jsonObject) {
        val e = expected.jsonObject
        val glyph = testFont.glyphId(char.codePointAt(0))
        val where = "'$char' at $location"
        assertWithMessage("glyph id of $where")
          .that(glyph)
          .isEqualTo(e.getValue("glyph").jsonPrimitive.int)

        val outline = testFont.outline(glyph, coords)
        assertWithMessage("advance of $where")
          .that(outline.advance)
          .isWithin(TOLERANCE)
          .of(e.getValue("advance").jsonPrimitive.float)

        val contours = e.getValue("contours").jsonArray
        assertWithMessage("contours of $where").that(outline.contours).hasSize(contours.size)
        contours.forEachIndexed { c, contour ->
          val points = contour.jsonArray
          val actual = outline.contours[c]
          assertWithMessage("points in contour $c of $where")
            .that(actual.size)
            .isEqualTo(points.size)
          points.forEachIndexed { i, point ->
            val (x, y, on) = point.jsonArray
            assertWithMessage("on-curve $c/$i of $where")
              .that(actual.onCurve[i])
              .isEqualTo(on.jsonPrimitive.boolean)
            val dx = abs(actual.xs[i] - x.jsonPrimitive.float)
            val dy = abs(actual.ys[i] - y.jsonPrimitive.float)
            assertWithMessage("point $c/$i of $where off by ($dx, $dy)")
              .that(maxOf(dx, dy))
              .isLessThan(TOLERANCE)
            checkedPoints++
          }
        }
      }
    }
    assertThat(checkedPoints).isGreaterThan(5_000)
  }

  @Test
  fun compositeGlyphsAreDecomposed() {
    val eAcute = testFont.outline(testFont.glyphId('é'.code), testFont.normalize(emptyMap()))
    val e = testFont.outline(testFont.glyphId('e'.code), testFont.normalize(emptyMap()))
    assertThat(eAcute.contours.size).isGreaterThan(e.contours.size)
  }

  @Test
  fun missingCharactersUseNotdef() {
    assertThat(testFont.glyphId(0x4E2D)).isEqualTo(0)
  }

  private companion object {
    /** Font units; fontTools rounds the golden coordinates to 4 decimal places. */
    const val TOLERANCE = 0.01f
  }
}
