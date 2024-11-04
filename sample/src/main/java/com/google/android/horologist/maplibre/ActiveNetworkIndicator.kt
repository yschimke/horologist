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

package com.google.android.horologist.maplibre

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Icon
import com.google.android.horologist.networks.data.NetworkType

@Composable
fun ActiveNetworkIndicator() {
    val network = MapDI.networkRepository.networkStatus.collectAsStateWithLifecycle()
    val offline = MapDI.networkOfflineFlow.collectAsStateWithLifecycle()
    val activeNetwork = network.value.activeNetwork?.networkInfo?.type

    Box {
        when (activeNetwork) {
            NetworkType.Cell -> Icon(
                Icons.Default.NetworkCell,
                contentDescription = "LTE",
                modifier = Modifier.size(16.dp)
            )

            NetworkType.BT -> Icon(
                Icons.Default.Bluetooth,
                contentDescription = "Bluetooth",
                modifier = Modifier.size(16.dp)
            )

            NetworkType.Wifi -> Icon(
                Icons.Default.Wifi,
                contentDescription = "Wifi",
                modifier = Modifier.size(16.dp)
            )

            null -> Icon(
                Icons.Default.WifiOff,
                contentDescription = "No Network",
                modifier = Modifier.size(16.dp)
            )

            else -> Icon(
                Icons.Default.QuestionMark,
                contentDescription = "Unknown Network",
                modifier = Modifier.size(16.dp)
            )
        }
    }
    if (offline.value) {
        Icon(
            Icons.Default.NotInterested,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}