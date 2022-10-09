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

package com.google.android.horologist.figma

import com.google.android.horologist.figma.api.FigmaApi
import com.google.android.horologist.figma.auth.FigmaAuthInterceptor
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

public class ImageFetcher(
    private val figmaApi: FigmaApi
) {
    public suspend fun fetchImages(
        file: String,
        nodes: Set<String>,
        scale: Double = 1.0
    ): Map<String, BufferedImage> {
        return withContext(Dispatchers.IO) {
            val results = figmaApi.images(file = file, ids = nodes.joinToString(","), scale = scale)

            results.images.map { (node, url) ->
                async {
                    val response = figmaApi.imagesFile(url)
                    response.use {
                        val image = ImageIO.read(response.byteStream())
                        node to image
                    }
                }
            }.awaitAll().toMap()
        }
    }

    public fun close() {
        // TODO close the OkHttpClient
    }

    public companion object {
        public fun build(token: String): ImageFetcher {
            val moshi = Moshi.Builder()
                .build()

            val moshiConverterFactory = MoshiConverterFactory.create(moshi)

            val callFactory = OkHttpClient.Builder()
                .addInterceptor(FigmaAuthInterceptor(token))
                .build()

            val retrofit = Retrofit.Builder()
                .addConverterFactory(moshiConverterFactory)
                .baseUrl("https://api.figma.com/")
                .callFactory(callFactory)
                .build()

            val figmaApi = retrofit.create(FigmaApi::class.java)

            return ImageFetcher(figmaApi)
        }
    }
}