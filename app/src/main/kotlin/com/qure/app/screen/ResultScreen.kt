package com.qure.app.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.qure.app.R
import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.PayloadKind
import com.qure.app.domain.QrRiskAnalyzer
import com.qure.app.domain.RiskLevel
import com.qure.app.domain.Severity
import com.qure.app.domain.UrlParser
import com.qure.app.domain.Verdict
import com.qure.app.ui.theme.RiskCaution
import com.qure.app.ui.theme.RiskDanger
import com.qure.app.ui.theme.RiskSafe
import com.qure.app.ui.theme.highlightHost

/**
 * The verdict.
 *
 * Everything on this screen is inert text. Nothing here is tappable through to the payload, by
 * design: an anti-phishing app that offers a one-tap "open it anyway" on the same screen as the
 * warning has just built a better phishing funnel than the attacker had.
 */
@Composable
fun ResultScreen(
    parsed: ParsedPayload,
    analyzer: QrRiskAnalyzer,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Failure is caught here and mapped to Verdict.Failed, never to a default-safe value.
    val verdict by produceState<Verdict>(initialValue = Verdict.NotAssessed, parsed) {
        value = runCatching { analyzer.analyze(parsed) }
            .getOrElse { Verdict.Failed(it.message ?: it::class.simpleName.orEmpty()) }
    }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            VerdictHeader(verdict)
            Spacer(Modifier.height(24.dp))
            PayloadCard(parsed)
            Spacer(Modifier.height(16.dp))
            SignalsCard(verdict)
            Spacer(Modifier.height(16.dp))
            EngineNotice()
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            ) { Text(stringResource(R.string.result_back)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VerdictHeader(verdict: Verdict) {
    val (color, title, subtitle) = when (verdict) {
        is Verdict.Assessed -> when (verdict.level) {
            RiskLevel.safe -> Triple(RiskSafe, verdict.headline, stringResource(R.string.verdict_sub_safe))
            RiskLevel.caution -> Triple(RiskCaution, verdict.headline, stringResource(R.string.verdict_sub_caution))
            RiskLevel.dangerous -> Triple(RiskDanger, verdict.headline, stringResource(R.string.verdict_sub_danger))
        }
        is Verdict.Failed -> Triple(
            RiskCaution, stringResource(R.string.verdict_failed), verdict.reason,
        )
        Verdict.NotAssessed -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(R.string.verdict_pending), "",
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = color)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PayloadCard(parsed: ParsedPayload) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Label(stringResource(R.string.result_host))
            Text(
                parsed.host ?: stringResource(R.string.result_no_host),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(14.dp))
            Label(stringResource(R.string.result_raw))
            Text(
                // Neutralised for display: invisible and direction-flipping characters are shown
                // as escapes, so the string on screen cannot differ from the string that was read.
                text = highlightHost(
                    UrlParser.toDisplayString(parsed.raw),
                    parsed.host,
                    dim = MaterialTheme.colorScheme.onSurfaceVariant,
                    bright = MaterialTheme.colorScheme.onSurface,
                ),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(14.dp))
            Label(stringResource(R.string.result_kind))
            Text(
                // Never parsed.kind.name: that is an internal identifier, and leaking it puts
                // "httpUrl" in front of a user who is trying to decide whether to trust a link.
                stringResource(kindLabelRes(parsed.kind)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@StringRes
private fun kindLabelRes(kind: PayloadKind): Int = when (kind) {
    PayloadKind.httpUrl -> R.string.kind_http_url
    PayloadKind.wifi -> R.string.kind_wifi
    PayloadKind.tel -> R.string.kind_tel
    PayloadKind.sms -> R.string.kind_sms
    PayloadKind.mailto -> R.string.kind_mailto
    PayloadKind.geo -> R.string.kind_geo
    PayloadKind.appIntent -> R.string.kind_app_intent
    PayloadKind.otherScheme -> R.string.kind_other_scheme
    PayloadKind.plainText -> R.string.kind_plain_text
}

@Composable
private fun SignalsCard(verdict: Verdict) {
    val assessed = verdict as? Verdict.Assessed
    val signals = assessed?.signals.orEmpty()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Label(stringResource(R.string.result_signals))
            Spacer(Modifier.height(8.dp))
            if (signals.isEmpty()) {
                Text(
                    stringResource(R.string.result_no_signals),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                signals.forEach { s ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        val c = when (s.severity) {
                            Severity.danger -> RiskDanger
                            Severity.warn -> RiskCaution
                            Severity.info -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Box(Modifier.padding(top = 6.dp).size(8.dp).background(c, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            s.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            // A run that lost rules is not a clean run. Saying so on the card keeps the omission
            // visible next to the findings rather than buried in a log.
            if (!assessed?.failedSignatures.isNullOrEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.result_incomplete),
                    style = MaterialTheme.typography.labelSmall,
                    color = RiskCaution,
                )
            }
        }
    }
}

@Composable
private fun EngineNotice() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column {
            Text(
                stringResource(R.string.result_engine_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
