/*
 * Copyright 2024 The Android Open Source Project
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

package com.google.android.horologist.benchmark.tools

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Environment
import androidx.test.internal.runner.listener.InstrumentationRunListener
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.Description
import org.junit.runner.Result
import java.io.File
import java.io.PrintStream

@SuppressLint("RestrictedApi")
public class MarkCompletionListener: InstrumentationRunListener() {
    private val markerFile = File(checkNotNull(InstrumentationRegistry.getArguments().getString("marker")) {
        "Instrument argument 'marker' missing"
    })

    private val markerSignal = InstrumentationRegistry.getArguments().getString("signal") ?: "Finished"

    override fun testRunStarted(description: Description?) {
        println("MarkCompletionListener.testRunStarted")

        check (!markerFile.exists()) {
            "Instrument argument 'marker' should not exist: ${markerFile.absolutePath}"
        }
    }

    override fun instrumentationRunFinished(
        streamResult: PrintStream?,
        resultBundle: Bundle?,
        junitResults: Result?
    ) {
        println("MarkCompletionListener.instrumentationRunFinished")

        markerFile.writeText(markerSignal)

        println("wrote $markerFile")
    }
}

private fun Context.getFirstMountedMediaDir(): File? {
    @Suppress("DEPRECATION")
    return externalMediaDirs.firstOrNull {
        Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED
    }
}