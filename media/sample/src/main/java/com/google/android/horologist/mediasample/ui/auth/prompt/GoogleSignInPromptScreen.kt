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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.wear.compose.material3.ConfirmationDialog
import androidx.wear.compose.material3.Text
import com.google.android.horologist.auth.composables.material3.buttons.GuestModeButton
import com.google.android.horologist.auth.composables.material3.buttons.SignInButton
import com.google.android.horologist.auth.ui.material3.common.screens.prompt.SignInPromptScreen
import com.google.android.horologist.mediasample.R
import com.google.android.horologist.mediasample.ui.navigation.UampNavigationScreen

@Composable
fun GoogleSignInPromptScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: UampSignInPromptViewModel,
) {
    var showAlreadySignedInDialog by rememberSaveable { mutableStateOf(false) }

    SignInPromptScreen(
        message = stringResource(id = R.string.google_sign_in_prompt_message),
        onAlreadySignedIn = { _ ->
            showAlreadySignedInDialog = true
        },
        modifier = modifier,
        viewModel = viewModel,
    ) {
        item {
            SignInButton(
                onClick = {
                    navController.navigate(UampNavigationScreen.GoogleSignInScreen.route)
                },
            )
        }
        item {
            GuestModeButton(
                onClick = {
                    viewModel.selectGuestMode()
                    navController.popBackStack()
                },
            )
        }
    }

    ConfirmationDialog(
        visible = showAlreadySignedInDialog,
        onDismissRequest = {
            showAlreadySignedInDialog = false
            navController.popBackStack()
        },
        text = {
            Text(
                text = stringResource(id = R.string.google_sign_in_prompt_already_signed_in_message),
                textAlign = TextAlign.Center,
            )
        },
        content = {},
    )
}
