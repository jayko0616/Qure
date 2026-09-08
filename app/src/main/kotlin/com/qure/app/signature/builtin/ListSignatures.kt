package com.qure.app.signature.builtin

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.signature.Brand
import com.qure.app.signature.Signature

/**
 * Rules whose behaviour is entirely determined by the data handed to them.
 *
 * These are the ones to reach for when the new detection is "one more domain" rather than "one more
 * idea": add the string to the corresponding list in `Signatures.kt` and it takes effect. The data
 * is a constructor parameter rather than a direct reference to `Signatures`, which is what keeps
 * that file free to import these without a cycle — and what lets tests hand in their own lists.
 */

/** Known-bad hosts. An exact match, or any subdomain of one. */
class BlockedHostSignature(private val hosts: Set<String>) : Signature {
    override val id = "blockedHost"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> {
        val host = payload.host ?: return emptyList()
        val hit = hosts.any { host == it || host.endsWith(".$it") }
        return if (hit) {
            listOf(
                Signal(
                    id, Severity.danger,
                    title = "알려진 악성 주소",
                    detail = "알려진 악성 주소 목록에 있는 도메인입니다",
                ),
            )
        } else emptyList()
    }
}

/** Substrings anywhere in the raw payload. Blunt, but useful for campaign-specific strings. */
class BlockedPatternSignature(private val patterns: Set<String>) : Signature {
    override val id = "blockedPattern"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> {
        val raw = payload.raw.lowercase()
        val hit = patterns.firstOrNull { it.isNotBlank() && raw.contains(it.lowercase()) }
        return if (hit != null) {
            listOf(
                Signal(
                    id, Severity.danger,
                    title = "알려진 악성 패턴",
                    detail = "알려진 피싱 캠페인에서 쓰인 문자열이 들어 있습니다",
                ),
            )
        } else emptyList()
    }
}

/**
 * Shorteners hide the destination, which is the whole point of using one in a quishing campaign.
 * Note this is a warning, not a verdict: the deep pass resolves the redirect and judges the target.
 */
class ShortenerSignature(private val shorteners: Set<String>) : Signature {
    override val id = "shortener"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> {
        val host = payload.host ?: return emptyList()
        return if (host in shorteners) {
            listOf(
                Signal(
                    id, Severity.warn,
                    title = "단축 주소",
                    detail = "실제 목적지를 알 수 없습니다",
                ),
            )
        } else emptyList()
    }
}

class RiskyTldSignature(private val tlds: Set<String>) : Signature {
    override val id = "riskyTld"
    override suspend fun inspect(payload: ParsedPayload): List<Signal> {
        val tld = payload.tld ?: return emptyList()
        return if (tld in tlds) {
            listOf(
                Signal(
                    id, Severity.info,
                    title = "위험 TLD (.$tld)",
                    detail = "악용 사례가 많은 최상위 도메인입니다",
                ),
            )
        } else emptyList()
    }
}

/**
 * Catches a domain that mentions a protected brand without actually being it —
 * "kakaobank.evil.example", "kakaobank-login.net", "kakaobank.com.evil.net".
 *
 * Matching is on whole tokens, never on raw substrings. A substring test looks fine until you
 * notice that "kakao" occurs inside "kakaobank", at which point the rule reports the REAL
 * kakaobank.com as an impostor of 카카오 — a false positive on the bank itself, which is about the
 * worst output an anti-phishing tool can produce. Hosts are split on dots and then on any
 * non-alphanumeric, and the brand label has to equal one of the resulting tokens.
 */
class BrandLookalikeSignature(private val brands: List<Brand>) : Signature {
    override val id = "brandLookalike"

    override suspend fun inspect(payload: ParsedPayload): List<Signal> {
        val host = payload.host ?: return emptyList()
        val suffix = payload.registrableSuffix
        val tokens = tokenize(host)

        val hit = brands.firstOrNull { brand ->
            val label = brand.domain.substringBefore('.').lowercase()
            label in tokens && suffix != brand.domain
        } ?: return emptyList()

        return listOf(
            Signal(
                id, Severity.danger,
                title = "브랜드 사칭",
                detail = "${hit.name}을(를) 사칭한 주소로 보입니다 (진짜 주소: ${hit.domain})",
            ),
        )
    }

    /** "kakaobank-login.evil.example" -> [kakaobank, login, evil, example] */
    private fun tokenize(host: String): Set<String> =
        host.lowercase()
            .split('.')
            .flatMap { label -> label.split(Regex("[^a-z0-9]+")) }
            .filter { it.isNotBlank() }
            .toSet()
}
