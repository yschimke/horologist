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

@file:OptIn(ExperimentalHorologistTilesApi::class)

package com.google.android.horologist.fitness

import com.google.android.horologist.sample.R
import com.google.android.horologist.tiles.ExperimentalHorologistTilesApi
import com.google.android.horologist.tiles.images.drawableResToImageResource
import com.google.android.horologist.tiles.render.RendererPreviewTileService

class WeeklyGoalTileService : RendererPreviewTileService<WeeklyGoalTileRenderer.TileState, WeeklyGoalTileRenderer.ResourceState, WeeklyGoalTileRenderer>() {
    override fun createTileRenderer() = WeeklyGoalTileRenderer(this)

    override suspend fun createResourcesInput() = WeeklyGoalTileRenderer.ResourceState(
        runsPreview = drawableResToImageResource(R.drawable.runs),
        runsLive = null
    )

    override suspend fun createTileState() = WeeklyGoalTileRenderer.TileState(
        activities = "8 runs this week",
        distance = "68 km total",
    )
}
