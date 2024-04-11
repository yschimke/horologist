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

import android.net.Network
import android.net.http.BidirectionalStream
import android.net.http.HttpEngine
import android.net.http.UploadDataProvider
import android.net.http.UrlRequest
import android.os.Build
import androidx.annotation.RequiresExtension
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.rules.NetworkingRulesEngine
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandlerFactory
import java.util.concurrent.Executor

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
class NetworkSelectingHttpEngine(
    private val delegate: HttpEngine,
    private val requestType: RequestType,
    private val networkingRulesEngine: NetworkingRulesEngine
) : HttpEngine() {
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
        return UrlRequestBuilderWrapper(delegate.newUrlRequestBuilder(url, executor, callback))
    }

    override fun newBidirectionalStreamBuilder(
        url: String,
        executor: Executor,
        callback: BidirectionalStream.Callback
    ): BidirectionalStream.Builder {
        return delegate.newBidirectionalStreamBuilder(url, executor, callback)
    }

    inner class UrlRequestBuilderWrapper(private val delegate: UrlRequest.Builder) :
        UrlRequest.Builder() {
        override fun setHttpMethod(method: String): UrlRequest.Builder {
            return delegate.setHttpMethod(method)
        }

        override fun addHeader(header: String, value: String): UrlRequest.Builder {
            return delegate.addHeader(header, value)
        }

        override fun setCacheDisabled(disableCache: Boolean): UrlRequest.Builder {
            return delegate.setCacheDisabled(disableCache)
        }

        override fun setPriority(priority: Int): UrlRequest.Builder {
            return delegate.setPriority(priority)
        }

        override fun setUploadDataProvider(
            uploadDataProvider: UploadDataProvider,
            executor: Executor
        ): UrlRequest.Builder {
            return delegate.setUploadDataProvider(uploadDataProvider, executor)
        }

        override fun setDirectExecutorAllowed(allowDirectExecutor: Boolean): UrlRequest.Builder {
            return delegate.setDirectExecutorAllowed(allowDirectExecutor)
        }

        override fun bindToNetwork(network: Network?): UrlRequest.Builder {
            // ignore overrides for now
            return delegate
        }

        override fun setTrafficStatsTag(tag: Int): UrlRequest.Builder {
            return delegate.setTrafficStatsTag(tag)
        }

        override fun setTrafficStatsUid(uid: Int): UrlRequest.Builder {
            return delegate.setTrafficStatsUid(uid)
        }

        override fun build(): UrlRequest {
            val network = networkingRulesEngine.preferredNetwork(requestType)

            println("Binding to $network")
            delegate.bindToNetwork(network?.network)

            return delegate.build()
        }
    }
}