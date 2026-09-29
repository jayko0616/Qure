package com.qure.app.camera

import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import android.util.Size
import androidx.annotation.MainThread
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.qure.app.BuildConfig
import com.qure.app.domain.QrDetection
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.compose.ui.geometry.Rect as ComposeRect

class QrScanAnalyzer(
    private val scanner: BarcodeScanner,
    private val callbackExecutor: Executor,
    private val onResult: (List<QrDetection>) -> Unit,
) : ImageAnalysis.Analyzer {

    @Volatile private var sensorToView: Matrix? = null

    override fun getTargetCoordinateSystem(): Int = ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED

    @MainThread
    override fun updateTransform(matrix: Matrix?) {

        sensorToView = matrix?.let { Matrix(it) }
    }

    override fun getDefaultTargetResolution(): Size = Size(1920, 1440)

    @AndroidXOptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val sensorToViewSnapshot = sensorToView ?: run { imageProxy.close(); return }
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val analysisToView = buildAnalysisToViewMatrix(imageProxy, sensorToViewSnapshot)
        val timestamp = imageProxy.imageInfo.timestamp

        if (BuildConfig.DEBUG && logged.compareAndSet(false, true)) {

            Log.i(logTag, "analysis buffer ${imageProxy.width}x${imageProxy.height} rot=$rotationDegrees")
        }

        try {
            scanner.process(mediaImage, rotationDegrees, analysisToView)
                .addOnSuccessListener(callbackExecutor) { barcodes ->
                    onResult(barcodes.mapNotNull { it.toDetection(timestamp) })
                }
                .addOnFailureListener(callbackExecutor) { onResult(emptyList()) }

                .addOnCompleteListener { imageProxy.close() }
        } catch (t: Throwable) {

            imageProxy.close()
        }
    }

    private fun Barcode.toDetection(timestamp: Long): QrDetection? {

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
