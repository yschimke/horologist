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

package com.google.android.horologist.mediasample.ui.auth.credman

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.google.android.horologist.auth.composables.chips.GuestModeChip
import com.google.android.horologist.auth.composables.chips.SignInChip
import com.google.android.horologist.auth.composables.model.AccountUiModel
import com.google.android.horologist.auth.ui.common.screens.prompt.SignInPromptScreen
import com.google.android.horologist.compose.material.Confirmation
import com.google.android.horologist.mediasample.R

@Composable
fun CredmanSignInPromptSampleScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: CredManSignInPromptViewModel = hiltViewModel(),
) {
    var showAlreadySignedInDialog by rememberSaveable { mutableStateOf(false) }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SignInPromptScreen(
        state = state,
        title = stringResource(id = R.string.horologist_signin_prompt_title),
        message = stringResource(id = R.string.google_sign_in_prompt_message),
        onIdleStateObserved = {
            // TODO trigger read
        },
        onAlreadySignedIn = { it: AccountUiModel -> showAlreadySignedInDialog = true },
        modifier = modifier,
        content = fun ScalingLazyListScope.() {
            item {
                val context = LocalContext.current
                SignInChip(
                    onClick = {
                        viewModel.signIn(context)
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
            item {
                GuestModeChip(
                    onClick = navController::popBackStack,
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
        },
    )

    if (showAlreadySignedInDialog) {
        Confirmation(
            onTimeout = {
                showAlreadySignedInDialog = false
                navController.popBackStack()
            },
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.google_sign_in_prompt_already_signed_in_message),
            )
        }
    }
}