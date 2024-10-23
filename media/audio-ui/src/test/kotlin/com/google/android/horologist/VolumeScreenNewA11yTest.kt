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

import android.graphics.Rect
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
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

        accessibilityValidator!!.addCheckListener { context, accessibilityViewCheckResults ->
            accessibilityViewCheckResults.forEach {
                println(it.accessibilityHierarchyCheckResult.type)
                println(it)
            }
        }

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
                    .setObtainCharacterLocations(true)
                    .build()

                println(mapFromElementIdToView)

                println(hierarchy.deviceState)
                println(hierarchy.activeWindow)
                println(hierarchy.allWindows)
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

    @Test
    fun extractA11yNewWay() = runComposeUiTest {
        val volumeState = VolumeState(
            current = 0,
            max = 100,
        )
        val audioOutput = AudioOutput.BluetoothHeadset("id", "Pixelbuds")

        lateinit var view: View

        setContent {
            VolumeScreenTestCase(
                colors = MaterialTheme.colors,
                volumeState = volumeState,
                audioOutput = audioOutput,
            )

            view = LocalView.current
        }

        ShadowBuild.setFingerprint("test_fingerprint")

        val nodeInfo: AccessibilityNodeInfo = view.createAccessibilityNodeInfo()
        nodeInfo.setQueryFromAppProcessEnabled(view, true)
        val results = parseChildren(view, nodeInfo, 0, 0)

        results.forEach {
            println(it)
        }
    }

    private fun parseChildren(
        rootView: View,
        nodeInfo: AccessibilityNodeInfo,
        parentX: Int,
        parentY: Int,
    ): List<ViewInfo> {
        val childCount = nodeInfo.childCount
        val children: MutableList<ViewInfo> = ArrayList(childCount)
        for (i in 0 until childCount) {
            val childNodeInfo = nodeInfo.getChild(i)
            val bounds = Rect().apply {
                childNodeInfo.getBoundsInScreen(this)
            }

            if (
                childNodeInfo.availableExtraData.contains(
                    AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
                )
            ) {
                val extras = childNodeInfo.extras
                extras.putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, 0)
                extras.putInt(
                    AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH,
                    childNodeInfo.text.length,
                )
                try {
                    childNodeInfo.refreshWithExtraData(
                        AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY,
                        extras,
                    )
                } catch (ex: Exception) {
                    println("AccessibilityViewInfoParser.kt $ex")
                }
            }

            // Create a ViewInfo for each AccessibilityNodeInfo.
            // Use the root view as the viewObject.
            // Bounds in ViewInfo are with respect to the parent.
            val result =
                ViewInfo(
                    childNodeInfo?.className.toString(),
                    bounds,
                    rootView,
                    childNodeInfo,
                    parseChildren(rootView, childNodeInfo, bounds.left, bounds.top)
                )

            children.add(result)
        }
        return children
    }

//    /**
//     * Returns the text coming from the [AccessibilityNodeInfo] associated with this [ViewInfo], or null
//     * if there is no [AccessibilityNodeInfo].
//     */
//    fun ViewInfo.getAccessibilityText() =
//        (accessibilityObject as? AccessibilityNodeInfo)?.text?.toString()
//
//    /**
//     * Returns the source id from the [AccessibilityNodeInfo] associated with this [ViewInfo]. If the
//     * [AccessibilityNodeInfo] does not exist, but viewObject is a [View], this creates an
//     * [AccessibilityNodeInfo] for that [View] first.
//     */
//    fun ViewInfo.getAccessibilitySourceId(): Long {
//        if (accessibilityObject != null) {
//            return (accessibilityObject as AccessibilityNodeInfo).sourceNodeId
//        } else {
//            val node = (viewObject as? View)?.createAccessibilityNodeInfo() ?: return -1
//            return node.sourceNodeId
//        }
//    }
}

data class ViewInfo(
    val name: String,
    val bounds: Rect,
    val rootView: View,
    val childNodeInfo: AccessibilityNodeInfo,
    val parseChildren: List<ViewInfo>
)
