package rahul.lohra.upisplit.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import rahul.lohra.upisplit.data.RecentPaymentDetails
import rahul.lohra.upisplit.data.RecentPaymentStore
import rahul.lohra.upisplit.payment.UpiClientStatus
import rahul.lohra.upisplit.payment.UpiPaymentRequest
import rahul.lohra.upisplit.payment.buildGooglePayIntent
import rahul.lohra.upisplit.payment.generateTransactionReference
import rahul.lohra.upisplit.payment.parseUpiClientResult
import rahul.lohra.upisplit.ui.theme.UPISplitTheme

private const val PrototypeMerchantName = "ABC Restaurant"
private const val PrototypeVpa = "restaurant@upi"
private const val MaximumSplitAmountPaise = 200_000L
private const val MaximumAutomaticPaymentCount = 10_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitUpiApp() {
    val context = LocalContext.current
    val recentPaymentStore = remember(context) { RecentPaymentStore(context) }
    var recentPayments by remember { mutableStateOf(recentPaymentStore.load()) }
    var screen by rememberSaveable { mutableStateOf(AppScreen.Home) }
    var inputSource by rememberSaveable { mutableStateOf(InputSource.Text) }
    var vpa by rememberSaveable { mutableStateOf("") }
    var merchantName by rememberSaveable { mutableStateOf("") }
    var merchantCategoryCode by rememberSaveable { mutableStateOf<String?>(null) }
    var amountInput by rememberSaveable { mutableStateOf("") }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProvider by rememberSaveable {
        mutableStateOf(PrototypeProviders.first().name)
    }
    var completedPayments by rememberSaveable { mutableIntStateOf(0) }
    var paymentOutcome by rememberSaveable { mutableStateOf<PaymentOutcome?>(null) }
    var hasActiveSession by rememberSaveable { mutableStateOf(false) }
    var showReturnSimulator by rememberSaveable { mutableStateOf(false) }
    var pendingTransactionReference by rememberSaveable { mutableStateOf<String?>(null) }
    var paymentLaunchError by rememberSaveable { mutableStateOf<String?>(null) }

    val totalPaise = remember(amountInput) { parseAmountToPaise(amountInput) ?: 0L }
    val splitValues = remember(totalPaise) {
        calculateAutomaticSplit(totalPaise)
    }
    val splitCount = splitValues.size
    val formattedSplitValues = remember(splitValues) {
        splitValues.map(::formatInr)
    }
    val formattedTotal = remember(totalPaise) { formatInr(totalPaise) }
    val currentAmount = formattedSplitValues.getOrNull(completedPayments)
        ?: formattedSplitValues.lastOrNull()
        ?: formatInr(0L)
    val nextAmount = formattedSplitValues.getOrNull(completedPayments)
    val remainingPaise = splitValues.drop(completedPayments).sum()

    fun handlePaymentOutcome(outcome: PaymentOutcome) {
        paymentOutcome = outcome
        if (outcome == PaymentOutcome.Success) {
            completedPayments = (completedPayments + 1).coerceAtMost(splitCount)
        }
        screen = AppScreen.Result
    }

    val googlePayLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        val expectedReference = pendingTransactionReference
        pendingTransactionReference = null
        if (expectedReference == null) {
            handlePaymentOutcome(PaymentOutcome.Unknown)
        } else {
            val clientResult = parseUpiClientResult(
                data = result.data,
                expectedTransactionReference = expectedReference
            )
            val outcome = when (clientResult.status) {
                UpiClientStatus.Success -> PaymentOutcome.Success
                UpiClientStatus.Failure -> PaymentOutcome.Failure
                UpiClientStatus.Submitted -> PaymentOutcome.Submitted
                UpiClientStatus.Unknown -> PaymentOutcome.Unknown
            }
            handlePaymentOutcome(outcome)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            inputSource = InputSource.Photos
            vpa = PrototypeVpa
            merchantName = PrototypeMerchantName
            merchantCategoryCode = "5812"
            amountInput = ""
            validationError = null
            screen = AppScreen.ConfirmDetails
        }
    }

    fun goHome() {
        screen = AppScreen.Home
        validationError = null
    }

    fun validateDraft(): Boolean {
        val parsedAmount = parseAmountToPaise(amountInput)
        validationError = when {
            merchantName.isBlank() -> "Enter a merchant name."
            vpa.isBlank() || vpa.count { it == '@' } != 1 -> "Enter a valid VPA."
            parsedAmount == null -> "Enter a positive amount with at most two decimals."
            automaticPaymentCount(parsedAmount) > MaximumAutomaticPaymentCount -> {
                "This amount creates too many ₹2,000 payments."
            }
            else -> null
        }
        return validationError == null
    }

    fun back() {
        screen = when (screen) {
            AppScreen.Home -> AppScreen.Home
            AppScreen.TextEntry,
            AppScreen.CameraScan,
            AppScreen.PhotoImport -> AppScreen.Home
            AppScreen.ConfirmDetails -> if (inputSource == InputSource.Camera) {
                AppScreen.CameraScan
            } else {
                AppScreen.PhotoImport
            }
            AppScreen.SplitSetup -> if (inputSource == InputSource.Text) {
                AppScreen.TextEntry
            } else {
                AppScreen.ConfirmDetails
            }
            AppScreen.Review -> AppScreen.SplitSetup
            AppScreen.AppSelection -> AppScreen.Review
            AppScreen.Payment,
            AppScreen.Result -> AppScreen.Home
        }
        validationError = null
    }

    BackHandler(enabled = screen != AppScreen.Home, onBack = ::back)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screen.title, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    if (screen != AppScreen.Home) {
                        IconButton(onClick = ::back) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (screen) {
                AppScreen.Home -> HomeScreen(
                    hasActiveSession = hasActiveSession,
                    merchantName = merchantName.ifBlank { PrototypeMerchantName },
                    completedPayments = completedPayments,
                    splitCount = splitCount,
                    remainingAmount = formatInr(remainingPaise),
                    pendingOutcome = paymentOutcome,
                    onTextEntry = {
                        inputSource = InputSource.Text
                        vpa = ""
                        merchantName = ""
                        merchantCategoryCode = null
                        amountInput = ""
                        validationError = null
                        screen = AppScreen.TextEntry
                    },
                    onCameraScan = {
                        inputSource = InputSource.Camera
                        validationError = null
                        screen = AppScreen.CameraScan
                    },
                    onPhotoImport = {
                        inputSource = InputSource.Photos
                        validationError = null
                        screen = AppScreen.PhotoImport
                    },
                    onContinueSession = {
                        screen = if (
                            paymentOutcome == PaymentOutcome.Submitted ||
                            paymentOutcome == PaymentOutcome.Unknown
                        ) {
                            AppScreen.Result
                        } else {
                            AppScreen.Payment
                        }
                    }
                )
                AppScreen.TextEntry -> TextEntryScreen(
                    vpa = vpa,
                    merchantName = merchantName,
                    amount = amountInput,
                    errorMessage = validationError,
                    recentPayments = recentPayments,
                    onRecentPaymentSelected = { payment ->
                        vpa = payment.vpa
                        merchantName = payment.merchantName
                        merchantCategoryCode = null
                        amountInput = formatAmountInput(payment.amountPaise)
                        validationError = null
                    },
                    onVpaChange = {
                        vpa = it
                        validationError = null
                    },
                    onMerchantNameChange = {
                        merchantName = it
                        validationError = null
                    },
                    onAmountChange = {
                        amountInput = it
                        validationError = null
                    },
                    onContinue = {
                        if (validateDraft()) screen = AppScreen.SplitSetup
                    }
                )
                AppScreen.CameraScan -> CameraScanScreen(
                    onQrDetected = {
                        inputSource = InputSource.Camera
                        vpa = PrototypeVpa
                        merchantName = PrototypeMerchantName
                        merchantCategoryCode = "5812"
                        amountInput = ""
                        validationError = null
                        screen = AppScreen.ConfirmDetails
                    }
                )
                AppScreen.PhotoImport -> PhotoImportScreen(
                    onChoosePhoto = {
                        photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    }
                )
                AppScreen.ConfirmDetails -> ConfirmDetailsScreen(
                    inputSource = inputSource,
                    merchantName = merchantName,
                    vpa = vpa,
                    amount = amountInput,
                    errorMessage = validationError,
                    onAmountChange = {
                        amountInput = it
                        validationError = null
                    },
                    onContinue = {
                        if (validateDraft()) screen = AppScreen.SplitSetup
                    }
                )
                AppScreen.SplitSetup -> SplitSetupScreen(
                    totalAmount = formattedTotal,
                    splitAmounts = formattedSplitValues,
                    maximumSplitAmount = formatInr(MaximumSplitAmountPaise),
                    onContinue = { screen = AppScreen.Review }
                )
                AppScreen.Review -> ReviewScreen(
                    merchantName = merchantName,
                    totalAmount = formattedTotal,
                    splitAmounts = formattedSplitValues,
                    onContinue = { screen = AppScreen.AppSelection }
                )
                AppScreen.AppSelection -> AppSelectionScreen(
                    providers = PrototypeProviders,
                    selectedProvider = selectedProvider,
                    onProviderSelected = { selectedProvider = it },
                    onContinue = {
                        recentPayments = recentPaymentStore.save(
                            RecentPaymentDetails(
                                vpa = vpa.trim(),
                                merchantName = merchantName.trim(),
                                amountPaise = totalPaise
                            )
                        )
                        completedPayments = 0
                        paymentOutcome = null
                        hasActiveSession = true
                        screen = AppScreen.Payment
                    }
                )
                AppScreen.Payment -> PaymentScreen(
                    merchantName = merchantName,
                    vpa = vpa,
                    amount = currentAmount,
                    totalAmount = formattedTotal,
                    completedPayments = completedPayments,
                    splitCount = splitCount,
                    selectedProvider = selectedProvider,
                    usesRealGooglePay = selectedProvider == "Google Pay",
                    onPay = {
                        if (selectedProvider == "Google Pay") {
                            val transactionReference = generateTransactionReference()
                            val googlePayIntent = buildGooglePayIntent(
                                UpiPaymentRequest(
                                    payeeVpa = vpa,
                                    payeeName = merchantName,
                                    amountPaise = splitValues.getOrNull(completedPayments) ?: 0L,
                                    transactionReference = transactionReference,
                                    merchantCategoryCode = merchantCategoryCode
                                )
                            )
                            if (googlePayIntent.resolveActivity(context.packageManager) == null) {
                                paymentLaunchError = "Google Pay is not installed or cannot handle this UPI payment."
                            } else {
                                pendingTransactionReference = transactionReference
                                try {
                                    googlePayLauncher.launch(googlePayIntent)
                                } catch (_: ActivityNotFoundException) {
                                    pendingTransactionReference = null
                                    paymentLaunchError = "Google Pay could not be opened."
                                }
                            }
                        } else {
                            showReturnSimulator = true
                        }
                    }
                )
                AppScreen.Result -> paymentOutcome?.let { outcome ->
                    ResultScreen(
                        outcome = outcome,
                        amount = if (outcome == PaymentOutcome.Success) {
                            formattedSplitValues.getOrNull((completedPayments - 1).coerceAtLeast(0))
                                ?: currentAmount
                        } else {
                            currentAmount
                        },
                        completedPayments = completedPayments,
                        splitCount = splitCount,
                        nextAmount = nextAmount,
                        onPrimaryAction = {
                            when (outcome) {
                                PaymentOutcome.Success -> {
                                    if (completedPayments >= splitCount) {
                                        hasActiveSession = false
                                        paymentOutcome = null
                                        goHome()
                                    } else {
                                        paymentOutcome = null
                                        screen = AppScreen.Payment
                                    }
                                }
                                PaymentOutcome.Failure -> {
                                    paymentOutcome = null
                                    screen = AppScreen.Payment
                                }
                                PaymentOutcome.Submitted,
                                PaymentOutcome.Unknown -> goHome()
                            }
                        },
                        onSecondaryAction = {
                            paymentOutcome = null
                            goHome()
                        }
                    )
                }
            }
        }
    }

    if (showReturnSimulator) {
        SimulatedUpiReturnDialog(
            onDismiss = { showReturnSimulator = false },
            onOutcome = { outcome ->
                showReturnSimulator = false
                handlePaymentOutcome(outcome)
            }
        )
    }

    paymentLaunchError?.let { message ->
        AlertDialog(
            onDismissRequest = { paymentLaunchError = null },
            title = { Text("Unable to open Google Pay") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { paymentLaunchError = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SimulatedUpiReturnDialog(
    onDismiss: () -> Unit,
    onOutcome: (PaymentOutcome) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulate UPI app return") },
        text = {
            Column {
                Text(
                    text = "Choose the callback state returned by the external UPI app.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PaymentOutcome.entries.forEach { outcome ->
                    TextButton(onClick = { onOutcome(outcome) }) {
                        Text(
                            when (outcome) {
                                PaymentOutcome.Success -> "SUCCESS"
                                PaymentOutcome.Failure -> "FAILURE"
                                PaymentOutcome.Submitted -> "SUBMITTED"
                                PaymentOutcome.Unknown -> "No or invalid response"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

internal fun parseAmountToPaise(raw: String): Long? {
    val normalized = raw
        .replace("₹", "")
        .replace(",", "")
        .trim()
    if (normalized.isEmpty()) return null
    val amount = normalized.toBigDecimalOrNull() ?: return null
    if (amount <= BigDecimal.ZERO || amount.scale() > 2) return null
    return runCatching {
        amount
            .setScale(2, RoundingMode.UNNECESSARY)
            .movePointRight(2)
            .longValueExact()
    }.getOrNull()
}

private fun calculateAutomaticSplit(totalPaise: Long): List<Long> {
    if (totalPaise <= 0L) return emptyList()
    val paymentCount = automaticPaymentCount(totalPaise)
    if (paymentCount > MaximumAutomaticPaymentCount) return emptyList()
    val fullPaymentCount = (totalPaise / MaximumSplitAmountPaise).toInt()
    val remainder = totalPaise % MaximumSplitAmountPaise
    return buildList(fullPaymentCount + if (remainder > 0L) 1 else 0) {
        repeat(fullPaymentCount) {
            add(MaximumSplitAmountPaise)
        }
        if (remainder > 0L) {
            add(remainder)
        }
    }
}

private fun automaticPaymentCount(totalPaise: Long): Long {
    if (totalPaise <= 0L) return 0L
    return ((totalPaise - 1L) / MaximumSplitAmountPaise) + 1L
}

private fun formatAmountInput(paise: Long): String = BigDecimal
    .valueOf(paise, 2)
    .setScale(2)
    .toPlainString()

internal fun formatInr(paise: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
        currency = Currency.getInstance("INR")
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(BigDecimal.valueOf(paise, 2))
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SplitUpiAppPreview() {
    UPISplitTheme {
        SplitUpiApp()
    }
}
