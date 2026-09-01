package com.qure.app.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.qure.app.R

private enum class PermState { needsRequest, granted, needsRationale, permanentlyDenied }

/**
 * Runs [content] only once CAMERA is granted.
 *
 * Handles the three states that actually occur on a real phone, which a bare permission request
 * does not: never asked, denied once (rationale, ask again), and denied permanently (the system
 * dialog will no longer appear at all, so the only way forward is app settings — and we have to
 * notice the user came back and granted it there).
 */
@Composable
fun CameraPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity

    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    var state by remember { mutableStateOf(if (granted()) PermState.granted else PermState.needsRequest) }
    var asked by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        asked = true
        state = when {
            ok -> PermState.granted
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) ->
                PermState.needsRationale
            else -> PermState.permanentlyDenied
        }
    }

    // The user may grant the permission in Settings and come back; nothing else would tell us.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (state != PermState.granted && granted()) state = PermState.granted
    }

    LaunchedEffect(Unit) {
        if (state != PermState.granted && !asked) launcher.launch(Manifest.permission.CAMERA)
    }

    when (state) {
        PermState.granted -> content()
        PermState.permanentlyDenied -> Explain(
            title = stringResource(R.string.perm_title),
            body = stringResource(R.string.perm_body_settings),
            cta = stringResource(R.string.perm_cta_settings),
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
            )
        }
        else -> Explain(
            title = stringResource(R.string.perm_title),
            body = stringResource(R.string.perm_body),
            cta = stringResource(R.string.perm_cta),
        ) { launcher.launch(Manifest.permission.CAMERA) }
    }
}

@Composable
private fun Explain(title: String, body: String, cta: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onClick) { Text(cta) }
    }
}
