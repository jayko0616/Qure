package com.qure.app.signature.builtin

import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.QrRiskAnalyzer
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.domain.UrlParser
import com.qure.app.domain.Verdict
import com.qure.app.network.RedirectResolver
import com.qure.app.signature.Signature

class RedirectSignature(
    private val resolver: RedirectResolver,

    private val destinationAnalyzer: QrRiskAnalyzer,

    private val quietHops: Int = 2,
) : Signature {

    override val id = "redirect"

    override suspend fun inspect(payload: ParsedPayload): List<Signal> {
        if (!payload.isWebLink) return emptyList()

        val chain = resolver.resolve(payload.raw.trim())
        if (chain.redirectCount == 0) return emptyList()

        val destination = UrlParser.parse(chain.finalUrl)
        val signals = mutableListOf<Signal>()

        if (chain.truncated) {
            signals += Signal(
                "redirectTruncated", Severity.warn,
                title = "추적 한도 초과",
                detail = "리다이렉트가 너무 많아 최종 목적지를 확인하지 못했습니다",
            )
        } else {
            signals += Signal(
                id, severityForArrival(payload, destination),
                title = "실제 목적지 확인됨",
                detail = destinationDetail(chain.redirectCount, destination),
            )
        }

        if (chain.redirectCount > quietHops) {
            signals += Signal(
                "redirectDepth", Severity.warn,
                title = "다단계 리다이렉트",
                detail = "${chain.redirectCount}번을 거쳐 이동합니다. 추적을 어렵게 하려는 구조일 수 있습니다",
            )
        }

        if (payload.isHttps && destination.isWebLink && !destination.isHttps) {
            signals += Signal(
                "redirectDowngrade", Severity.danger,
                title = "암호화 해제",
                detail = "https로 시작했지만 암호화되지 않은 http 주소로 내려갑니다",
            )
        }

        if (!chain.truncated) signals += destinationSignals(destination)
        return signals
    }

    private suspend fun destinationSignals(destination: ParsedPayload): List<Signal> =
        when (val verdict = destinationAnalyzer.analyze(destination)) {
            is Verdict.Assessed -> verdict.signals.map {
                it.copy(id = "dest.${it.id}", title = "목적지 — ${it.title}")
            }

            is Verdict.Failed -> throw IllegalStateException(verdict.reason)
            Verdict.NotAssessed -> emptyList()
        }

    private fun severityForArrival(scanned: ParsedPayload, destination: ParsedPayload): Severity =
        if (scanned.registrableSuffix != null &&
            destination.registrableSuffix != null &&
            scanned.registrableSuffix != destination.registrableSuffix
        ) Severity.warn else Severity.info

    private fun destinationDetail(hops: Int, destination: ParsedPayload): String {
        val host = destination.host ?: UrlParser.toDisplayString(destination.raw, max = 60)
        return if (hops == 1) "실제로는 $host 로 이동합니다"
        else "${hops}번 이동한 끝에 $host 에 도착합니다"
    }
}
