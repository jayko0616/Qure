package com.qure.app.signature

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.RiskLevel
import com.qure.app.domain.Severity
import com.qure.app.domain.Stage
import com.qure.app.domain.UrlParser
import com.qure.app.domain.Verdict
import com.qure.app.network.RedirectResolver
import com.qure.app.signature.builtin.RedirectSignature
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RedirectSignatureTest {

    private class FakeResolver(
        private val chains: Map<String, List<String>> = emptyMap(),
        private val failWith: IOException? = null,
    ) : RedirectResolver() {
        override suspend fun resolve(startUrl: String): Chain {
            failWith?.let { throw it }
            val hops = chains[startUrl] ?: listOf(startUrl)
            return Chain(hops, truncated = false)
        }
    }

    private fun parse(url: String) = UrlParser.parse(url)

    private fun signature(resolver: RedirectResolver) =
        RedirectSignature(resolver, SignatureEngine(Signatures.rules))

    @Test fun `a link that does not redirect raises nothing`() = runBlocking {
        val s = signature(FakeResolver())
        assertTrue(s.inspect(parse("https://example.com/a")).isEmpty())
    }

    @Test fun `a non-web payload is not resolved at all`() = runBlocking {

        val s = signature(FakeResolver(failWith = IOException("must not be called")))
        assertTrue(s.inspect(parse("WIFI:S:cafe;T:WPA;P:1234;;")).isEmpty())
    }

    @Test fun `the destination host is reported`() = runBlocking {
        val s = signature(
            FakeResolver(mapOf("https://bit.ly/x" to listOf("https://bit.ly/x", "https://real.example/p"))),
        )
        val signals = s.inspect(parse("https://bit.ly/x"))
        val arrival = signals.first { it.id == "redirect" }
        assertTrue(arrival.detail!!.contains("real.example"))
    }

    @Test fun `findings about the destination cannot be confused with the scanned code`() = runBlocking {
        val s = signature(
            FakeResolver(
                mapOf(
                    "https://bit.ly/x" to listOf(
                        "https://bit.ly/x",
                        "https://kakaobank-login.evil.example/auth",
                    ),
                ),
            ),
        )
        val signals = s.inspect(parse("https://bit.ly/x"))
        val brand = signals.first { it.id == "dest.brandLookalike" }
        assertEquals(Severity.danger, brand.severity)
        assertTrue(brand.title.startsWith("목적지 — "))
    }

    @Test fun `a chain that drops to http is flagged as a downgrade`() = runBlocking {
        val s = signature(
            FakeResolver(mapOf("https://a.example" to listOf("https://a.example", "http://b.example"))),
        )
        val ids = s.inspect(parse("https://a.example")).map { it.id }
        assertTrue("redirectDowngrade" in ids)
    }

    @Test fun `many hops are called out on their own`() = runBlocking {
        val chain = listOf("https://a.example", "https://b.example", "https://c.example", "https://d.example")
        val s = signature(FakeResolver(mapOf("https://a.example" to chain)))
        val ids = s.inspect(parse("https://a.example")).map { it.id }
        assertTrue("redirectDepth" in ids)
    }

    @Test fun `a network failure makes the verdict incomplete, never clean`() = runBlocking {
        val engine = SignatureEngine(
            Signatures.rules + signature(FakeResolver(failWith = IOException("timeout"))),
            stage = Stage.s2,
        )
        val verdict = engine.analyze(parse("https://example.com")) as Verdict.Assessed
        assertTrue("redirect" in verdict.failedSignatures)

        assertEquals(RiskLevel.safe, verdict.level)
        assertFalse(verdict.headline.contains("없습니다"))
    }

    @Test fun `a clean deep pass scores higher than a clean offline pass`() = runBlocking {
        val payload: ParsedPayload = parse("https://example.com")
        val shallow = SignatureEngine(Signatures.rules).analyze(payload) as Verdict.Assessed
        val deep = SignatureEngine(
            Signatures.rules + signature(FakeResolver()),
            stage = Stage.s2,
        ).analyze(payload) as Verdict.Assessed
        assertEquals(RiskLevel.safe, deep.level)
        assertTrue(deep.score > shallow.score)
    }
}
