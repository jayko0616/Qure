package com.qure.app.signature

import com.qure.app.domain.RiskLevel
import com.qure.app.domain.UrlParser
import com.qure.app.domain.Verdict
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The canonical examples the app is demonstrated with, pinned to the verdict they are supposed to
 * produce.
 *
 * These double as documentation of what each risk level is *for*: safe means nothing visible is
 * wrong, caution means something needs a look that this offline pass cannot finish, and dangerous
 * means a signal fired that on its own justifies not opening the link. If a rule change moves one
 * of these buckets, that is worth noticing deliberately rather than discovering during a demo.
 *
 * Every hostile domain here is fictional: attacker.example / .invalid are reserved by RFC 2606 and
 * 203.0.113.0/24 by RFC 5737, so none of them can resolve to anything real.
 */
class ClassificationExamplesTest {

    private val engine = SignatureEngine()

    private fun assess(url: String) = runBlocking {
        engine.analyze(UrlParser.parse(url)) as Verdict.Assessed
    }

    private fun ids(url: String) = assess(url).signals.map { it.id }.toSet()

    // ── safe ───────────────────────────────────────────────────────────────────────────────────

    @Test fun `naver is clean`() {
        val v = assess("https://www.naver.com/")
        assertEquals(RiskLevel.safe, v.level)
        assertTrue(v.signals.isEmpty())
    }

    @Test fun `google is clean`() {
        val v = assess("https://www.google.com/")
        assertEquals(RiskLevel.safe, v.level)
        assertTrue(v.signals.isEmpty())
    }

    // ── dangerous ──────────────────────────────────────────────────────────────────────────────

    @Test fun `at-sign disguise wearing a bank name is dangerous`() {
        val url = "https://www.kakaobank.com@secure-login.attacker.example/auth"
        assertEquals(RiskLevel.dangerous, assess(url).level)
        assertTrue("userinfo" in ids(url))
    }

    @Test fun `raw ip over plain http is dangerous`() {
        val url = "http://203.0.113.77/kbstar/verify.php"
        assertEquals(RiskLevel.dangerous, assess(url).level)
        assertTrue("ipHost" in ids(url))
        assertTrue("noHttps" in ids(url))
    }

    // ── ambiguous: something is off, but this pass cannot finish the job ───────────────────────

    @Test fun `a shortener is caution, never safe and never dangerous`() {
        val url = "https://bit.ly/3xK9mQr"
        assertEquals(RiskLevel.caution, assess(url).level)
        assertTrue("shortener" in ids(url))
    }

    @Test fun `an unusually deep subdomain is caution`() {
        val url = "https://login.account.secure.mybank.example.com/verify"
        assertEquals(RiskLevel.caution, assess(url).level)
        assertTrue("subdomainDepth" in ids(url))
    }
}
