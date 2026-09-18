package rahul.lohra.upisplit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import rahul.lohra.upisplit.data.RecentPaymentDetails
import rahul.lohra.upisplit.ui.components.PrimaryAction
import rahul.lohra.upisplit.ui.components.ScreenHeading
import rahul.lohra.upisplit.ui.components.ScreenList
import rahul.lohra.upisplit.ui.theme.dimensions
import rahul.lohra.upisplit.ui.theme.spacing

@Composable
internal fun TextEntryScreen(
    vpa: String,
    merchantName: String,
    amount: String,
    errorMessage: String?,
    recentPayments: List<RecentPaymentDetails>,
    onRecentPaymentSelected: (RecentPaymentDetails) -> Unit,
    onVpaChange: (String) -> Unit,
    onMerchantNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    ScreenList {
        item {
            ScreenHeading(
                title = "Enter payment details",
                supportingText = "All three fields are required. Confirm the payee before continuing."
            )
        }
        if (recentPayments.isNotEmpty()) {
            item {
                Text(
                    text = "Recent payment details",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(
                count = recentPayments.size,
                key = { index -> recentPayments[index].vpa.lowercase() }
            ) { index ->
                val payment = recentPayments[index]
                RecentPaymentCard(
                    payment = payment,
                    selected = vpa.equals(payment.vpa, ignoreCase = true) &&
                        merchantName == payment.merchantName &&
                        parseAmountToPaise(amount) == payment.amountPaise,
                    onClick = { onRecentPaymentSelected(payment) }
                )
            }
        }
        item {
            OutlinedTextField(
                value = vpa,
                onValueChange = onVpaChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("VPA") },
                placeholder = { Text("restaurant@upi") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = errorMessage != null
            )
        }
        item {
            OutlinedTextField(
                value = merchantName,
                onValueChange = onMerchantNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Merchant name") },
                placeholder = { Text("ABC Restaurant") },
                singleLine = true,
                isError = errorMessage != null
            )
        }
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Amount") },
                prefix = { Text("₹") },
                placeholder = { Text("10,000.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = errorMessage != null,
                supportingText = errorMessage?.let { message ->
                    { Text(text = message) }
                }
            )
        }
        item { PrimaryAction(text = "Continue", onClick = onContinue) }
    }
}

@Composable
private fun RecentPaymentCard(
    payment: RecentPaymentDetails,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        border = BorderStroke(
            width = if (selected) {
                MaterialTheme.dimensions.selectedBorderWidth
            } else {
                MaterialTheme.dimensions.borderWidth
            },
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.large),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
            ) {
                Text(
                    text = payment.merchantName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = payment.vpa,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatInr(payment.amountPaise),
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}
