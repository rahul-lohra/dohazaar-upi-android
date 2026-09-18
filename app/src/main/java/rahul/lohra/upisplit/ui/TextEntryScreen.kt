package rahul.lohra.upisplit.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import rahul.lohra.upisplit.ui.components.PrimaryAction
import rahul.lohra.upisplit.ui.components.ScreenHeading
import rahul.lohra.upisplit.ui.components.ScreenList

@Composable
internal fun TextEntryScreen(
    vpa: String,
    merchantName: String,
    amount: String,
    errorMessage: String?,
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
