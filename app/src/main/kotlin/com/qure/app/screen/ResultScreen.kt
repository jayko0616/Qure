package com.qure.app.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qure.app.R
import com.qure.app.blacklist.Blacklist
import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.PayloadKind
import com.qure.app.domain.QrRiskAnalyzer
import com.qure.app.domain.RiskLevel
import com.qure.app.domain.Severity
import com.qure.app.domain.Signal
import com.qure.app.domain.UrlParser
import com.qure.app.domain.Verdict
import com.qure.app.signature.ScoreRubric
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
 *
 * What the screen shows about the score: the total, and nothing else. Findings are sentences,
 * ordered by severity. The order reveals rank, which cannot be hidden and does not need to be; it
 * never reveals distance, so the order alone cannot be used to find the edge of green.
 */
@Composable
fun ResultScreen(
    parsed: ParsedPayload,
    riskAnalyzer: QrRiskAnalyzer,
    onBack: () -> Unit,
    /** Null when the payload is not something a browser can open (Wi-Fi credentials, plain text…). */
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
    // Failure is caught here and mapped to Verdict.Failed, never to a default-safe value.
    val verdict by produceState<Verdict>(initialValue = Verdict.NotAssessed, parsed) {
        value = runCatching { riskAnalyzer.analyze(parsed) }
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
                    // No lists yet? Go straight to naming one. Showing an empty picker first would
                    // be a dead end wearing the costume of a choice.
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

// ── header ─────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun verdictColor(verdict: Verdict): Color = when (verdict) {
    is Verdict.Assessed -> when (verdict.level) {
        RiskLevel.safe -> RiskSafe
        RiskLevel.caution -> RiskCaution
        RiskLevel.dangerous -> RiskDanger
    }
    // Failed and not-yet-run are exactly as unresolved as caution, and borrow its colour. Neither
    // is ever allowed to borrow green.
    is Verdict.Failed -> RiskCaution
    Verdict.NotAssessed -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun VerdictHeader(verdict: Verdict) {
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
            // A clean run that lost rules says so in the headline, not in a footnote.
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
        }
    }
}

/** The one number the screen shows. Null while the analysis has not run yet. */
@Composable
private fun ScoreBadge(score: Int?, color: Color) {
    Box(
        Modifier
            .size(72.dp)
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

// ── findings ───────────────────────────────────────────────────────────────────────────────────

/**
 * Green lists what was checked; yellow and red list what was found.
 *
 * A green card with nothing on it looks like a check that never happened, so the rules that ran
 * clean are shown as ticks. Yellow and red show each signal as a title with one line of specifics
 * beneath it, worst first.
 */
@Composable
private fun FindingsCard(verdict: Verdict, parsed: ParsedPayload) {
    val assessed = verdict as? Verdict.Assessed
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
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

// ── actions ────────────────────────────────────────────────────────────────────────────────────

/**
 * The one place the user acts.
 *
 * Opening the link has to be possible — an inspection the user cannot act on is a dead end, and a
 * tool that strands people trains them to stop using it. But the two choices are not weighted
 * equally at every risk level. On a dangerous verdict the primary, thumb-sized button is the one
 * that takes you back, and opening anyway is a plain text button that names what it is. That is a
 * deliberate asymmetry, not a hidden control: both actions are always visible and always one tap.
 *
 * A failed or not-yet-run analysis is treated exactly like caution. It is emphatically not treated
 * like safe, because nothing has actually cleared the link.
 */
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
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.result_open_anyway),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // caution, analysis failed, or still running — same treatment: openable, but the label
            // says the user is the one deciding.
            else -> {
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(R.string.result_open_acknowledged)) }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(backLabel) }
            }
        }

        // Available at every risk level. The verdict the engine reached is an opinion; the user
        // deciding this particular code is bad is a fact, and the app should take it either way.
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

// ── payload ────────────────────────────────────────────────────────────────────────────────────

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
private fun EngineNotice() {
    Text(
        stringResource(R.string.result_engine_notice),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ── blacklist dialogs ──────────────────────────────────────────────────────────────────────────

@Composable
private fun ListPickerDialog(
    lists: List<Blacklist>,
    onPick: (Blacklist) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
