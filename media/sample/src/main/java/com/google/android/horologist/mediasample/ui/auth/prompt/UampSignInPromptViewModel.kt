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

package com.google.android.horologist.mediasample.ui.auth.prompt

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.auth.data.credman.LocalCredentialRepository
import com.google.android.horologist.auth.ui.common.screens.prompt.CredManSignInPromptViewModel
import com.google.android.horologist.auth.ui.googlesignin.mapper.AccountUiModelMapper
import com.google.android.horologist.mediasample.BuildConfig
import com.google.android.horologist.mediasample.domain.SettingsRepository
import com.google.android.horologist.mediasample.domain.proto.copy
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UampSignInPromptViewModel
@Inject
constructor(
    localCredentialRepository: LocalCredentialRepository,
    credentialManager: CredentialManager,
    private val settingsRepository: SettingsRepository,
) :
    CredManSignInPromptViewModel(localCredentialRepository, credentialManager, mapper = {
        AccountUiModelMapper.map(GoogleIdTokenCredential.createFrom(it.data))
    }) {
    fun selectGuestMode() {
        viewModelScope.launch {
            settingsRepository.edit {
                it.copy {
                    guestMode = true
                }
            }
        }
    }

    suspend fun signIn(context: Context) {
        signIn(
            context = context,
            request = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetSignInWithGoogleOption.Builder(BuildConfig.GSI_CLIENT_ID)
                        .build()
                )
                .build()
        )
    }
}
