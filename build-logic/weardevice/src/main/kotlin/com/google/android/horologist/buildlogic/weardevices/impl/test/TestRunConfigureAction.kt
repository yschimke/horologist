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

package com.google.android.horologist.buildlogic.weardevices.impl.test

import com.android.build.api.instrumentation.manageddevice.DeviceTestRunConfigureAction
import com.google.android.horologist.buildlogic.weardevices.WearDevice
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

open class TestRunConfigureAction @Inject constructor(
    private val objectFactory: ObjectFactory,
    private val providerFactory: ProviderFactory,
    private val project: Project,
): DeviceTestRunConfigureAction<WearDevice, DeviceTestRunInput> {

    override fun configureTaskInput(deviceDSL: WearDevice): DeviceTestRunInput =
        objectFactory.newInstance(DeviceTestRunInput::class.java).apply {
            serial.set(deviceDSL.serial)
            serial.disallowChanges()
            runMode.set(deviceDSL.runMode)
            runMode.disallowChanges()
        }
}