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

package com.google.android.horologist.auth.provider.google

import android.content.Context
import android.content.Intent
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialUnsupportedException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.horologist.auth.data.credman.CredentialResponseReceiver
import com.google.android.horologist.auth.data.credman.CredentialResponseReceiver.Companion.toBundle
import com.google.android.horologist.auth.provider.google.activity.WearAuthActivity
import kotlinx.coroutines.coroutineScope

class WearCredentialManager(
    val credentialManager: CredentialManager,
    val wearProviders: List<SuspendingCredentialProvider> = listOf(),
) : CredentialManager by credentialManager {
    private suspend fun availableProviders(): List<SuspendingCredentialProvider> = wearProviders.filter {
        it.isAvailableOnDevice()
    }

    override suspend fun clearCredentialState(request: ClearCredentialStateRequest) {
        availableProviders().forEach {
            try {
                it.onClearCredential(request)
            } catch (ce: ClearCredentialUnsupportedException) {
                // expected
            }
        }

        if (useCredentialManager) {
            try {
                credentialManager.clearCredentialState(request)
            } catch (ce: ClearCredentialUnsupportedException) {
                // expected
            }
        }
    }

    // TODO update
    private val useCredentialManager: Boolean = false

    override suspend fun getCredential(
        context: Context,
        request: GetCredentialRequest,
    ): GetCredentialResponse {
        if (useCredentialManager) {
            // TODO handle cancel and fallback?
            return credentialManager.getCredential(context, request)
        }

        val relevantProviders = availableProviders()

        val result = getExistingCredentialResponse(relevantProviders, context, request)

        if (result != null) {
            return result
        }

        // TODO work out when to call

        return getCredentialFromActivity(context, request)
    }

    private suspend fun getCredentialFromActivity(
        context: Context,
        request: GetCredentialRequest,
    ): GetCredentialResponse {
        return coroutineScope {
            val receiver = CredentialResponseReceiver()

            context.startActivity(
                Intent(context, WearAuthActivity::class.java).apply {
                    putExtra(
                        CredentialResponseReceiver.Request,
                        request.toBundle(),
                    )
                    putExtra(CredentialResponseReceiver.Receiver, receiver.resultReceiver)
                },
            )

            receiver.await()
        }
    }

    private suspend fun getExistingCredentialResponse(
        relevantProviders: List<SuspendingCredentialProvider>,
        context: Context,
        request: GetCredentialRequest,
    ): GetCredentialResponse? {
        relevantProviders.forEach { provider ->
            try {
                return provider.getExistingCredential(context = context, request = request)
            } catch (ce: NoCredentialException) {
                // expected
            } catch (ce: GetCredentialUnsupportedException) {
                // expected
            }
        }
        return null
    }

    inline fun <reified T : SuspendingCredentialProvider> get(): T {
        return wearProviders.first { it is T } as T
    }

    interface Factory {
        val credentialManager: WearCredentialManager
    }
}
