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

package com.google.android.horologist.mediasample.sc
import com.google.android.horologist.mediasample.sc.model.Playlist
import com.google.android.horologist.mediasample.sc.model.Stream
import com.google.android.horologist.mediasample.sc.model.Waveform
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface SoundcloudService {
    @GET("users/{user}/playlists")
    suspend fun playlists(
        @Path("user") user: Int,
    ): List<Playlist>

    @GET
    suspend fun stream(
        @Url url: String,
    ): Stream

    @GET
    suspend fun waveform(
        @Url url: String,
    ): Waveform
    companion object {
        const val BaseUrl = "https://api.soundcloud.com/"
    }
}
