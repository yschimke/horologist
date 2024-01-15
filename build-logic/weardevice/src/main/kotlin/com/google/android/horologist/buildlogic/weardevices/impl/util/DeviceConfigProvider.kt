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

package com.google.android.horologist.buildlogic.weardevices.impl.util

import com.android.builder.testing.api.DeviceConfigProvider

class DeviceConfigProvider(private val props: Map<String, String>) : DeviceConfigProvider {
//    val config = adb.execute(
//        request = ShellCommandRequest("am get-config"),
//        serial = serial
//    )
//
//    // mcc310-mnc260-en-rUS-ldltr-sw240dp-w240dp-h240dp-small-notlong-round-nowidecg-lowdr-port-watch-notnight-256dpi-finger-keysexposed-qwerty-navexposed-dpad-384x384-v33
////            println(config.output)

    // TODO read from props
    override fun getConfigFor(abi: String?): String? = abi

    override fun getDensity(): Int =
        (props[PROP_DEVICE_DENSITY] ?: props[PROP_DEVICE_DENSITY])?.toInt()
            ?: throw Exception("density not found")

    override fun getLanguage(): String? = props[PROP_DEVICE_LANGUAGE]

    override fun getRegion(): String? = props[PROP_DEVICE_REGION]

    override fun getAbis(): List<String> = props[PROP_DEVICE_CPU_ABI_LIST]?.split(",")
        ?: listOfNotNull(props[PROP_DEVICE_CPU_ABI], props[PROP_DEVICE_CPU_ABI2])

    companion object {
        // https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:ddmlib/src/main/java/com/android/ddmlib/IDevice.java?q=PROP_DEVICE_LANGUAGE
        val PROP_DEVICE_DENSITY: String = "ro.sf.lcd_density"
        val PROP_DEVICE_EMULATOR_DENSITY: String = "qemu.sf.lcd_density"
        val PROP_DEVICE_LANGUAGE: String = "persist.sys.language"
        val PROP_DEVICE_REGION: String = "persist.sys.country"
        val PROP_DEVICE_CPU_ABI_LIST = "ro.product.cpu.abilist";
        val PROP_DEVICE_CPU_ABI = "ro.product.cpu.abi";
        val PROP_DEVICE_CPU_ABI2 = "ro.product.cpu.abi2";
    }
}