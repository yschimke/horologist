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

import com.google.android.horologist.remotecompose.fontvariation.testFonts
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import org.junit.Test

/**
 * The sources in `src/debug/.../generated` are what [VariableFontCodegen] makes of [specs]. Set
 * `CODEGEN_WRITE=1` to regenerate them.
 */
class VariableFontCodegenTest {
  @Test
  fun generatedSourcesAreUpToDate() {
    val write = System.getenv("CODEGEN_WRITE") == "1"
    val stale = mutableListOf<String>()
    for (spec in specs) {
      val file = File(DIR, "${spec.function}.kt")
      val source = spec.generate()
      if (write) {
        file.parentFile!!.mkdirs()
        file.writeText(source)
      } else if (!file.exists() || file.readText() != source) {
        stale += file.name
      }
    }
    assertWithMessage("stale generated sources; run with CODEGEN_WRITE=1").that(stale).isEmpty()
  }

  internal class Spec(
    val function: String,
    val text: String,
    val axes: List<String>,
    val pixelSize: Float? = null,
    val tolerancePixels: Float = 1f / 16,
  ) {
    val font = testFonts[1]

    fun generate(): String =
      VariableFontCodegen.generate(
        PACKAGE,
        function,
        font.font,
        font.name,
        text,
        axes,
        pixelSize = pixelSize,
        tolerancePixels = tolerancePixels,
      )
  }

  companion object {
    const val PACKAGE = "com.google.android.horologist.remotecompose.fontvariation.generated"
    val DIR = File("src/debug/java/" + PACKAGE.replace('.', '/'))

    internal val specs =
      listOf(
        Spec("HamburgWght", "Hamburg", listOf("wght")),
        Spec("HamburgWghtSlnt", "Hamburg", listOf("wght", "slnt")),
        Spec("HamburgWghtSlntAt44Px", "Hamburg", listOf("wght", "slnt"), pixelSize = 44f),
        Spec("HelloWearWght", "Hello, Wear OS 12:45!", listOf("wght")),
      )
  }
}
