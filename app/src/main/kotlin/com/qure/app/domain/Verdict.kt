package com.qure.app.domain

import androidx.compose.runtime.Immutable

enum class Severity { info, warn, danger }

@Immutable
data class Signal(

    val id: String,

    val severity: Severity,

    val title: String,

    val detail: String? = null,
)

enum class RiskLevel { safe, caution, dangerous }

enum class Stage { s1, s2 }

@Immutable
sealed interface Verdict {

    @Immutable
    data class Assessed(
        val level: RiskLevel,
        val headline: String,
        val signals: List<Signal>,

        val score: Int,
        val stage: Stage = Stage.s1,

        val failedSignatures: List<String> = emptyList(),

        val ranSignatures: List<String> = emptyList(),
    ) : Verdict

    @Immutable
    data class Failed(val reason: String) : Verdict

    @Immutable
    data object NotAssessed : Verdict
}

interface QrRiskAnalyzer {
    suspend fun analyze(payload: ParsedPayload): Verdict
}
