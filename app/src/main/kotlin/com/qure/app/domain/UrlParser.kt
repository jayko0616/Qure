package com.qure.app.domain

/**
 * Turns a raw QR payload into [ParsedPayload].
 *
 * Deliberately does NOT use android.net.Uri. Uri normalises, lowercases and returns null on exactly
 * the malformed authorities attackers rely on, which means the string you analyse stops matching the
 * string the platform will actually open — the analyser ends up reasoning about a different URL than
 * the one the user is about to visit. So the browser's own rules are reproduced here, including the
 * one that matters most: the host is whatever follows the LAST '@'.
 *
 * This object is expected to stay still. Detection rules live in the signature package and change
 * freely; parsing does not, because every rule downstream trusts it.
 */
object UrlParser {

    /** Characters browsers strip from a URL outright before parsing. */
    private val strippedChars = charArrayOf('\t', '\n', '\r')

    private val bidiControls = setOf(
        '‎', '‏', '‪', '‫', '‬', '‭', '‮',
        '⁦', '⁧', '⁨', '⁩',
    )
    private val zeroWidthChars = setOf('​', '‌', '‍', '﻿')

    fun parse(rawInput: String): ParsedPayload {
        val raw = rawInput.filterNot { it in strippedChars }.trim()
        val scheme = extractScheme(raw)?.lowercase()
        val kind = classify(scheme, raw)

        val hasBidi = rawInput.any { it in bidiControls }
        val hasZeroWidth = rawInput.any { it in zeroWidthChars }
        val hasControl = rawInput.any { it.code in 0x00..0x1F || it.code == 0x7F }

        if (kind != PayloadKind.httpUrl) {
            return ParsedPayload(
                raw = rawInput, kind = kind, scheme = scheme, host = null, userInfo = null,
                port = null, isIpHost = false, hasPunycode = false,
                hasBidiControls = hasBidi, hasZeroWidth = hasZeroWidth, hasControlChars = hasControl,
            )
        }

        val authority = extractAuthority(raw, scheme ?: "")
        // The rule that defeats https://www.kakaobank.com@evil.example/login
        val atIndex = authority.lastIndexOf('@')
        val userInfo = if (atIndex >= 0) authority.substring(0, atIndex) else null
        val hostPort = if (atIndex >= 0) authority.substring(atIndex + 1) else authority
        val (rawHost, port) = splitHostPort(hostPort)
        val host = rawHost.lowercase().trimEnd('.').ifBlank { null }

        return ParsedPayload(
            raw = rawInput,
            kind = kind,
            scheme = scheme,
            host = host,
            userInfo = userInfo,
            port = port,
            isIpHost = host != null && isIpLiteral(host),
            hasPunycode = host?.split('.')?.any { it.startsWith("xn--") } == true,
            hasBidiControls = hasBidi,
            hasZeroWidth = hasZeroWidth,
            hasControlChars = hasControl,
        )
    }

    /**
     * Renders a payload safe to PUT ON SCREEN. Invisible and direction-flipping characters become
     * visible escapes, so a payload can never paint a different string than the one it is.
     */
    fun toDisplayString(raw: String, max: Int = 300): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw.take(max)) {
            when {
                ch in bidiControls || ch in zeroWidthChars ->
                    sb.append("\\u").append(ch.code.toString(16).uppercase().padStart(4, '0'))
                ch.code in 0x00..0x1F || ch.code == 0x7F ->
                    sb.append("\\x").append(ch.code.toString(16).uppercase().padStart(2, '0'))
                else -> sb.append(ch)
            }
        }
        if (raw.length > max) sb.append('…')
        return sb.toString()
    }

    // ── internals ──────────────────────────────────────────────────────────────────────────

    private fun extractScheme(s: String): String? {
        val colon = s.indexOf(':')
        if (colon <= 0) return null
        val candidate = s.substring(0, colon)
        if (candidate.isEmpty() || !candidate[0].isLetter()) return null
        if (!candidate.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }) return null
        return candidate
    }

    private fun classify(scheme: String?, raw: String): PayloadKind = when {
        scheme == "http" || scheme == "https" -> PayloadKind.httpUrl
        scheme == "wifi" -> PayloadKind.wifi
        scheme == "tel" -> PayloadKind.tel
        scheme == "sms" || scheme == "smsto" || scheme == "mms" -> PayloadKind.sms
        scheme == "mailto" || scheme == "matmsg" -> PayloadKind.mailto
        scheme == "geo" -> PayloadKind.geo
        scheme == "intent" || scheme == "android-app" -> PayloadKind.appIntent
        scheme != null -> PayloadKind.otherScheme
        // A bare "kakaobank.com/login" is still a link to a human eye.
        raw.contains('.') && !raw.contains(' ') -> PayloadKind.httpUrl
        else -> PayloadKind.plainText
    }

    /** Authority = everything after the scheme's slashes, up to the first /, \, ?, or #. */
    private fun extractAuthority(raw: String, scheme: String): String {
        var i = if (scheme.isNotEmpty() && raw.startsWith("$scheme:", true)) scheme.length + 1 else 0
        // Browsers tolerate any run of / and \ here, including none and including three.
        while (i < raw.length && (raw[i] == '/' || raw[i] == '\\')) i++
        val end = raw.drop(i).indexOfFirst { it == '/' || it == '\\' || it == '?' || it == '#' }
        return if (end < 0) raw.substring(i) else raw.substring(i, i + end)
    }

    private fun splitHostPort(hostPort: String): Pair<String, String?> {
        if (hostPort.startsWith("[")) {                 // IPv6 literal
            val close = hostPort.indexOf(']')
            if (close < 0) return hostPort to null
            return hostPort.substring(0, close + 1) to
                hostPort.substring(close + 1).removePrefix(":").ifBlank { null }
        }
        val colon = hostPort.lastIndexOf(':')
        return if (colon >= 0) {
            hostPort.substring(0, colon) to hostPort.substring(colon + 1).ifBlank { null }
        } else hostPort to null
    }

    /** Covers dotted, bare-decimal (http://2130706433/) and hex forms, plus IPv6 literals. */
    private fun isIpLiteral(host: String): Boolean {
        if (host.startsWith("[") && host.endsWith("]")) return true
        if (host.isEmpty()) return false
        val parts = host.split('.')
        if (parts.size == 4 && parts.all { it.isNotEmpty() && it.all(Char::isDigit) }) return true
        if (parts.size == 1) {
            if (host.all(Char::isDigit)) return true
            if (host.startsWith("0x", true) &&
                host.drop(2).all { it.isDigit() || it in "abcdefABCDEF" }
            ) return true
        }
        return false
    }
}
