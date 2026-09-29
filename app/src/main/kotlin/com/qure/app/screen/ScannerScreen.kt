package com.qure.app.screen

import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.qure.app.BuildConfig
import com.qure.app.R
import com.qure.app.camera.QrScanAnalyzer
import com.qure.app.domain.QrDetection
import com.qure.app.domain.QrRiskAnalyzer
import com.qure.app.domain.RiskLevel
import com.qure.app.domain.UrlParser
import com.qure.app.domain.Verdict
import com.qure.app.link.LinkHandoff
import com.qure.app.ui.theme.Motion
import com.qure.app.ui.theme.QrYellow
import com.qure.app.ui.theme.Radius
import com.qure.app.ui.theme.RiskDanger
import com.qure.app.ui.theme.RiskSafe
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import kotlin.math.hypot
import com.qure.app.ui.text.highlightHost
import com.qure.app.ui.text.middleEllipsis
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.qure.app.link.defaultBrowserRequestIntent
import com.qure.app.link.isDefaultBrowser

private const val stabilityFrames = 3

private const val ghostFrames = 4

private const val handoverMargin = 1.25f

@Composable
fun ScannerScreen(
    riskAnalyzer: QrRiskAnalyzer,
    onInspect: (QrDetection) -> Unit,
    onOpenCameraLink: () -> Unit,
    onOpenMenu: () -> Unit,
    onCreateBlacklist: () -> Unit,

    onAddToBlacklist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    val trackedState = remember { mutableStateOf<QrDetection?>(null) }
    val promptedState = remember { mutableStateOf<String?>(null) }
    val dismissedState = remember { mutableStateOf<String?>(null) }
    val candidateState = remember { mutableStateOf<String?>(null) }
    val streakState = remember { mutableStateOf(0) }
    val missState = remember { mutableStateOf(0) }
    val viewSizeState = remember { mutableStateOf(IntSize.Zero) }
    val lastCountState = remember { mutableStateOf(-1) }

    val tracked by trackedState
    val prompted by promptedState
    var linked by remember { mutableStateOf(context.isDefaultBrowser()) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { linked = context.isDefaultBrowser() }
    var bannerVisible by rememberSaveable { mutableStateOf(true) }
    var justAdded by remember { mutableStateOf(false) }
    LaunchedEffect(justAdded) {
        if (justAdded) { delay(2000); justAdded = false }
    }

    var assessedValue by remember { mutableStateOf<String?>(null) }
    var assessedLevel by remember { mutableStateOf<RiskLevel?>(null) }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { linked = context.isDefaultBrowser() }
    val hapticsRef by rememberUpdatedState(haptics)

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { analysisExecutor.shutdown() } }

    val scanner = remember {
        BarcodeScanning.getClient(

            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }
    DisposableEffect(scanner) { onDispose { scanner.close() } }

    val analyzer = remember(scanner) {

        QrScanAnalyzer(scanner, mainExecutor) { results ->
            val valid = results.filter { it.rawValue.isNotBlank() }

            if (BuildConfig.DEBUG && valid.size != lastCountState.value) {

                lastCountState.value = valid.size
                Log.i(logTag, "decoded ${valid.size} code(s)")
            }

            val dismissed = dismissedState.value
            if (dismissed != null && valid.none { it.rawValue == dismissed }) {
                dismissedState.value = null
            }

            val chosen = pickBest(
                candidates = valid.filter { it.rawValue != dismissedState.value },
                view = viewSizeState.value,
                incumbent = trackedState.value?.rawValue,
            )

            if (chosen == null) {

                missState.value += 1
                if (missState.value > ghostFrames) {
                    trackedState.value = null
                    promptedState.value = null
                    candidateState.value = null
                    streakState.value = 0
                }
                return@QrScanAnalyzer
            }

            missState.value = 0
            trackedState.value = chosen

            if (chosen.rawValue == candidateState.value) {
                streakState.value += 1
            } else {
                candidateState.value = chosen.rawValue
                streakState.value = 1
            }

            if (streakState.value >= stabilityFrames && promptedState.value != chosen.rawValue) {
                promptedState.value = chosen.rawValue
                hapticsRef.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    val trackedValue = tracked?.rawValue
    LaunchedEffect(trackedValue) {
        if (trackedValue == null) {
            assessedValue = null
            assessedLevel = null
            return@LaunchedEffect
        }
        val verdict = runCatching { riskAnalyzer.analyze(UrlParser.parse(trackedValue)) }.getOrNull()
        assessedValue = trackedValue

        assessedLevel = (verdict as? Verdict.Assessed)?.level
    }
    val trackedLevel: RiskLevel? = if (assessedValue == trackedValue) assessedLevel else null

    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            imageAnalysisOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888

            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        android.util.Size(1920, 1440),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    )
                ).build()

            previewResolutionSelector = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
        }
    }

    DisposableEffect(cameraController, analyzer, lifecycleOwner) {

        cameraController.setImageAnalysisAnalyzer(analysisExecutor, analyzer)
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
        }
    }

    Box(modifier = modifier.fillMaxSize().onSizeChanged { viewSizeState.value = it }) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE

                    controller = cameraController
                }
            },
            onRelease = { it.controller = null },
        )

        tracked?.let { QrOutline(it, outlineColorFor(trackedLevel)) }

        if (prompted == null) {
            Column(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onOpenMenu, modifier = Modifier.padding(4.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = stringResource(R.string.menu_open),
                            tint = Color.White.copy(alpha = 0.85f),
                        )
                    }

                    Box(Modifier.padding(4.dp)) {
                        IconButton(onClick = onOpenCameraLink) {
                            Icon(
                                imageVector = Icons.Outlined.Link,
                                contentDescription = stringResource(R.string.cd_camera_link),
                                tint = Color.White.copy(alpha = 0.85f),
                            )
                        }
                        if (!linked) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(10.dp).size(8.dp)
                                    .background(QrYellow, CircleShape),
                            )
                        }
                    }
                }

                if (!linked && bannerVisible) {
                    Surface(
                        shape = Radius.card,
                        color = Color(0xFF15181D).copy(alpha = 0.92f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    ) {
                        Row(
                            Modifier.padding(start = 14.dp, end = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.banner_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                            )
                            TextButton(
                                onClick = { roleLauncher.launch(context.defaultBrowserRequestIntent()) },
                                contentPadding = PaddingValues(horizontal = 10.dp),
                            ) {
                                Text(
                                    stringResource(R.string.banner_cta),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = QrYellow,
                                )
                            }
                            IconButton(
                                onClick = { bannerVisible = false },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.banner_dismiss),
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = tracked != null,
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut(),
                ) {
                    Text(
                        text = stringResource(R.string.scanner_hint_found),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,

                        modifier = Modifier
                            .padding(top = 12.dp)
                            .background(Color(0xFF0B0D10).copy(alpha = 0.6f), Radius.pill)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onCreateBlacklist) {
                        Text(
                            stringResource(R.string.scanner_make_blacklist),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                    AnimatedVisibility(visible = tracked != null, enter = fadeIn(), exit = fadeOut()) {
                        TextButton(
                            onClick = {
                                tracked?.rawValue?.let { onAddToBlacklist(it); justAdded = true }
                            },
                        ) {
                            Text(
                                stringResource(R.string.scanner_add_blacklist),
                                style = MaterialTheme.typography.labelMedium,
                                color = QrYellow,
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = justAdded, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        stringResource(R.string.scanner_added_toast),
                        style = MaterialTheme.typography.labelMedium,
                        color = QrYellow,
                        modifier = Modifier
                            .background(Color(0xFF0B0D10).copy(alpha = 0.7f), Radius.pill)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = prompted == null,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PrimaryScanButton(tracked = tracked, onInspect = onInspect)
                Spacer(Modifier.height(20.dp))
            }
        }

        AnimatedVisibility(
            visible = prompted != null && tracked != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            tracked?.let { d ->
                InspectPrompt(
                    detection = d,
                    onYes = { onInspect(d) },
                    onNo = {

                        if (LinkHandoff.isWebLink(d.rawValue)) {
                            LinkHandoff.open(context, android.net.Uri.parse(d.rawValue))
                        }

                        dismissedState.value = d.rawValue
                        promptedState.value = null
                        candidateState.value = null
                        streakState.value = 0
                    },
                )
            }
        }
    }
}

private const val logTag = "QureScan"

private fun pickBest(
    candidates: List<QrDetection>,
    view: IntSize,
    incumbent: String?,
): QrDetection? {
    if (candidates.isEmpty()) return null
    if (candidates.size == 1) return candidates.first()

    val cx = view.width / 2f
    val cy = view.height / 2f
    val diagonal = hypot(view.width.toFloat(), view.height.toFloat()).coerceAtLeast(1f)

    fun score(d: QrDetection): Float {
        val r = d.boxInViewPx
        val area = (r.width * r.height).coerceAtLeast(1f)
        val offCentre = (hypot(r.center.x - cx, r.center.y - cy) / diagonal).coerceIn(0f, 1f)
        return area * (1f - 0.6f * offCentre)
    }

    val best = candidates.maxByOrNull { score(it) } ?: return null
    val held = candidates.firstOrNull { it.rawValue == incumbent } ?: return best
    return if (score(best) > score(held) * handoverMargin) best else held
}

@Composable
private fun PrimaryScanButton(tracked: QrDetection?, onInspect: (QrDetection) -> Unit) {
    Button(
        onClick = { tracked?.let(onInspect) },
        enabled = tracked != null,
        shape = Radius.card,
        colors = ButtonDefaults.buttonColors(
            containerColor = QrYellow,
            contentColor = Color(0xFF201A00),

            disabledContainerColor = Color(0xFF15181D).copy(alpha = 0.72f),
            disabledContentColor = Color.White.copy(alpha = 0.66f),
        ),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(
            stringResource(
                if (tracked == null) R.string.manual_button_idle else R.string.manual_button
            ),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun outlineColorFor(level: RiskLevel?): Color = when (level) {
    RiskLevel.safe -> RiskSafe
    RiskLevel.dangerous -> RiskDanger
    RiskLevel.caution, null -> QrYellow
}

@Composable
private fun QrOutline(detection: QrDetection, color: Color) {
    val outline by animateColorAsState(color, tween(Motion.medium), label = "outline")
    Canvas(Modifier.fillMaxSize()) {

        val edgeWidth = 4.dp.toPx()
        val bracketWidth = 9.dp.toPx()
        val corners = detection.cornersInViewPx

        val pts: List<Offset> = if (corners.size == 4) corners else {
            val r = detection.boxInViewPx
            listOf(
                Offset(r.left, r.top), Offset(r.right, r.top),
                Offset(r.right, r.bottom), Offset(r.left, r.bottom),
            )
        }
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
            close()
        }
        drawPath(path, outline.copy(alpha = 0.18f))
        drawPath(path, outline, style = Stroke(width = edgeWidth))

        val arm = 0.22f
        for (i in pts.indices) {
            val p = pts[i]
            val prev = pts[(i + pts.size - 1) % pts.size]
            val next = pts[(i + 1) % pts.size]
            drawLine(
                outline, p, Offset(p.x + (next.x - p.x) * arm, p.y + (next.y - p.y) * arm),
                strokeWidth = bracketWidth, cap = StrokeCap.Round,
            )
            drawLine(
                outline, p, Offset(p.x + (prev.x - p.x) * arm, p.y + (prev.y - p.y) * arm),
                strokeWidth = bracketWidth, cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun InspectPrompt(detection: QrDetection, onYes: () -> Unit, onNo: () -> Unit) {
    val parsed = detection.parsed
    val safeText = remember(detection.rawValue) {
        middleEllipsis(UrlParser.toDisplayString(detection.rawValue))
    }
    Card(
        shape = Radius.sheet,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.prompt_title),
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
                text = stringResource(R.string.prompt_not_opened),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onNo) {

                    Text(
                        stringResource(
                            if (LinkHandoff.isWebLink(detection.rawValue)) R.string.prompt_open_directly
                            else R.string.prompt_no
                        )
                    )
                }
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
