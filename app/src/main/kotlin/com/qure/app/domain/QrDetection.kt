package com.qure.app.domain

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

@Immutable
data class QrDetection(
    val rawValue: String,
    val boxInViewPx: Rect,
    val cornersInViewPx: List<Offset>,
    val frameTimestampNs: Long,
) {

    val parsed: ParsedPayload by lazy(LazyThreadSafetyMode.NONE) { UrlParser.parse(rawValue) }
}
