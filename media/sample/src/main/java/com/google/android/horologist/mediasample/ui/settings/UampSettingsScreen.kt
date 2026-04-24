/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.android.horologist.mediasample.ui.settings

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import com.google.android.horologist.media.ui.navigation.NavigationScreen
import com.google.android.horologist.mediasample.R
import com.google.android.horologist.mediasample.ui.navigation.UampNavigationScreen.DeveloperOptions
import com.google.android.horologist.mediasample.ui.navigation.UampNavigationScreen.GoogleSignInScreen
import com.google.android.horologist.mediasample.ui.navigation.UampNavigationScreen.GoogleSignOutScreen

@Composable
fun UampSettingsScreen(
    viewModel: SettingsScreenViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

    UampSettingsScreen(
        state = screenState,
        modifier = modifier,
        onLoginClick = { navController.navigate(GoogleSignInScreen) },
        onLogoutClick = {
            navController.navigate(GoogleSignOutScreen) {
                popUpTo<NavigationScreen.Player>()
            }
        },
        onGuestModeChange = viewModel::setGuestMode,
        onDeveloperOptionsClick = { navController.navigate(DeveloperOptions) },
        onShowLicensesClick = {
            activity?.startActivity(
                Intent().apply {
                    setPackage(activity.packageName)
                    setAction("com.google.wear.ACTION_SHOW_LICENSE")
                },
            )
        },
    )
}

@Composable
fun UampSettingsScreen(
    state: SettingsScreenState,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onGuestModeChange: (Boolean) -> Unit,
    onDeveloperOptionsClick: () -> Unit,
    onShowLicensesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transformationSpec = rememberTransformationSpec()
    val columnState = rememberTransformingLazyColumnState()
    val contentPadding = rememberResponsiveColumnPadding(
        first = ColumnItemType.ListHeader,
        last = ColumnItemType.Button,
    )

    ScreenScaffold(
        scrollState = columnState,
        modifier = modifier,
        contentPadding = contentPadding,
    ) { padding ->
        TransformingLazyColumn(state = columnState, contentPadding = padding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding)
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(text = stringResource(id = R.string.sample_settings))
                }
            }
            item {
                if (state.authUser == null) {
                    Button(
                        onClick = onLoginClick,
                        enabled = !state.guestMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        label = { Text(stringResource(id = R.string.login)) },
                    )
                } else {
                    Button(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        label = { Text(stringResource(id = R.string.logout)) },
                    )
                }
            }
            item {
                CheckboxButton(
                    checked = state.guestMode,
                    onCheckedChange = onGuestModeChange,
                    enabled = state.writable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    label = { Text(stringResource(id = R.string.sample_guest_mode)) },
                )
            }
            if (state.showDeveloperOptions) {
                item {
                    Button(
                        onClick = onDeveloperOptionsClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.DataObject,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(id = R.string.sample_developer_options)) },
                    )
                }
            }
            item {
                Button(
                    onClick = onShowLicensesClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    label = { Text(stringResource(id = R.string.show_licenses)) },
                )
            }
        }
    }
}
