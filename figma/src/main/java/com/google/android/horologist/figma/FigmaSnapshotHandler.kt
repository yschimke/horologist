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

import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

public class FigmaSnapshotHandler(
    private val delegate: SnapshotHandler,
    private val token: String? = null,
    private val file: String,
    private val figmaImage: String,
    private val figmaOverlay: String? = null,
) : SnapshotHandler {
    private val figmaApi by lazy { ImageFetcher.build(token!!) }

    override fun close() {
        figmaApi.close()

        delegate.close()
    }

    override fun newFrameHandler(
        snapshot: Snapshot,
        frameCount: Int,
        fps: Int
    ): SnapshotHandler.FrameHandler {
        val delegateFrameHandler = delegate.newFrameHandler(snapshot, frameCount, fps)
        return object : SnapshotHandler.FrameHandler {
            override fun close() {
                delegateFrameHandler.close()
            }

            override fun handle(image: BufferedImage) {
                val figmaExports = runBlocking {
                    figmaApi.fetchImages(
                        file = file,
                        nodes = setOfNotNull(figmaImage, figmaOverlay),
                        scale = 4.0
                    )
                }

                val modifiedImage = concatImages(
                    image,
                    figmaExports[figmaImage],
                    figmaOverlay?.let { figmaExports[it] })

                delegateFrameHandler.handle(modifiedImage)
            }
        }
    }

    private fun concatImages(
        image: BufferedImage,
        figma: BufferedImage?,
        overlay: BufferedImage?,
    ): BufferedImage {
        val images = listOfNotNull(image, figma, overlay)

        val modifiedImage =
            BufferedImage(
                images.size * image.width,
                image.height,
                image.type
            )

        modifiedImage.withGraphics2D {
            var offset = 0
            drawImage(
                image,
                0,
                0,
                image.width,
                image.height,
                0,
                0,
                image.width,
                image.height,
                null
            )
            offset += image.width
            if (figma != null) {
                drawImage(
                    figma,
                    offset,
                    0,
                    offset + image.width,
                    image.height,
                    0,
                    0,
                    figma.width,
                    figma.height,
                    null
                )
                offset += image.width
            }
            if (overlay != null) {
                drawImage(
                    image,
                    offset,
                    0,
                    offset + image.width,
                    image.height,
                    0,
                    0,
                    image.width,
                    image.height,
                    null
                )
                drawImage(
                    overlay,
                    offset,
                    0,
                    offset + image.width,
                    image.height,
                    0,
                    0,
                    overlay.width,
                    overlay.height,
                    null
                )
            }
        }
        return modifiedImage
    }

    private fun BufferedImage.withGraphics2D(fn: Graphics2D.() -> Unit = {}): BufferedImage {
        createGraphics().apply {
            try {
                fn()
            } finally {
                dispose()
            }
        }

        return this
    }
}
