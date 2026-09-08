package com.qure.app.signature

import com.qure.app.domain.RiskLevel
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.domain.Stage
import com.qure.app.domain.Verdict

/**
 * Turns a verdict that has ALREADY been coloured into a 0..100 score.
 *
 * The order of operations is the whole design, and it is not negotiable:
 *   1. signatures raise signals
 *   2. the engine picks the level from the single worst severity present
 *   3. the level picks a band here
 *   4. the signals place the score inside that band
 *
 * The score is display. It gives a colour a finer grain, so that "노랑 78" (barely caught) and
 * "노랑 45" (quite suspicious) read differently without a fourth colour. It is never an input to
 * step 2: a points total that could promote or demote the colour would let one decisive danger
 * sink to amber for lack of company, or let a pile of warnings add up to red. Both are design
 * violations, because a signal like the "@" disguise justifies "do not open" entirely on its own.
 *
 * Bands do not overlap, so the number alone tells you the colour and the colour alone bounds the
 * number. Per-signal penalties are internal to this object and are never attached to a [Signal]
 * or shown on screen; only the total leaves the engine. Every constant lives here so tuning against
 * a test set is a one-file change.
 */
object ScoreRubric {

    // ── Bands. Disjoint by construction; bandOf() is the single source of truth for the edges. ──
    const val greenCeiling = 100
    const val greenFloor = 85
    const val yellowCeiling = 84
    const val yellowFloor = 40
    const val redCeiling = 39
    const val redFloor = 0

    // ── Green: start at 100, minor deductions only. ──
    /** At least one rule threw. The run is incomplete, and the number should say so a little. */
    const val failedRulePenalty = 5
    /** Only the offline pass ran. A deep (s2) pass that comes back clean earns the full 100. */
    const val offlineOnlyPenalty = 3

    // ── Yellow: start at 84, deduct per signal. ──
    const val firstWarnPenalty = 12
    const val nextWarnPenalty = 8
    const val infoPenalty = 5
    /** A verdict that never happened (failed, or not yet run) sits here: unresolved, not suspect. */
    const val unresolvedScore = 60

    // ── Red: where you start depends on what fired. ──
    /** Signals that on their own put the start at [strongestDangerStart]. */
    val strongestSignals: Set<String> = setOf("brandLookalike", "userBlacklist", "userinfo")
    const val strongestDangerStart = 15
    const val dangerStart = 30
    const val extraDangerPenalty = 8

    fun score(
        level: RiskLevel,
        signals: List<Signal>,
        failedSignatures: List<String>,
        stage: Stage,
    ): Int = when (level) {
        RiskLevel.safe -> {
            var s = greenCeiling
            if (failedSignatures.isNotEmpty()) s -= failedRulePenalty
            if (stage == Stage.s1) s -= offlineOnlyPenalty
            s.coerceIn(greenFloor, greenCeiling)
        }

        RiskLevel.caution -> {
            val warns = signals.count { it.severity == Severity.warn }
            val infos = signals.count { it.severity == Severity.info }
            var s = yellowCeiling
            if (warns > 0) s -= firstWarnPenalty + (warns - 1) * nextWarnPenalty
            s -= infos * infoPenalty
            s.coerceIn(yellowFloor, yellowCeiling)
        }

        RiskLevel.dangerous -> {
            // Warnings do not move the needle inside red: the colour already says everything a
            // warning could add, and counting them would leak how many fired.
            val dangers = signals.filter { it.severity == Severity.danger }
            val start = if (dangers.any { it.id in strongestSignals }) strongestDangerStart else dangerStart
            val extra = (dangers.size - 1).coerceAtLeast(0) * extraDangerPenalty
            (start - extra).coerceIn(redFloor, redCeiling)
        }
    }

    /** The number to show for any verdict, including the ones that carry no score of their own. */
    fun scoreOf(verdict: Verdict): Int = when (verdict) {
        is Verdict.Assessed -> verdict.score
        is Verdict.Failed, Verdict.NotAssessed -> unresolvedScore
    }

    /** The band a score falls in. Exists so tests can assert score and level never disagree. */
    fun bandOf(score: Int): RiskLevel = when {
        score >= greenFloor -> RiskLevel.safe
        score >= yellowFloor -> RiskLevel.caution
        else -> RiskLevel.dangerous
    }
}
