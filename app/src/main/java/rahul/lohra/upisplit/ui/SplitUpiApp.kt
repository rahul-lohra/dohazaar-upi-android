package rahul.lohra.upisplit.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Box
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
import rahul.lohra.upisplit.payment.buildUpiPaymentIntent
import rahul.lohra.upisplit.payment.generateTransactionReference
import rahul.lohra.upisplit.payment.parseUpiClientResult
import rahul.lohra.upisplit.qr.UpiQrImageScanResult
import rahul.lohra.upisplit.qr.scanUpiQrImage
import rahul.lohra.upisplit.ui.theme.UPISplitTheme

private const val PrototypeMerchantName = "ABC Restaurant"
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
    var transactionNote by rememberSaveable { mutableStateOf<String?>(null) }
    var amountInput by rememberSaveable { mutableStateOf("") }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var isPhotoScanning by rememberSaveable { mutableStateOf(false) }
    var photoScanError by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraScanError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedProvider by rememberSaveable {
        mutableStateOf(UpiPaymentProviders.first().name)
    }
    var completedPayments by rememberSaveable { mutableIntStateOf(0) }
    var paymentOutcome by rememberSaveable { mutableStateOf<PaymentOutcome?>(null) }
    var hasActiveSession by rememberSaveable { mutableStateOf(false) }
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

    val upiPaymentLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
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
        if (uri == null) {
            isPhotoScanning = false
            photoScanError = null
            screen = AppScreen.Home
        } else {
            inputSource = InputSource.Photos
            isPhotoScanning = true
            photoScanError = null
            scanUpiQrImage(context, uri) { result ->
                isPhotoScanning = false
                if (screen != AppScreen.PhotoImport) return@scanUpiQrImage
                when (result) {
                    is UpiQrImageScanResult.Success -> {
                        val paymentData = result.paymentData
                        vpa = paymentData.payeeVpa
                        merchantName = paymentData.payeeName
                        merchantCategoryCode = paymentData.merchantCategoryCode
                        transactionNote = paymentData.transactionNote
                        amountInput = ""
                        validationError = null
                        screen = AppScreen.ConfirmDetails
                    }
                    UpiQrImageScanResult.NoQrCode -> {
                        photoScanError = "No QR code found in this image."
                    }
                    UpiQrImageScanResult.UnsupportedQrCode -> {
                        photoScanError = "This QR code is not a supported UPI payment QR."
                    }
                    is UpiQrImageScanResult.UnsafeToSplit -> {
                        photoScanError = buildString {
                            append("This QR fixes or binds transaction details and cannot be split safely.")
                            append("\nDetected: ")
                            append(result.paymentData.payeeName)
                            append(" · ")
                            append(result.paymentData.payeeVpa)
                        }
                    }
                    UpiQrImageScanResult.ImageReadFailure -> {
                        photoScanError = "This image could not be read. Choose another image."
                    }
                }
            }
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
                        transactionNote = null
                        amountInput = ""
                        validationError = null
                        screen = AppScreen.TextEntry
                    },
                    onCameraScan = {
                        inputSource = InputSource.Camera
                        cameraScanError = null
                        validationError = null
                        screen = AppScreen.CameraScan
                    },
                    onPhotoImport = {
                        inputSource = InputSource.Photos
                        photoScanError = null
                        isPhotoScanning = false
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
                        transactionNote = null
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
                    errorMessage = cameraScanError,
                    onScanResult = { result ->
                        when (result) {
                            is UpiQrImageScanResult.Success -> {
                                val paymentData = result.paymentData
                                inputSource = InputSource.Camera
                                vpa = paymentData.payeeVpa
                                merchantName = paymentData.payeeName
                                merchantCategoryCode = paymentData.merchantCategoryCode
                                transactionNote = paymentData.transactionNote
                                amountInput = ""
                                validationError = null
                                cameraScanError = null
                                screen = AppScreen.ConfirmDetails
                            }
                            UpiQrImageScanResult.NoQrCode -> Unit
                            UpiQrImageScanResult.UnsupportedQrCode -> {
                                cameraScanError = "This QR code is not a supported UPI payment QR."
                            }
                            is UpiQrImageScanResult.UnsafeToSplit -> {
                                cameraScanError =
                                    "This QR fixes or binds transaction details and cannot be split safely."
                            }
                            UpiQrImageScanResult.ImageReadFailure -> {
                                cameraScanError = "The QR code could not be read. Try again."
                            }
                        }
                    },
                    onCameraError = { message ->
                        cameraScanError = message
                    }
                )
                AppScreen.PhotoImport -> PhotoImportScreen(
                    isScanning = isPhotoScanning,
                    errorMessage = photoScanError,
                    onChoosePhoto = {
                        photoScanError = null
                        photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                    }
                )
                AppScreen.ConfirmDetails -> ConfirmDetailsScreen(
                    inputSource = inputSource,
                    merchantName = merchantName,
                    vpa = vpa,
                    merchantCategoryCode = merchantCategoryCode,
                    transactionNote = transactionNote,
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
                    providers = UpiPaymentProviders,
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
                    onPay = {
                        val provider = UpiPaymentProviders.first { candidate ->
                            candidate.name == selectedProvider
                        }
                        val transactionReference = generateTransactionReference()
                        val paymentIntent = buildUpiPaymentIntent(
                            request = UpiPaymentRequest(
                                payeeVpa = vpa,
                                payeeName = merchantName,
                                amountPaise = splitValues.getOrNull(completedPayments) ?: 0L,
                                transactionReference = transactionReference,
                                merchantCategoryCode = merchantCategoryCode,
                                transactionNote = transactionNote
                            ),
                            packageName = provider.packageName
                        )
                        if (paymentIntent.resolveActivity(context.packageManager) == null) {
                            paymentLaunchError =
                                "$selectedProvider is not installed, not set up for UPI, or cannot handle this payment."
                        } else {
                            pendingTransactionReference = transactionReference
                            try {
                                upiPaymentLauncher.launch(paymentIntent)
                            } catch (_: ActivityNotFoundException) {
                                pendingTransactionReference = null
                                paymentLaunchError = "$selectedProvider could not be opened."
                            }
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

    paymentLaunchError?.let { message ->
        AlertDialog(
            onDismissRequest = { paymentLaunchError = null },
            title = { Text("Unable to open $selectedProvider") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { paymentLaunchError = null }) {
                    Text("OK")
                }
            }
        )
    }
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
