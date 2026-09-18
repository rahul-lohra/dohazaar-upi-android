package rahul.lohra.upisplit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import rahul.lohra.upisplit.ui.components.InformationCard
import rahul.lohra.upisplit.ui.components.LabelValueRow
import rahul.lohra.upisplit.ui.components.PrimaryAction
import rahul.lohra.upisplit.ui.components.ScreenHeading
import rahul.lohra.upisplit.ui.components.ScreenList
import rahul.lohra.upisplit.ui.theme.dimensions
import rahul.lohra.upisplit.ui.theme.spacing

@Composable
internal fun ReviewScreen(
    merchantName: String,
    totalAmount: String,
    splitAmounts: List<String>,
    onContinue: () -> Unit
) {
    ScreenList {
        item {
            ScreenHeading(
                title = "Review split",
                supportingText = "Nothing is paid until you approve each payment."
            )
        }
        item {
            InformationCard {
                LabelValueRow(label = "Payee", value = merchantName)
                LabelValueRow(label = "Total", value = totalAmount, emphasizeValue = true)
            }
        }
        itemsIndexed(splitAmounts) { index, amount ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    MaterialTheme.dimensions.borderWidth,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Box(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                    LabelValueRow(
                        label = "Payment ${index + 1}",
                        value = amount,
                        emphasizeValue = true
                    )
                }
            }
        }
        item { PrimaryAction(text = "Choose UPI app", onClick = onContinue) }
    }
}
