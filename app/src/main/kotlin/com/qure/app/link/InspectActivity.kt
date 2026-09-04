package com.qure.app.link

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qure.app.BuildConfig
import com.qure.app.R
import com.qure.app.signature.SignatureEngine
import androidx.compose.ui.platform.LocalContext
import com.qure.app.blacklist.LocalBlacklistStore
import com.qure.app.blacklist.UserBlacklistSignature
import com.qure.app.signature.Signatures
import com.qure.app.domain.ParsedPayload
import com.qure.app.domain.UrlParser
import com.qure.app.screen.ResultScreen
import com.qure.app.ui.theme.QrYellow
import com.qure.app.ui.theme.QureTheme
import com.qure.app.ui.theme.highlightHost
import com.qure.app.ui.theme.middleEllipsis

/**
 * The stock-camera path.
 *
 * The user points Samsung's camera at a QR, Samsung shows its chip, the user taps it, and Android
 * fires ACTION_VIEW. This activity is what receives that — but only while Qure holds the default
 * browser role, because Android 12+ removed the disambiguation dialog for unverified web links and
 * routes them straight to the browser. A non-browser app is never even offered.
 *
 * The theme is translucent, so the prompt appears over whatever the user was looking at (the camera
 * viewfinder) instead of yanking them into a different app. No overlay permission, no accessibility
 * service, no background service.
 */
class InspectActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (uri == null) { finish(); return }

        val from = referrer?.host
        if (BuildConfig.DEBUG) {
            // The one fact we cannot know without a real device: what the stock camera actually
            // reports as the referrer. Logged (never the payload) so the routing list can be
            // tightened from evidence instead of guesswork.
            Log.i(logTag, "VIEW referrer=$from scheme=${uri.scheme}")
        }

        if (LinkOrigin.decide(from, packageName) == LinkOrigin.Decision.passThrough) {
            // Decided BEFORE setContent, so no frame is ever composed and ordinary browsing sees
            // no flash and no perceptible delay.
            LinkHandoff.open(this, uri)
            // No transition override needed: Theme.Qure.Transparent already sets
            // windowAnimationStyle to null, so this activity never animates in or out.
            finish()
            return
        }

        val parsed = UrlParser.parse(uri.toString())
        setContent {
            QureTheme(forceDark = true) {
                InspectFlow(
                    parsed = parsed,
                    onOpenDirectly = {
                        // Declining the check is not the same as cancelling the link. The user
                        // tapped a QR in their camera and is owed the page they asked for —
                        // swallowing it would make Qure feel broken and teach people to
                        // uninstall it. Qure's job is to offer the check, not to hold links
                        // hostage.
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
    // The stock-camera path must honour the user's lists too, or the same code would be judged
    // differently depending on which way it was scanned — and it must be able to ADD to them, or
    // the user would have to remember the address and go find the app.
    val store = remember(context) { LocalBlacklistStore(context) }
    val blacklists by store.lists.collectAsStateWithLifecycle()
    val analyzer = remember(store) {
        SignatureEngine(Signatures.rules + UserBlacklistSignature { store.allEntries() })
    }
    var inspected by remember { mutableStateOf(false) }

    if (!inspected) {
        // Scrim only — the camera behind stays visible, which is what makes this feel like an
        // overlay on the viewfinder rather than a context switch.
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
                onBack = onDone,
                onOpen = onOpenDirectly,
                backLabel = stringResource(R.string.result_close),
                blacklists = blacklists,
                onAddToList = { id, v -> scope.launch { store.addEntry(id, v) } },
                onCreateListAndAdd = { name, v ->
                    scope.launch { store.addEntry(store.createList(name), v) }
                },
            )
        }
    }
}

@Composable
private fun ConfirmCard(parsed: ParsedPayload, onYes: () -> Unit, onNo: () -> Unit) {
    val safeText = remember(parsed.raw) { middleEllipsis(UrlParser.toDisplayString(parsed.raw)) }
    Card(
        shape = RoundedCornerShape(20.dp),
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
                // Says what it does. A bare "No" would leave the user guessing whether the page
                // is still coming.
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
