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

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3ExpressiveApi::class)

package com.google.android.horologist.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.graphics.shapes.Morph
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import com.google.android.horologist.compose.material3.RoundButtonDefaults.Standard
import com.google.android.horologist.compose.material3.RoundButtonDefaults.circleSquareMorph
import com.google.android.horologist.compose.material3.RoundButtonDefaults.toShape

@Composable
fun RoundButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shapeMorph: Morph = circleSquareMorph,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    size: Dp = Standard,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val progress = 0f

    val shape = remember(shapeMorph) { shapeMorph.toShape { progress } }

    Button(
        onClick = onClick,
        modifier = modifier.size(size),
        enabled = enabled,
        colors = colors,
        shape = shape,
    ) {
        // TODO avoid this box and implement correctly
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Preview
@Composable
fun RoundButtonPreviewSmall() {
    RoundButton(
        onClick = {},
        shapeMorph = circleSquareMorph,
        size = RoundButtonDefaults.Small
    ) {
        Icon(Icons.Default.Stream, "")
    }
}

@Preview
@Composable
fun RoundButtonPreviewStandard() {
    RoundButton(onClick = {}, shapeMorph = circleSquareMorph) {
        Icon(Icons.Default.Stream, "")
    }
}

@Preview
@Composable
fun RoundButtonPreviewL() {
    RoundButton(
        onClick = {},
        shapeMorph = circleSquareMorph,
        size = RoundButtonDefaults.Large
    ) {
        Icon(Icons.Default.Stream, "")
    }
}

@Preview
@Composable
fun RoundButtonPreviewXl() {
    RoundButton(
        onClick = {},
        shapeMorph = circleSquareMorph,
        size = RoundButtonDefaults.ExtraLarge
    ) {
        Icon(Icons.Default.Stream, "")
    }
}

@Preview
@Composable
fun RoundButtonPreviewStandardDisabled() {
    RoundButton(onClick = {}, shapeMorph = circleSquareMorph, enabled = false) {
        Icon(Icons.Default.Stream, "")
    }
}

@Preview
@Composable
fun RoundButtonPreviewInteractive() {
    var state by remember { mutableStateOf(false) }
    RoundButton(onClick = { state = !state }, shapeMorph = circleSquareMorph) {
        Icon(Icons.Default.Stream, "")
    }
}
