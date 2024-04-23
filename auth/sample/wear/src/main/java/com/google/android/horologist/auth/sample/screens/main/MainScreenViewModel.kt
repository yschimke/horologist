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

package com.google.android.horologist.auth.sample.screens.main

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.auth.data.credman.CredentialRepository
import com.google.android.horologist.auth.sample.shared.PasskeyAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.IOException
import javax.inject.Inject

@SuppressLint("MissingPermission")
@HiltViewModel
class MainScreenViewModel @Inject
constructor(
    private val credentialManager: CredentialManager,
    private val passkeyAuthRepository: PasskeyAuthRepository,
    private val credentialRepository: CredentialRepository,
    private val json: Json
) : ViewModel() {
    private val lastError = MutableStateFlow<GetCredentialException?>(null)
    val screenState = combine(credentialRepository.flow, lastError) { credentials, error ->
        MainScreenState(credentials, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainScreenState())

    internal fun signOut() {
        viewModelScope.launch {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            credentialRepository.signOut()
        }
    }

    internal fun attemptSignIn(activityContext: Context, options: List<CredentialOption>) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            signIn(activityContext, options)
        }
    }

    private suspend fun signIn(activityContext: Context, options: List<CredentialOption>) {
        try {
            val credentialResponse =
                credentialManager.getCredential(
                    context = activityContext,
                    request = GetCredentialRequest.Builder()
                        .apply {
                            options.forEach {
                                addCredentialOption(it)
                            }
                        }
                        .build()
                )

            credentialRepository.store(credentialResponse.credential)
        } catch (e: GetCredentialException) {
            credentialRepository.signOut()
            lastError.value = e
        } catch (e: Exception) {
            credentialRepository.signOut()
            lastError.value = GetCredentialUnknownException(e.toString())
        }
    }

    fun attemptPasskeySignIn(activityContext: Context) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            val requestJson = try {
                // TODO this is technically unsafe if it switches threads
                val challenge = passkeyAuthRepository.getServerChallenge()

                // TODO raise a bug
                json.encodeToString(challenge).replace(",\"extensions\":null", "")
            } catch (e: IOException) {
                credentialRepository.signOut()
                lastError.value = GetCredentialUnknownException(e.toString())
                return@launch
            }

            val passkeyOption = GetPublicKeyCredentialOption(requestJson, null)

            signIn(activityContext = activityContext, options = listOf(passkeyOption))
        }
    }
}