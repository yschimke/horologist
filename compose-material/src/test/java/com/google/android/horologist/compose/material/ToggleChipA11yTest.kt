/*
 * Copyright 2023 The Android Open Source Project
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

package com.google.android.horologist.compose.material

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import com.google.android.horologist.images.base.paintable.ImageVectorPaintable.Companion.asPaintable
import com.google.android.horologist.screenshots.rng.WearLegacyA11yTest
import org.junit.Test

class ToggleChipA11yTest : WearLegacyA11yTest() {

    @Test
    fun withSecondaryLabelAndIcon() {
        runComponentTest {
            ToggleChip(
                checked = true,
                onCheckedChanged = { },
                label = "Primary label",
                toggleControl = ToggleChipToggleControl.Switch,
                icon = Icons.Default.Image.asPaintable(),
                secondaryLabel = "Secondary label",
            )
        }
    }

    @Test
    fun unchecked() {
        runComponentTest {
            ToggleChip(
                checked = false,
                onCheckedChanged = { },
                label = "Primary label",
                toggleControl = ToggleChipToggleControl.Switch,
                icon = Icons.Default.Image.asPaintable(),
                secondaryLabel = "Secondary label",
            )
        }
    }

    @Test
    fun disabled() {
        runComponentTest {
            ToggleChip(
                checked = true,
                onCheckedChanged = { },
                label = "Primary label",
                toggleControl = ToggleChipToggleControl.Switch,
                icon = Icons.Default.Image.asPaintable(),
                secondaryLabel = "Secondary label",
                enabled = false,
            )
        }
    }

    @Test
    fun uncheckedAndDisabled() {
        runComponentTest {
            ToggleChip(
                checked = false,
                onCheckedChanged = { },
                label = "Primary label",
                toggleControl = ToggleChipToggleControl.Switch,
                icon = Icons.Default.Image.asPaintable(),
                secondaryLabel = "Secondary label",
                enabled = false,
            )
        }
    }
}
