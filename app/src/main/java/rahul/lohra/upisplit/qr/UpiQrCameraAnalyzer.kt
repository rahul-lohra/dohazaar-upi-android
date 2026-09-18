package rahul.lohra.upisplit.qr

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

internal class UpiQrCameraAnalyzer(
    private val onResult: (UpiQrImageScanResult) -> Unit
) : ImageAnalysis.Analyzer, AutoCloseable {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    @Volatile
    private var active = true
    private var lastReportedPayload: String? = null

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (!active) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (!active) return@addOnSuccessListener
                if (barcodes.isEmpty()) {
                    lastReportedPayload = null
                    return@addOnSuccessListener
                }
                val payloadSignature = barcodes.mapNotNull { it.rawValue }.joinToString()
                if (payloadSignature.isEmpty() || payloadSignature == lastReportedPayload) {
                    return@addOnSuccessListener
                }
                lastReportedPayload = payloadSignature
                val result = classifyUpiBarcodes(barcodes)
                if (result is UpiQrImageScanResult.Success) {
                    active = false
                }
                onResult(result)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    override fun close() {
        active = false
        scanner.close()
    }
}
