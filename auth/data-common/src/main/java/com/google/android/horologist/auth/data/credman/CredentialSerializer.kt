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

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.datastore.core.Serializer
import com.google.android.horologist.auth.data.credman.BundlesUtil.fromProto
import com.google.android.horologist.auth.data.credman.BundlesUtil.toProto
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.io.InputStream
import java.io.OutputStream

object CredentialSerializer : Serializer<Credential> {
    override val defaultValue: Credential
        get() = None

    @SuppressLint("RestrictedApi")
    override suspend fun readFrom(input: InputStream): Credential {
        val proto = CredentialProto.Credential.parseFrom(input)

        val data = proto.data.fromProto()
        return when (proto.type) {
            "", None.type -> None
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> GoogleIdTokenCredential.createFrom(
                data
            )

            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL -> GoogleIdTokenCredential.createFrom(
                data
            )

            PasswordCredential.TYPE_PASSWORD_CREDENTIAL -> PasswordCredential.createFrom(data)

            PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL -> PublicKeyCredential.createFrom(data)

            else -> CustomCredential(proto.type, data)
        }
    }

    override suspend fun writeTo(t: Credential, output: OutputStream) {
        val proto = credential {
            type = t.type.ifEmpty { None.type }
            data = t.data.toProto()
        }

        proto.writeTo(output)
    }

    val None = CustomCredential("None", Bundle.EMPTY)
}
