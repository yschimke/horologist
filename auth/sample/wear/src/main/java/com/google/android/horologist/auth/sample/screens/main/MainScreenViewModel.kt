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
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.auth.data.credman.LocalCredentialRepository
import com.google.android.horologist.auth.sample.shared.PasskeyAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.IOException
import javax.inject.Inject

@SuppressLint("MissingPermission")
@HiltViewModel
class MainScreenViewModel
@Inject
constructor(
    private val credentialManager: CredentialManager,
    private val passkeyAuthRepository: PasskeyAuthRepository,
    private val localCredentialRepository: LocalCredentialRepository,
    private val json: Json,
) : ViewModel() {
    private val lastError = MutableStateFlow<GetCredentialException?>(null)
    val screenState = combine(localCredentialRepository.flow, lastError) { credentials, error ->
        MainScreenState(credentials, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainScreenState())

    internal suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
        localCredentialRepository.signOut()
    }

    internal suspend fun attemptSignIn(activityContext: Context, options: List<CredentialOption>) {
        signIn(activityContext, options)
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
                        .build(),
                )

            localCredentialRepository.store(credentialResponse.credential)
        } catch (e: GetCredentialException) {
            localCredentialRepository.signOut()
            lastError.value = e
        } catch (e: Exception) {
            localCredentialRepository.signOut()
            lastError.value = GetCredentialUnknownException(e.toString())
        }
    }

    suspend fun attemptPasskeySignIn(activityContext: Context) {
        val requestJson = try {
            // TODO this is technically unsafe if it switches threads
            val challenge = passkeyAuthRepository.getServerChallenge()

            // TODO raise a bug
            json.encodeToString(challenge).replace(",\"extensions\":null", "")
        } catch (e: IOException) {
            localCredentialRepository.signOut()
            lastError.value = GetCredentialUnknownException(e.toString())
            return
        }

        val passkeyOption = GetPublicKeyCredentialOption(requestJson, null)

        signIn(activityContext = activityContext, options = listOf(passkeyOption))
    }
}
