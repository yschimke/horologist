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

package com.google.android.horologist.compose.material

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults.listTextPadding
import com.google.android.horologist.compose.material.ListHeaderDefaults.firstItemPadding
import com.google.android.horologist.compose.material.util.DECORATIVE_ELEMENT_CONTENT_DESCRIPTION
import com.google.android.horologist.images.base.paintable.ImageVectorPaintable.Companion.asPaintable

/**
 * A primary title heading to group and identify items.
 */
@ExperimentalHorologistApi
@Composable
public fun Title(
    @StringRes textId: Int,
    modifier: Modifier = Modifier,
) {
    Title(
        text = stringResource(id = textId),
        modifier = modifier,
    )
}

/**
 * A primary title heading to group and identify items.
 */
@ExperimentalHorologistApi
@Composable
public fun Title(
    text: String,
    modifier: Modifier = Modifier,
) {
    ResponsiveListHeader(modifier = modifier, contentPadding = firstItemPadding()) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .listTextPadding(),
            color = MaterialTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 3,
        )
    }
}

/**
 * A secondary title heading to group and identify items with optional icon.
 */
@ExperimentalHorologistApi
@Composable
public fun SecondaryTitle(
    @StringRes textId: Int,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconSize: Dp = 24.dp,
) {
    SecondaryTitle(
        text = stringResource(id = textId),
        modifier = modifier,
        icon = icon,
        iconSize = iconSize,
    )
}

/**
 * A secondary title heading to group and identify items with optional icon.
 */
@ExperimentalHorologistApi
@Composable
public fun SecondaryTitle(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colors.onBackground,
    iconSize: Dp = 24.dp,
) {
    ResponsiveListHeader(modifier = modifier) {
        icon?.let {
            Icon(
                paintable = icon.asPaintable(),
                contentDescription = DECORATIVE_ELEMENT_CONTENT_DESCRIPTION,
                modifier = Modifier
                    .size(iconSize)
                    .align(Alignment.CenterVertically),
                tint = iconTint,
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .listTextPadding()
                .align(Alignment.CenterVertically),
            color = MaterialTheme.colors.onBackground,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            maxLines = 3,
            style = MaterialTheme.typography.caption1,
        )
    }
}
