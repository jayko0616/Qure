package com.qure.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.qure.app.camera.CameraPermissionGate
import com.qure.app.signature.SignatureEngine
import com.qure.app.domain.QrDetection
import com.qure.app.screen.CameraLinkScreen
import com.qure.app.screen.ResultScreen
import com.qure.app.screen.ScannerScreen
import com.qure.app.ui.theme.QureTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35+ enforces edge-to-edge whether or not you opt in, so opt in and handle the
        // insets rather than discovering the prompt sitting under the gesture bar.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // forceDark: the whole app sits on a camera preview. A light surface here is glare.
            QureTheme(forceDark = true) { QureApp() }
        }
    }
}

private sealed interface Route {
    data object Scanner : Route
    data class Result(val detection: QrDetection) : Route
    data object CameraLink : Route
}

/**
 * First launch asks for CAMERA and then goes straight to the scanner.
 *
 * Becoming the phone's default browser is deliberately NOT part of first launch. It is a large
 * thing to ask of someone who has not yet seen the app do anything, and an app that demands it on
 * the install screen reads as pushy at exactly the moment it is asking to be trusted. It lives
 * behind the link button on the scanner instead, for whenever the user decides they want it.
 */
@Composable
private fun QureApp() {
    val analyzer = remember { SignatureEngine() }
    var route by remember { mutableStateOf<Route>(Route.Scanner) }

    CameraPermissionGate {
        when (val r = route) {
            Route.Scanner -> ScannerScreen(
                onInspect = { route = Route.Result(it) },
                onOpenCameraLink = { route = Route.CameraLink },
            )

            is Route.Result -> {
                BackHandler { route = Route.Scanner }
                ResultScreen(
                    parsed = r.detection.parsed,
                    analyzer = analyzer,
                    onBack = { route = Route.Scanner },
                )
            }

            Route.CameraLink -> {
                BackHandler { route = Route.Scanner }
                CameraLinkScreen(onClose = { route = Route.Scanner })
            }
        }
    }
}
