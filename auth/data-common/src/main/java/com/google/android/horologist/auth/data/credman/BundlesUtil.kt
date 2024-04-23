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

package com.google.android.horologist.auth.data.credman

import android.os.Bundle
import com.google.protobuf.ByteString

// https://github.com/androidx/androidx/blob/45f66422685d4166a16549379e734d0f930cbd9f/health/health-services-client/src/main/java/androidx/health/services/client/data/BundlesUtil.kt#L50
/** Utility methods for working with Bundles. */
internal object BundlesUtil {

    @JvmStatic
    internal fun Bundle.toProto(): CredentialProto.Bundle {
        val builder = CredentialProto.Bundle.newBuilder()

        for (key in keySet()) {
            @Suppress("DEPRECATION")
            when (val value = get(key)) {
                is Boolean -> builder.putBools(key, value)
                is String -> builder.putStrings(key, value)
                is Int -> builder.putInts(key, value)
                is Long -> builder.putLongs(key, value)
                is Float -> builder.putFloats(key, value)
                is Double -> builder.putDoubles(key, value)
                is Byte -> builder.putBytes(key, value.toInt())
                is ByteArray -> builder.putByteArrays(key, ByteString.copyFrom(value))
                is Bundle -> if (value != this) builder.putBundles(key, value.toProto())
            }
        }

        return builder.build()
    }

    @JvmStatic
    internal fun CredentialProto.Bundle.fromProto(): Bundle {
        val bundle = Bundle()

        boolsMap.forEach { bundle.putBoolean(it.key, it.value) }
        stringsMap.forEach { bundle.putString(it.key, it.value) }
        intsMap.forEach { bundle.putInt(it.key, it.value) }
        longsMap.forEach { bundle.putLong(it.key, it.value) }
        floatsMap.forEach { bundle.putFloat(it.key, it.value) }
        doublesMap.forEach { bundle.putDouble(it.key, it.value) }
        bytesMap.forEach { bundle.putByte(it.key, it.value.toByte()) }
        byteArraysMap.forEach { bundle.putByteArray(it.key, it.value.toByteArray()) }
        bundlesMap.forEach { bundle.putBundle(it.key, it.value.fromProto()) }

        return bundle
    }
}