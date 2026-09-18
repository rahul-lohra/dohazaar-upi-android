package rahul.lohra.upisplit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import rahul.lohra.upisplit.ui.theme.dimensions
import rahul.lohra.upisplit.ui.theme.spacing

@Composable
internal fun ScannerPrompt(
    title: String,
    supportingText: String,
    icon: ImageVector,
    frameTitle: String,
    frameSupportingText: String,
    actionText: String,
    onAction: () -> Unit
) {
    ScreenList {
        item { ScreenHeading(title = title, supportingText = supportingText) }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                    .border(
                        width = MaterialTheme.dimensions.selectedBorderWidth,
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(MaterialTheme.spacing.extraLarge),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.dimensions.iconLarge),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(text = frameTitle, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = frameSupportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { PrimaryAction(text = actionText, onClick = onAction) }
    }
}
