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

package com.google.android.horologist.mediasample.data.repository

import com.google.android.horologist.mediasample.data.datasource.PlaylistRemoteDataSource
import com.google.android.horologist.mediasample.domain.PlaylistRepository
import com.google.android.horologist.mediasample.domain.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class PlaylistRepositoryImpl(
    private val playlistRemoteDataSource: PlaylistRemoteDataSource
) : PlaylistRepository {

    // temporary implementation of cache
    private lateinit var playlistCache: List<Playlist>

    override suspend fun getPlaylist(id: String): Playlist? =
        playlistCache.firstOrNull { it.id == id }

    override fun getPlaylists(): Flow<List<Playlist>> =
        playlistRemoteDataSource.getPlaylists().onEach {
            playlistCache = it
        }
}
