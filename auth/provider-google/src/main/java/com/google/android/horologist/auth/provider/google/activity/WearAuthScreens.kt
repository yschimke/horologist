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

import androidx.compose.runtime.Composable
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.horologist.compose.layout.AppScaffold
import com.google.android.horologist.compose.nav.SwipeDismissableNavHost
import com.google.android.horologist.compose.nav.composable
import kotlinx.serialization.Serializable

@Composable
fun WearAuthScreens(
    onResult: (Result<GetCredentialResponse>) -> Unit,
    request: GetCredentialRequest,
) {
    val viewModel: WearAuthViewModel = viewModel(factory = WearAuthViewModel.Factory(request))

    val navController = rememberSwipeDismissableNavController()

    val supportedDestinations = viewModel.supportedDestinations(request, onNavigate = {
        navController.navigate(it)
    })

    val startDestination = when (supportedDestinations.size) {
        0 -> null
        1 -> (supportedDestinations.first().route)
        else -> MenuScreen
    }

    if (startDestination == null) {
        onResult(Result.failure(GetCredentialUnsupportedException()))
    } else {
        AppScaffold {
            SwipeDismissableNavHost(
                startDestination = startDestination,
                navController = navController,
            ) {
                composable<MenuScreen> {
                    WearAuthMenuScreen(
                        supportedDestinations = supportedDestinations,
                    )
                }
                viewModel.wearCredentialManager.wearProviders.forEach {
                    with(it) {
                        defineRoutes(navController, onResult)
                    }
                }
            }
        }
    }
}

@Serializable
data object MenuScreen
