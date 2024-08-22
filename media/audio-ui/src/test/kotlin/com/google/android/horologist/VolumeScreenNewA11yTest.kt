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

@file:OptIn(ExperimentalTestApi::class)

package com.google.android.horologist

import android.view.View
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material.MaterialTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.google.android.apps.common.testing.accessibility.framework.uielement.AccessibilityHierarchyAndroid
import com.google.android.apps.common.testing.accessibility.framework.uielement.ViewHierarchyElementAndroid
import com.google.android.horologist.audio.AudioOutput
import com.google.android.horologist.audio.VolumeState
import com.google.android.horologist.audio.ui.VolumeScreenTestCase
import com.google.android.horologist.screenshots.rng.ScreenshotTest
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowBuild


@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.WearOSLargeRound,
)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Category(ScreenshotTest::class)
class VolumeScreenNewA11yTest {
    @Test
    fun volumeScreenA11y() = runComposeUiTest {
        val volumeState = VolumeState(
            current = 0,
            max = 100,
        )
        val audioOutput = AudioOutput.BluetoothHeadset("id", "Pixelbuds")

        setContent {
            VolumeScreenTestCase(
                colors = MaterialTheme.colors,
                volumeState = volumeState,
                audioOutput = audioOutput,
            )
        }

        ShadowBuild.setFingerprint("test_fingerprint")

        enableAccessibilityChecks()

        onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun extractA11y() = runComposeUiTest {
        val volumeState = VolumeState(
            current = 0,
            max = 100,
        )
        val audioOutput = AudioOutput.BluetoothHeadset("id", "Pixelbuds")

        setContent {
            VolumeScreenTestCase(
                colors = MaterialTheme.colors,
                volumeState = volumeState,
                audioOutput = audioOutput,
            )

            val view = LocalView.current

            SideEffect {
                ShadowBuild.setFingerprint("test_fingerprint")

                val mapFromElementIdToView: BiMap<Long, View> = HashBiMap.create()
                val hierarchy = AccessibilityHierarchyAndroid.newBuilder(view)
                    .setViewOriginMap(mapFromElementIdToView)
                    .setObtainCharacterLocations(false)
                    .build()

                println(mapFromElementIdToView)

                println(hierarchy.deviceState)
                println(hierarchy.activeWindow)
                println(hierarchy.origin)

                hierarchy.activeWindow.allViews.forEach {
                    printOutViewAndChildren(it)
                }
            }
        }
    }

    private fun printOutViewAndChildren(vhea: ViewHierarchyElementAndroid) {
        println()
        println(vhea.accessibilityClassName)
        println(vhea.backgroundDrawableColor)
        println(vhea.boundsInScreen)
        println(vhea.contentDescription)
        println(vhea.stateDescription)

        (0 until vhea.childViewCount).forEach {
            printOutViewAndChildren(vhea.getChildView(it))
        }
    }
}
