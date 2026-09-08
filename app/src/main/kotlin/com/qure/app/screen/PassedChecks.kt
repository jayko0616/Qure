package com.qure.app.screen

import androidx.annotation.StringRes
import com.qure.app.R
import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.Verdict

/**
 * What a clean verdict gets to claim it checked.
 *
 * A green screen with an empty findings list reads as "nothing was checked". So for a verdict with
 * no signals, the rules that actually ran are folded into a handful of plain-language lines the
 * user can recognise. Each line requires every rule behind it to have run; a rule that threw is
 * not a rule that passed, so its line is left out and the card says the run was incomplete.
 *
 * Lines that only make sense for a web link (HTTPS, host shape) are skipped for Wi-Fi, text and
 * the other payload kinds, where they would be claiming something that was never at stake.
 */
private class Check(
    /** A string resource id. */
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

/** String resource ids of the lines a clean verdict may show, in display order. */
fun passedChecks(verdict: Verdict.Assessed, parsed: ParsedPayload): List<Int> {
    if (verdict.signals.isNotEmpty()) return emptyList()
    return checks
        .filter { !it.webOnly || parsed.isWebLink }
        .filter { check -> check.rules.all { it in verdict.ranSignatures } }
        .map { it.label }
}
