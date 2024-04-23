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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.google.android.horologist.auth.data.credman

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.google.android.horologist.auth.data.credman.CredentialSerializer.None
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.credentialStore: DataStore<Credential> by dataStore(
    fileName = "credentials.pb",
    serializer = CredentialSerializer,
)

class CredentialRepository(
    private val context: Context,
) : TokenBundleRepository<Credential?> {
    val credentialStore = context.credentialStore

    suspend fun signOut() {
        credentialStore.updateData { None }
    }

    suspend fun store(credential: Credential) {
        credentialStore.updateData { credential }
    }

    override val flow: Flow<Credential?>
        get() {
            return credentialStore.data.map { if (it.type == None.type) null else it }
                .map { it?.normalise() }
        }

    companion object {
        fun Credential.normalise(): Credential {
            if (this is CustomCredential) {
                return when (type) {
                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> GoogleIdTokenCredential.createFrom(
                        data
                    )

                    GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL -> GoogleIdTokenCredential.createFrom(
                        data
                    )

                    PasswordCredential.TYPE_PASSWORD_CREDENTIAL -> PasswordCredential.createFrom(
                        data
                    )

                    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL -> PublicKeyCredential.createFrom(
                        data
                    )

                    else -> CustomCredential(type, data)
                }
            }

            return this
        }
    }
}