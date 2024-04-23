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

package com.google.android.horologist.auth.provider.google.pkce

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.auth.composables.dialogs.SignedInConfirmationDialog
import com.google.android.horologist.auth.composables.screens.AuthErrorScreen
import com.google.android.horologist.auth.composables.screens.CheckYourPhoneScreen
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@ExperimentalHorologistApi
@Composable
public fun PKCESignInScreen(
    onCompletion: (Result<GetCredentialResponse>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PKCESignInViewModel<*, *> =
        viewModel(factory = PKCESignInViewModel.Factory)

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (state) {
        PKCEScreenState.Idle -> {
            SideEffect {
                viewModel.startAuthFlow()
            }
        }

        PKCEScreenState.Loading,
        PKCEScreenState.CheckPhone,
        -> {
            CheckYourPhoneScreen(modifier = modifier)
        }

        is PKCEScreenState.Failed -> {
            AuthErrorScreen(modifier = modifier)
            LaunchedEffect(Unit) {
                delay(1.seconds)
                onCompletion(viewModel.result)
            }
        }

        is PKCEScreenState.Success -> {
            SignedInConfirmationDialog(
                onDismissOrTimeout = { onCompletion(viewModel.result) },
                modifier = modifier,
            )
        }
    }
}
