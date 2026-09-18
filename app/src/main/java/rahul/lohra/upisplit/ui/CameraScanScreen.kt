package rahul.lohra.upisplit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.runtime.Composable
import rahul.lohra.upisplit.ui.components.ScannerPrompt

@Composable
internal fun CameraScanScreen(onQrDetected: () -> Unit) {
    ScannerPrompt(
        title = "Scan UPI QR",
        supportingText = "Point the camera at an amount-open UPI QR.",
        icon = Icons.Outlined.QrCodeScanner,
        frameTitle = "Place QR inside frame",
        frameSupportingText = "Scanning happens on this device",
        actionText = "Simulate QR detected",
        onAction = onQrDetected
    )
}
