/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.android.horologist.mediasample.ui.auth.prompt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.wear.compose.material.ChipDefaults
import com.google.android.horologist.auth.composables.chips.GuestModeChip
import com.google.android.horologist.auth.composables.chips.SignInChip
import com.google.android.horologist.auth.ui.common.screens.prompt.SignInPromptScreen
import com.google.android.horologist.mediasample.R
import kotlinx.coroutines.launch

@Composable
fun GoogleSignInPromptScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: UampSignInPromptViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    SignInPromptScreen(
        title = stringResource(id = com.google.android.horologist.auth.composables.R.string.horologist_signin_prompt_title),
        message = stringResource(id = R.string.google_sign_in_prompt_message),
        onAlreadySignedIn = {
            navController.popBackStack()
        },
        onIdleStateObserved = { viewModel.onIdleStateObserved() },
        modifier = modifier,
        state = state,
    ) {
        item {
            val context = LocalContext.current
            SignInChip(
                onClick = {
                    coroutineScope.launch {
                        viewModel.signIn(context)
                    }
                },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
        item {
            GuestModeChip(
                onClick = {
                    viewModel.selectGuestMode()
                    navController.popBackStack()
                },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
    }
}
