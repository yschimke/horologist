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

package com.google.android.horologist.scratch

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import com.google.android.horologist.composables.DatePicker
import com.google.android.horologist.sample.R

class ScratchActivity : ComponentActivity() {
    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val provider = GoogleFont.Provider(
            providerAuthority = "com.google.android.gms.fonts",
            providerPackage = "com.google.android.gms",
            certificates = R.array.com_google_android_gms_fonts_certs
        )

        val fontName = GoogleFont("Lobster Two")

        val fontFamily = FontFamily(
            Font(googleFont = fontName, fontProvider = provider)
        )

        val typography = Typography(defaultFontFamily = fontFamily)

        setContent {
            MaterialTheme(typography = typography) {
                DatePicker(onDateConfirm = {})
            }
        }
    }
}

@WearPreviewLargeRound
@Composable
fun WearAppPreview() {
    DatePicker(onDateConfirm = {})
}
