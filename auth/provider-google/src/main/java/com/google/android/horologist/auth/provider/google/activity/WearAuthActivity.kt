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

@file:Suppress("DEPRECATION", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.google.android.horologist.auth.provider.google.activity

import android.os.Bundle
import android.os.ResultReceiver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.os.BundleCompat
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.horologist.auth.data.credman.CredentialResponseReceiver
import com.google.android.horologist.auth.data.credman.CredentialResponseReceiver.Companion.toBundle
import com.google.android.horologist.auth.data.credman.CredentialResponseReceiver.Companion.toGetCredentialRequest

open class WearAuthActivity : ComponentActivity() {
    private var completed: Boolean = false
    private var receiver: ResultReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        receiver = intent.extras?.let {
            BundleCompat.getParcelable(
                it,
                CredentialResponseReceiver.Receiver,
                ResultReceiver::class.java
            )
        }

        val request = intent.extras?.let {
            it.getBundle(CredentialResponseReceiver.Request)?.toGetCredentialRequest()
        }

        if (receiver == null || request == null) {
            finish()
            return
        }

        setContent {
            WearAuthScreens(
                request = request,
                onResult = {
                    receiver!!.send(RESULT_OK, it.toBundle())
                    completed = true
                    finish()
                }
            )
        }
    }

    override fun onDestroy() {
        println("onDestroy $completed")
        if (!completed) {
            println("cancelling")
            receiver?.send(
                RESULT_OK,
                Result.failure<GetCredentialResponse>(GetCredentialCancellationException())
                    .toBundle()
            )
        }
        super.onDestroy()
    }
}
