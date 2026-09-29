package com.qure.app.domain

import androidx.compose.runtime.Immutable

enum class PayloadKind { httpUrl, wifi, tel, sms, mailto, geo, appIntent, otherScheme, plainText }

@Immutable
data class ParsedPayload(
    val raw: String,
    val kind: PayloadKind,
    val scheme: String?,

    val host: String?,

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

    val hostLabelCount: Int get() = host?.split('.')?.count { it.isNotBlank() } ?: 0
    val tld: String? get() = host?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }

    val registrableSuffix: String?
        get() = host?.split('.')?.filter { it.isNotBlank() }?.takeLast(2)
            ?.takeIf { it.size == 2 }?.joinToString(".")
}
