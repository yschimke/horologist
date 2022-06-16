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

import com.google.android.horologist.fitness.FitnessActivitiesTileRenderer.TileState
import com.google.android.horologist.tiles.ExperimentalHorologistTilesApi
import com.google.android.horologist.tiles.render.RendererPreviewTileService

class FitnessActivitiesTileService : RendererPreviewTileService<TileState, Unit, FitnessActivitiesTileRenderer>() {
    override fun createTileRenderer() = FitnessActivitiesTileRenderer(this)

    override suspend fun createResourcesInput() = Unit

    override suspend fun createTileState(): TileState = TileState(0)
}
