package rahul.lohra.upisplit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import rahul.lohra.upisplit.ui.components.InformationCard
import rahul.lohra.upisplit.ui.components.LabelValueRow
import rahul.lohra.upisplit.ui.components.PrimaryAction
import rahul.lohra.upisplit.ui.components.ScreenHeading
import rahul.lohra.upisplit.ui.components.ScreenList
import rahul.lohra.upisplit.ui.theme.spacing

@Composable
internal fun SplitSetupScreen(
    totalAmount: String,
    splitAmounts: List<String>,
    maximumSplitAmount: String,
    onContinue: () -> Unit
) {
    val splitCount = splitAmounts.size
    val fullPaymentCount = splitAmounts.count { it == maximumSplitAmount }
    val finalAmount = splitAmounts.lastOrNull().orEmpty()

    ScreenList {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
                ScreenHeading(
                    title = "Automatic split",
                    supportingText = "Each payment is capped at $maximumSplitAmount."
                )
                Text(text = totalAmount, style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            InformationCard {
                LabelValueRow(label = "Maximum per payment", value = maximumSplitAmount)
                LabelValueRow(label = "Payments", value = splitCount.toString(), emphasizeValue = true)
                if (fullPaymentCount > 0) {
                    LabelValueRow(
                        label = "Full payments",
                        value = "$fullPaymentCount × $maximumSplitAmount"
                    )
                }
                HorizontalDivider()
                LabelValueRow(
                    label = "Final payment",
                    value = finalAmount,
                    emphasizeValue = true
                )
            }
        }
        item { PrimaryAction(text = "Review split", onClick = onContinue) }
    }
}
