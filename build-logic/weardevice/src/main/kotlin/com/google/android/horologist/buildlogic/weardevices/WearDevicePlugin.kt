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

@file:Suppress("UnstableApiUsage")

package com.google.android.horologist.buildlogic.weardevices

import com.android.build.api.variant.AndroidComponentsExtension
import com.google.android.horologist.buildlogic.weardevices.impl.SetupConfigureAction
import com.google.android.horologist.buildlogic.weardevices.impl.SetupTaskAction
import com.google.android.horologist.buildlogic.weardevices.impl.TestRunConfigureAction
import com.google.android.horologist.buildlogic.weardevices.impl.TestRunTaskAction
import com.google.android.horologist.buildlogic.weardevices.impl.WearDevice
import com.google.android.horologist.buildlogic.weardevices.impl.WearDeviceImpl
import org.gradle.api.Plugin
import org.gradle.api.Project

class WearDevicePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Registering with the Device registry will take care of the test options binding.
        project.extensions.getByType(AndroidComponentsExtension::class.java).apply {
            managedDeviceRegistry.registerDeviceType(WearDevice::class.java) {
                dslImplementationClass = WearDeviceImpl::class.java
                setSetupActions(
                    SetupConfigureAction::class.java,
                    SetupTaskAction::class.java
                )
                setTestRunActions(
                    TestRunConfigureAction::class.java,
                    TestRunTaskAction::class.java
                )
            }
        }
    }

}