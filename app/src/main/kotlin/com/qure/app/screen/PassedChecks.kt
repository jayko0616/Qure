package com.qure.app.screen

import com.qure.app.R
import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.Verdict

private class Check(

    val label: Int,
    val rules: Set<String>,
    val webOnly: Boolean = false,
)

private val checks: List<Check> = listOf(
    Check(R.string.check_https, setOf("noHttps"), webOnly = true),
    Check(R.string.check_no_disguise, setOf("userinfo"), webOnly = true),
    Check(
        R.string.check_domain_shape,
        setOf("ipHost", "punycode", "noHost", "subdomainDepth", "nonStandardPort"),
        webOnly = true,
    ),
    Check(R.string.check_no_hidden_chars, setOf("unicodeTrick")),
    Check(R.string.check_no_brand_lookalike, setOf("brandLookalike"), webOnly = true),
    Check(R.string.check_not_shortener, setOf("shortener"), webOnly = true),
    Check(R.string.check_not_listed, setOf("blockedHost", "blockedPattern", "userBlacklist", "riskyTld")),
)

fun passedChecks(verdict: Verdict.Assessed, parsed: ParsedPayload): List<Int> {
    if (verdict.signals.isNotEmpty()) return emptyList()
    return checks
        .filter { !it.webOnly || parsed.isWebLink }
        .filter { check -> check.rules.all { it in verdict.ranSignatures } }
        .map { it.label }
}
