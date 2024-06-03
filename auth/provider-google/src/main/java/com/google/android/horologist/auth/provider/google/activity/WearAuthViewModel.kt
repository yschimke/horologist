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

package com.google.android.horologist.auth.provider.google.activity

import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.android.horologist.auth.provider.google.SuspendingCredentialProvider.MenuChip
import com.google.android.horologist.auth.provider.google.WearCredentialManager

class WearAuthViewModel(
    val request: GetCredentialRequest,
    val wearCredentialManager: WearCredentialManager,
) : ViewModel() {
    fun supportedDestinations(
        request: GetCredentialRequest,
        onNavigate: (Any) -> Unit,
    ): List<MenuChip> {
        return wearCredentialManager.wearProviders.flatMap {
            it.supportedRoutes(request, onNavigate = onNavigate)
        }
    }

    public class Factory(val request: GetCredentialRequest) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            check(modelClass == WearAuthViewModel::class.java)

            return WearAuthViewModel(
                request = request,
                extras.wearCredentialManager,
            ) as T
        }
    }
}

val CreationExtras.wearCredentialManager: WearCredentialManager
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WearCredentialManager.Factory).credentialManager
