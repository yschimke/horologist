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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.android.horologist.auth.sample.screens.main.MainScreen
import com.google.android.horologist.compose.layout.AppScaffold
import com.google.android.horologist.compose.nav.SwipeDismissableNavHost
import com.google.android.horologist.compose.nav.composable

@Composable
fun WearApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberSwipeDismissableNavController(),
) {
    AppScaffold(modifier = modifier) {
        SwipeDismissableNavHost(
            startDestination = MainScreen,
            navController = navController,
        ) {
            composable<MainScreen> {
                MainScreen()
            }
        }
    }
}
