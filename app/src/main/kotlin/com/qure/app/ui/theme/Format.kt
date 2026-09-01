package com.qure.app.ui.theme

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color

/**
 * Renders a URL with the host emphasised and everything else dimmed, so the part that decides
 * where you are actually going is the part the eye lands on.
 *
 * [safeText] must already have been through UrlParser.toDisplayString — this function only styles;
 * it never sanitises.
 */
fun highlightHost(safeText: String, host: String?, dim: Color, bright: Color): AnnotatedString =
    buildAnnotatedString {
        // Local val so the null check smart-casts; the parameter itself never can.
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

/**
 * Truncates in the MIDDLE, never at the end.
 *
 * Ellipsising the tail of a URL is how phishing wins a display: the real destination often sits at
 * the far right (a@b redirect, a deep path, a lookalike suffix), and cutting it off hides exactly
 * the evidence the user needs.
 */
fun middleEllipsis(s: String, max: Int = 96): String =
    if (s.length <= max) s
    else s.take(max / 2 - 1) + "…" + s.takeLast(max / 2 - 1)
