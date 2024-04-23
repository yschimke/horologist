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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.credentials.Credential
import androidx.credentials.CredentialOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.horologist.auth.sample.BuildConfig
import com.google.android.horologist.auth.sample.R
import com.google.android.horologist.auth.sample.ui.theme.HorologistTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = hiltViewModel(),
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onIdentityCallback(result)
    }

    val context = LocalContext.current

    MainScreen(
        screenState = screenState,
        onUpdateToken = { viewModel.onUpdateTokenCustom(screenState.credential) },
        onClearToken = { viewModel.onUpdateTokenCustom(null) },
        onSignInWithGoogle = {
            viewModel.attemptSignIn(it)
        },
        onSignInWithPasskey = {
            viewModel.attemptPasskeySignIn()
        },
        onSignOut = {
            viewModel.signOut()
        },
        onAuthorize = {
            viewModel.attemptAuthorization(launcher)
        },
        openInChrome = {
            val urlString = "https://enchanting-hexagonal-chauffeur.glitch.me"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setPackage("com.android.chrome")
            try {
                context.startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                // Chrome browser presumably not installed so allow user to choose instead
                intent.setPackage(null)
                context.startActivity(intent)
            }
        }
    )
}

@Composable
fun MainScreen(
    screenState: MainScreenState,
    onUpdateToken: () -> Unit,
    onClearToken: () -> Unit,
    onSignInWithGoogle: (CredentialOption) -> Unit,
    onSignInWithPasskey: () -> Unit,
    onSignOut: () -> Unit,
    onAuthorize: () -> Unit,
    openInChrome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = {
                onSignInWithGoogle(signInWithGoogleOption)
            },
            modifier = Modifier.wrapContentHeight(),
            enabled = screenState.credential == null,
        ) { Text("Sign In With Google") }

        Button(
            onClick = {
                onSignInWithGoogle(googleIdOptionNew)
            },
            modifier = Modifier.wrapContentHeight(),
            enabled = screenState.credential == null,
        ) { Text("Sign In with Google Id") }

        Button(
            onClick = {
                onSignInWithPasskey()
            },
            modifier = Modifier.wrapContentHeight(),
            enabled = screenState.credential == null,
        ) { Text("Sign In with Passkey") }

        Button(
            onClick = {
                onAuthorize()
            },
            modifier = Modifier.wrapContentHeight(),
            enabled = screenState.credential != null && screenState.authorizedScopes.isEmpty()
        ) { Text("Authorize") }
        if (screenState.authorizedScopes.isNotEmpty()) {
            Text(text = screenState.authorizedScopes.toString())
        }

        Button(
            onClick = {
                onSignOut()
            },
            modifier = Modifier.wrapContentHeight(),
            enabled = screenState.credential != null,
        ) { Text("Sign Out") }

        if (screenState.credential != null) {
            Text(text = "Logged In")

            when (screenState.credential) {
                is GoogleIdTokenCredential -> {
                    Text(text = screenState.credential.displayName ?: "Unknown")
                }

                is PasswordCredential -> {
                    Text(text = "Password")
                    Text(text = screenState.credential.id)
                }

                is PublicKeyCredential -> {
                    Text(text = "Public Key ${screenState.credential.extractKey()}")
                }

                else -> {
                    Text(text = "Custom")
                    Text(text = screenState.credential.type)
                }
            }
        } else {
            Text(text = "Not Logged In")
        }
        if (screenState.error != null) {
            Text(text = screenState.error.toString(), color = MaterialTheme.colorScheme.error)
        }

        HorizontalDivider()

        Button(
            onClick = {
                onUpdateToken()
            },
            modifier = Modifier.wrapContentHeight(),
            enabled = screenState.apiAvailable && screenState.credential != null,
        ) { Text("Write Wear Credential") }

        Button(
            onClick = {
                onClearToken()
            },
            modifier = Modifier.wrapContentHeight(),
            enabled = screenState.apiAvailable,
        ) { Text("Clear Wear Credential") }

        if (screenState.wearTokenState != null) {
            Text(text = "Wear Logged In")

            if (screenState.wearTokenState is GoogleIdTokenCredential) {
                Text(text = screenState.wearTokenState.displayName ?: "Unknown")
            } else {
                Text(text = screenState.wearTokenState.type)
            }
        } else {
            Text(text = "Wear Not Logged In")
        }

        if (!screenState.apiAvailable) {
            Text(
                text = stringResource(R.string.token_share_message_api_unavailable),
                modifier.fillMaxWidth(),
                color = Color.Red,
                textAlign = TextAlign.Center,
            )
        }

        HorizontalDivider()

        Button(
            onClick = {
                openInChrome()
            },
            modifier = Modifier.wrapContentHeight(),
        ) { Text("Test with Chrome") }
    }
}

fun PublicKeyCredential.extractKey(): String {
    val regexp = "\"rawId\":\"(.*?)\"".toRegex()
    println(this.authenticationResponseJson)
    return regexp.find(this.authenticationResponseJson)?.groupValues?.get(1) ?: "Unknown"
}

val signInWithGoogleOption: GetSignInWithGoogleOption =
    GetSignInWithGoogleOption.Builder(BuildConfig.GSI_CLIENT_ID)
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
    val apiAvailable: Boolean = true,
    val error: Exception? = null,
    val wearTokenState: Credential? = null,
    val authorizedScopes: List<String> = listOf()
)

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    HorologistTheme {
        MainScreen(
            screenState = MainScreenState(null, true),
            onUpdateToken = {},
            onClearToken = { },
            onSignInWithGoogle = {},
            onSignInWithPasskey = {},
            onSignOut = {},
            onAuthorize = {},
            openInChrome = {}
        )
    }
}