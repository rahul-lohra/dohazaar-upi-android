package rahul.lohra.upisplit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import rahul.lohra.upisplit.ui.components.PrimaryAction
import rahul.lohra.upisplit.ui.components.ScreenList
import rahul.lohra.upisplit.ui.theme.dimensions
import rahul.lohra.upisplit.ui.theme.spacing

@Composable
internal fun ResultScreen(
    outcome: PaymentOutcome,
    amount: String,
    completedPayments: Int,
    splitCount: Int,
    nextAmount: String?,
    onPrimaryAction: () -> Unit,
    onSecondaryAction: () -> Unit
) {
    val content = when (outcome) {
        PaymentOutcome.Success -> ResultContent(
            icon = Icons.Rounded.CheckCircle,
            title = if (completedPayments == splitCount) "All payments completed" else "Payment reported successful",
            detail = "$completedPayments of $splitCount payments completed",
            warning = null,
            primaryAction = if (completedPayments == splitCount) "Done" else "Pay next $nextAmount",
            secondaryAction = if (completedPayments == splitCount) null else "Finish later"
        )
        PaymentOutcome.Failure -> ResultContent(
            icon = Icons.Outlined.ErrorOutline,
            title = "Payment failed",
            detail = "The UPI app explicitly reported failure.",
            warning = null,
            primaryAction = "Try again",
            secondaryAction = "Return home"
        )
        PaymentOutcome.Submitted -> ResultContent(
            icon = Icons.Outlined.Schedule,
            title = "Payment submitted",
            detail = "The final status is not confirmed yet.",
            warning = "Verify in your UPI or bank app. Do not retry this split.",
            primaryAction = "Return home",
            secondaryAction = null
        )
        PaymentOutcome.Unknown -> ResultContent(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            title = "Payment status unavailable",
            detail = "SplitUPI did not receive a definitive result.",
            warning = "Verify whether money was deducted before taking any action.",
            primaryAction = "Return home",
            secondaryAction = null
        )
    }

    ScreenList {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                ResultIcon(outcome = outcome, icon = content.icon)
                Text(text = content.title, style = MaterialTheme.typography.headlineSmall)
                Text(text = amount, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = content.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        content.warning?.let { warning ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = warning,
                        modifier = Modifier.padding(MaterialTheme.spacing.large),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            if (outcome == PaymentOutcome.Failure) {
                OutlinedButton(
                    onClick = onPrimaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.dimensions.actionHeight),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(content.primaryAction)
                }
            } else {
                PrimaryAction(text = content.primaryAction, onClick = onPrimaryAction)
            }
        }
        content.secondaryAction?.let { action ->
            item {
                FilledTonalButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.dimensions.actionHeight),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(action)
                }
            }
        }
    }
}

@Composable
private fun ResultIcon(outcome: PaymentOutcome, icon: ImageVector) {
    val containerColor: Color
    val contentColor: Color
    when (outcome) {
        PaymentOutcome.Success -> {
            containerColor = MaterialTheme.colorScheme.primaryContainer
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        }
        PaymentOutcome.Failure -> {
            containerColor = MaterialTheme.colorScheme.errorContainer
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        }
        PaymentOutcome.Submitted,
        PaymentOutcome.Unknown -> {
            containerColor = MaterialTheme.colorScheme.secondaryContainer
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        }
    }
    Surface(
        modifier = Modifier.size(MaterialTheme.dimensions.resultIconSize),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(MaterialTheme.dimensions.iconLarge)
            )
        }
    }
}

private data class ResultContent(
    val icon: ImageVector,
    val title: String,
    val detail: String,
    val warning: String?,
    val primaryAction: String,
    val secondaryAction: String?
)
