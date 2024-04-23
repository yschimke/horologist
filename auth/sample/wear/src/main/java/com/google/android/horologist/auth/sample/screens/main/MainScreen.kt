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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.concurrent.futures.await
import androidx.credentials.Credential
import androidx.credentials.CredentialOption
import androidx.credentials.GetCustomCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.horologist.auth.provider.google.pkce.OauthPkceOption
import com.google.android.horologist.auth.provider.google.tokensharing.TokenSharingAuthStrategy
import com.google.android.horologist.auth.sample.BuildConfig
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults.ItemType
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults.listTextPadding
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults.padding
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.material.Chip
import com.google.android.horologist.compose.material.ListHeaderDefaults.firstItemPadding
import com.google.android.horologist.compose.material.ResponsiveListHeader
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.sign


@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = hiltViewModel(),
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    MainScreen(
        modifier = modifier,
        screenState = screenState,
        onSignIn = {
            viewModel.attemptSignIn(context, it)
        },
        onSignInWithPasskey = {
            viewModel.attemptPasskeySignIn(context)
        },
        onSignOut = {
            viewModel.signOut()
        },
        openInChrome = {
            val urlString = "https://enchanting-hexagonal-chauffeur.glitch.me"

            val intent =
                Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(Uri.parse(urlString))

            val helper = RemoteActivityHelper(context)
            coroutineScope.launch {
                // TODO check availability
                helper.startRemoteActivity(intent).await()
            }
        },
    )
}

@Composable
fun MainScreen(
    screenState: MainScreenState,
    onSignIn: (List<CredentialOption>) -> Unit,
    onSignInWithPasskey: () -> Unit,
    onSignOut: () -> Unit,
    openInChrome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val columnState = rememberResponsiveColumnState(
        contentPadding = padding(
            first = ItemType.Text,
            last = ItemType.Chip
        )
    )

    ScreenScaffold(modifier = modifier, scrollState = columnState) {
        ScalingLazyColumn(columnState = columnState) {
            item {
                ResponsiveListHeader(contentPadding = firstItemPadding()) {
                    Text(text = "Authentication")
                }
            }
            if (screenState.credential != null) {
                ShowCredentials(screenState.credential)
            } else {
                item {
                    Text(text = "Not Logged In")
                }
            }

            if (screenState.error != null) {
                item {
                    ShowError(screenState.error)
                }
            }
            item {
                ResponsiveListHeader(contentPadding = firstItemPadding()) {
                    Text(text = "Sign In Options")
                }
            }
            item {
                Chip(
                    label = "Sign In",
                    onClick = {
                        onSignIn(
                            listOf(
                                signInWithGoogleOption,
                                tokenSharingOption,
                                oauthPkceOption
                            )
                        )
                    },
                    modifier = Modifier.wrapContentHeight(),
                    enabled = screenState.credential == null,
                )
            }
            item {
                Chip(
                    label = "Sign In",
                    secondaryLabel = "With Google",
                    onClick = {
                        onSignIn(listOf(signInWithGoogleOption))
                    },
                    modifier = Modifier.wrapContentHeight(),
                    enabled = screenState.credential == null,
                )
            }
            item {
                Chip(
                    label = "Sign In",
                    secondaryLabel = "Token Sharing",
                    onClick = {
                        onSignIn(listOf(tokenSharingOption))
                    },
                    modifier = Modifier.wrapContentHeight(),
                    enabled = screenState.credential == null,
                )
            }
            item {
                Chip(
                    label = "Sign In",
                    secondaryLabel = "via Mobile",
                    onClick = {
                        onSignIn(listOf(oauthPkceOption))
                    },
                    modifier = Modifier.wrapContentHeight(),
                    enabled = screenState.credential == null,
                )
            }
            item {
                Chip(
                    label = "Sign In",
                    secondaryLabel = "Passkey",
                    onClick = {
                        onSignInWithPasskey()
                    },
                    modifier = Modifier.wrapContentHeight(),
                    enabled = screenState.credential == null,
                )
            }
            item {
                Chip(
                    label = "Sign Out",
                    onClick = {
                        onSignOut()
                    },
                    modifier = Modifier.wrapContentHeight(),
                    enabled = screenState.credential != null,
                )
            }
            item {
                ResponsiveListHeader(modifier = Modifier.listTextPadding()) {
                    Text(text = "Other")
                }
            }
            item {
                Chip(
                    label = "Launch Mobile Chrome",
                    onClick = {
                        openInChrome()
                    },
                    modifier = Modifier.wrapContentHeight(),
                )
            }
        }
    }
}

@Composable
private fun ShowError(error: GetCredentialException) {
    val errorMessage = when (error) {
        is NoCredentialException -> "No credential"
        is GetCredentialUnsupportedException -> "Unsupported ${error.message.orEmpty()}"
        is GetCredentialCancellationException -> "Cancelled ${error.message.orEmpty()}"
        else ->error.toString()
    }
    Text(text = errorMessage, color = MaterialTheme.colors.error)
}

private fun ScalingLazyListScope.ShowCredentials(credential: Credential) {
    item {
        Text(text = "Logged In")
    }

    when (credential) {
        is GoogleIdTokenCredential -> {
            item {
                Text(text = credential.displayName ?: "Unknown")
            }
        }

        is PasswordCredential -> {
            item {
                Text(text = "Password")
            }
            item {
                Text(text = credential.id)
            }
        }

        is PublicKeyCredential -> {
            item {
                Text(text = "Public Key ${credential.extractKey()}")
            }
        }

        else -> {
            item {
                Text(text = "Custom")
            }
            item {
                Text(text = credential.type)
            }
        }
    }
}

fun PublicKeyCredential.extractKey(): String {
    val regexp = "\"rawId\":\"(.*?)\"".toRegex()
    return regexp.find(this.authenticationResponseJson)?.groupValues?.get(1) ?: "Unknown"
}

val tokenSharingOption = GetCustomCredentialOption(
    type = TokenSharingAuthStrategy.TokenSharing,
    requestData = Bundle(),
    candidateQueryData = Bundle(),
    isSystemProviderRequired = false
)

val signInWithGoogleOption: GetSignInWithGoogleOption =
    GetSignInWithGoogleOption.Builder(BuildConfig.GSI_CLIENT_ID)
        .build()

val oauthPkceOption: OauthPkceOption =
    OauthPkceOption.Builder()
        .build()

val googleIdOptionExisting = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setAutoSelectEnabled(true)
    .setServerClientId(BuildConfig.GSI_CLIENT_ID)
    .build()

val googleIdOptionNew = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false)
    .setServerClientId(BuildConfig.GSI_CLIENT_ID)
    .build()

data class MainScreenState(
    val credential: Credential? = null,
    val error: GetCredentialException? = null,
)

@Serializable
object MainScreen
