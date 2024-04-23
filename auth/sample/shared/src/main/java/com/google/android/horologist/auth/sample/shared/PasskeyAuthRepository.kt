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

package com.google.android.horologist.auth.sample.shared

import io.github.ryunen344.webauthn2.json.core.PublicKeyCredentialRequestOptions
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class PasskeyAuthRepository(
    val httpClient: HttpClient
) {
    suspend fun getServerChallenge(): PublicKeyCredentialRequestOptions {
        return httpClient.post("https://enchanting-hexagonal-chauffeur.glitch.me/auth/signinRequest") {
            setBody("{}")
            contentType(ContentType.Application.Json)
            header("X-Requested-With", "XMLHttpRequest")
        }.body()
    }
}