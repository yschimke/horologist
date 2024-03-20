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

package com.google.android.horologist.media.benchmark

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File

object Util {

    fun UiDevice.executeShellScript(script: String, stdin: String? = null): String {
        // externalFilesDir is writable, but we can't execute there (as of Q),
        // so we copy to /data/local/tmp
        val context = InstrumentationRegistry.getInstrumentation().context
        val externalDir = context.getExternalFilesDir(null)!!

        val stdinFile = File.createTempFile("temporaryStdin", null, externalDir)
        val writableScriptFile = File.createTempFile("temporaryScript", "sh", externalDir)
        val runnableScriptPath = "/data/local/tmp/" + writableScriptFile.name

        try {
            if (stdin != null) {
                stdinFile.writeText(stdin)
                writableScriptFile.writeText("cat ${stdinFile.absolutePath} | $script")
            } else {
                writableScriptFile.writeText(script)
            }

            // Note: we don't check for return values from the below, since shell based file
            // permission errors generally crash our process.
            executeShellCommand("cp ${writableScriptFile.absolutePath} $runnableScriptPath")
            executeShellCommand("chmod +x $runnableScriptPath")

            return executeShellCommand(runnableScriptPath)
        } finally {
            stdinFile.delete()
            writableScriptFile.delete()
            executeShellCommand("rm $runnableScriptPath")
        }
    }
}