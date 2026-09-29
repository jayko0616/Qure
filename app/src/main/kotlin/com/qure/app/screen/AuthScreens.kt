package com.qure.app.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.qure.app.R
import com.qure.app.account.AuthError
import com.qure.app.account.AuthResult
import com.qure.app.account.PasswordPolicy
import com.qure.app.account.PasswordRule
import com.qure.app.ui.theme.Motion
import com.qure.app.ui.theme.Radius
import com.qure.app.ui.theme.RiskDanger
import com.qure.app.ui.theme.RiskSafe
import com.qure.app.ui.theme.Spacing
import com.qure.app.ui.component.BrandMark
import com.qure.app.ui.component.PasswordField
import com.qure.app.ui.component.PrimaryButton
import com.qure.app.ui.component.QureScaffold
import com.qure.app.ui.component.QureTextField
import com.qure.app.ui.component.SectionLabel
import com.qure.app.ui.component.ActionStack
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onSubmit: suspend (userId: String, password: String) -> AuthResult,
    onSuccess: () -> Unit,
    onGoSignUp: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var id by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<AuthError?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun submit() {
        if (busy || id.isBlank() || password.isBlank()) return
        scope.launch {
            busy = true
            when (val result = onSubmit(id, password)) {
                AuthResult.Success -> onSuccess()
                is AuthResult.Failure -> error = result.error
            }
            busy = false
        }
    }

    QureScaffold(
        title = stringResource(R.string.auth_login_title),
        onBack = onBack,
        bottomBar = {
            ActionStack {
                PrimaryButton(
                    text = stringResource(R.string.auth_login_title),
                    onClick = { submit() },
                    enabled = id.isNotBlank() && password.isNotBlank(),
                    loading = busy,
                )
                TextButton(onClick = onGoSignUp, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.auth_go_signup),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) {
        Spacer(Modifier.height(Spacing.xl))
        BrandMark()
        Spacer(Modifier.height(Spacing.md))
        Text(
            stringResource(R.string.auth_login_lead),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.xxl))
        QureTextField(
            value = id,
            onValueChange = { id = it; error = null },
            label = stringResource(R.string.auth_field_id),
            leading = Icons.Outlined.AccountCircle,
            isError = error == AuthError.invalidCredentials,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(Spacing.md))
        PasswordField(
            value = password,
            onValueChange = { password = it; error = null },
            label = stringResource(R.string.auth_field_password),
            leading = Icons.Outlined.Lock,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        error?.let {
            Spacer(Modifier.height(Spacing.md))
            ErrorBanner(it)
        }
    }
}

@Composable
fun SignUpScreen(
    onSubmit: suspend (name: String, userId: String, password: String) -> AuthResult,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var id by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<AuthError?>(null) }
    var busy by remember { mutableStateOf(false) }

    val satisfied = PasswordPolicy.satisfied(password)
    val complete = name.isNotBlank() && id.isNotBlank() && password.isNotBlank()

    fun submit() {
        if (busy || !complete) return
        scope.launch {
            busy = true
            when (val result = onSubmit(name, id, password)) {
                AuthResult.Success -> onSuccess()
                is AuthResult.Failure -> error = result.error
            }
            busy = false
        }
    }

    QureScaffold(
        title = stringResource(R.string.auth_signup_title),
        onBack = onBack,
        bottomBar = {
            PrimaryButton(
                text = stringResource(R.string.auth_signup_title),
                onClick = { submit() },
                enabled = complete,
                loading = busy,
            )
        },
    ) {
        Text(
            stringResource(R.string.auth_signup_lead),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionLabel(stringResource(R.string.auth_section_account))
        QureTextField(
            value = name,
            onValueChange = { name = it; error = null },
            label = stringResource(R.string.auth_field_name),
            leading = Icons.Outlined.Person,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(Spacing.md))
        QureTextField(
            value = id,
            onValueChange = { id = it; error = null },
            label = stringResource(R.string.auth_field_id),
            leading = Icons.Outlined.AccountCircle,
            isError = error == AuthError.duplicateId,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(Spacing.md))
        PasswordField(
            value = password,
            onValueChange = { password = it; error = null },
            label = stringResource(R.string.auth_field_password),
            leading = Icons.Outlined.Lock,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )

        Spacer(Modifier.height(Spacing.md))
        PasswordChecklist(satisfied)

        error?.let {
            Spacer(Modifier.height(Spacing.md))
            ErrorBanner(it)
        }
    }
}

@Composable
private fun PasswordChecklist(satisfied: Set<PasswordRule>) {
    Surface(
        shape = Radius.card,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(
                stringResource(R.string.auth_pw_rules),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            PasswordRule.entries.forEach { rule ->
                RuleRow(text = stringResource(ruleLabelRes(rule)), met = rule in satisfied)
            }
        }
    }
}

@Composable
private fun RuleRow(text: String, met: Boolean) {
    val tint by animateColorAsState(
        if (met) RiskSafe else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        tween(Motion.fast),
        label = "ruleTint",
    )
    Row(
        Modifier.padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (met) Icons.Outlined.CheckCircle
            else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (met) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorBanner(error: AuthError) {
    Surface(
        shape = Radius.card,
        color = RiskDanger.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = RiskDanger,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                stringResource(errorLabelRes(error)),
                style = MaterialTheme.typography.bodySmall,
                color = RiskDanger,
            )
        }
    }
}

private fun errorLabelRes(error: AuthError): Int = when (error) {
    AuthError.emptyField -> R.string.auth_err_empty
    AuthError.invalidCredentials -> R.string.auth_err_invalid
    AuthError.duplicateId -> R.string.auth_err_duplicate
    AuthError.weakPassword -> R.string.auth_err_weak
}

private fun ruleLabelRes(rule: PasswordRule): Int = when (rule) {
    PasswordRule.minLength -> R.string.auth_pw_rule_length
    PasswordRule.special -> R.string.auth_pw_rule_special
    PasswordRule.lowercase -> R.string.auth_pw_rule_lower
    PasswordRule.uppercase -> R.string.auth_pw_rule_upper
}
