package com.qure.app.signature

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.QrRiskAnalyzer
import com.qure.app.domain.RiskLevel
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.domain.Stage
import com.qure.app.domain.Verdict
import kotlinx.coroutines.coroutineScope

class SignatureEngine(
    private val signatures: List<Signature> = Signatures.rules,

    private val stage: Stage = Stage.s1,
) : QrRiskAnalyzer {

    override suspend fun analyze(payload: ParsedPayload): Verdict = coroutineScope {
        val active = signatures.filter { it.enabled }
        if (active.isEmpty()) return@coroutineScope Verdict.Failed("등록된 검사 규칙이 없습니다")

        val signals = mutableListOf<Signal>()
        val failed = mutableListOf<String>()

        for (signature in active) {
            try {
                signals += signature.inspect(payload)
            } catch (t: Throwable) {

                failed += signature.id
            }
        }

        if (failed.size == active.size) {
            return@coroutineScope Verdict.Failed("모든 검사 규칙이 실행에 실패했습니다")
        }

        val ordered = signals
            .distinctBy { it.id }
            .sortedByDescending { it.severity.ordinal }

        val level = when (ordered.maxByOrNull { it.severity.ordinal }?.severity) {
            Severity.danger -> RiskLevel.dangerous
            Severity.warn, Severity.info -> RiskLevel.caution
            null -> RiskLevel.safe
        }

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

        incomplete && level == RiskLevel.safe -> "일부 검사를 완료하지 못했습니다"
        level == RiskLevel.dangerous -> "위험 신호가 발견되었습니다"
        level == RiskLevel.caution -> "주의가 필요합니다"
        else -> "눈에 띄는 위험 신호는 없습니다"
    }
}
