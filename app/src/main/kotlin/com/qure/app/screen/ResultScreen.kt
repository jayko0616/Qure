package com.qure.app.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.qure.app.R
import com.qure.app.blacklist.Blacklist
import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.PayloadKind
import com.qure.app.domain.QrRiskAnalyzer
import com.qure.app.domain.RiskLevel
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.domain.UrlParser
import com.qure.app.domain.Stage
import com.qure.app.domain.Verdict
import com.qure.app.signature.ScoreRubric
import com.qure.app.ui.theme.Radius
import com.qure.app.ui.theme.RiskCaution
import com.qure.app.ui.theme.RiskDanger
import com.qure.app.ui.theme.RiskSafe
import com.qure.app.ui.theme.Sizing
import com.qure.app.ui.theme.Spacing
import com.qure.app.ui.component.NoteCard
import com.qure.app.ui.text.highlightHost
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun ResultScreen(
    parsed: ParsedPayload,
    riskAnalyzer: QrRiskAnalyzer,
    onBack: () -> Unit,

    deepAnalyzer: QrRiskAnalyzer? = null,

    onOpen: (() -> Unit)? = null,
    backLabel: String = stringResource(R.string.result_back),
    blacklists: List<Blacklist> = emptyList(),
    onAddToList: (listId: String, value: String) -> Unit = { _, _ -> },
    onCreateListAndAdd: (name: String, value: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }
    var namingList by remember { mutableStateOf(false) }
    var addedTo by remember { mutableStateOf<String?>(null) }

    val verdict by produceState<Verdict>(initialValue = Verdict.NotAssessed, parsed, deepAnalyzer) {
        val shallow = runCatching { riskAnalyzer.analyze(parsed) }
            .getOrElse { Verdict.Failed(it.message ?: it::class.simpleName.orEmpty()) }
        value = shallow

        val deep = deepAnalyzer ?: return@produceState
        if (!parsed.isWebLink) return@produceState
        runCatching { deep.analyze(parsed) }.onSuccess { value = it }
    }
    val deepening = deepAnalyzer != null && parsed.isWebLink &&
        (verdict as? Verdict.Assessed)?.stage == Stage.s1

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            VerdictHeader(verdict, deepening)
            Spacer(Modifier.height(24.dp))
            PayloadCard(parsed)
            Spacer(Modifier.height(16.dp))
            FindingsCard(verdict, parsed)
            Spacer(Modifier.height(16.dp))
            EngineNotice()
            Spacer(Modifier.height(24.dp))
            Actions(
                verdict = verdict,
                canOpen = onOpen != null && parsed.isWebLink,
                backLabel = backLabel,
                addedTo = addedTo,
                onOpen = onOpen ?: {},
                onBack = onBack,
                onAddToBlacklist = {

                    if (blacklists.isEmpty()) namingList = true else picking = true
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (picking) {
        ListPickerDialog(
            lists = blacklists,
            onPick = { list ->
                onAddToList(list.id, parsed.raw)
                addedTo = list.name
                picking = false
            },
            onCreateNew = { picking = false; namingList = true },
            onDismiss = { picking = false },
        )
    }
    if (namingList) {
        NameListDialog(
            onConfirm = { name ->
                onCreateListAndAdd(name, parsed.raw)
                addedTo = name
                namingList = false
            },
            onDismiss = { namingList = false },
        )
    }
}

@Composable
private fun verdictColor(verdict: Verdict): Color = when (verdict) {
    is Verdict.Assessed -> when (verdict.level) {
        RiskLevel.safe -> RiskSafe
        RiskLevel.caution -> RiskCaution
        RiskLevel.dangerous -> RiskDanger
    }

    is Verdict.Failed -> RiskCaution
    Verdict.NotAssessed -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun VerdictHeader(verdict: Verdict, deepening: Boolean = false) {
    val color = verdictColor(verdict)
    val title = when (verdict) {
        is Verdict.Assessed -> stringResource(
            when (verdict.level) {
                RiskLevel.safe -> R.string.level_safe
                RiskLevel.caution -> R.string.level_caution
                RiskLevel.dangerous -> R.string.level_danger
            },
        )
        is Verdict.Failed -> stringResource(R.string.verdict_failed)
        Verdict.NotAssessed -> stringResource(R.string.verdict_pending)
    }
    val subtitle = when (verdict) {
        is Verdict.Assessed -> when {

            verdict.level == RiskLevel.safe && verdict.failedSignatures.isNotEmpty() -> verdict.headline
            verdict.level == RiskLevel.safe -> stringResource(R.string.verdict_sub_safe)
            verdict.level == RiskLevel.caution -> stringResource(R.string.verdict_sub_caution)
            else -> stringResource(R.string.verdict_sub_danger)
        }
        is Verdict.Failed -> verdict.reason
        Verdict.NotAssessed -> ""
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ScoreBadge(
            score = if (verdict is Verdict.NotAssessed) null else ScoreRubric.scoreOf(verdict),
            color = color,
        )
        Spacer(Modifier.width(16.dp))
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

            if (deepening) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.verdict_deepening),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreBadge(score: Int?, color: Color) {
    Box(
        Modifier
            .size(Sizing.scoreBadge)
            .background(color.copy(alpha = 0.12f), CircleShape)
            .border(3.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = score?.toString() ?: "–",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun FindingsCard(verdict: Verdict, parsed: ParsedPayload) {
    val assessed = verdict as? Verdict.Assessed
    Card(
        shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            when {
                assessed == null -> {
                    Label(stringResource(R.string.result_pending_title))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(
                            if (verdict is Verdict.Failed) R.string.result_not_run
                            else R.string.verdict_pending
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                assessed.level == RiskLevel.safe -> {
                    Label(stringResource(R.string.result_checked_title))
                    Spacer(Modifier.height(8.dp))
                    val passed = passedChecks(assessed, parsed)
                    if (passed.isEmpty()) {
                        Text(
                            stringResource(R.string.result_no_signals),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        passed.forEach { CheckRow(stringResource(it)) }
                    }
                }

                else -> {
                    Label(
                        stringResource(
                            if (assessed.level == RiskLevel.dangerous) R.string.result_danger_title
                            else R.string.result_pending_title
                        ),
                    )
                    Spacer(Modifier.height(8.dp))
                    assessed.signals.forEach { SignalRow(it) }
                }
            }

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
private fun CheckRow(text: String) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.Check,
            contentDescription = null,
            tint = RiskSafe,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SignalRow(signal: Signal) {
    val dot = when (signal.severity) {
        Severity.danger -> RiskDanger
        Severity.warn -> RiskCaution
        Severity.info -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp).size(8.dp).background(dot, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                signal.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            signal.detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Actions(
    verdict: Verdict,
    canOpen: Boolean,
    backLabel: String,
    addedTo: String?,
    onOpen: () -> Unit,
    onBack: () -> Unit,
    onAddToBlacklist: () -> Unit,
) {
    val level = (verdict as? Verdict.Assessed)?.level
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        when {
            !canOpen -> {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(backLabel)
                }
            }

            level == RiskLevel.safe -> {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RiskSafe, contentColor = Color(0xFF04210E),
                    ),
                ) { Text(stringResource(R.string.result_open), style = MaterialTheme.typography.titleMedium) }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(backLabel) }
            }

            level == RiskLevel.dangerous -> {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(backLabel, style = MaterialTheme.typography.titleMedium) }
                Spacer(Modifier.height(Spacing.xs))
                TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.result_open_anyway),

                        color = RiskDanger.copy(alpha = 0.85f),
                    )
                }
            }

            else -> {
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(R.string.result_open_acknowledged)) }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(backLabel) }
            }
        }

        if (addedTo == null) {
            TextButton(onClick = onAddToBlacklist, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.result_add_blacklist),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Text(
                stringResource(R.string.result_added_to, addedTo),
                style = MaterialTheme.typography.labelMedium,
                color = RiskCaution,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PayloadCard(parsed: ParsedPayload) {
    Card(
        shape = Radius.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
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
private fun EngineNotice() {

    NoteCard(stringResource(R.string.result_engine_notice))
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

@Composable
private fun ListPickerDialog(
    lists: List<Blacklist>,
    onPick: (Blacklist) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Radius.sheet,
        title = { Text(stringResource(R.string.result_pick_list)) },
        text = {
            Column {
                lists.forEach { list ->
                    TextButton(
                        onClick = { onPick(list) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "${list.name}  (${list.entries.size})",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreateNew) { Text(stringResource(R.string.blacklist_new_list)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.blacklist_cancel)) }
        },
    )
}

@Composable
private fun NameListDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Radius.sheet,
        title = { Text(stringResource(R.string.blacklist_new_list)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(R.string.blacklist_list_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }, enabled = value.isNotBlank()) {
                Text(stringResource(R.string.blacklist_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.blacklist_cancel)) }
        },
    )
}
