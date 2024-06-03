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

import androidx.credentials.Credential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.wear.phone.interactions.authentication.CodeVerifier
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.auth.provider.google.activity.wearCredentialManager
import com.google.android.horologist.auth.ui.ext.compareAndSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@ExperimentalHorologistApi
public open class PKCESignInViewModel<C, T>(
    private val authStrategy: OAuthPkceAuthStrategy<C, T>,
) : ViewModel() {
    private val _uiState = MutableStateFlow<PKCEScreenState>(PKCEScreenState.Idle)
    public val uiState: StateFlow<PKCEScreenState> = _uiState

    public val result: Result<GetCredentialResponse>
        get() = when (val state = uiState.value) {
            is PKCEScreenState.Success -> Result.success(GetCredentialResponse(state.credential))
            is PKCEScreenState.Failed -> Result.failure(state.exception)
            else -> Result.failure(GetCredentialCancellationException())
        }

    public fun startAuthFlow() {
        _uiState.compareAndSet(
            expect = PKCEScreenState.Idle,
            update = PKCEScreenState.Loading,
        ) {
            viewModelScope.launch {
                val codeVerifier = CodeVerifier()
                val config = authStrategy.config()

                // Step 1: Retrieve the OAuth code
                _uiState.value = PKCEScreenState.CheckPhone
                val oAuthCodePayload = authStrategy.fetchOAuthCode(
                    config = config,
                    codeVerifier = codeVerifier,
                ).getOrElse {
                    _uiState.value = PKCEScreenState.Failed(it)
                    return@launch
                }

                // Step 2: Retrieve the access token
                _uiState.value = PKCEScreenState.Loading
                val tokenPayload = authStrategy.fetchToken(
                    config = config,
                    codeVerifier = codeVerifier,
                    oAuthCodePayload = oAuthCodePayload,
                )
                    .getOrElse {
                        _uiState.value = PKCEScreenState.Failed(it)
                        return@launch
                    }

                _uiState.value = PKCEScreenState.Success(tokenPayload)
            }
        }
    }

    public object Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            check(modelClass == PKCESignInViewModel::class.java)

            return createViewModel<Any, Any>(extras) as T
        }

        private fun <C, T> createViewModel(extras: CreationExtras): PKCESignInViewModel<C, T> {
            val pkceAuthStrategy = extras.wearCredentialManager.get<OAuthPkceAuthStrategy<C, T>>()

            return PKCESignInViewModel(
                pkceAuthStrategy,
            )
        }
    }
}

@ExperimentalHorologistApi
public sealed class PKCEScreenState {
    public data object Idle : PKCEScreenState()
    public data object Loading : PKCEScreenState()
    public data object CheckPhone : PKCEScreenState()
    public data class Success(val credential: Credential) : PKCEScreenState()
    public data class Failed(val exception: Throwable) : PKCEScreenState()
}
