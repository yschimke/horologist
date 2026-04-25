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

package com.google.android.horologist.mediasample.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme

private val UampPrimary = Color(0xFF981F68)
private val UampPrimaryContainer = Color(0xFF66003d)
private val UampSecondary = Color(0xFF981F68)
private val UampError = Color(0xFFE24444)
private val UampOnPrimary = Color.White
private val UampOnSurfaceVariant = Color(0xFFDADCE0)
private val UampSurface = Color(0xFF303133)
private val UampOnError = Color.Black

@Composable
fun UampTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = UampPrimary,
            primaryContainer = UampPrimaryContainer,
            secondary = UampSecondary,
            error = UampError,
            onPrimary = UampOnPrimary,
            onSurfaceVariant = UampOnSurfaceVariant,
            surfaceContainer = UampSurface,
            onError = UampOnError,
        ),
        content = content,
    )
}
