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

package com.google.android.horologist.remotecompose.fontvariation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.schimke.composeai.preview.AnimatedPreview

@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@Composable
fun HelloRoundness0Preview() {
  VariableFontTextPreview("Hello", "ROND", 0f, Modifier.size(200.dp, 50.dp))
}

@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@Composable
fun HelloRoundness100Preview() {
  VariableFontTextPreview("Hello", "ROND", 100f, Modifier.size(200.dp, 50.dp))
}

@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@AnimatedPreview(durationMs = 2000, frameIntervalMs = 100, showCurves = false)
@Composable
fun HelloRoundnessAnimatedPreview() {
  VariableFontTextAnimatedPreview("Hello", "ROND", 0f, 100f, Modifier.size(200.dp, 50.dp))
}

@Preview(backgroundColor = 0xff000000, showBackground = true, widthDp = 200, heightDp = 50)
@AnimatedPreview(durationMs = 2000, frameIntervalMs = 100, showCurves = false)
@Composable
fun HelloWeightAnimatedPreview() {
  VariableFontTextAnimatedPreview("Hello", "wght", 100f, 1000f, Modifier.size(200.dp, 50.dp))
}
