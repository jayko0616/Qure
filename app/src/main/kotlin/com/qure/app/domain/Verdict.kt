package com.qure.app.domain

import androidx.compose.runtime.Immutable

/** How much a single finding should worry the user. Ordinal order is significance order. */
enum class Severity { info, warn, danger }

/**
 * One thing worth telling the user about, produced by exactly one signature.
 *
 * Deliberately carries NO number. What a signal costs in points lives in the score rubric and
 * never travels with the signal, so it cannot leak into a screen that lists findings. A per-item
 * penalty on screen is a rubric an attacker reconstructs by scanning a handful of codes, and then
 * designs a URL that lands one point inside green.
 */
@Immutable
data class Signal(
    /** Stable identifier of the signature that raised it. Never shown to the user. */
    val id: String,
    /** Sorting only. The colour is decided from the worst severity present; see SignatureEngine. */
    val severity: Severity,
    /** Short, Korean, user-facing. The line the eye lands on: "주소 위장", "단축 주소". */
    val title: String,
    /** One sentence of specifics under the title. Null when the title already says it all. */
    val detail: String? = null,
)

enum class RiskLevel { safe, caution, dangerous }

/** Which pass produced a verdict. s1 = offline rules only; s2 = deep (network) analysis on top. */
enum class Stage { s1, s2 }

/**
 * The outcome of analysing a payload.
 *
 * Deliberately a sealed hierarchy and NOT something with an `isSafe: Boolean`. A boolean invites
 * `if (result.isSafe)`, and then every error path has to remember to set it false. Here a caller
 * must handle [Failed] explicitly to get anything out of the result, so an analysis that broke can
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
        /**
         * 0..100, derived from [level] AFTER the level was decided, and placed inside the band that
         * level owns. It is a finer grain for display, never an input to the colour.
         */
        val score: Int,
        val stage: Stage = Stage.s1,
        /** Signatures that threw. Non-empty means this verdict is INCOMPLETE, not clean. */
        val failedSignatures: List<String> = emptyList(),
        /** Signatures that ran to completion, fired or not. Lets the UI list what was checked. */
        val ranSignatures: List<String> = emptyList(),
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
