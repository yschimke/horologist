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

package com.google.android.horologist.auth.provider.google.pkce

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.credentials.Credential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.wear.phone.interactions.authentication.CodeVerifier
import com.google.android.horologist.auth.provider.google.SuspendingCredentialProvider
import com.google.android.horologist.auth.provider.google.types
import com.google.android.horologist.compose.material.Chip
import com.google.android.horologist.compose.nav.composable
import kotlinx.serialization.Serializable

abstract class OAuthPkceAuthStrategy<OauthConfig, OAuthCodePayload>(
    val context: Context,
) : SuspendingCredentialProvider() {
    override val types: List<String> = listOf(OauthPkceOption.Type)

    override suspend fun isAvailableOnDevice(): Boolean {
        return true
    }

    override suspend fun getExistingCredential(
        context: Context,
        request: GetCredentialRequest,
    ): GetCredentialResponse {
        // No internal storage
        // TODO reconsider, especially with refresh
        throw NoCredentialException()
    }

    abstract suspend fun config(): OauthConfig

    abstract suspend fun fetchOAuthCode(
        config: OauthConfig,
        codeVerifier: CodeVerifier,
    ): Result<OAuthCodePayload>

    abstract suspend fun fetchToken(
        config: OauthConfig,
        codeVerifier: CodeVerifier,
        oAuthCodePayload: OAuthCodePayload,
    ): Result<Credential>

    override val startRoute = PkceScreen

    @Composable
    open fun signInLabel(): String {
        return "Sign In via Mobile"
    }

    override fun supportedRoutes(request: GetCredentialRequest, onNavigate: (Any) -> Unit): List<MenuChip> {
        return if (request.types.contains(OauthPkceOption.Type)) {
            listOf(
                MenuChip(PkceScreen) {
                    Chip(label = signInLabel(), onClick = { onNavigate(PkceScreen) })
                },
            )
        } else {
            listOf()
        }
    }

    override fun NavGraphBuilder.defineRoutes(
        navController: NavHostController,
        onCompletion: (Result<GetCredentialResponse>) -> Unit,
    ) {
        composable<PkceScreen> {
            PKCESignInScreen(onCompletion = onCompletion)
        }
    }

    @Serializable
    object PkceScreen
}
