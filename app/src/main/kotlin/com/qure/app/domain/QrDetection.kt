package com.qure.app.domain

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * One QR code seen in one frame.
 *
 * [boxInViewPx] and [cornersInViewPx] are ALREADY in PreviewView pixel coordinates
 * (origin = top-left of the PreviewView, +y down), because the analyzer hands ML Kit the
 * analysis->view matrix and ML Kit applies it internally. The UI layer therefore does no
 * transforming of its own — if the highlight is ever misplaced, the bug is in the analyzer's
 * matrix, never in the Compose code.
 */
@Immutable
data class QrDetection(
    val rawValue: String,
    val boxInViewPx: Rect,
    val cornersInViewPx: List<Offset>,
    val frameTimestampNs: Long,
) {
    /**
     * Parsed with the browser's own rules, not android.net.Uri's.
     * See [UrlParser.parse] for why that distinction is load-bearing in an anti-phishing app.
     */
    val parsed: ParsedPayload by lazy(LazyThreadSafetyMode.NONE) { UrlParser.parse(rawValue) }
}
