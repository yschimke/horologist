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

package com.google.android.horologist.remotecompose.fontvariation.codegen

import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.asRdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.horologist.remotecompose.fontvariation.RemoteVariableFontText
import com.google.android.horologist.remotecompose.fontvariation.VariableFont
import com.google.android.horologist.remotecompose.fontvariation.VariableTextOutline
import com.google.android.horologist.remotecompose.fontvariation.generated.HamburgWght
import com.google.android.horologist.remotecompose.fontvariation.generated.HamburgWghtSlnt
import com.google.android.horologist.remotecompose.fontvariation.generated.HelloWearWght
import com.google.android.horologist.remotecompose.fontvariation.outline
import com.google.android.horologist.remotecompose.fontvariation.testFonts
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * A generated composable writes exactly the document [RemoteVariableFontText] writes from the font,
 * byte for byte; it just gets there without the font. With `CODEGEN_BENCH=1`, also how long each
 * takes to make.
 */
@Config(sdk = [35])
@RunWith(AndroidJUnit4::class)
class GeneratedVsLibraryTest {
  private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
  private val fontFile = File("src/debug/res/raw/${testFonts[1].resource}.ttf")
  private val font = testFonts[1].font
  private val size = 22.dp.asRdp()

  private val cases: List<Triple<String, @Composable () -> Unit, @Composable () -> Unit>> =
    listOf(
      Triple(
        "Hamburg wght",
        { RemoteVariableFontText("Hamburg", font, axes("wght"), size) },
        { HamburgWght(axes("wght").getValue("wght"), size) },
      ),
      Triple(
        "Hamburg wght+slnt",
        { RemoteVariableFontText("Hamburg", font, axes("wght", "slnt"), size) },
        {
          val a = axes("wght", "slnt")
          HamburgWghtSlnt(a.getValue("wght"), a.getValue("slnt"), size)
        },
      ),
      Triple(
        "Hello Wear wght",
        { RemoteVariableFontText("Hello, Wear OS 12:45!", font, axes("wght"), size) },
        { HelloWearWght(axes("wght").getValue("wght"), size) },
      ),
    )

  @Test
  fun generatedDocumentsAreTheLibrarys() = runBlocking {
    for ((name, library, generated) in cases) {
      val expected = captureSingleRemoteDocument(context = context, content = library).bytes
      val actual = captureSingleRemoteDocument(context = context, content = generated).bytes
      assertWithMessage(name).that(actual.toList()).isEqualTo(expected.toList())
    }
  }

  @Test
  fun creationTime() {
    assumeTrue("set CODEGEN_BENCH=1 to run", System.getenv("CODEGEN_BENCH") == "1")
    val timed =
      listOf<Pair<String, @Composable () -> Unit>>(
        "Hamburg wght  library+parse" to
          {
            RemoteVariableFontText(
              "Hamburg",
              VariableFont.parse(fontFile.readBytes()),
              axes("wght"),
              size,
            )
          }
      ) +
        cases.flatMap { (name, library, generated) ->
          listOf("$name library" to library, "$name generated" to generated)
        }
    val best = timed.associate { it.first to Double.MAX_VALUE }.toMutableMap()
    repeat(3) {
      for ((name, content) in timed) {
        val times = runBlocking {
          repeat(10) { captureSingleRemoteDocument(context = context, content = content) }
          List(41) {
            val t0 = System.nanoTime()
            captureSingleRemoteDocument(context = context, content = content)
            System.nanoTime() - t0
          }
        }
        best[name] = minOf(best.getValue(name), times.sorted()[20] / 1e6)
      }
    }
    println("capture time, best of 3 rounds' medians of 41 (the library's outline is cached)")
    best.forEach { (name, ms) -> println("  %-32s %7.2f ms".format(name, ms)) }

    // What codegen moves to build time: working the outline out, against decoding it.
    val encoded = font.outline("Hello, Wear OS 12:45!", listOf("wght")).encode()
    fun median(block: () -> Unit): Double {
      repeat(20) { block() }
      return List(41) { System.nanoTime().also { block() }.let { t -> System.nanoTime() - t } }
        .sorted()[20] / 1e6
    }
    println("\"Hello, Wear OS 12:45!\" wght, median of 41")
    println(
      "  %-32s %7.2f ms".format("parse font", median { VariableFont.parse(fontFile.readBytes()) })
    )
    println(
      "  %-32s %7.2f ms"
        .format(
          "outline from parsed font",
          median { font.outline("Hello, Wear OS 12:45!", listOf("wght")) },
        )
    )
    println(
      "  %-32s %7.2f ms"
        .format("decode generated outline", median { VariableTextOutline.decode(encoded) })
    )
  }

  @Composable
  private fun axes(vararg tags: String): Map<String, RemoteFloat> = tags.associateWith {
    rememberNamedRemoteFloat("axis.$it") { 400f.rf }
  }
}
