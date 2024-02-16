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

package com.google.android.horologist.buildlogic.weardevices

import com.google.android.horologist.buildlogic.weardevices.impl.test.adb.AdbHolder
import com.malinskiy.adam.request.shell.v2.ShellCommandRequest
import kotlinx.coroutines.delay
import java.io.File
import java.io.Serializable

sealed interface AdbDisconnect: Serializable {

    suspend fun disconnect(adb: AdbHolder)

    suspend fun reconnect(adb: AdbHolder)

    object InputSuspend: AdbDisconnect {
        fun readResolve(): Any = InputSuspend

        override suspend fun disconnect(adb: AdbHolder) {
            val x = adb.execute(ShellCommandRequest("echo 1 > /d/google_charger/input_suspend"))
            if (x.exitCode != 0) {
                throw Exception("disconnect failed (${x.exitCode}) " + x.errorOutput)
            }
        }

        override suspend fun reconnect(adb: AdbHolder) {
            val x = adb.execute(ShellCommandRequest("echo 0 > /d/google_charger/input_suspend"))
            if (x.exitCode != 0) {
                throw Exception("disconnect failed (${x.exitCode}) " + x.errorOutput)
            }
        }
    }

    class UsbControl(
        private val port: Int,
        private val location: String,
        private val uhubctl: File,
    ): AdbDisconnect {

        override suspend fun disconnect(adb: AdbHolder) {
            println("disconnect")
            adb.close()

            execute(this.uhubctl.toString(), "-p", port.toString(), "-l", location, "-a", "off")
        }

        override suspend fun reconnect(adb: AdbHolder) {
            println("reconnect")
            execute(this.uhubctl.toString(), "-p", port.toString(), "-l", location, "-a", "on")

            var error: Exception? = null
            repeat(10) {
                try {
                    adb.connect()
                    println("connected")
                    return
                } catch (e: Exception) {
                    println(e)
                    error = e
                    delay(500)
                }
            }
            throw error!!
        }

        private fun execute(vararg command: String) {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start()

            val result = process.waitFor()

            if (result != 0) {
                throw Exception("USB control failed")
            }
        }
    }

    object Simulate: AdbDisconnect {

        override suspend fun disconnect(adb: AdbHolder) {
            adb.close()
        }

        override suspend fun reconnect(adb: AdbHolder) {
            adb.connect()
        }
    }
}