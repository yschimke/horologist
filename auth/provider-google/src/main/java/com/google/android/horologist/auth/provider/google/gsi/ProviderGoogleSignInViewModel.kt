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

import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.horologist.auth.data.googlesignin.GoogleSignInEventListener
import com.google.android.horologist.auth.provider.google.activity.wearCredentialManager
import com.google.android.horologist.auth.ui.googlesignin.signin.GoogleSignInViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class ProviderGoogleSignInViewModel(
    googleSignInClient: GoogleSignInClient,
) : GoogleSignInViewModel(googleSignInClient) {
    val credentialResponse = MutableStateFlow<GetCredentialResponse?>(null)

    override val googleSignInEventListener: GoogleSignInEventListener = GoogleSignInEventListener {
        credentialResponse.value = GoogleSignInAuthStrategy.buildCredentialResponse(it)
    }

    public object Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            check(modelClass == ProviderGoogleSignInViewModel::class.java)

            val googleAuthStrategy = extras.wearCredentialManager.get<GoogleSignInAuthStrategy>()

            return ProviderGoogleSignInViewModel(
                googleAuthStrategy.googleSignIn,
            ) as T
        }
    }
}
