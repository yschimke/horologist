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

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.auth.composables.model.AccountUiModel
import com.google.android.horologist.auth.ui.common.screens.prompt.SignInPromptScreenState
import com.google.android.horologist.images.coil.CoilPaintable
import com.google.android.horologist.mediasample.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
    .setServerClientId(BuildConfig.GSI_CLIENT_ID)
    .build()

@HiltViewModel
class CredManSignInPromptViewModel
@Inject
constructor(
    val credentialManager: CredentialManager,
) : ViewModel() {
    fun signIn(context: Context) {

        viewModelScope.launch {
            val credentials = signInWithGoogle(context)
            if (credentials != null) {
                uiState.value = SignInPromptScreenState.SignedIn(
                    AccountUiModel(
                        email = credentials.id,
                        name = credentials.displayName,
                        avatar = CoilPaintable(credentials.profilePictureUri)
                    )
                )
            }
        }
    }

    private suspend fun signInWithGoogle(context: Context): GoogleIdTokenCredential? {
        try {
            val result = credentialManager.getCredential(
                request = GetCredentialRequest(listOf(googleIdOption)),
                context = context,
            )
            val credentials = GoogleIdTokenCredential.createFrom(result.credential.data)
            println(credentials)
            return credentials
        } catch (e: NoCredentialException) {
            e.printStackTrace()
        } catch (e: GetCredentialException) {
            e.printStackTrace()
        }
        return null
    }

    val uiState: MutableStateFlow<SignInPromptScreenState> =
        MutableStateFlow(SignInPromptScreenState.SignedOut)
}