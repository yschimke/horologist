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

package com.google.android.horologist.mediasample.data.api

import com.google.android.horologist.mediasample.data.api.model.CatalogApiModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Provider

class KtorUampService(private val clientFactory: dagger.Lazy<HttpClient>) : UampService {
    val client: HttpClient by lazy { clientFactory.get() }

    override suspend fun catalog(): CatalogApiModel {
        println("catalog")
        return withContext(Dispatchers.IO) { client.get("catalog.json").body() }
    }
}