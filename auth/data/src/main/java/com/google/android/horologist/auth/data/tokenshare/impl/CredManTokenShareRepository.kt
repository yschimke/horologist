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

package com.google.android.horologist.auth.data.tokenshare.impl

import androidx.credentials.Credential
import com.google.android.horologist.auth.data.credman.CredentialSerializer
import com.google.android.horologist.auth.data.credman.CredentialSerializer.None
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import com.google.android.horologist.data.TargetNodeId
import com.google.android.horologist.data.WearDataLayerRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CredManTokenShareRepository(
    private val registry: WearDataLayerRegistry,
    private val node: TargetNodeId,
    private val path: String,
) : TokenBundleRepository<Credential?> {
    override val flow: Flow<Credential?>
        get() = registry.protoFlow(node, CredentialSerializer, path).map {
            if (it.type == None.type) {
                null
            } else {
                it
            }
        }

    public companion object {

        public const val DEFAULT_TOKEN_BUNDLE_KEY = "/horologist_credentials"

        /**
         * Factory method for [TokenBundleRepositoryImpl].
         *
         * If multiple [token bundles][T] are available, specify the [key] of the specific
         * token bundle wished to be retrieved. Otherwise the token bundle stored with the
         * [default][DEFAULT_TOKEN_BUNDLE_KEY] key will be used.
         */
        public fun create(
            registry: WearDataLayerRegistry,
            key: String = DEFAULT_TOKEN_BUNDLE_KEY,
            node: TargetNodeId = TargetNodeId.PairedPhone,
        ): TokenBundleRepository<Credential?> = CredManTokenShareRepository(
            registry = registry,
            path = buildPath(key),
            node = node,
        )

        private fun buildPath(key: String) =
            if (key.startsWith("/")) {
                key
            } else {
                "/$key"
            }
    }
}
