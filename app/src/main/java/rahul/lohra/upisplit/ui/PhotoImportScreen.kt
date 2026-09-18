package rahul.lohra.upisplit.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.Composable
import rahul.lohra.upisplit.ui.components.ScannerPrompt

@Composable
internal fun PhotoImportScreen(onChoosePhoto: () -> Unit) {
    ScannerPrompt(
        title = "Choose a QR image",
        supportingText = "Only the photo you select is shared with SplitUPI.",
        icon = Icons.Outlined.Image,
        frameTitle = "Select one image",
        frameSupportingText = "The QR will be decoded locally",
        actionText = "Open Photos",
        onAction = onChoosePhoto
    )
}
