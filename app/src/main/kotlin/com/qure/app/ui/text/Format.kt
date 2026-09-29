package com.qure.app.ui.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color

fun highlightHost(safeText: String, host: String?, dim: Color, bright: Color): AnnotatedString =
    buildAnnotatedString {

        val h = host?.takeIf { it.isNotBlank() }
        val idx = if (h == null) -1 else safeText.indexOf(h, ignoreCase = true)
        if (h == null || idx < 0) {
            withStyle(SpanStyle(color = bright)) { append(safeText) }
            return@buildAnnotatedString
        }
        withStyle(SpanStyle(color = dim)) { append(safeText.substring(0, idx)) }
        withStyle(SpanStyle(color = bright, fontWeight = FontWeight.Bold)) {
            append(safeText.substring(idx, idx + h.length))
        }
        withStyle(SpanStyle(color = dim)) { append(safeText.substring(idx + h.length)) }
    }

fun middleEllipsis(s: String, max: Int = 96): String =
    if (s.length <= max) s
    else s.take(max / 2 - 1) + "…" + s.takeLast(max / 2 - 1)
