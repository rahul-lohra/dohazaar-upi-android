package rahul.lohra.upisplit.qr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.IOException

internal sealed interface UpiQrImageScanResult {
    data class Success(val paymentData: UpiPaymentData) : UpiQrImageScanResult
    data object NoQrCode : UpiQrImageScanResult
    data object UnsupportedQrCode : UpiQrImageScanResult
    data class UnsafeToSplit(val paymentData: UpiPaymentData) : UpiQrImageScanResult
    data object ImageReadFailure : UpiQrImageScanResult
}

internal fun scanUpiQrImage(
    context: Context,
    imageUri: Uri,
    onResult: (UpiQrImageScanResult) -> Unit
) {
    val inputImage = try {
        InputImage.fromFilePath(context, imageUri)
    } catch (_: IOException) {
        onResult(UpiQrImageScanResult.ImageReadFailure)
        return
    }

    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    val scanner = BarcodeScanning.getClient(options)
    scanner.process(inputImage)
        .addOnSuccessListener { barcodes ->
            onResult(classifyUpiBarcodes(barcodes))
        }
        .addOnFailureListener {
            onResult(UpiQrImageScanResult.ImageReadFailure)
        }
        .addOnCompleteListener {
            scanner.close()
        }
}

internal fun classifyUpiBarcodes(barcodes: List<Barcode>): UpiQrImageScanResult {
    if (barcodes.isEmpty()) return UpiQrImageScanResult.NoQrCode

    var unsafePaymentData: UpiPaymentData? = null
    barcodes.forEach { barcode ->
        val payload = barcode.rawValue ?: return@forEach
        when (val parsed = parseUpiQrPayload(payload)) {
            is UpiQrParseResult.Eligible -> {
                return UpiQrImageScanResult.Success(parsed.paymentData)
            }
            is UpiQrParseResult.Ineligible -> unsafePaymentData = parsed.paymentData
            UpiQrParseResult.Invalid -> Unit
        }
    }

    return if (unsafePaymentData != null) {
        UpiQrImageScanResult.UnsafeToSplit(unsafePaymentData)
    } else {
        UpiQrImageScanResult.UnsupportedQrCode
    }
}
