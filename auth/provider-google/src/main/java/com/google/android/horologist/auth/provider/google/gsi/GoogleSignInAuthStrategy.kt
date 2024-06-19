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


@file:Suppress("DEPRECATION")

package com.google.android.horologist.auth.provider.google.gsi

import android.content.Context
import android.os.Build
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.horologist.auth.provider.google.SuspendingCredentialProvider
import com.google.android.horologist.auth.provider.google.types
import com.google.android.horologist.compose.material.Chip
import com.google.android.horologist.compose.nav.composable
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

open class GoogleSignInAuthStrategy(
    val context: Context,
) : SuspendingCredentialProvider() {
    override val types: List<String> =
        listOf(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL)

    override suspend fun isAvailableOnDevice(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }

    override suspend fun onClearCredential(
        request: ClearCredentialStateRequest,
    ) {
        withContext(Dispatchers.IO) {
            googleSignIn().signOut().await()
        }
    }

    override suspend fun getExistingCredential(
        context: Context,
        request: GetCredentialRequest,
    ): GetCredentialResponse {
        val account = try {
            val clientId =
                GetGoogleIdOption.createFrom(request.credentialOptions.first { types.contains(it.type) }.requestData).serverClientId
            withContext(Dispatchers.IO) {
                googleSignIn(clientId).silentSignIn().await()
            }
        } catch (e: ApiException) {
            val isNoCredential = when (e.statusCode) {
                CommonStatusCodes.SIGN_IN_REQUIRED -> true
                else -> false
            }

            if (isNoCredential) {
                throw NoCredentialException()
            } else {
                throw GetCredentialUnknownException("ApiException ${e.statusCode} ${e.status.statusMessage}")
            }
        }

        return buildCredentialResponse(account)
    }

    fun googleSignIn(clientId: String? = null): GoogleSignInClient {
        // TODO consider some form of caching?
        return GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .apply {
                    if (clientId != null) {
                        requestIdToken(clientId)
                    }
                }
                .build()
        )
    }

    override fun supportedRoutes(
        request: GetCredentialRequest,
        onNavigate: (Any) -> Unit,
    ): List<MenuChip> {
        val requestTypes = request.types

        return if (requestTypes.contains(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) || requestTypes.contains(
                TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
            )) {
            listOf(
                MenuChip(GoogleSignInScreen) {
                    Chip(
                        label = "Sign In With Google",
                        onClick = { onNavigate(GoogleSignInScreen) },
                    )
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
        composable<GoogleSignInScreen> {
            ProviderGoogleSignInScreen(
                onCompletion = onCompletion,
            )
        }
    }

    override val startRoute: Any = GoogleSignInScreen

    @Serializable
    object GoogleSignInScreen

    companion object {
        fun buildCredentialResponse(account: GoogleSignInAccount) =
            GetCredentialResponse(
                GoogleIdTokenCredential.Builder()
                    .apply {
                        account.id?.let {
                            setId(it)
                        }
                        account.idToken?.let {
                            setIdToken(it)
                        }
                    }
                    .setGivenName(account.givenName)
                    .setFamilyName(account.familyName)
                    .setDisplayName(account.displayName)
                    .setProfilePictureUri(account.photoUrl)
                    .build(),
            )
    }
}
