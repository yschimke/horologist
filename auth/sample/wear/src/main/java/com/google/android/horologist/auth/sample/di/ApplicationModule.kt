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

@file:Suppress("DEPRECATION")

package com.google.android.horologist.auth.sample.di

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.horologist.auth.data.credman.LocalCredentialRepository
import com.google.android.horologist.auth.data.tokenshare.impl.CredManTokenShareRepository
import com.google.android.horologist.auth.data.watch.oauth.common.impl.google.api.GoogleOAuthService
import com.google.android.horologist.auth.data.watch.oauth.common.impl.google.api.GoogleOAuthServiceFactory
import com.google.android.horologist.auth.data.watch.oauth.devicegrant.impl.DeviceGrantConfigRepositoryDefaultImpl
import com.google.android.horologist.auth.data.watch.oauth.devicegrant.impl.google.DeviceGrantTokenRepositoryGoogleImpl
import com.google.android.horologist.auth.data.watch.oauth.devicegrant.impl.google.DeviceGrantVerificationInfoRepositoryGoogleImpl
import com.google.android.horologist.auth.data.watch.oauth.pkce.impl.PKCEOAuthCodeRepositoryImpl
import com.google.android.horologist.auth.data.watch.oauth.pkce.impl.google.PKCEConfigRepositoryGoogleImpl
import com.google.android.horologist.auth.data.watch.oauth.pkce.impl.google.PKCETokenRepositoryGoogleImpl
import com.google.android.horologist.auth.provider.google.WearCredentialManager
import com.google.android.horologist.auth.provider.google.gsi.GoogleSignInAuthStrategy
import com.google.android.horologist.auth.provider.google.tokensharing.TokenSharingAuthStrategy
import com.google.android.horologist.auth.sample.BuildConfig
import com.google.android.horologist.auth.sample.oauth.SampleOauthPkceAuthStrategy
import com.google.android.horologist.data.WearDataLayerRegistry
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    @Singleton
    @Provides
    fun wearCredentialManager(
        @ApplicationContext context: Context,
        coroutineScope: CoroutineScope,
        okHttpClient: OkHttpClient,
    ): WearCredentialManager {
        val registry = WearDataLayerRegistry.fromContext(context, coroutineScope)
        return WearCredentialManager(
            CredentialManager.create(context),
            listOf(
                TokenSharingAuthStrategy(CredManTokenShareRepository.create(registry)),
                GoogleSignInAuthStrategy(context),
                SampleOauthPkceAuthStrategy(context, okHttpClient),
            ),
        )
    }

    @Singleton
    @Provides
    fun credentialManager(
        credentialManager: WearCredentialManager,
    ): CredentialManager = credentialManager

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
    fun googleSignInClient(
        @ApplicationContext context: Context,
    ): GoogleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(BuildConfig.GSI_CLIENT_ID)
            .build(),
    )

    @Singleton
    @Provides
    fun googleOAuthService(okHttpClient: OkHttpClient, moshi: Moshi) = GoogleOAuthServiceFactory(
        okHttpClient = okHttpClient,
        moshi = moshi,
    ).get()

    @Singleton
    @Provides
    fun deviceGrantConfigRepository() = DeviceGrantConfigRepositoryDefaultImpl(
        clientId = BuildConfig.OAUTH_DEVICE_GRANT_CLIENT_ID,
        clientSecret = BuildConfig.OAUTH_DEVICE_GRANT_CLIENT_SECRET,
    )

    @Singleton
    @Provides
    fun deviceGrantVerificationInfoRepository(
        googleOAuthService: GoogleOAuthService,
    ) = DeviceGrantVerificationInfoRepositoryGoogleImpl(
        googleOAuthService = googleOAuthService,
    )

    @Singleton
    @Provides
    fun deviceGrantTokenRepository(
        @ApplicationContext context: Context,
        googleOAuthService: GoogleOAuthService,
    ) = DeviceGrantTokenRepositoryGoogleImpl(
        context = context,
        googleOAuthService = googleOAuthService,
    )

    @Singleton
    @Provides
    fun pkceConfigRepository() = PKCEConfigRepositoryGoogleImpl(
        clientId = BuildConfig.OAUTH_PKCE_CLIENT_ID,
        clientSecret = BuildConfig.OAUTH_PKCE_CLIENT_SECRET,
    )

    @Singleton
    @Provides
    fun pkceOAuthCodeRepository(
        @ApplicationContext context: Context,
    ) = PKCEOAuthCodeRepositoryImpl(
        context,
    )

    @Singleton
    @Provides
    fun pkceTokenRepository(
        googleOAuthService: GoogleOAuthService,
    ) = PKCETokenRepositoryGoogleImpl(
        googleOAuthService,
    )

    @Singleton
    @Provides
    fun mainCredentialRepository(
        @ApplicationContext context: Context,
    ) = LocalCredentialRepository(context)
}
