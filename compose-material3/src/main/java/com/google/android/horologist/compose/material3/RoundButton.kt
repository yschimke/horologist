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

package com.google.android.horologist.compose.material3

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stream
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonColors
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.Text
import com.google.android.horologist.compose.material3.RoundButtonDefaults.Standard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun RoundButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    size: Dp = Standard,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val pressed by interactionSource.collectIsPressedAsState()

    val progress = animateFloatAsState(targetValue = if (pressed) 1f else 0f, label = "Pressed")

    val shape = remember { RoundButtonDefaults.circleSquareShape { progress.value } }

    Box {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(size),
            enabled = enabled,
            colors = colors,
            shape = shape,
            interactionSource = interactionSource,
        ) {
            content()
        }
    }
}

@Preview
@Composable
fun RoundButtonPreviewSmall() {
    RoundButton(
        onClick = {},
        size = RoundButtonDefaults.Small
    ) {
        Icon(Icons.Default.Stream, "")
    }
}

@Preview
@Composable
fun RoundButtonPreviewStandard() {
    RoundButton(onClick = {}) {
        Icon(Icons.Default.Stream, "")
    }
}

@Preview
@Composable
fun RoundButtonPreviewL() {
    RoundButton(
        onClick = {},
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
        size = RoundButtonDefaults.ExtraLarge
    ) {
        Icon(Icons.Default.Stream, "")
    }
}

@Preview
@Composable
fun RoundButtonPreviewStandardDisabled() {
    RoundButton(onClick = {}, enabled = false) {
        Icon(Icons.Default.Stream, "")
    }
}

@Preview
@Composable
fun RoundButtonPreviewPressed() {
    val interactionSource = object : MutableInteractionSource {
        override val interactions: Flow<Interaction>
            get() = MutableStateFlow(PressInteraction.Press(Offset(0f, 0f)))

        override suspend fun emit(interaction: Interaction) {
        }

        override fun tryEmit(interaction: Interaction): Boolean = false
    }

    RoundButton(onClick = {}, enabled = false, interactionSource = interactionSource) {
        Icon(Icons.Default.Stream, "")
    }
}

@Preview
@Composable
fun RoundButtonPreviewInteractive() {
    RoundButton(onClick = { }) {
        Icon(Icons.Default.Stream, "")
    }
}
