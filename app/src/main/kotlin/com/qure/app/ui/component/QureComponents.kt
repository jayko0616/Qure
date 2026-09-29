package com.qure.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.qure.app.R
import com.qure.app.account.PlanTier
import com.qure.app.ui.theme.Motion
import com.qure.app.ui.theme.QrYellow
import com.qure.app.ui.theme.Radius
import com.qure.app.ui.theme.RiskDanger
import com.qure.app.ui.theme.Sizing
import com.qure.app.ui.theme.Spacing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun QureScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter),
            ) {
                Spacer(Modifier.height(Spacing.md))
                content()
                Spacer(Modifier.height(Spacing.xxl))
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
            ) {
                bottomBar?.let {
                    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter)) {
                        it()
                        Spacer(Modifier.height(Spacing.md))
                    }
                }
            }
        }
    }
}

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    tagline: String? = stringResource(R.string.menu_tagline),
    align: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier, horizontalAlignment = align) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = QrYellow,
        )
        tagline?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = Spacing.sm),
    )
}

@Composable
fun NoteCard(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Radius.card,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.lg),
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).background(
                MaterialTheme.colorScheme.surface, CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.let {
            Spacer(Modifier.height(Spacing.xl))
            it()
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    container: Color = QrYellow,
    onContainer: Color = Color(0xFF201A00),
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = Radius.card,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = onContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        ),
        modifier = modifier.fillMaxWidth().height(Sizing.button),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = onContainer,
            )
        } else {
            Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = Radius.card,
        modifier = modifier.fillMaxWidth().height(Sizing.button),
    ) {
        leading?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun QureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        shape = Radius.card,
        leadingIcon = leading?.let {
            { Icon(it, contentDescription = null, modifier = Modifier.size(20.dp)) }
        },
        trailingIcon = trailing,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = QrYellow,
            focusedLabelColor = QrYellow,
            cursorColor = QrYellow,
            errorBorderColor = RiskDanger,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by remember { mutableStateOf(false) }
    QureTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        leading = leading,
        trailing = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff
                    else Icons.Outlined.Visibility,
                    contentDescription = stringResource(
                        if (visible) R.string.auth_password_hide else R.string.auth_password_show
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None
        else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password, imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
    )
}

@Composable
fun QuotaDialog(
    limit: Int,
    onDismiss: () -> Unit,
    onSignUp: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Radius.sheet,
        icon = {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = QrYellow,
                modifier = Modifier.size(24.dp),
            )
        },
        title = { Text(stringResource(R.string.quota_title, limit)) },
        text = {
            Text(
                stringResource(
                    if (onSignUp == null) R.string.quota_in_app else R.string.quota_body
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            if (onSignUp == null) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.quota_ok), color = QrYellow)
                }
            } else {
                TextButton(onClick = onSignUp) {
                    Text(stringResource(R.string.quota_signup), color = QrYellow)
                }
            }
        },
        dismissButton = if (onSignUp == null) null else {
            {
                TextButton(onClick = onDismiss) {
                    Text(
                        stringResource(R.string.quota_later),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Composable
fun TierChip(tier: PlanTier, modifier: Modifier = Modifier) {
    val pro = tier == PlanTier.pro
    Text(
        stringResource(if (pro) R.string.plan_pro else R.string.plan_free),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (pro) Color(0xFF201A00) else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(
                if (pro) QrYellow else MaterialTheme.colorScheme.surfaceVariant, Radius.pill,
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
    )
}

@Composable
fun CountBadge(text: String, modifier: Modifier = Modifier, tint: Color? = null) {

    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(
                tint?.copy(alpha = 0.14f) ?: MaterialTheme.colorScheme.surfaceVariant,
                Radius.pill,
            )
            .padding(horizontal = Spacing.sm, vertical = 2.dp),
    )
}

@Composable
fun InitialAvatar(initial: String?, modifier: Modifier = Modifier) {
    val signedIn = !initial.isNullOrBlank()
    val bg by animateColorAsState(
        if (signedIn) QrYellow.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
        tween(Motion.medium),
        label = "avatarBg",
    )
    Box(
        modifier.size(Sizing.avatar).background(bg, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial?.take(1) ?: "?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (signedIn) QrYellow else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ActionStack(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        content = content,
    )
}
