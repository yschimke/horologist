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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.onRectChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound
import com.google.android.horologist.sample.R

class ScratchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val targetSize = 20.dp
        Row(modifier = Modifier.fillMaxWidth().height(targetSize * 4).onRectChanged {
        }, horizontalArrangement = Arrangement.Center) {
            Image(
                painterResource(R.drawable.ic_power),
                contentDescription = null,
                modifier = Modifier.scale(4f)
            )
            Spacer(modifier = Modifier.size(40.dp))
            Image(
                painterResource(R.drawable.ic_power),
                contentDescription = null,
                modifier = Modifier.size(targetSize)
            )
            Image(
                painterResource(R.drawable.ic_power),
                contentDescription = null,
                modifier = Modifier.size(targetSize * 4f)
            )
//            Image(
//                painterResource(R.drawable.ic_power),
//                contentDescription = null,
//                modifier = Modifier.scale(4f)
//            )
//            Image(
//                painterResource(R.drawable.ic_power),
//                contentDescription = null,
//                modifier = Modifier.size()
//            )
        }

    }
}

@WearPreviewLargeRound
@Composable
fun WearAppPreview() {
    WearApp()
}
