package com.qure.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These are the cases android.net.Uri gets wrong or normalises away. If any regress, the whole
 * signature layer starts reasoning about a different URL than the one the phone would open.
 */
class UrlParserTest {

    private fun host(s: String) = UrlParser.parse(s).host

    @Test fun `host is what follows the LAST at-sign`() {
        assertEquals("evil.example", host("https://www.kakaobank.com@evil.example/login"))
        assertEquals("evil.example", host("https://a@b@evil.example/"))
        assertEquals("www.kakaobank.com", UrlParser.parse("https://www.kakaobank.com@evil.example/x").userInfo)
    }

    @Test fun `backslashes are treated as separators like a browser does`() {
        assertEquals("evil.example", host("https://evil.example\\@notthis.com"))
    }

    @Test fun `port is split off the host`() {
        val p = UrlParser.parse("http://example.com:8443/x")
        assertEquals("example.com", p.host)
        assertEquals("8443", p.port)
    }

    @Test fun `ipv6 literal survives host-port splitting`() {
        assertEquals("[::1]", host("http://[::1]:8080/x"))
    }

    @Test fun `ip hosts are recognised in every notation`() {
        assertTrue(UrlParser.parse("http://192.168.0.1/pay").isIpHost)
        assertTrue(UrlParser.parse("http://2130706433/pay").isIpHost)
        assertTrue(UrlParser.parse("http://0x7f000001/pay").isIpHost)
        assertFalse(UrlParser.parse("https://example.com/").isIpHost)
    }

    @Test fun `punycode is recognised`() {
        assertTrue(UrlParser.parse("https://xn--80ak6aa92e.com/").hasPunycode)
        assertFalse(UrlParser.parse("https://example.com/").hasPunycode)
    }

    @Test fun `bidi and zero-width characters are recognised and neutralised for display`() {
        val payload = "https://example.com/‮gnp.exe"
        assertTrue(UrlParser.parse(payload).hasBidiControls)
        assertTrue(UrlParser.toDisplayString(payload).contains("\\u202E"))
        assertTrue(UrlParser.parse("https://exa​mple.com/").hasZeroWidth)
    }

    @Test fun `tabs and newlines are stripped before parsing like a browser does`() {
        assertEquals("example.com", host("https://exa\tmple.com/x"))
    }

    @Test fun `trailing dot and case are normalised`() {
        assertEquals("example.com", host("https://ExAmPlE.CoM./x"))
    }

    @Test fun `non-url schemes are classified rather than forced into a url shape`() {
        assertEquals(PayloadKind.wifi, UrlParser.parse("WIFI:S=cafe;T=WPA;P=pw;;").kind)
        assertEquals(PayloadKind.appIntent, UrlParser.parse("intent://x#Intent;scheme=y;end").kind)
        assertEquals(PayloadKind.plainText, UrlParser.parse("hello there").kind)
        assertEquals(PayloadKind.httpUrl, UrlParser.parse("https://naver.com/").kind)
    }

    @Test fun `structural facts carry no judgement`() {
        // A clean URL must produce a payload with every risk flag false — the parser is not allowed
        // to have an opinion, only to report shape.
        val p = UrlParser.parse("https://www.naver.com/")
        assertFalse(p.isIpHost)
        assertFalse(p.hasPunycode)
        assertFalse(p.hasBidiControls)
        assertFalse(p.hasZeroWidth)
        assertFalse(p.hasControlChars)
        assertEquals(null, p.userInfo)
        assertEquals("naver.com", p.registrableSuffix)
    }
}
