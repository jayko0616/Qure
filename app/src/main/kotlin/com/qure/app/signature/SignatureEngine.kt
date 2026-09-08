package com.qure.app.signature

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.QrRiskAnalyzer
import com.qure.app.domain.RiskLevel
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.domain.Stage
import com.qure.app.domain.Verdict
import kotlinx.coroutines.coroutineScope

/**
 * Runs every enabled [Signature] and folds their signals into one [Verdict].
 *
 * This is the only place that aggregates. Individual rules deliberately cannot see each other's
 * output, so a rule can never suppress another's finding, and the severity policy lives in exactly
 * one readable place instead of being smeared across a dozen files.
 *
 * The engine never decides anything itself beyond that fold — swapping the whole detection strategy
 * means editing `Signatures.kt`, not this class.
 */
class SignatureEngine(
    private val signatures: List<Signature> = Signatures.rules,
    /** Which pass this engine represents. Only affects the score, never the level. */
    private val stage: Stage = Stage.s1,
) : QrRiskAnalyzer {

    override suspend fun analyze(payload: ParsedPayload): Verdict = coroutineScope {
        val active = signatures.filter { it.enabled }
        if (active.isEmpty()) return@coroutineScope Verdict.Failed("등록된 검사 규칙이 없습니다")

        val signals = mutableListOf<Signal>()
        val failed = mutableListOf<String>()

        // Sequential on purpose: today every rule is pure and instant, so concurrency would buy
        // nothing but nondeterministic signal ordering. The moment a rule does real I/O, this is
        // the one line to change.
        for (signature in active) {
            try {
                signals += signature.inspect(payload)
            } catch (t: Throwable) {
                // A rule that blew up has told us nothing. It must not be mistaken for a rule that
                // looked and found nothing.
                failed += signature.id
            }
        }

        if (failed.size == active.size) {
            return@coroutineScope Verdict.Failed("모든 검사 규칙이 실행에 실패했습니다")
        }

        val ordered = signals
            .distinctBy { it.id }
            .sortedByDescending { it.severity.ordinal }

        // ── The colour. Worst severity present, nothing else. ──────────────────────────────────
        // This `when` takes exactly one input: the most severe signal. No score, count, or sum is
        // allowed to reach it. A points system would let a single decisive danger (the "@"
        // disguise, a blacklisted host) be outvoted by the absence of company, or let several
        // warnings add up to red. Either would break the promise each severity makes on its own.
        val level = when (ordered.maxByOrNull { it.severity.ordinal }?.severity) {
            Severity.danger -> RiskLevel.dangerous
            Severity.warn, Severity.info -> RiskLevel.caution
            null -> RiskLevel.safe
        }

        // ── The number. Computed FROM the level, after it is final. ────────────────────────────
        // Nothing below this line feeds back into the `when` above. The rubric only ever places a
        // score inside the band the level already chose.
        val score = ScoreRubric.score(level, ordered, failed, stage)

        Verdict.Assessed(
            level = level,
            headline = headlineFor(level, failed.isNotEmpty()),
            signals = ordered,
            score = score,
            stage = stage,
            failedSignatures = failed,
            ranSignatures = active.map { it.id }.filterNot { it in failed },
        )
    }

    private fun headlineFor(level: RiskLevel, incomplete: Boolean): String = when {
        // An incomplete run must never be presented as a clean one, even when nothing fired.
        incomplete && level == RiskLevel.safe -> "일부 검사를 완료하지 못했습니다"
        level == RiskLevel.dangerous -> "위험 신호가 발견되었습니다"
        level == RiskLevel.caution -> "주의가 필요합니다"
        else -> "눈에 띄는 위험 신호는 없습니다"
    }
}
