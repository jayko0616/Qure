package com.qure.app.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.qure.app.R
import com.qure.app.link.defaultBrowserRequestIntent
import com.qure.app.link.isDefaultBrowser
import com.qure.app.ui.theme.QrYellow

/**
 * Opt-in for reading QR codes that the PHONE'S OWN camera app finds.
 *
 * Reached from the scanner, never forced at first launch — see the note in MainActivity.
 *
 * The surprising part, and the reason this screen has to explain itself: there is no permission for
 * this. Android has no "let this app see the camera app's results" grant. The only supported hook
 * is the ACTION_VIEW intent the camera fires when the user taps its QR chip, and since Android 12
 * that intent does not reach non-browser apps at all — unverified web links go straight to the
 * default browser with no chooser. So the switch is the default-browser role, and only the user can
 * flip it.
 */
@Composable
fun CameraLinkScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(context.isDefaultBrowser()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isDefault = context.isDefaultBrowser() }

    // The role dialog and the settings page both return without a useful result code on some
    // devices, so re-read the real state whenever we come back to the foreground.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { isDefault = context.isDefaultBrowser() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.onb_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.onb_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Step("1", stringResource(R.string.onb_step1))
                    Spacer(Modifier.height(10.dp))
                    Step("2", stringResource(R.string.onb_step2))
                    Spacer(Modifier.height(10.dp))
                    Step("3", stringResource(R.string.onb_step3))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.onb_tradeoff),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            if (isDefault) {
                Text(
                    stringResource(R.string.onb_enabled),
                    style = MaterialTheme.typography.titleSmall,
                    color = QrYellow,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QrYellow, contentColor = Color(0xFF201A00),
                    ),
                ) { Text(stringResource(R.string.onb_continue)) }
            } else {
                Button(
                    onClick = { launcher.launch(context.defaultBrowserRequestIntent()) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QrYellow, contentColor = Color(0xFF201A00),
                    ),
                ) { Text(stringResource(R.string.onb_enable)) }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onb_skip))
                }
            }
        }
    }
}

@Composable
private fun Step(n: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(n, style = MaterialTheme.typography.titleSmall, color = QrYellow)
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
