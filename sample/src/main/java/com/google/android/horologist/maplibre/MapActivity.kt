/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.android.horologist.maplibre

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.horologist.components.SampleApplication
import com.google.android.horologist.compose.nav.SwipeDismissableNavHost
import com.google.android.horologist.compose.nav.composable
import kotlinx.serialization.Serializable
import java.text.DecimalFormat

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapDI.init(application as SampleApplication)

        setContent {
            MapApp()
        }
    }
}

@Composable
fun MapApp() {
    val navController = rememberSwipeDismissableNavController()

    MaterialTheme {
        AppScaffold(timeText = {
            val dataUsageText = dataUsage()
            TimeText(maxSweepAngle = 130f) {
                composable {
                    ActiveNetworkIndicator()
                }
                separator()
                time()
                separator()
                text(dataUsageText)
            }
        }) {
            SwipeDismissableNavHost(navController = navController, startDestination = Map) {
                composable<Map> {
                    MapScreen(onSettingsClick = { navController.navigate(Settings) })
                }
                composable<Settings> {
                    SettingsScreen()
                }
            }
        }
    }
}

@Composable
fun dataUsage(): String {
    val format = remember { DecimalFormat("0.00") }
    val dataUsage = remember {
        val dataRequestRepository = MapDI.dataRequestRepository
        dataRequestRepository.currentPeriodUsage()
    }

    val report = dataUsage.collectAsStateWithLifecycle(null)

    val bytes = report.value?.dataByType?.values?.sum()

    return if (bytes == null) {
        "Loading..."
    } else {
        val mb = bytes.toDouble() / 1024 / 1024
        "${format.format(mb)} MB"
    }
}

@Serializable
object Map

@Serializable
object Settings
