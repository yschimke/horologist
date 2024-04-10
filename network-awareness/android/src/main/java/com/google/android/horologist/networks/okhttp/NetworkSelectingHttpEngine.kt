/*
 * Copyright 2024 The Android Open Source Project
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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.google.android.horologist.networks.okhttp

import android.net.http.BidirectionalStream
import android.net.http.HttpEngine
import android.net.http.UrlRequest
import android.os.Build
import androidx.annotation.RequiresExtension
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandlerFactory
import java.util.concurrent.Executor

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
class NetworkSelectingHttpEngine(private val delegate: HttpEngine,): HttpEngine() {
    override fun shutdown() {
        delegate.shutdown()
    }

    override fun openConnection(url: URL): URLConnection {
        return delegate.openConnection(url)
    }

    override fun createUrlStreamHandlerFactory(): URLStreamHandlerFactory {
        return delegate.createUrlStreamHandlerFactory()
    }

    override fun newUrlRequestBuilder(
        url: String,
        executor: Executor,
        callback: UrlRequest.Callback
    ): UrlRequest.Builder {
        return delegate.newUrlRequestBuilder(url, executor, callback)
    }

    override fun newBidirectionalStreamBuilder(
        url: String,
        executor: Executor,
        callback: BidirectionalStream.Callback
    ): BidirectionalStream.Builder {
        return delegate.newBidirectionalStreamBuilder(url, executor, callback)
    }
}