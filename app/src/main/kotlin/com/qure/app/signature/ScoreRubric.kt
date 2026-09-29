package com.qure.app.signature

import com.qure.app.domain.RiskLevel
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.domain.Stage
import com.qure.app.domain.Verdict

object ScoreRubric {

    const val greenCeiling = 100
    const val greenFloor = 85
    const val yellowCeiling = 84
    const val yellowFloor = 40
    const val redCeiling = 39
    const val redFloor = 0

    const val failedRulePenalty = 5

    const val offlineOnlyPenalty = 3

    const val firstWarnPenalty = 12
    const val nextWarnPenalty = 8
    const val infoPenalty = 5

    const val unresolvedScore = 60

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

            val dangers = signals.filter { it.severity == Severity.danger }
            val start = if (dangers.any { it.id in strongestSignals }) strongestDangerStart else dangerStart
            val extra = (dangers.size - 1).coerceAtLeast(0) * extraDangerPenalty
            (start - extra).coerceIn(redFloor, redCeiling)
        }
    }

    fun scoreOf(verdict: Verdict): Int = when (verdict) {
        is Verdict.Assessed -> verdict.score
        is Verdict.Failed, Verdict.NotAssessed -> unresolvedScore
    }

    fun bandOf(score: Int): RiskLevel = when {
        score >= greenFloor -> RiskLevel.safe
        score >= yellowFloor -> RiskLevel.caution
        else -> RiskLevel.dangerous
    }
}
