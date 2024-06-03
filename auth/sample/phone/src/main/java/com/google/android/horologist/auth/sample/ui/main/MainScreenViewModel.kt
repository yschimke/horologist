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

package com.google.android.horologist.auth.sample.ui.main

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.internal.Preconditions.checkMainThread
import com.google.android.horologist.auth.data.credman.LocalCredentialRepository.Companion.normalise
import com.google.android.horologist.auth.data.phone.tokenshare.credman.CredentialRepositoryImpl
import com.google.android.horologist.auth.sample.py.CurrentActivityProvider
import com.google.android.horologist.auth.sample.shared.PasskeyAuthRepository
import com.google.android.horologist.datalayer.phone.PhoneDataLayerAppHelper
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.IOException
import javax.inject.Inject

@SuppressLint("MissingPermission")
@HiltViewModel
class MainScreenViewModel @Inject
constructor(
    private val credentialManagerTokenRepository: CredentialRepositoryImpl,
    private val phoneDataLayerAppHelper: PhoneDataLayerAppHelper,
    private val credentialManager: CredentialManager,
    private val authorizationClient: AuthorizationClient,
    private val activityProvider: CurrentActivityProvider,
    private val passkeyAuthRepository: PasskeyAuthRepository,
    private val json: Json
) : ViewModel() {
    val screenState = MutableStateFlow(MainScreenState())

    private val tokenFlow = flow {
        if (credentialManagerTokenRepository.isAvailable()) {
            emitAll(credentialManagerTokenRepository.flow)
        }
    }

    init {
        viewModelScope.launch {
            screenState.update {
                it.copy(
                    apiAvailable = credentialManagerTokenRepository.isAvailable()
                )
            }

            // TODO avoid this running always
            tokenFlow.collect { token ->
                screenState.update {
                    it.copy(
                        wearTokenState = token
                    )
                }
            }
        }
    }

    internal fun signOut() {
        viewModelScope.launch {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())

            screenState.update {
                it.copy(
                    error = null,
                    credential = null,
                    apiAvailable = phoneDataLayerAppHelper.isAvailable(),
                    authorizedScopes = listOf()
                )
            }
        }
    }

    internal fun attemptSignIn(option: CredentialOption) {
        viewModelScope.launch {
            signIn(option)
        }
    }

    private suspend fun signIn(option: CredentialOption) {
        try {
            // TODO this is nasty, but safe given main thread
            checkMainThread()
            val activity = activityProvider.withActivity {
                this
            }

            val credentialResponse =
                credentialManager.getCredential(
                    activity, GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()
                )

            val credential = credentialResponse.credential.normalise()

            screenState.update {
                it.copy(
                    error = null,
                    credential = credential,
                )
            }
        } catch (e: GetCredentialCancellationException) {
            e.printStackTrace()
            screenState.update {
                it.copy(
                    error = null,
                    credential = null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            screenState.update {
                it.copy(
                    error = e,
                )
            }
        }
    }

    internal fun attemptAuthorization(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        viewModelScope.launch {
            val requestedScopes = listOf(DriveScopes.DRIVE_APPDATA).map { Scope(it) }
            val authorizationRequest: AuthorizationRequest =
                AuthorizationRequest.builder().setRequestedScopes(requestedScopes).build()

            try {
                val authorizationResult = authorizationClient
                    .authorize(authorizationRequest).await()

                if (authorizationResult.hasResolution()) {
                    val pendingIntent: PendingIntent = authorizationResult.pendingIntent!!

                    val request = IntentSenderRequest.Builder(pendingIntent)
                        .build()

                    launcher.launch(request)
                } else {
                    screenState.update {
                        it.copy(
                            error = null,
                            authorizedScopes = authorizationRequest.requestedScopes.map { it.scopeUri }
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                screenState.update {
                    it.copy(
                        error = e,
                    )
                }
            }
        }
    }

    internal fun onUpdateTokenCustom(credential: Credential?) {
        viewModelScope.launch {
            credentialManagerTokenRepository.update(
                credential
            )
        }
    }

    fun onIdentityCallback(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            val authResult = authorizationClient.getAuthorizationResultFromIntent(result.data)
            screenState.update {
                it.copy(
                    error = null,
                    authorizedScopes = authResult.grantedScopes
                )
            }
        } else {
            screenState.update {
                it.copy(
                    error = Exception("" + result.data),
                    authorizedScopes = listOf()
                )
            }
        }

    }

    fun attemptPasskeySignIn() {
        viewModelScope.launch {
            try {
                val challenge = passkeyAuthRepository.getServerChallenge()

                // TODO raise a bug
                val requestJson = json.encodeToString(challenge).replace(",\"extensions\":null", "")

                println(requestJson)

                val passkeyOption = GetPublicKeyCredentialOption(requestJson, null)

                signIn(passkeyOption)
            } catch (ioe: IOException) {
                screenState.update {
                    it.copy(
                        error = ioe,
                        authorizedScopes = listOf()
                    )
                }
            }
        }
    }
}