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

package com.google.android.horologist.composables

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import androidx.wear.compose.material.ButtonColors
import androidx.wear.compose.material.ButtonDefaults
import com.google.android.horologist.compose.material.ButtonSize

@Composable
fun MorphButton(
    shapeMorph: Morph,
    shapeProgress: () -> Float,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    colors: ButtonColors = ButtonDefaults.primaryButtonColors(),
    buttonSize: ButtonSize = ButtonSize.Default,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val currentProgress = shapeProgress()
    val shape = shapeMorph.asShape(currentProgress)
    SideEffect {
        println(currentProgress)
    }

    androidx.wear.compose.material.Button(
        onClick = onClick,
        modifier = modifier.size(buttonSize.tapTargetSize),
        enabled = enabled,
        colors = colors,
        shape = shape,
    ) {
        content()
    }
}

private fun Morph.asShape(progress: Float): Shape {
    val path = toPath(progress).asComposePath()
    return GenericShape { size, ltr ->
        addPath(path)
        close()
    }
}
