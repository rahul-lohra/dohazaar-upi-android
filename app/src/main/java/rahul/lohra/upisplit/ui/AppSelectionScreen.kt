package rahul.lohra.upisplit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import rahul.lohra.upisplit.ui.components.PrimaryAction
import rahul.lohra.upisplit.ui.components.ScreenHeading
import rahul.lohra.upisplit.ui.components.ScreenList
import rahul.lohra.upisplit.ui.theme.dimensions
import rahul.lohra.upisplit.ui.theme.spacing

@Composable
internal fun AppSelectionScreen(
    providers: List<PaymentProvider>,
    selectedProvider: String,
    onProviderSelected: (String) -> Unit,
    onContinue: () -> Unit
) {
    ScreenList {
        item {
            ScreenHeading(
                title = "Choose UPI app",
                supportingText = "Availability and UPI readiness are checked before launch."
            )
        }
        items(providers) { provider ->
            val selected = selectedProvider == provider.name
            Surface(
                onClick = { onProviderSelected(provider.name) },
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    if (selected) {
                        MaterialTheme.dimensions.selectedBorderWidth
                    } else {
                        MaterialTheme.dimensions.borderWidth
                    },
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    Surface(
                        modifier = Modifier.size(MaterialTheme.dimensions.minimumTouchTarget),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = provider.initials, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    Text(
                        text = provider.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    RadioButton(
                        selected = selected,
                        onClick = { onProviderSelected(provider.name) }
                    )
                }
            }
        }
        item {
            PrimaryAction(
                text = "Continue with $selectedProvider",
                onClick = onContinue
            )
        }
    }
}
