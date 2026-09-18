package rahul.lohra.upisplit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import rahul.lohra.upisplit.ui.components.InformationCard
import rahul.lohra.upisplit.ui.components.PrimaryAction
import rahul.lohra.upisplit.ui.components.ScreenList
import rahul.lohra.upisplit.ui.theme.dimensions

@Composable
internal fun ConfirmDetailsScreen(
    inputSource: InputSource,
    merchantName: String,
    vpa: String,
    amount: String,
    errorMessage: String?,
    onAmountChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    ScreenList {
        item {
            AssistChip(
                onClick = {},
                label = {
                    Text(if (inputSource == InputSource.Photos) "From Photos" else "Camera scan")
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (inputSource == InputSource.Photos) {
                            Icons.Outlined.Image
                        } else {
                            Icons.Outlined.QrCodeScanner
                        },
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.dimensions.iconSmall)
                    )
                }
            )
        }
        item {
            InformationCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = merchantName, style = MaterialTheme.typography.titleMedium)
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "Parsed UPI payee",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = vpa,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Merchant category 5812",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Total amount") },
                prefix = { Text("₹") },
                placeholder = { Text("10,000.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = errorMessage != null,
                supportingText = {
                    Text(errorMessage ?: "This amount-open QR does not define an amount.")
                }
            )
        }
        item {
            Text(
                text = "Confirm the merchant name and VPA before continuing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { PrimaryAction(text = "Continue", onClick = onContinue) }
    }
}
