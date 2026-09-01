package com.qure.app.domain

import androidx.compose.runtime.Immutable

/** How much a single finding should worry the user. Ordinal order is significance order. */
enum class Severity { info, warn, danger }

/** One thing worth telling the user about, produced by exactly one signature. */
@Immutable
data class Signal(
    /** Stable identifier of the signature that raised it. Never shown to the user. */
    val id: String,
    val severity: Severity,
    /** Korean, user-facing, and specific enough to act on. */
    val label: String,
)

enum class RiskLevel { safe, caution, dangerous }

/**
 * The outcome of analysing a payload.
 *
 * Deliberately a sealed hierarchy and NOT something with an `isSafe: Boolean`. A boolean invites
 * `if (result.isSafe)`, and then every error path has to remember to set it false. Here a caller
 * must handle [failed] explicitly to get anything out of the result, so an analysis that broke can
 * never be silently rendered as an all-clear.
 */
@Immutable
sealed interface Verdict {

    /** Analysis completed. [level] is meaningful. */
    @Immutable
    data class Assessed(
        val level: RiskLevel,
        val headline: String,
        val signals: List<Signal>,
        /** Signatures that threw. Non-empty means this verdict is INCOMPLETE, not clean. */
        val failedSignatures: List<String> = emptyList(),
    ) : Verdict

    /** Analysis could not run at all. NOT a safe result. */
    @Immutable
    data class Failed(val reason: String) : Verdict

    /** Nothing has run yet. Distinct from both a clean bill of health and a failure. */
    @Immutable
    data object NotAssessed : Verdict
}

/**
 * The seam every detection engine plugs into.
 *
 * Takes an already-[ParsedPayload] rather than a raw string on purpose: parsing is the part that is
 * easy to get subtly wrong, so it happens exactly once and every implementation inherits the same
 * browser-accurate view of the URL.
 *
 * `suspend` so a future implementation can resolve redirects, query a reputation feed, or ask an
 * LLM without the interface having to change.
 */
interface QrRiskAnalyzer {
    suspend fun analyze(payload: ParsedPayload): Verdict
}
