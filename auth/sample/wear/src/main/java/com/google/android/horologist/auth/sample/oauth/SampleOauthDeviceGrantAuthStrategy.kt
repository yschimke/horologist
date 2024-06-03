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

package com.google.android.horologist.auth.sample.oauth

import com.google.android.horologist.auth.provider.google.devicegrant.OAuthDeviceGrantAuthStrategy
import com.google.android.horologist.auth.sample.BuildConfig

class SampleOauthDeviceGrantAuthStrategy : OAuthDeviceGrantAuthStrategy() {
    override val clientId: String = BuildConfig.OAUTH_DEVICE_GRANT_CLIENT_ID
    override val clientSecret: String = BuildConfig.OAUTH_DEVICE_GRANT_CLIENT_SECRET
}
