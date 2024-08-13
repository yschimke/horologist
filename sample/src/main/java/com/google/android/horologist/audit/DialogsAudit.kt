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

package com.google.android.horologist.audit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.WhereToVote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import com.google.android.horologist.compose.layout.rememberColumnState
import com.google.android.horologist.compose.material.Button
import com.google.android.horologist.compose.material.Chip
import com.google.android.horologist.compose.material.ResponsiveButton
import com.google.android.horologist.compose.material.responsiveButtonWidth

@Composable
fun DialogsAudit(route: AuditNavigation.Dialogs.Audit) {
    when (route.config) {
        AuditNavigation.Dialogs.Config.Title -> {
            M3AlertDialog(showDialog = true, title = "Title", onCancel = {})
        }

        AuditNavigation.Dialogs.Config.IconAndTitle -> {
            M3AlertDialog(
                showDialog = true,
                title = "Title",
                onCancel = {},
                icon = { Icon(Icons.Default.MedicalServices, contentDescription = "") },
            )
        }

        AuditNavigation.Dialogs.Config.OneButtonChip -> {
            M3AlertDialog(showDialog = true, title = "Title", onCancel = {}) {
                item {
                    Chip("A Chip", onClick = {})
                }
            }
        }
        AuditNavigation.Dialogs.Config.TwoBottomButtons -> {
            M3AlertDialog(showDialog = true, title = "Title", onOk = {}, onCancel = {})
        }
        AuditNavigation.Dialogs.Config.NoBottomButton -> {
            M3AlertDialog(showDialog = true, title = "Title", onCancel = {})
        }
        AuditNavigation.Dialogs.Config.OneBottomButton -> {
            M3AlertDialog(showDialog = true, title = "Title", onCancel = {}) {
                item {
                    Button(
                        onClick = {},
                        imageVector = Icons.Default.WhereToVote,
                        contentDescription = "",
                    )
                }
            }
        }

        AuditNavigation.Dialogs.Config.NonScrollable -> {
            M3AlertDialog(
                showDialog = true,
                onCancel = {},
                title = "Title"
            )
        }
    }
}

@Composable
fun M3AlertDialog(
    showDialog: Boolean = true,
    onCancel: (() -> Unit)? = null,
    onOk: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    title: String,
    message: String? = null,
    okButtonContentDescription: String = stringResource(android.R.string.ok),
    cancelButtonContentDescription: String = stringResource(android.R.string.cancel),
    @Suppress("DEPRECATION") state: ScalingLazyColumnState = rememberColumnState(
        ScalingLazyColumnDefaults.responsive(),
    ),
    content: (ScalingLazyListScope.() -> Unit)? = null,
) {
    val (_, buttonWidth) = responsiveButtonWidth(if (onOk != null && onCancel != null) 2 else 1)
    androidx.wear.compose.material3.dialog.AlertDialog(
        modifier = modifier,
        show = showDialog,
        onDismissRequest = { onCancel?.invoke() },
        confirmButton = {
            if (onOk != null) {
                ResponsiveButton(
                    icon = Icons.Default.Check,
                    okButtonContentDescription,
                    onClick = onOk,
                    buttonWidth,
                )
            }
        },
        dismissButton = {
            if (onOk != null) {
                ResponsiveButton(
                    icon = Icons.Default.Close,
                    cancelButtonContentDescription,
                    onClick = onOk,
                    buttonWidth,
                    ChipDefaults.secondaryChipColors(),
                )
            }
        },
        title = { Text(title) },
        icon = icon,
    ) {
        if (message != null) {
            item {
                Text(message)
            }
        }
        if (content != null) {
            content()
        }
    }
}
