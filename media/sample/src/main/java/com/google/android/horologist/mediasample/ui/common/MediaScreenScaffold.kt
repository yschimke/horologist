/*
 * Copyright 2026 The Android Open Source Project
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

package com.google.android.horologist.mediasample.ui.common

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator

/**
 * Thin wrapper around [ScreenScaffold] that hides the [ScrollIndicator] while a stitched scrolling
 * preview capture is in progress. This keeps `@ScrollingPreview(modes = [LONG])` renders stable —
 * the transient fade of the indicator would otherwise show up at different opacities per slice.
 */
@Composable
fun MediaScreenScaffold(
    scrollState: TransformingLazyColumnState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    ScreenScaffold(
        scrollState = scrollState,
        modifier = modifier,
        contentPadding = contentPadding,
        scrollIndicator = {
            if (!LocalScrollCaptureInProgress.current) {
                ScrollIndicator(state = scrollState)
            }
        },
        content = content,
    )
}
