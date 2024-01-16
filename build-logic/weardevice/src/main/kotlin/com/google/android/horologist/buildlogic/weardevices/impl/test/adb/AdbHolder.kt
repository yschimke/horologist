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

package com.google.android.horologist.buildlogic.weardevices.impl.test.adb

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.request.AsyncChannelRequest
import com.malinskiy.adam.request.ComplexRequest
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.MultiRequest
import com.malinskiy.adam.request.emu.EmulatorCommandRequest
import com.malinskiy.adam.request.misc.FetchHostFeaturesRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow

class AdbHolder(val serial: String) {
    private var instance: AndroidDebugBridgeClient? = null
    var supportedFeatures: List<Feature> = listOf()

    val adb: AndroidDebugBridgeClient
        get() = checkNotNull(instance) { "AdbHolder not connected" }

    suspend fun connect() {
        check(instance == null) { "AdbHolder already connected" }
        instance = AndroidDebugBridgeClientFactory().build()

        supportedFeatures = adb.execute(
            request = FetchHostFeaturesRequest(), serial = serial
        )
    }

    fun close() {
        checkNotNull(instance) { "AdbHolder not connected" }.close()
        instance = null
    }


    suspend fun <T> execute(request: ComplexRequest<T>): T = adb.execute(request, serial)

    fun <T, I> execute(
        request: AsyncChannelRequest<T, I>,
        scope: CoroutineScope
    ): Flow<T> = adb.execute(request, scope, serial).consumeAsFlow()

    suspend fun execute(request: EmulatorCommandRequest): String = adb.execute(request)

    suspend fun <T> execute(request: MultiRequest<T>): T = adb.execute(request, serial)

}