package com.qure.app.link

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.qure.app.BuildConfig
import com.qure.app.R
import com.qure.app.signature.SignatureEngine
import androidx.compose.ui.platform.LocalContext
import com.qure.app.account.LocalAccountStore
import com.qure.app.blacklist.BlacklistQuota
import com.qure.app.blacklist.LocalBlacklistStore
import com.qure.app.blacklist.UserBlacklistSignature
import com.qure.app.signature.Signatures
import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.UrlParser
import com.qure.app.ui.component.QuotaDialog
import com.qure.app.screen.ResultScreen
import com.qure.app.ui.theme.QrYellow
import com.qure.app.ui.theme.Radius
import com.qure.app.ui.theme.QureTheme
import com.qure.app.ui.text.highlightHost
import com.qure.app.ui.text.middleEllipsis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

class InspectActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (uri == null) { finish(); return }

        val from = referrer?.host
        if (BuildConfig.DEBUG) {

            Log.i(logTag, "VIEW referrer=$from scheme=${uri.scheme}")
        }

        if (LinkOrigin.decide(from, packageName) == LinkOrigin.Decision.passThrough) {

            LinkHandoff.open(this, uri)

            finish()
            return
        }

        val parsed = UrlParser.parse(uri.toString())
        setContent {
            QureTheme(forceDark = true) {
                InspectFlow(
                    parsed = parsed,
                    onOpenDirectly = {

                        if (!LinkHandoff.open(this, uri)) Log.w(logTag, "no browser to hand off to")
                        finish()
                    },
                    onDone = { finish() },
                )
            }
        }
    }

    private companion object { const val logTag = "QureLink" }
}

@Composable
private fun InspectFlow(
    parsed: ParsedPayload,
    onOpenDirectly: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val store = remember(context) { LocalBlacklistStore(context) }
    val blacklists by store.lists.collectAsStateWithLifecycle()

    val account = remember(context) { LocalAccountStore(context) }
    val profile by account.profile.collectAsStateWithLifecycle()
    var quotaBlocked by remember { mutableStateOf(false) }

    fun refusedByQuota(listId: String?, value: String): Boolean {
        val targetEntries = blacklists.firstOrNull { it.id == listId }?.entries.orEmpty()
        val refused = BlacklistQuota.wouldExceed(profile.signedIn, blacklists, targetEntries, value)
        if (refused) quotaBlocked = true
        return refused
    }
    val userRule = remember(store) { UserBlacklistSignature { store.allEntries() } }
    val analyzer = remember(userRule) { SignatureEngine(Signatures.rules + userRule) }

    val deepAnalyzer = remember(userRule) { Signatures.deepEngine(extraRules = listOf(userRule)) }
    var inspected by remember { mutableStateOf(false) }

    if (!inspected) {

        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            ConfirmCard(
                parsed = parsed,
                onYes = { inspected = true },
                onNo = onOpenDirectly,
            )
        }
    } else {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ResultScreen(
                parsed = parsed,
                riskAnalyzer = analyzer,
                deepAnalyzer = deepAnalyzer,
                onBack = onDone,
                onOpen = onOpenDirectly,
                backLabel = stringResource(R.string.result_close),
                blacklists = blacklists,
                onAddToList = { id, v ->
                    if (!refusedByQuota(id, v)) scope.launch { store.addEntry(id, v) }
                },
                onCreateListAndAdd = { name, v ->
                    if (!refusedByQuota(null, v)) {
                        scope.launch { store.addEntry(store.createList(name), v) }
                    }
                },
            )
        }
    }

    if (quotaBlocked) {
        QuotaDialog(
            limit = BlacklistQuota.anonymousEntryLimit,
            onDismiss = { quotaBlocked = false },
        )
    }
}

@Composable
private fun ConfirmCard(parsed: ParsedPayload, onYes: () -> Unit, onNo: () -> Unit) {
    val safeText = remember(parsed.raw) { middleEllipsis(UrlParser.toDisplayString(parsed.raw)) }
    Card(
        shape = Radius.sheet,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.prompt_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = highlightHost(
                    safeText, parsed.host,
                    dim = MaterialTheme.colorScheme.onSurfaceVariant,
                    bright = MaterialTheme.colorScheme.onSurface,
                ),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.prompt_not_opened),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {

                TextButton(onClick = onNo) { Text(stringResource(R.string.prompt_open_directly)) }
                Button(
                    onClick = onYes,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QrYellow, contentColor = Color(0xFF201A00),
                    ),
                ) { Text(stringResource(R.string.prompt_yes)) }
            }
        }
    }
}
