package rahul.lohra.upisplit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import rahul.lohra.upisplit.ui.components.InformationCard
import rahul.lohra.upisplit.ui.components.PrimaryAction
import rahul.lohra.upisplit.ui.components.ScreenHeading
import rahul.lohra.upisplit.ui.components.ScreenList
import rahul.lohra.upisplit.ui.components.SessionProgress
import rahul.lohra.upisplit.ui.theme.spacing

@Composable
internal fun PaymentScreen(
    merchantName: String,
    vpa: String,
    amount: String,
    totalAmount: String,
    completedPayments: Int,
    splitCount: Int,
    selectedProvider: String,
    onPay: () -> Unit
) {
    val sequence = completedPayments + 1
    ScreenList {
        item {
            ScreenHeading(
                title = "Payment $sequence of $splitCount",
                supportingText = "$merchantName · $vpa"
            )
        }
        item {
            InformationCard {
                Text(
                    text = "Pay now",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = amount, style = MaterialTheme.typography.headlineMedium)
                SessionProgress(completed = completedPayments, total = splitCount)
                Text(
                    text = "$totalAmount total · ${splitCount - completedPayments} payments remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            InformationCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(text = selectedProvider, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "External UPI authorization",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            PrimaryAction(text = "Pay $amount in $selectedProvider", onClick = onPay)
        }
        item {
            Text(
                text = "Prototype: choose a simulated UPI return state on the next dialog.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
