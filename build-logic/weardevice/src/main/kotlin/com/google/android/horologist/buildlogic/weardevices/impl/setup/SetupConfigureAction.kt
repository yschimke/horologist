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

package com.google.android.horologist.buildlogic.weardevices.impl.setup

import com.android.build.api.instrumentation.manageddevice.DeviceSetupConfigureAction
import com.google.android.horologist.buildlogic.weardevices.WearDevice
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class SetupConfigureAction @Inject constructor(
    private val objectFactory: ObjectFactory,
    private val project: Project,
) : DeviceSetupConfigureAction<WearDevice, DeviceSetupInput> {

    override fun configureTaskInput(deviceDsl: WearDevice): DeviceSetupInput {
        return objectFactory.newInstance(DeviceSetupInput::class.java).apply {
            serial.set(deviceDsl.serial)
            serial.disallowChanges()
        }
    }
}