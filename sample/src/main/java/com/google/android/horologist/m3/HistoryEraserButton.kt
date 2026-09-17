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

package com.google.android.horologist.m3

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SplitSwitchButton
import androidx.wear.compose.material3.SwitchButtonDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteSplitSwitchButton
import androidx.wear.compose.remote.material3.RemoteSplitSwitchButtonDefaults
import androidx.wear.compose.remote.material3.RemoteText
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound

/**
 * Styled Wear Compose split toggle button referencing the Ren and Stimpy "History Eraser Button".
 * Uses a custom scary style featuring `errorDim` and other error theme colors.
 */
@Composable
fun HistoryEraserButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  checked: Boolean = true,
  onCheckedChange: (Boolean) -> Unit = {},
  enabled: Boolean = true,
) {
  val colorScheme = MaterialTheme.colorScheme
  val scaryColors =
    SwitchButtonDefaults.splitSwitchButtonColors(
      checkedContainerColor = colorScheme.errorDim,
      checkedContentColor = colorScheme.onError,
      checkedSecondaryContentColor = colorScheme.onError.copy(alpha = 0.8f),
      checkedSplitContainerColor = colorScheme.errorContainer,
      checkedThumbColor = colorScheme.error,
      checkedThumbIconColor = colorScheme.onError,
      checkedTrackColor = colorScheme.onErrorContainer,
      checkedTrackBorderColor = colorScheme.error,
      uncheckedContainerColor = colorScheme.errorContainer,
      uncheckedContentColor = colorScheme.onErrorContainer,
      uncheckedSecondaryContentColor = colorScheme.onErrorContainer.copy(alpha = 0.8f),
      uncheckedSplitContainerColor = colorScheme.errorDim,
      uncheckedThumbColor = colorScheme.onErrorContainer,
      uncheckedTrackColor = colorScheme.errorContainer,
      uncheckedTrackBorderColor = colorScheme.onErrorContainer,
    )

  SplitSwitchButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    toggleContentDescription = "Arm History Eraser",
    onContainerClick = onClick,
    modifier = modifier.fillMaxWidth(),
    enabled = enabled,
    colors = scaryColors,
    secondaryLabel = { Text("Don't touch it!") },
    label = { Text("History Eraser") },
  )
}

/**
 * Styled Remote Compose split toggle button referencing the Ren and Stimpy "History Eraser Button".
 * Uses a custom scary style featuring `errorDim` and other error theme colors.
 */
@SuppressLint("RestrictedApi")
@RemoteComposable
@Composable
fun RemoteHistoryEraserButton(
  onClick: Action,
  modifier: RemoteModifier = RemoteModifier,
  checked: RemoteBoolean = true.rb,
  onCheckedChange: Action = Action.Empty,
  enabled: RemoteBoolean = true.rb,
) {
  val colorScheme = RemoteMaterialTheme.colorScheme
  val scaryColors =
    RemoteSplitSwitchButtonDefaults.splitSwitchButtonColors(
      checkedContainerColor = colorScheme.errorDim,
      checkedContentColor = colorScheme.onError,
      checkedSecondaryContentColor = colorScheme.onError.copy(alpha = 0.8f.rf),
      checkedSplitContainerColor = colorScheme.errorContainer,
      checkedThumbColor = colorScheme.error,
      checkedThumbIconColor = colorScheme.onError,
      checkedTrackColor = colorScheme.onErrorContainer,
      checkedTrackBorderColor = colorScheme.error,
      uncheckedContainerColor = colorScheme.errorContainer,
      uncheckedContentColor = colorScheme.onErrorContainer,
      uncheckedSecondaryContentColor = colorScheme.onErrorContainer.copy(alpha = 0.8f.rf),
      uncheckedSplitContainerColor = colorScheme.errorDim,
      uncheckedThumbColor = colorScheme.onErrorContainer,
      uncheckedTrackColor = colorScheme.errorContainer,
      uncheckedTrackBorderColor = colorScheme.onErrorContainer,
    )

  RemoteSplitSwitchButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    toggleContentDescription = "Arm History Eraser".rs,
    onContainerClick = onClick,
    modifier = modifier.fillMaxWidth(),
    enabled = enabled,
    colors = scaryColors,
    secondaryLabel = { RemoteText("Don't touch it!".rs) },
    label = { RemoteText("History Eraser".rs) },
  )
}

/** Overload of [HistoryEraserButton] for Remote Compose. */
@SuppressLint("RestrictedApi")
@RemoteComposable
@Composable
fun HistoryEraserButton(
  onClick: Action,
  modifier: RemoteModifier = RemoteModifier,
  checked: RemoteBoolean = true.rb,
  onCheckedChange: Action = Action.Empty,
  enabled: RemoteBoolean = true.rb,
) {
  RemoteHistoryEraserButton(
    onClick = onClick,
    modifier = modifier,
    checked = checked,
    onCheckedChange = onCheckedChange,
    enabled = enabled,
  )
}

@WearPreviewLargeRound
@Composable
fun HistoryEraserButtonWearPreview() {
  MaterialTheme {
    Box(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
      contentAlignment = Alignment.Center,
    ) {
      HistoryEraserButton(onClick = {})
    }
  }
}

@SuppressLint("RestrictedApi")
@WearPreviewLargeRound
@Composable
fun HistoryEraserButtonRemotePreview() {
  RemoteContentPreview {
    RemoteMaterialTheme {
      RemoteBox(
        modifier = RemoteModifier.fillMaxSize().padding(horizontal = 16.rdp),
        contentAlignment = RemoteAlignment.Center,
      ) {
        RemoteHistoryEraserButton(onClick = Action.Empty)
      }
    }
  }
}
