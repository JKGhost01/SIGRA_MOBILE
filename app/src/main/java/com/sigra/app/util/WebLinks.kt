package com.sigra.app.util

import java.net.URI

fun webUrl(value: String?, baseUrl: String): String? = runCatching {
    if (value.isNullOrBlank()) return@runCatching null
    val uri = URI(baseUrl).resolve(value.trim())
    uri.takeIf {
        it.scheme in listOf("http", "https") && !it.host.isNullOrBlank() && it.userInfo == null
    }?.toASCIIString()
}.getOrNull()
