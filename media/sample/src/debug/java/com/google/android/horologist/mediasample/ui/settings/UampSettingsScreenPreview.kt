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

package com.google.android.horologist.mediasample.ui.settings

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeSource
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import androidx.wear.compose.ui.tooling.preview.WearPreviewSmallRound
import com.google.android.horologist.auth.data.common.model.AuthUser

private object FixedTimeSource : TimeSource {
    @Composable
    override fun currentTime(): String = "10:10"
}

@Composable
private fun PreviewScaffold(content: @Composable () -> Unit) {
    AppScaffold(timeText = { TimeText(timeSource = FixedTimeSource) }) { content() }
}

@WearPreviewSmallRound
@WearPreviewLargeRound
@Composable
fun UampSettingsScreenSignedOutPreview() {
    PreviewScaffold {
        UampSettingsScreen(
            state = SettingsScreenState(
                authUser = null,
                guestMode = false,
                writable = true,
                showDeveloperOptions = true,
            ),
            onLoginClick = {},
            onLogoutClick = {},
            onGuestModeChange = {},
            onDeveloperOptionsClick = {},
            onShowLicensesClick = {},
        )
    }
}

@WearPreviewSmallRound
@WearPreviewLargeRound
@Composable
fun UampSettingsScreenSignedInPreview() {
    PreviewScaffold {
        UampSettingsScreen(
            state = SettingsScreenState(
                authUser = AuthUser(
                    displayName = "Jane Smith",
                    email = "jane.smith@example.com",
                ),
                guestMode = false,
                writable = true,
                showDeveloperOptions = true,
            ),
            onLoginClick = {},
            onLogoutClick = {},
            onGuestModeChange = {},
            onDeveloperOptionsClick = {},
            onShowLicensesClick = {},
        )
    }
}

@WearPreviewSmallRound
@WearPreviewLargeRound
@Composable
fun UampSettingsScreenGuestModePreview() {
    PreviewScaffold {
        UampSettingsScreen(
            state = SettingsScreenState(
                authUser = null,
                guestMode = true,
                writable = true,
                showDeveloperOptions = false,
            ),
            onLoginClick = {},
            onLogoutClick = {},
            onGuestModeChange = {},
            onDeveloperOptionsClick = {},
            onShowLicensesClick = {},
        )
    }
}
