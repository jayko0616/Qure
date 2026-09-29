package com.qure.app.signature

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.RiskLevel
import com.qure.app.domain.Signal
import com.qure.app.domain.Stage
import com.qure.app.domain.UrlParser
import com.qure.app.domain.Verdict
import com.qure.app.signature.builtin.IpHostSignature
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreRubricTest {

    private val engine = SignatureEngine()

    private fun assess(url: String, e: SignatureEngine = engine): Verdict.Assessed = runBlocking {
        e.analyze(UrlParser.parse(url)) as Verdict.Assessed
    }

    @Test fun `bands are disjoint and cover 0 to 100 with no gap`() {
        assertEquals(ScoreRubric.greenFloor, ScoreRubric.yellowCeiling + 1)
        assertEquals(ScoreRubric.yellowFloor, ScoreRubric.redCeiling + 1)
        for (s in 0..100) {
            val expected = when {
                s >= 85 -> RiskLevel.safe
                s >= 40 -> RiskLevel.caution
                else -> RiskLevel.dangerous
            }
            assertEquals("score $s", expected, ScoreRubric.bandOf(s))
        }
    }

    @Test fun `the score always lands inside the band its level owns`() {
        val corpus = listOf(
            "https://www.naver.com/",
            "https://bit.ly/abc",
            "http://example.com/",
            "http://example.com:8080/",
            "http://shop.example.xyz/login",
            "https://login.account.secure.mybank.example.com/verify",
            "https://www.kakaobank.com@evil.example/login",
            "http://192.168.0.9/pay",
            "https://xn--80ak6aa92e.com/login",
            "https://kakaobank-login.evil.example/",
            "https://www.kbstar.com@203.0.113.5/",
            "intent://scan/#Intent;scheme=zxing;package=com.evil.app;end",
            "WIFI:S:cafe;T:WPA;P:pw;;",
            "hello there",
        )
        for (url in corpus) {
            val v = assess(url)
            assertEquals(url, v.level, ScoreRubric.bandOf(v.score))
        }
    }

    @Test fun `a clean offline verdict is 97`() {
        assertEquals(97, assess("https://www.naver.com/").score)
    }

    @Test fun `a clean deep verdict is the full 100`() {
        val deep = SignatureEngine(stage = Stage.s2)
        assertEquals(100, assess("https://www.naver.com/", deep).score)
    }

    @Test fun `one warning is 72, a second warning is 64`() {
        assertEquals(72, assess("https://bit.ly/abc").score)
        assertEquals(64, assess("http://example.com:8080/").score)
    }

    @Test fun `an info signal costs 5 on top of a warning`() {

        assertEquals(67, assess("http://shop.example.xyz/login").score)
    }

    @Test fun `the strongest dangers start at 15, other dangers at 30`() {
        assertEquals(15, assess("https://www.kakaobank.com@evil.example/login").score)
        assertEquals(15, assess("https://kakaobank-login.evil.example/").score)
        assertEquals(30, assess("https://192.168.0.9/pay").score)
        assertEquals(30, assess("https://xn--80ak6aa92e.com/login").score)
    }

    @Test fun `each additional danger costs 8`() {

        assertEquals(7, assess("https://www.kbstar.com@203.0.113.5/").score)
    }

    @Test fun `warnings do not move a red score`() {

        assertEquals(assess("https://192.168.0.9/pay").score, assess("http://192.168.0.9/pay").score)
    }

    @Test fun `failed and not-yet-run verdicts sit at 60`() {
        assertEquals(60, ScoreRubric.scoreOf(Verdict.Failed("x")))
        assertEquals(60, ScoreRubric.scoreOf(Verdict.NotAssessed))
    }

    @Test fun `a rule that throws costs green 5 points and never the colour`() {
        val boom = object : Signature {
            override val id = "boom"
            override suspend fun inspect(payload: ParsedPayload): List<Signal> = error("down")
        }
        val v = assess("https://www.naver.com/", SignatureEngine(listOf(boom, IpHostSignature)))
        assertEquals(RiskLevel.safe, v.level)
        assertEquals(92, v.score)
        assertTrue("boom" in v.failedSignatures)
        assertEquals(listOf("ipHost"), v.ranSignatures)
    }

    @Test fun `many warnings pile up inside yellow and never cross into red`() {

        val v = assess("http://a.b.c.d.e.f.example.top:8080/")
        assertEquals(RiskLevel.caution, v.level)
        assertEquals(43, v.score)
        assertEquals(RiskLevel.caution, ScoreRubric.bandOf(v.score))
    }

    @Test fun `a single danger with no company is still red`() {
        val v = assess("https://xn--80ak6aa92e.com/")
        assertEquals(RiskLevel.dangerous, v.level)
        assertEquals(1, v.signals.size)
        assertEquals(RiskLevel.dangerous, ScoreRubric.bandOf(v.score))
    }

    @Test fun `a signal carries no number of its own`() {

        val fields = Signal::class.java.declaredFields
            .filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            .map { it.name }
            .toSet()
        assertEquals(setOf("id", "severity", "title", "detail"), fields)
    }
}
