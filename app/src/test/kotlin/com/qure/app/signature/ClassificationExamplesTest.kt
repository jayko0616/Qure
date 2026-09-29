package com.qure.app.signature

import com.qure.app.domain.RiskLevel
import com.qure.app.domain.UrlParser
import com.qure.app.domain.Verdict
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassificationExamplesTest {

    private val engine = SignatureEngine()

    private fun assess(url: String) = runBlocking {
        engine.analyze(UrlParser.parse(url)) as Verdict.Assessed
    }

    private fun ids(url: String) = assess(url).signals.map { it.id }.toSet()

    @Test fun `naver is clean`() {
        val v = assess("https://www.naver.com/")
        assertEquals(RiskLevel.safe, v.level)
        assertTrue(v.signals.isEmpty())
        assertEquals(97, v.score)
    }

    @Test fun `google is clean`() {
        val v = assess("https://www.google.com/")
        assertEquals(RiskLevel.safe, v.level)
        assertTrue(v.signals.isEmpty())
        assertEquals(97, v.score)
    }

    @Test fun `the real kakaobank domain is clean`() {
        val v = assess("https://www.kakaobank.com/login")
        assertEquals(RiskLevel.safe, v.level)
        assertEquals(97, v.score)
    }

    @Test fun `at-sign disguise wearing a bank name is dangerous`() {
        val url = "https://www.kakaobank.com@secure-login.attacker.example/auth"
        val v = assess(url)
        assertEquals(RiskLevel.dangerous, v.level)
        assertTrue("userinfo" in ids(url))
        assertEquals(15, v.score)
    }

    @Test fun `raw ip over plain http is dangerous`() {
        val url = "http://203.0.113.77/kbstar/verify.php"
        val v = assess(url)
        assertEquals(RiskLevel.dangerous, v.level)
        assertTrue("ipHost" in ids(url))
        assertTrue("noHttps" in ids(url))
        assertEquals(30, v.score)
    }

    @Test fun `a brand pasted in front of a stranger's domain is dangerous`() {
        val url = "https://toss.im.verify-account.attacker.example/"
        val v = assess(url)
        assertEquals(RiskLevel.dangerous, v.level)
        assertTrue("brandLookalike" in ids(url))
        assertEquals(15, v.score)
    }

    @Test fun `a shortener is caution, never safe and never dangerous`() {
        val url = "https://bit.ly/3xK9mQr"
        val v = assess(url)
        assertEquals(RiskLevel.caution, v.level)
        assertTrue("shortener" in ids(url))
        assertEquals(72, v.score)
    }

    @Test fun `an unusually deep subdomain is caution`() {
        val url = "https://login.account.secure.mybank.example.com/verify"
        val v = assess(url)
        assertEquals(RiskLevel.caution, v.level)
        assertTrue("subdomainDepth" in ids(url))
        assertEquals(72, v.score)
    }

    @Test fun `plain http with a risky tld sits lower inside caution`() {
        val v = assess("http://shop.example.xyz/login")
        assertEquals(RiskLevel.caution, v.level)
        assertEquals(67, v.score)
    }
}
