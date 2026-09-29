package com.qure.app.signature

import com.qure.app.signature.builtin.BlockedHostSignature
import com.qure.app.signature.builtin.BlockedPatternSignature
import com.qure.app.signature.builtin.BrandLookalikeSignature
import com.qure.app.signature.builtin.IpHostSignature
import com.qure.app.signature.builtin.MissingHostSignature
import com.qure.app.signature.builtin.NonStandardPortSignature
import com.qure.app.signature.builtin.PayloadKindSignature
import com.qure.app.signature.builtin.PunycodeSignature
import com.qure.app.signature.builtin.RedirectSignature
import com.qure.app.signature.builtin.RiskyTldSignature
import com.qure.app.signature.builtin.ShortenerSignature
import com.qure.app.signature.builtin.SubdomainDepthSignature
import com.qure.app.signature.builtin.TransportSignature
import com.qure.app.signature.builtin.UnicodeTrickSignature
import com.qure.app.signature.builtin.UserInfoSignature
import com.qure.app.domain.Stage
import com.qure.app.network.RedirectResolver

object Signatures {

    val blockedHosts: Set<String> = setOf(

    )

    val blockedPatterns: Set<String> = setOf(

    )

    val urlShorteners: Set<String> = setOf(
        "bit.ly", "tinyurl.com", "goo.gl", "t.co", "ow.ly", "is.gd", "buff.ly", "adf.ly",
        "bitly.com", "cutt.ly", "rebrand.ly", "shorturl.at", "rb.gy", "vo.la", "me2.do",
        "han.gl", "bit.do", "url.kr", "muz.so", "durl.kr",
    )

    val riskyTlds: Set<String> = setOf(
        "zip", "mov", "top", "xyz", "tk", "ml", "ga", "cf", "gq", "work", "click", "link",
        "country", "kim", "loan", "download", "rest", "quest", "cam",
    )

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

    val rules: List<Signature> = listOf(

        UserInfoSignature,
        IpHostSignature,
        PunycodeSignature,
        TransportSignature,
        MissingHostSignature,
        UnicodeTrickSignature,
        PayloadKindSignature,
        SubdomainDepthSignature(maxLabels = 5),
        NonStandardPortSignature(),

        BlockedHostSignature(blockedHosts),
        BlockedPatternSignature(blockedPatterns),
        ShortenerSignature(urlShorteners),
        RiskyTldSignature(riskyTlds),
        BrandLookalikeSignature(protectedBrands),

    )

    fun deepEngine(
        extraRules: List<Signature> = emptyList(),
        resolver: RedirectResolver = RedirectResolver(),
    ): SignatureEngine {

        val all = rules + extraRules
        return SignatureEngine(
            all + RedirectSignature(resolver, SignatureEngine(all)),
            stage = Stage.s2,
        )
    }
}
