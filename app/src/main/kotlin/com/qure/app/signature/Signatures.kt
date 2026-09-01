package com.qure.app.signature

import com.qure.app.signature.builtin.BlockedHostSignature
import com.qure.app.signature.builtin.BlockedPatternSignature
import com.qure.app.signature.builtin.BrandLookalikeSignature
import com.qure.app.signature.builtin.IpHostSignature
import com.qure.app.signature.builtin.MissingHostSignature
import com.qure.app.signature.builtin.NonStandardPortSignature
import com.qure.app.signature.builtin.PayloadKindSignature
import com.qure.app.signature.builtin.PunycodeSignature
import com.qure.app.signature.builtin.RiskyTldSignature
import com.qure.app.signature.builtin.ShortenerSignature
import com.qure.app.signature.builtin.SubdomainDepthSignature
import com.qure.app.signature.builtin.TransportSignature
import com.qure.app.signature.builtin.UnicodeTrickSignature
import com.qure.app.signature.builtin.UserInfoSignature

/**
 * ★ THIS IS THE FILE YOU EDIT ★
 *
 * Everything the app knows about what is dangerous lives here. Nothing outside this file and the
 * `builtin/` package needs to change to add, remove or retune a detection.
 *
 * ── Adding a bad link or domain ────────────────────────────────────────────────────────────────
 * Put the string in [blockedHosts] or [blockedPatterns]. That is the whole change.
 *
 * ── Adding a new detection technique ───────────────────────────────────────────────────────────
 * Write a class implementing [Signature] (see `builtin/` for a dozen short examples), then add an
 * instance to [rules]. It runs on the next scan. Both entry points — Qure's own scanner and the
 * stock-camera intent path — go through the same engine, so a rule added here is live in both.
 *
 * ── Adding a rule that needs the network, or an LLM ────────────────────────────────────────────
 * [Signature.inspect] is `suspend`, so a rule may resolve redirects, query a reputation feed or ask
 * a model. Two things to remember when you do:
 *   1. The app currently declares NO INTERNET permission. Add it to AndroidManifest.xml first, and
 *      understand that doing so changes what the app can claim about itself.
 *   2. Never let a network failure read as "clean". Throw, and [SignatureEngine] will record the
 *      rule as failed and refuse to present the verdict as complete. Returning an empty list means
 *      "I looked and found nothing", which is a much stronger claim than you can make when the
 *      request timed out.
 */
object Signatures {

    // ── Data ───────────────────────────────────────────────────────────────────────────────────
    // Add entries here. Matching is exact for hosts (subdomains included) and substring for
    // patterns. Keep them lowercase.

    /** Known-bad hosts. Matches the host itself and any subdomain of it. */
    val blockedHosts: Set<String> = setOf(
        // "malicious.example",
    )

    /** Substrings looked for anywhere in the raw payload. Useful for campaign-specific strings. */
    val blockedPatterns: Set<String> = setOf(
        // "/verify-account-now",
    )

    /** URL shorteners: the destination is hidden until it is resolved. */
    val urlShorteners: Set<String> = setOf(
        "bit.ly", "tinyurl.com", "goo.gl", "t.co", "ow.ly", "is.gd", "buff.ly", "adf.ly",
        "bitly.com", "cutt.ly", "rebrand.ly", "shorturl.at", "rb.gy", "vo.la", "me2.do",
        "han.gl", "bit.do", "url.kr", "muz.so", "durl.kr",
    )

    /** TLDs with a disproportionate share of abuse. Informational only — never decisive alone. */
    val riskyTlds: Set<String> = setOf(
        "zip", "mov", "top", "xyz", "tk", "ml", "ga", "cf", "gq", "work", "click", "link",
        "country", "kim", "loan", "download", "rest", "quest", "cam",
    )

    /** Brands worth protecting from lookalike domains. */
    val protectedBrands: List<Brand> = listOf(
        Brand("카카오뱅크", "kakaobank.com"),
        Brand("토스", "toss.im"),
        Brand("국민은행", "kbstar.com"),
        Brand("신한은행", "shinhan.com"),
        Brand("우리은행", "wooribank.com"),
        Brand("하나은행", "hanabank.com"),
        Brand("네이버", "naver.com"),
        Brand("카카오", "kakao.com"),
        Brand("쿠팡", "coupang.com"),
        Brand("국세청", "hometax.go.kr"),
        Brand("우체국", "epost.go.kr"),
    )

    // ── Rules ──────────────────────────────────────────────────────────────────────────────────
    // Order does not matter: rules cannot see each other, and the engine aggregates. Comment one
    // out to disable it.

    val rules: List<Signature> = listOf(
        // Structure — nothing but the shape of the URL.
        UserInfoSignature,
        IpHostSignature,
        PunycodeSignature,
        TransportSignature,
        MissingHostSignature,
        UnicodeTrickSignature,
        PayloadKindSignature,
        SubdomainDepthSignature(maxLabels = 5),
        NonStandardPortSignature(),

        // Data — driven entirely by the lists above.
        BlockedHostSignature(blockedHosts),
        BlockedPatternSignature(blockedPatterns),
        ShortenerSignature(urlShorteners),
        RiskyTldSignature(riskyTlds),
        BrandLookalikeSignature(protectedBrands),

        // Network- or model-backed rules go here. See the class note above before adding one.
    )
}
