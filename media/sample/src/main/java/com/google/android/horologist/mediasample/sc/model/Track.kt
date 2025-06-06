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
data class Track(
    @Json(name = "access")
    val access: String?,
    @Json(name = "artwork_url")
    val artworkUrl: String?,
    @Json(name = "available_country_codes")
    val availableCountryCodes: Any?,
    @Json(name = "bpm")
    val bpm: Any?,
    @Json(name = "comment_count")
    val commentCount: Int?,
    @Json(name = "commentable")
    val commentable: Boolean?,
    @Json(name = "created_at")
    val createdAt: String?,
    @Json(name = "description")
    val description: Any?,
    @Json(name = "download_count")
    val downloadCount: Int?,
    @Json(name = "download_url")
    val downloadUrl: Any?,
    @Json(name = "downloadable")
    val downloadable: Boolean?,
    @Json(name = "duration")
    val duration: Int?,
    @Json(name = "embeddable_by")
    val embeddableBy: String?,
    @Json(name = "favoritings_count")
    val favoritingsCount: Int?,
    @Json(name = "genre")
    val genre: String?,
    @Json(name = "id")
    val id: Int?,
    @Json(name = "isrc")
    val isrc: String?,
    @Json(name = "key_signature")
    val keySignature: Any?,
    @Json(name = "kind")
    val kind: String?,
    @Json(name = "label_name")
    val labelName: String?,
    @Json(name = "license")
    val license: String?,
    @Json(name = "monetization_model")
    val monetizationModel: Any?,
    @Json(name = "permalink_url")
    val permalinkUrl: String?,
    @Json(name = "playback_count")
    val playbackCount: Int?,
    @Json(name = "policy")
    val policy: Any?,
    @Json(name = "purchase_title")
    val purchaseTitle: Any?,
    @Json(name = "purchase_url")
    val purchaseUrl: Any?,
    @Json(name = "release")
    val release: Any?,
    @Json(name = "release_day")
    val releaseDay: Int?,
    @Json(name = "release_month")
    val releaseMonth: Int?,
    @Json(name = "release_year")
    val releaseYear: Int?,
    @Json(name = "reposts_count")
    val repostsCount: Int?,
    @Json(name = "secret_uri")
    val secretUri: Any?,
    @Json(name = "sharing")
    val sharing: String?,
    @Json(name = "stream_url")
    val streamUrl: String?,
    @Json(name = "streamable")
    val streamable: Boolean?,
    @Json(name = "tag_list")
    val tagList: String?,
    @Json(name = "title")
    val title: String?,
    @Json(name = "uri")
    val uri: String?,
    @Json(name = "user")
    val user: User?,
    @Json(name = "user_favorite")
    val userFavorite: Boolean?,
    @Json(name = "user_playback_count")
    val userPlaybackCount: Int?,
    @Json(name = "waveform_url")
    val waveformUrl: String?,
)
