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

package com.google.android.horologist.auth.sample

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.android.horologist.auth.data.phone.tokenshare.credman.CredentialRepositoryImpl
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.horologist.datalayer.phone.PhoneDataLayerAppHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Singleton
    @Provides
    fun registry(
        @ApplicationContext context: Context,
        coroutineScope: CoroutineScope,
    ) = WearDataLayerRegistry.fromContext(
        application = context,
        coroutineScope = coroutineScope,
    )

    @Singleton
    @Provides
    fun tokenBundleRepositoryCustomKey(
        registry: WearDataLayerRegistry,
        coroutineScope: CoroutineScope,
    ) = CredentialRepositoryImpl(
        registry = registry,
        coroutineScope = coroutineScope,
    )

    @Singleton
    @Provides
    fun phoneDataLayerAppHelper(
        @ApplicationContext context: Context,
        registry: WearDataLayerRegistry,
    ) = PhoneDataLayerAppHelper(
        context = context,
        registry = registry,
    )

    @Singleton
    @Provides
    fun credentialManager(
        @ApplicationContext context: Context,
    ): CredentialManager {
        return CredentialManager.create(context)
    }
}
