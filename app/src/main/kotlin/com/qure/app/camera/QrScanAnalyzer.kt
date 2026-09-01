package com.qure.app.camera

import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import android.util.Size
import androidx.annotation.MainThread
import androidx.annotation.OptIn as AndroidXOptIn   // androidx.annotation.OptIn shadows kotlin.OptIn
import androidx.camera.core.ExperimentalGetImage    // androidx.camera.core, NOT androidx.annotation
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.qure.app.BuildConfig
import com.qure.app.domain.QrDetection
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Turns camera frames into [QrDetection]s whose geometry is ALREADY in PreviewView pixels.
 *
 * How the coordinates work, because this is the part every tutorial gets wrong:
 *  1. [getTargetCoordinateSystem] returns COORDINATE_SYSTEM_VIEW_REFERENCED. A CameraController
 *     attached to a PreviewView then calls [updateTransform] with the SENSOR -> PreviewView matrix.
 *     That matrix already bakes in preview resolution, view size, ScaleType (FILL_CENTER crops!),
 *     display rotation and mirroring — all the things hand-rolled math gets subtly wrong.
 *     This only happens with a CameraController; under a bare ProcessCameraProvider,
 *     updateTransform is called with null forever.
 *  2. ML Kit reports in the rotation-applied ANALYSIS buffer space, not sensor space. So we build
 *     analysis -> sensor from sensorToBufferTransformMatrix composed with ML Kit's rotation,
 *     invert it, then postConcat sensor -> view.
 *  3. That matrix goes to process(image, rotation, matrix); ML Kit applies it internally, so
 *     cornerPoints come back in PreviewView pixels and the UI does no math at all.
 *
 * Analysis is continuous and never pauses: the caller decides what to do with the results, and
 * the outline it draws has to keep up with the code as the phone moves.
 *
 * The other half of this class is close() discipline. Under STRATEGY_KEEP_ONLY_LATEST a single
 * missed [ImageProxy.close] stalls the pipeline permanently — the preview simply freezes with no
 * error anywhere. Every path below closes exactly once.
 */
class QrScanAnalyzer(
    private val scanner: BarcodeScanner,
    private val callbackExecutor: Executor,
    private val onResult: (List<QrDetection>) -> Unit,
) : ImageAnalysis.Analyzer {

    @Volatile private var sensorToView: Matrix? = null

    override fun getTargetCoordinateSystem(): Int = ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED

    @MainThread
    override fun updateTransform(matrix: Matrix?) {
        // Called on the main thread; analyze() runs on the analysis thread. Copy defensively.
        // Null is normal for the first few frames before the PreviewView is laid out.
        sensorToView = matrix?.let { Matrix(it) }
    }

    /**
     * Only consulted when the caller sets no ResolutionSelector, but kept in agreement with the
     * one ScannerScreen does set so the two can never silently disagree. 4:3 on purpose — see the
     * note there about 16:9 cropping the horizontal field in portrait.
     */
    override fun getDefaultTargetResolution(): Size = Size(1920, 1440)

    @AndroidXOptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val sensorToViewSnapshot = sensorToView ?: run { imageProxy.close(); return }
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val analysisToView = buildAnalysisToViewMatrix(imageProxy, sensorToViewSnapshot)
        val timestamp = imageProxy.imageInfo.timestamp

        if (BuildConfig.DEBUG && logged.compareAndSet(false, true)) {
            // One line, once: the actual buffer the analyzer receives. Frame geometry is the first
            // thing to check when codes near the edges stop being seen.
            Log.i(logTag, "analysis buffer ${imageProxy.width}x${imageProxy.height} rot=$rotationDegrees")
        }

        try {
            scanner.process(mediaImage, rotationDegrees, analysisToView)
                .addOnSuccessListener(callbackExecutor) { barcodes ->
                    onResult(barcodes.mapNotNull { it.toDetection(timestamp) })
                }
                .addOnFailureListener(callbackExecutor) { onResult(emptyList()) }
                // The single authoritative close: fires on success, failure AND cancellation.
                // Closing earlier corrupts the read — ML Kit reads the Image asynchronously.
                .addOnCompleteListener { imageProxy.close() }
        } catch (t: Throwable) {
            // process() throws synchronously if the scanner has already been closed.
            imageProxy.close()
        }
    }

    private fun Barcode.toDetection(timestamp: Long): QrDetection? {
        // rawValue, not displayValue: displayValue is sanitised for presentation, and a security
        // tool must reason about the bytes the code actually carries.
        val value = rawValue ?: return null
        val box = boundingBox ?: return null
        return QrDetection(
            rawValue = value,
            boxInViewPx = ComposeRect(
                box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat(),
            ),
            cornersInViewPx = cornerPoints?.map { Offset(it.x.toFloat(), it.y.toFloat()) }.orEmpty(),
            frameTimestampNs = timestamp,
        )
    }

    private companion object {
        const val logTag = "QureScan"
        val logged = AtomicBoolean(false)

        val normalizedRect = RectF(-1f, -1f, 1f, 1f)

        fun buildAnalysisToViewMatrix(imageProxy: ImageProxy, sensorToView: Matrix): Matrix {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val sensorToAnalysis = Matrix(imageProxy.imageInfo.sensorToBufferTransformMatrix)

            val sourceRect = RectF(0f, 0f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
            val rotatedRect = rotateRect(sourceRect, rotationDegrees)
            sensorToAnalysis.postConcat(getRectToRect(sourceRect, rotatedRect, rotationDegrees))

            val analysisToView = Matrix()
            if (!sensorToAnalysis.invert(analysisToView)) return Matrix()
            analysisToView.postConcat(sensorToView)
            return analysisToView
        }

        fun rotateRect(rect: RectF, rotationDegrees: Int): RectF =
            if (((rotationDegrees % 360) + 360) % 360 % 180 == 90) {
                RectF(0f, 0f, rect.height(), rect.width())
            } else rect

        fun getRectToRect(source: RectF, target: RectF, rotationDegrees: Int): Matrix {
            val m = Matrix()
            m.setRectToRect(source, normalizedRect, Matrix.ScaleToFit.FILL)
            m.postRotate(rotationDegrees.toFloat())
            val normalizedToTarget = Matrix()
            normalizedToTarget.setRectToRect(normalizedRect, target, Matrix.ScaleToFit.FILL)
            m.postConcat(normalizedToTarget)
            return m
        }
    }
}
