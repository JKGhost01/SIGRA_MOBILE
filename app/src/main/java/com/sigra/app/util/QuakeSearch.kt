package com.sigra.app.util

import com.sigra.app.data.SismoEvent
import java.text.Normalizer
import java.util.Locale

private val accents = Regex("\\p{M}+")
private val whitespace = Regex("\\s+")

private fun normalized(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(accents, "").lowercase(Locale.ROOT)

fun SismoEvent.matchesSearch(query: String): Boolean {
    val terms = normalized(query).trim().split(whitespace).filter { it.isNotEmpty() }
    if (terms.isEmpty()) return true
    val text = normalized("$ubicacion $fuente ${agency.orEmpty()}")
    return terms.all { it in text }
}
