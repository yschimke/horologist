/*
 * Copyright 2023 The Android Open Source Project
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

package com.google.android.horologist.auth.data.phone.tokenshare.credman

import androidx.credentials.Credential
import androidx.datastore.core.DataStore
import com.google.android.horologist.auth.data.credman.CredentialSerializer
import com.google.android.horologist.auth.data.credman.CredentialSerializer.None
import com.google.android.horologist.auth.data.phone.tokenshare.WritableTokenBundleRepository
import com.google.android.horologist.auth.data.phone.tokenshare.credman.CredentialRepositoryImpl.Companion.DEFAULT_TOKEN_BUNDLE_KEY
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.data.WearableApiAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Default implementation for [WritableTokenBundleRepository].
 *
 * If multiple [token bundles][T] are required to be shared, specify a [key] in
 * order to identify each one, otherwise the same [default][DEFAULT_TOKEN_BUNDLE_KEY] key
 * will be used and only a single token bundle will be persisted.
 *
 * @sample com.google.android.horologist.auth.sample.MainActivity
 */
public open class CredentialRepositoryImpl(
    private val registry: WearDataLayerRegistry,
    private val coroutineScope: CoroutineScope,
) : WritableTokenBundleRepository<Credential?> {
    override val flow: Flow<Credential?>
        get() = registry.protoFlow(TargetNodeId.ThisNodeId, CredentialSerializer, buildPath(DEFAULT_TOKEN_BUNDLE_KEY)).map {
            if (it.type == None.type) {
                null
            } else {
                it
            }
        }

    override suspend fun update(tokenBundle: Credential?) {
        getDataStore()?.updateData { tokenBundle ?: None }
    }

    override suspend fun isAvailable(): Boolean =
        WearableApiAvailability.isAvailable(registry.dataClient)

    private suspend fun getDataStore(): DataStore<Credential>? =
        if (isAvailable()) {
            registry.protoDataStore(
                path = buildPath(DEFAULT_TOKEN_BUNDLE_KEY),
                coroutineScope = coroutineScope,
                serializer = CredentialSerializer,
            )
        } else {
            null
        }

    private fun buildPath(key: String) =
        if (key.startsWith("/")) {
            key
        } else {
            "/$key"
        }

    companion object {
        public const val DEFAULT_TOKEN_BUNDLE_KEY = "/horologist_credentials"
    }
}
