package rahul.lohra.upisplit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.Composable
import rahul.lohra.upisplit.ui.components.ScannerPrompt

@Composable
internal fun PhotoImportScreen(
    isScanning: Boolean,
    errorMessage: String?,
    onChoosePhoto: () -> Unit
) {
    ScannerPrompt(
        title = "Choose a QR image",
        supportingText = "Only the photo you select is shared with SplitUPI.",
        icon = Icons.Outlined.Image,
        frameTitle = if (isScanning) "Reading QR code" else "Select one image",
        frameSupportingText = if (isScanning) {
            "Extracting UPI details on this device"
        } else {
            "The QR will be decoded locally"
        },
        actionText = if (isScanning) "Scanning…" else "Open Photos",
        actionEnabled = !isScanning,
        isWorking = isScanning,
        errorMessage = errorMessage,
        onAction = onChoosePhoto
    )
}
