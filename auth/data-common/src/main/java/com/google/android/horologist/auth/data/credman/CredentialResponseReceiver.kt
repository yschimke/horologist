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
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.os.ResultReceiver
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import kotlinx.coroutines.CompletableDeferred
import java.util.UUID

internal class CredentialResponseReceiver {
    private val deferred = CompletableDeferred<GetCredentialResponse>()

    val resultReceiver = object : ResultReceiver(
        Handler(Looper.getMainLooper()),
    ) {
        public override fun onReceiveResult(
            resultCode: Int,
            resultData: Bundle,
        ) {
            try {
                if (resultData.keySet().contains(ResultType)) {
                    val credential = CustomCredential(
                        resultData.getString(ResultType)!!,
                        resultData.getBundle(ResultData)!!,
                    )
                    println("complete $credential")
                    deferred.complete(GetCredentialResponse(credential))
                } else {
                    val exceptionType = resultData.getString(ExceptionType)
                    val exceptionMessage = resultData.getString(ExceptionMessage)

                    throw createCredentialExceptionTypeToException(exceptionType, exceptionMessage)
                }
            } catch (e: Exception) {
                println("complete $e")
                e.printStackTrace()
                deferred.completeExceptionally(e)
            }
        }
    }

    // From https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:credentials/credentials-play-services-auth/src/main/java/androidx/credentials/playservices/controllers/CredentialProviderBaseController.kt
    fun toParcelable(): Parcelable {
        val parcel: Parcel = Parcel.obtain()
        resultReceiver.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val ipcFriendly = ResultReceiver.CREATOR.createFromParcel(parcel)
        parcel.recycle()
        return ipcFriendly
    }

    internal fun createCredentialExceptionTypeToException(typeName: String?, msg: String?): GetCredentialException {
        return when (typeName) {
            GetCredentialCancellationException::class.java.name -> GetCredentialCancellationException(
                msg,
            )

            NoCredentialException::class.java.name -> NoCredentialException(msg)
            GetCredentialInterruptedException::class.java.name -> GetCredentialInterruptedException(
                msg,
            )

            else -> GetCredentialUnknownException(msg)
        }
    }

    suspend fun await(): GetCredentialResponse {
        return deferred.await()
    }

    internal companion object {
        const val Request = "credman.request"
        const val ResultType = "credman.result.type"
        const val ResultData = "credman.result.data"
        const val ExceptionType = "credman.exception.type"
        const val ExceptionMessage = "credman.exception.message"
        const val Receiver = "credman.receiver"

        public fun Result<GetCredentialResponse>.toBundle(): Bundle = Bundle().also { bundle ->
            if (this.isSuccess) {
                val credential = this.getOrThrow().credential
                bundle.putString(ResultType, credential.type)
                bundle.putBundle(ResultData, credential.data)
            } else {
                val throwable = this.exceptionOrNull()
                bundle.putString(ExceptionType, throwable?.javaClass?.name)
                bundle.putString(ExceptionMessage, throwable?.message)
            }
        }

        fun Bundle.toGetCredentialRequest(): GetCredentialRequest {
            val uuid = this.getString("uuid")
            return BigTerribleHack[uuid]!!
        }

        fun GetCredentialRequest.toBundle(): Bundle {
            // Too much work to implement bundle serialization here
            val uuid = UUID.randomUUID().toString()
            BigTerribleHack[uuid] = this
            return Bundle().apply {
                putString("uuid", uuid)
            }
        }

        private val BigTerribleHack = mutableMapOf<String, GetCredentialRequest>()
    }
}
