package rahul.lohra.upisplit.payment

import android.content.Intent
import android.net.Uri
import java.math.BigDecimal
import java.security.SecureRandom
import java.util.Locale

internal data class UpiPaymentRequest(
    val payeeVpa: String,
    val payeeName: String,
    val amountPaise: Long,
    val transactionReference: String,
    val merchantCategoryCode: String? = null,
    val transactionNote: String? = null
)

internal enum class UpiClientStatus {
    Success,
    Failure,
    Submitted,
    Unknown
}

internal data class UpiClientResult(
    val status: UpiClientStatus,
    val transactionId: String?,
    val transactionReference: String?,
    val responseCode: String?,
    val approvalReferenceNumber: String?,
    val rawResponse: String?
)

internal fun buildUpiPaymentIntent(
    request: UpiPaymentRequest,
    packageName: String
): Intent {
    require(request.amountPaise > 0L) { "UPI amount must be positive." }
    require(packageName.isNotBlank()) { "UPI app package name must not be blank." }
    require(request.transactionReference.all(Char::isDigit)) {
        "UPI transaction reference must be numeric."
    }
    require(request.transactionReference.length <= 35) {
        "UPI transaction reference must not exceed 35 digits."
    }

    val uriBuilder = Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", request.payeeVpa)
        .appendQueryParameter("pn", request.payeeName)
        .appendQueryParameter("tr", request.transactionReference)
        .appendQueryParameter("am", formatUpiAmount(request.amountPaise))
        .appendQueryParameter("cu", "INR")

    request.merchantCategoryCode?.let { merchantCategoryCode ->
        uriBuilder.appendQueryParameter("mc", merchantCategoryCode)
    }
    request.transactionNote?.let { transactionNote ->
        uriBuilder.appendQueryParameter("tn", transactionNote)
    }

    return Intent(Intent.ACTION_VIEW, uriBuilder.build()).apply {
        setPackage(packageName)
    }
}

internal fun generateTransactionReference(): String {
    val timestamp = System.currentTimeMillis().toString()
    val randomSuffix = SecureRandom().nextInt(1_000_000).toString().padStart(6, '0')
    return timestamp + randomSuffix
}

internal fun parseUpiClientResult(
    data: Intent?,
    expectedTransactionReference: String
): UpiClientResult {
    val dataString = data?.dataString
    val rawResponse = data?.getStringExtra("response")
        ?: data?.getStringExtra("Response")
        ?: dataString?.substringAfter('?', dataString)

    if (rawResponse.isNullOrBlank()) {
        return UpiClientResult(
            status = UpiClientStatus.Unknown,
            transactionId = null,
            transactionReference = null,
            responseCode = null,
            approvalReferenceNumber = null,
            rawResponse = rawResponse
        )
    }

    val fields = parseResponseFields(rawResponse)
        ?: return UpiClientResult(
            status = UpiClientStatus.Unknown,
            transactionId = null,
            transactionReference = null,
            responseCode = null,
            approvalReferenceNumber = null,
            rawResponse = rawResponse
        )

    val statusValue = fields["status"]
    val transactionId = fields["txnid"]
    val transactionReference = fields["txnref"]
    val referenceMatches = transactionReference == expectedTransactionReference

    val status = when {
        statusValue.equals("SUCCESS", ignoreCase = true) &&
            !transactionId.isNullOrBlank() && referenceMatches -> UpiClientStatus.Success
        statusValue.equals("FAILURE", ignoreCase = true) &&
            (transactionReference == null || referenceMatches) -> UpiClientStatus.Failure
        statusValue.equals("SUBMITTED", ignoreCase = true) &&
            (transactionReference == null || referenceMatches) -> UpiClientStatus.Submitted
        else -> UpiClientStatus.Unknown
    }

    return UpiClientResult(
        status = status,
        transactionId = transactionId,
        transactionReference = transactionReference,
        responseCode = fields["responsecode"],
        approvalReferenceNumber = fields["approvalrefno"],
        rawResponse = rawResponse
    )
}

private fun parseResponseFields(rawResponse: String): Map<String, String>? {
    if (rawResponse.length > 8_192) return null
    val fields = linkedMapOf<String, String>()
    rawResponse.split('&').forEach { entry ->
        val separatorIndex = entry.indexOf('=')
        if (separatorIndex <= 0) return null
        val key = Uri.decode(entry.substring(0, separatorIndex)).trim().lowercase(Locale.ROOT)
        val value = Uri.decode(entry.substring(separatorIndex + 1)).trim()
        if (key.isEmpty() || key in fields || key.hasControlCharacters() || value.hasControlCharacters()) {
            return null
        }
        fields[key] = value
    }
    return fields
}

private fun String.hasControlCharacters(): Boolean = any { it.code in 0..31 || it.code == 127 }

private fun formatUpiAmount(amountPaise: Long): String =
    BigDecimal.valueOf(amountPaise, 2).setScale(2).toPlainString()
