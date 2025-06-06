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

package com.google.android.horologist.mediasample.sc.model
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
@JsonClass(generateAdapter = true)
data class Playlist(
    @Json(name = "artwork_url")
    val artworkUrl: Any?,
    @Json(name = "created_at")
    val createdAt: String?,
    @Json(name = "description")
    val description: Any?,
    @Json(name = "downloadable")
    val downloadable: Any?,
    @Json(name = "duration")
    val duration: Int?,
    @Json(name = "ean")
    val ean: Any?,
    @Json(name = "embeddable_by")
    val embeddableBy: String?,
    @Json(name = "genre")
    val genre: String?,
    @Json(name = "id")
    val id: Int?,
    @Json(name = "kind")
    val kind: String?,
    @Json(name = "label")
    val label: Any?,
    @Json(name = "label_id")
    val labelId: Any?,
    @Json(name = "label_name")
    val labelName: Any?,
    @Json(name = "last_modified")
    val lastModified: String?,
    @Json(name = "license")
    val license: String?,
    @Json(name = "likes_count")
    val likesCount: Int?,
    @Json(name = "permalink")
    val permalink: String?,
    @Json(name = "permalink_url")
    val permalinkUrl: String?,
    @Json(name = "playlist_type")
    val playlistType: String?,
    @Json(name = "purchase_title")
    val purchaseTitle: Any?,
    @Json(name = "purchase_url")
    val purchaseUrl: Any?,
    @Json(name = "release")
    val release: Any?,
    @Json(name = "release_day")
    val releaseDay: Any?,
    @Json(name = "release_month")
    val releaseMonth: Any?,
    @Json(name = "release_year")
    val releaseYear: Any?,
    @Json(name = "sharing")
    val sharing: String?,
    @Json(name = "streamable")
    val streamable: Boolean?,
    @Json(name = "tag_list")
    val tagList: String?,
    @Json(name = "tags")
    val tags: String?,
    @Json(name = "title")
    val title: String?,
    @Json(name = "track_count")
    val trackCount: Int?,
    @Json(name = "tracks")
    val tracks: List<Track>,
    @Json(name = "tracks_uri")
    val tracksUri: String?,
    @Json(name = "type")
    val type: String?,
    @Json(name = "uri")
    val uri: String?,
    @Json(name = "user")
    val user: User,
    @Json(name = "user_id")
    val userId: Int?,
)
