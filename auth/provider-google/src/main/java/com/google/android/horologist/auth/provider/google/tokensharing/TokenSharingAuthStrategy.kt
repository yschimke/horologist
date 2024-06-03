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

package com.google.android.horologist.auth.provider.google.tokensharing

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import com.google.android.horologist.auth.provider.google.SuspendingCredentialProvider
import kotlinx.coroutines.flow.firstOrNull

class TokenSharingAuthStrategy(
    val tokenBundleRepository: TokenBundleRepository<Credential?>,
) : SuspendingCredentialProvider() {
    override val types: List<String> = listOf(TokenSharing)

    override suspend fun isAvailableOnDevice(): Boolean {
        return tokenBundleRepository.isAvailable()
    }

    @SuppressLint("NewApi")
    override suspend fun getExistingCredential(
        context: Context,
        request: GetCredentialRequest,
    ): GetCredentialResponse {
        val credential = tokenBundleRepository.flow.firstOrNull()

        return if (credential != null) {
            GetCredentialResponse(credential)
        } else {
            throw NoCredentialException("No credential found")
        }
    }

    companion object {
        val TokenSharing = "TokenSharingAuthStrategy"
    }

    // TODO add mobile prompting flow
    override val startRoute = null
}
