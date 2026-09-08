package com.qure.app.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.qure.app.R
import com.qure.app.account.AuthError
import com.qure.app.account.AuthResult
import com.qure.app.account.PasswordPolicy
import com.qure.app.account.PasswordRule
import com.qure.app.ui.theme.QrYellow
import com.qure.app.ui.theme.RiskDanger
import com.qure.app.ui.theme.RiskSafe
import kotlinx.coroutines.launch

/**
 * 로그인.
 *
 * The screen owns its form state and hands the credentials to [onSubmit]; whatever the repository
 * answers is mapped to a sentence under the fields. Nothing here knows whether the check happened
 * on the device or on a server, which is what lets the backend arrive without a UI change.
 */
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

    AccountScaffold(title = stringResource(R.string.auth_login_title), onBack = onBack) {
        Text(
            stringResource(R.string.auth_login_lead),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = id,
            onValueChange = { id = it; error = null },
            label = { Text(stringResource(R.string.auth_field_id)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text(stringResource(R.string.auth_field_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            ErrorText(it)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { submit() },
            enabled = !busy && id.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = QrYellow, contentColor = Color(0xFF201A00),
            ),
        ) { Text(stringResource(R.string.auth_login_title)) }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onGoSignUp, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.auth_go_signup))
        }
    }
}

/**
 * 회원가입: name, id, password, verification code.
 *
 * The password rules are shown as a live checklist rather than discovered on submit, and the
 * verification code is displayed on the screen itself, labelled as a test code, until an SMS or
 * e-mail channel exists to deliver it.
 */
@Composable
fun SignUpScreen(
    onRequestCode: () -> String,
    onSubmit: suspend (name: String, userId: String, password: String, code: String) -> AuthResult,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var id by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var issuedCode by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<AuthError?>(null) }
    var busy by remember { mutableStateOf(false) }

    val satisfied = PasswordPolicy.satisfied(password)
    val complete = name.isNotBlank() && id.isNotBlank() && password.isNotBlank() && code.isNotBlank()

    fun submit() {
        if (busy || !complete) return
        scope.launch {
            busy = true
            when (val result = onSubmit(name, id, password, code)) {
                AuthResult.Success -> onSuccess()
                is AuthResult.Failure -> error = result.error
            }
            busy = false
        }
    }

    AccountScaffold(title = stringResource(R.string.auth_signup_title), onBack = onBack) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; error = null },
            label = { Text(stringResource(R.string.auth_field_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = id,
            onValueChange = { id = it; error = null },
            label = { Text(stringResource(R.string.auth_field_id)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text(stringResource(R.string.auth_field_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.auth_pw_rules),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        PasswordRule.entries.forEach { rule ->
            RuleRow(text = stringResource(ruleLabelRes(rule)), met = rule in satisfied)
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it; error = null },
                label = { Text(stringResource(R.string.auth_field_code)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { issuedCode = onRequestCode(); error = null },
                modifier = Modifier.height(56.dp),
            ) { Text(stringResource(R.string.auth_request_code)) }
        }
        issuedCode?.let {
            Spacer(Modifier.height(6.dp))
            // Development stand-in for the SMS. Remove this line when a real channel delivers it.
            Text(
                stringResource(R.string.auth_code_dev_hint, it),
                style = MaterialTheme.typography.labelSmall,
                color = QrYellow,
            )
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            ErrorText(it)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { submit() },
            enabled = !busy && complete,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = QrYellow, contentColor = Color(0xFF201A00),
            ),
        ) { Text(stringResource(R.string.auth_signup_title)) }
    }
}

@Composable
private fun RuleRow(text: String, met: Boolean) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(7.dp).background(
                if (met) RiskSafe else MaterialTheme.colorScheme.onSurfaceVariant,
                CircleShape,
            ),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (met) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorText(error: AuthError) {
    Text(
        stringResource(
            when (error) {
                AuthError.emptyField -> R.string.auth_err_empty
                AuthError.invalidCredentials -> R.string.auth_err_invalid
                AuthError.duplicateId -> R.string.auth_err_duplicate
                AuthError.weakPassword -> R.string.auth_err_weak
                AuthError.codeNotRequested -> R.string.auth_err_code_missing
                AuthError.wrongCode -> R.string.auth_err_code_wrong
            },
        ),
        style = MaterialTheme.typography.bodySmall,
        color = RiskDanger,
    )
}

private fun ruleLabelRes(rule: PasswordRule): Int = when (rule) {
    PasswordRule.minLength -> R.string.auth_pw_rule_length
    PasswordRule.special -> R.string.auth_pw_rule_special
    PasswordRule.lowercase -> R.string.auth_pw_rule_lower
    PasswordRule.uppercase -> R.string.auth_pw_rule_upper
}
