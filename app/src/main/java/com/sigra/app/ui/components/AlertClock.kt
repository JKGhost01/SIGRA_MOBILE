package com.sigra.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun rememberAlertTime(uuid: String): Long {
    var now by remember(uuid) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(uuid) {
        while (isActive) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    return now
}
