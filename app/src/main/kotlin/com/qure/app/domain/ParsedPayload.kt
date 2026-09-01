package com.qure.app.domain

import androidx.compose.runtime.Immutable

/** What a QR payload turned out to be. Not every QR is a URL, and the non-URL ones still bite. */
enum class PayloadKind { httpUrl, wifi, tel, sms, mailto, geo, appIntent, otherScheme, plainText }

/**
 * The structural FACTS about a payload. Deliberately contains no judgement.
 *
 * Parsing and judging are separated on purpose: parsing is the part that is hard to get right and
 * must never change, while the rules that decide what is dangerous are expected to change
 * constantly. Everything here is something you could point at in the raw string; whether any of it
 * is *bad* is decided in the signature layer.
 */
@Immutable
data class ParsedPayload(
    val raw: String,
    val kind: PayloadKind,
    val scheme: String?,
    /** Host exactly as a browser would compute it, lowercased. Null when there is no authority. */
    val host: String?,
    /** Everything before the last '@' in the authority. Non-null is already worth a look. */
    val userInfo: String?,
    val port: String?,
    val isIpHost: Boolean,
    val hasPunycode: Boolean,
    val hasBidiControls: Boolean,
    val hasZeroWidth: Boolean,
    val hasControlChars: Boolean,
) {
    val isHttps: Boolean get() = scheme == "https"
    val isWebLink: Boolean get() = kind == PayloadKind.httpUrl && (scheme == "http" || scheme == "https")

    /** e.g. "login.account.example.co.kr" -> 5 */
    val hostLabelCount: Int get() = host?.split('.')?.count { it.isNotBlank() } ?: 0
    val tld: String? get() = host?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }

    /** Domain-ish suffix used for brand comparison: the last two labels. */
    val registrableSuffix: String?
        get() = host?.split('.')?.filter { it.isNotBlank() }?.takeLast(2)
            ?.takeIf { it.size == 2 }?.joinToString(".")
}
