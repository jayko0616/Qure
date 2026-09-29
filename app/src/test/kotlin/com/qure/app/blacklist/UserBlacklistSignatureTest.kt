package com.qure.app.blacklist

import com.qure.app.domain.RiskLevel
import com.qure.app.domain.UrlParser
import com.qure.app.domain.Verdict
import com.qure.app.signature.SignatureEngine
import com.qure.app.signature.Signatures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserBlacklistSignatureTest {

    private fun engineWith(vararg entries: String) =
        SignatureEngine(Signatures.rules + UserBlacklistSignature { entries.toList() })

    private fun fired(engine: SignatureEngine, url: String) = runBlocking {
        val v = engine.analyze(UrlParser.parse(url)) as Verdict.Assessed
        "userBlacklist" in v.signals.map { it.id }
    }

    @Test fun `a bare domain entry matches the host and its subdomains`() {
        val engine = engineWith("evil.example")
        assertTrue(fired(engine, "https://evil.example/pay"))
        assertTrue(fired(engine, "https://login.evil.example/pay"))
        assertFalse(fired(engine, "https://notevil.example/pay"))
        assertFalse(fired(engine, "https://www.naver.com/"))
    }

    @Test fun `a full url entry matches as a substring of the payload`() {
        val engine = engineWith("https://evil.example/campaign-42")
        assertTrue(fired(engine, "https://evil.example/campaign-42?x=1"))
        assertFalse(fired(engine, "https://evil.example/other"))
    }

    @Test fun `a blacklisted address is dangerous even when nothing else is wrong`() {

        val engine = engineWith("boring.example")
        val v = runBlocking {
            engine.analyze(UrlParser.parse("https://boring.example/")) as Verdict.Assessed
        }
        assertEquals(RiskLevel.dangerous, v.level)
    }

    @Test fun `an empty list changes nothing`() {
        val engine = engineWith()
        val v = runBlocking {
            engine.analyze(UrlParser.parse("https://www.naver.com/")) as Verdict.Assessed
        }
        assertEquals(RiskLevel.safe, v.level)
        assertTrue(v.signals.isEmpty())
    }
}
