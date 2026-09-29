package com.qure.app.signature

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.RiskLevel
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.domain.UrlParser
import com.qure.app.domain.Verdict
import com.qure.app.signature.builtin.BlockedHostSignature
import com.qure.app.signature.builtin.BrandLookalikeSignature
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureEngineTest {

    private val engine = SignatureEngine()

    private fun assess(url: String): Verdict.Assessed = runBlocking {
        engine.analyze(UrlParser.parse(url)) as Verdict.Assessed
    }

    private fun ids(url: String) = assess(url).signals.map { it.id }.toSet()

    @Test fun `a clean https url raises nothing and is safe`() {
        val v = assess("https://www.naver.com/")
        assertTrue(v.signals.isEmpty())
        assertEquals(RiskLevel.safe, v.level)
    }

    @Test fun `the at-sign disguise is dangerous`() {
        val v = assess("https://www.kakaobank.com@evil.example/login")
        assertTrue("userinfo" in v.signals.map { it.id })
        assertEquals(RiskLevel.dangerous, v.level)
    }

    @Test fun `raw ip over plain http raises both signals`() {
        val found = ids("http://192.168.0.9/pay")
        assertTrue("ipHost" in found)
        assertTrue("noHttps" in found)
    }

    @Test fun `shortener is a warning, not a verdict`() {
        val v = assess("https://bit.ly/abc")
        assertTrue("shortener" in v.signals.map { it.id })
        assertEquals(RiskLevel.caution, v.level)
    }

    @Test fun `punycode is dangerous`() {
        assertEquals(RiskLevel.dangerous, assess("https://xn--80ak6aa92e.com/login").level)
    }

    @Test fun `brand lookalike fires on an impostor but not on the real domain`() {
        assertTrue("brandLookalike" in ids("https://kakaobank.evil.example/login"))
        assertTrue("brandLookalike" in ids("https://kakaobank-login.evil.example/"))
        assertTrue("brandLookalike" !in ids("https://www.kakaobank.com/login"))
    }

    @Test fun `a brand whose name is a prefix of another brand does not flag the longer one`() {

        assertTrue("brandLookalike" !in ids("https://www.kakaobank.com/"))
        assertTrue("brandLookalike" !in ids("https://kakaobank.com/login"))

        assertTrue("brandLookalike" in ids("https://kakao.evil.example/"))
    }

    @Test fun `adding a host to the blocklist is enough to flag it`() {
        val custom = SignatureEngine(listOf(BlockedHostSignature(setOf("malicious.example"))))
        val v = runBlocking {
            custom.analyze(UrlParser.parse("https://sub.malicious.example/x")) as Verdict.Assessed
        }
        assertEquals(RiskLevel.dangerous, v.level)
    }

    @Test fun `adding a brand is enough to protect it`() {
        val custom = SignatureEngine(
            listOf(BrandLookalikeSignature(listOf(Brand("테스트은행", "testbank.co.kr")))),
        )
        val v = runBlocking {
            custom.analyze(UrlParser.parse("https://testbank.attacker.example/")) as Verdict.Assessed
        }
        assertTrue("brandLookalike" in v.signals.map { it.id })
    }

    @Test fun `a disabled signature does not run`() {
        val off = object : Signature {
            override val id = "off"
            override val enabled = false
            override suspend fun inspect(payload: ParsedPayload) =
                listOf(Signal(id, Severity.danger, "should never appear"))
        }
        val v = runBlocking { SignatureEngine(listOf(off)).analyze(UrlParser.parse("https://a.com/")) }
        assertTrue(v is Verdict.Failed)
    }

    @Test fun `a signature that throws is never mistaken for one that found nothing`() {
        val boom = object : Signature {
            override val id = "boom"
            override suspend fun inspect(payload: ParsedPayload): List<Signal> = error("network down")
        }
        val v = runBlocking {
            SignatureEngine(listOf(boom, com.qure.app.signature.builtin.IpHostSignature))
                .analyze(UrlParser.parse("https://www.naver.com/")) as Verdict.Assessed
        }
        assertTrue("boom" in v.failedSignatures)

        assertNotEquals("눈에 띄는 위험 신호는 없습니다", v.headline)
    }

    @Test fun `every signature failing is a Failed verdict, not a safe one`() {
        val boom = object : Signature {
            override val id = "boom"
            override suspend fun inspect(payload: ParsedPayload): List<Signal> = error("down")
        }
        val v = runBlocking { SignatureEngine(listOf(boom)).analyze(UrlParser.parse("https://a.com/")) }
        assertTrue(v is Verdict.Failed)
    }
}
