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
data class User(
    @Json(name = "avatar_url")
    val avatarUrl: String?,
    @Json(name = "city")
    val city: String?,
    @Json(name = "comments_count")
    val commentsCount: Int?,
    @Json(name = "country")
    val country: Any?,
    @Json(name = "created_at")
    val createdAt: String?,
    @Json(name = "description")
    val description: String?,
    @Json(name = "discogs_name")
    val discogsName: Any?,
    @Json(name = "first_name")
    val firstName: String?,
    @Json(name = "followers_count")
    val followersCount: Int?,
    @Json(name = "followings_count")
    val followingsCount: Int?,
    @Json(name = "full_name")
    val fullName: String?,
    @Json(name = "id")
    val id: Int?,
    @Json(name = "kind")
    val kind: String?,
    @Json(name = "last_modified")
    val lastModified: String?,
    @Json(name = "last_name")
    val lastName: String?,
    @Json(name = "likes_count")
    val likesCount: Int?,
    @Json(name = "myspace_name")
    val myspaceName: String?,
    @Json(name = "online")
    val online: Boolean?,
    @Json(name = "permalink")
    val permalink: String?,
    @Json(name = "permalink_url")
    val permalinkUrl: String?,
    @Json(name = "plan")
    val plan: String?,
    @Json(name = "playlist_count")
    val playlistCount: Int?,
    @Json(name = "public_favorites_count")
    val publicFavoritesCount: Int?,
    @Json(name = "reposts_count")
    val repostsCount: Int?,
    @Json(name = "subscriptions")
    val subscriptions: List<Subscription>?,
    @Json(name = "track_count")
    val trackCount: Int?,
    @Json(name = "uri")
    val uri: String?,
    @Json(name = "username")
    val username: String?,
    @Json(name = "website")
    val website: String?,
    @Json(name = "website_title")
    val websiteTitle: String?,
)
