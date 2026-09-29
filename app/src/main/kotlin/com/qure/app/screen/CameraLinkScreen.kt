package com.qure.app.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.qure.app.R
import com.qure.app.ui.theme.Motion
import com.qure.app.ui.theme.QrYellow
import com.qure.app.ui.theme.Radius
import com.qure.app.ui.theme.RiskSafe
import com.qure.app.ui.theme.Spacing
import com.qure.app.ui.component.NoteCard
import com.qure.app.ui.component.PrimaryButton
import com.qure.app.ui.component.QureScaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.qure.app.link.defaultBrowserRequestIntent
import com.qure.app.link.isDefaultBrowser

@Composable
fun CameraLinkScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(context.isDefaultBrowser()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isDefault = context.isDefaultBrowser() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { isDefault = context.isDefaultBrowser() }

    QureScaffold(
        title = stringResource(R.string.menu_camera_link),
        onBack = onClose,
        bottomBar = {
            if (isDefault) {
                PrimaryButton(
                    text = stringResource(R.string.onb_continue),
                    onClick = onClose,
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.onb_enable),
                    onClick = { launcher.launch(context.defaultBrowserRequestIntent()) },
                )
            }
        },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.onb_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.sm))
            StatusChip(isDefault)
        }

        Spacer(Modifier.height(Spacing.md))
        Text(
            stringResource(R.string.onb_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.xl))
        Card(
            shape = Radius.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(Spacing.lg)) {
                Step(1, stringResource(R.string.onb_step1))
                Spacer(Modifier.height(Spacing.md))
                Step(2, stringResource(R.string.onb_step2))
                Spacer(Modifier.height(Spacing.md))
                Step(3, stringResource(R.string.onb_step3))
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        NoteCard(stringResource(R.string.onb_tradeoff))
    }
}

@Composable
private fun StatusChip(on: Boolean) {
    val tint by animateColorAsState(
        if (on) RiskSafe else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(Motion.medium),
        label = "statusTint",
    )
    Row(
        Modifier
            .background(tint.copy(alpha = 0.14f), Radius.pill)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).background(tint, CircleShape))
        Spacer(Modifier.width(Spacing.sm))
        Text(
            stringResource(if (on) R.string.onb_status_on else R.string.onb_status_off),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}

@Composable
private fun Step(n: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(24.dp).background(QrYellow.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                n.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = QrYellow,
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
