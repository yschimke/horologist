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

package com.google.android.horologist.auth.ui.common.screens.prompt

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.auth.composables.model.AccountUiModel
import com.google.android.horologist.auth.data.common.repository.AuthUserRepository
import com.google.android.horologist.auth.data.credman.LocalCredentialRepository
import com.google.android.horologist.auth.ui.ext.compareAndSet
import com.google.android.horologist.auth.ui.mapper.AccountUiModelMapper
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.horologist.auth.ui.googlesignin.mapper.AccountUiModelMapper as GoogleAccountUiModelMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * A view model for a sign-in prompt screen.
 *
 * It checks if there is a user already signed in, and emits the appropriate
 * [states][SignInPromptScreenState] through the [uiState] property.
 *
 * @sample com.google.android.horologist.auth.sample.screens.googlesignin.prompt.GoogleSignInPromptSampleScreen
 * @sample com.google.android.horologist.auth.sample.screens.oauth.devicegrant.prompt.DeviceGrantSignInPromptScreen
 * @sample com.google.android.horologist.auth.sample.screens.oauth.pkce.prompt.PKCESignInPromptScreen
 */
public open class SignInPromptViewModel(
    private val authUserRepository: AuthUserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInPromptScreenState>(SignInPromptScreenState.Idle)
    public val uiState: StateFlow<SignInPromptScreenState> = _uiState

    /**
     * Indicate that the screen has observed the [idle][SignInPromptScreenState.Idle] state and that
     * the view model can start its work.
     */
    public fun onIdleStateObserved() {
        _uiState.compareAndSet(
            expect = SignInPromptScreenState.Idle,
            update = SignInPromptScreenState.Loading,
        ) {
            viewModelScope.launch {
                authUserRepository.getAuthenticated()?.let { authUser ->
                    _uiState.value =
                        SignInPromptScreenState.SignedIn(AccountUiModelMapper.map(authUser))
                } ?: run {
                    _uiState.value = SignInPromptScreenState.SignedOut()
                }
            }
        }
    }
}

open class CredManSignInPromptViewModel(
    private val localCredentialRepository: LocalCredentialRepository,
    private val credentialManager: CredentialManager,
    private val mapper: (Credential) -> AccountUiModel,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInPromptScreenState>(SignInPromptScreenState.Idle)
    public val uiState: StateFlow<SignInPromptScreenState> = _uiState

    /**
     * Indicate that the screen has observed the [idle][SignInPromptScreenState.Idle] state and that
     * the view model can start its work.
     */
    public fun onIdleStateObserved() {
        _uiState.compareAndSet(
            expect = SignInPromptScreenState.Idle,
            update = SignInPromptScreenState.Loading,
        ) {
            viewModelScope.launch {
                val credential = localCredentialRepository.flow.first()
                if (credential != null) {
                    _uiState.value =
                        SignInPromptScreenState.SignedIn(mapper(credential))
                } else {
                    _uiState.value = SignInPromptScreenState.SignedOut()
                }
            }
        }
    }

    suspend fun signIn(context: Context, request: GetCredentialRequest) {
        try {
            val credentialResponse =
                credentialManager.getCredential(
                    context = context,
                    request = request,
                )

            localCredentialRepository.store(credentialResponse.credential)

            _uiState.value = SignInPromptScreenState.SignedIn(GoogleAccountUiModelMapper.map(
                GoogleIdTokenCredential.createFrom(credentialResponse.credential.data)
            ))
        } catch (e: GetCredentialException) {
            localCredentialRepository.signOut()
            _uiState.value = SignInPromptScreenState.SignedOut(e)
        }
    }
}

/**
 * The states for a sign-in prompt screen.
 */
public sealed class SignInPromptScreenState {

    public object Idle : SignInPromptScreenState()

    public object Loading : SignInPromptScreenState()

    public data class SignedIn(val account: AccountUiModel) : SignInPromptScreenState()

    public data class SignedOut(val error: GetCredentialException? = null) :
        SignInPromptScreenState()
}
