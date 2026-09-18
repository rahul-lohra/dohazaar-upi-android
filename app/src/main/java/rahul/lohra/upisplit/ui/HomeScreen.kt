package rahul.lohra.upisplit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import rahul.lohra.upisplit.ui.components.EntryMethodCard
import rahul.lohra.upisplit.ui.components.ScreenHeading
import rahul.lohra.upisplit.ui.components.ScreenList
import rahul.lohra.upisplit.ui.components.SessionProgress
import rahul.lohra.upisplit.ui.theme.dimensions
import rahul.lohra.upisplit.ui.theme.spacing

@Composable
internal fun HomeScreen(
    hasActiveSession: Boolean,
    merchantName: String,
    completedPayments: Int,
    splitCount: Int,
    remainingAmount: String,
    pendingOutcome: PaymentOutcome?,
    onTextEntry: () -> Unit,
    onCameraScan: () -> Unit,
    onPhotoImport: () -> Unit,
    onContinueSession: () -> Unit
) {
    ScreenList {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)) {
                Surface(
                    modifier = Modifier.size(MaterialTheme.dimensions.appMarkSize),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "₹÷", style = MaterialTheme.typography.titleLarge)
                    }
                }
                ScreenHeading(
                    title = "Split a UPI payment",
                    supportingText = "Choose how you want to add payment details."
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                EntryMethodCard(
                    title = "Enter details",
                    supportingText = "VPA, merchant and amount",
                    icon = Icons.Outlined.Keyboard,
                    onClick = onTextEntry
                )
                EntryMethodCard(
                    title = "Scan QR",
                    supportingText = "Use the camera",
                    icon = Icons.Outlined.QrCodeScanner,
                    onClick = onCameraScan
                )
                EntryMethodCard(
                    title = "Choose photo",
                    supportingText = "Scan a saved QR image",
                    icon = Icons.Outlined.Image,
                    onClick = onPhotoImport
                )
            }
        }
        if (hasActiveSession) {
            item {
                Text(
                    text = if (pendingOutcome == PaymentOutcome.Submitted || pendingOutcome == PaymentOutcome.Unknown) {
                        "Needs verification"
                    } else {
                        "Continue payment"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Card(
                    onClick = onContinueSession,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(
                        MaterialTheme.dimensions.borderWidth,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(MaterialTheme.spacing.large),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = merchantName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "$completedPayments / $splitCount",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text(
                            text = if (pendingOutcome == PaymentOutcome.Submitted || pendingOutcome == PaymentOutcome.Unknown) {
                                "Verify the last payment before continuing"
                            } else {
                                "$remainingAmount remaining"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SessionProgress(completed = completedPayments, total = splitCount)
                    }
                }
            }
        }
    }
}
