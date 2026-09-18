package rahul.lohra.upisplit.qr

import android.net.Uri
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

internal data class UpiPaymentData(
    val payeeVpa: String,
    val payeeName: String,
    val merchantCategoryCode: String?,
    val transactionNote: String?,
    val currency: String,
    val additionalParameters: Map<String, String>
)

internal sealed interface UpiQrParseResult {
    data class Eligible(val paymentData: UpiPaymentData) : UpiQrParseResult
    data class Ineligible(val paymentData: UpiPaymentData) : UpiQrParseResult
    data object Invalid : UpiQrParseResult
}

internal fun parseUpiQrPayload(rawPayload: String): UpiQrParseResult {
    if (rawPayload.isBlank() || rawPayload.length > MaximumPayloadLength) {
        return UpiQrParseResult.Invalid
    }

    val uri = try {
        URI(rawPayload)
    } catch (_: URISyntaxException) {
        return UpiQrParseResult.Invalid
    }

    if (!uri.scheme.equals(UpiScheme, ignoreCase = true) ||
        !uri.rawAuthority.equals(PayAuthority, ignoreCase = true) ||
        !uri.rawPath.isNullOrEmpty() ||
        uri.rawFragment != null
    ) {
        return UpiQrParseResult.Invalid
    }

    val parameters = parseQuery(uri.rawQuery ?: return UpiQrParseResult.Invalid)
        ?: return UpiQrParseResult.Invalid

    val additionalParameters = parameters.filterKeys { it !in AllowedKeys }
    val containsBindingParameter = parameters.keys.any { key ->
        key.lowercase(Locale.ROOT) in BindingParameterKeys
    }
    val payeeVpa = parameters[PayeeAddressKey]?.trim()
        ?: return UpiQrParseResult.Invalid
    val payeeName = parameters[PayeeNameKey]?.trim()
        ?: return UpiQrParseResult.Invalid
    val merchantCategoryCode = parameters[MerchantCategoryKey]
    val transactionNote = parameters[TransactionNoteKey]
    val currency = parameters[CurrencyKey]?.uppercase(Locale.ROOT) ?: InrCurrency

    if (!isValidVpa(payeeVpa) ||
        payeeName.isEmpty() ||
        payeeName.codePointCount() > MaximumPayeeNameLength ||
        payeeName.hasControlCharacters() ||
        merchantCategoryCode?.matches(MerchantCategoryPattern) == false ||
        transactionNote?.codePointCount()?.let { it > MaximumTransactionNoteLength } == true ||
        transactionNote?.hasControlCharacters() == true ||
        currency != InrCurrency
    ) {
        return UpiQrParseResult.Invalid
    }

    val paymentData = UpiPaymentData(
        payeeVpa = payeeVpa,
        payeeName = payeeName,
        merchantCategoryCode = merchantCategoryCode,
        transactionNote = transactionNote,
        currency = currency,
        additionalParameters = additionalParameters
    )
    return if (containsBindingParameter) {
        UpiQrParseResult.Ineligible(paymentData)
    } else {
        UpiQrParseResult.Eligible(paymentData)
    }
}

private fun parseQuery(rawQuery: String): Map<String, String>? {
    if (rawQuery.isEmpty()) return null
    val parameters = linkedMapOf<String, String>()
    rawQuery.split('&').forEach { field ->
        val separatorIndex = field.indexOf('=')
        if (separatorIndex <= 0) return null
        val rawKey = field.substring(0, separatorIndex)
        val rawValue = field.substring(separatorIndex + 1)
        if (!rawKey.hasValidPercentEscapes() || !rawValue.hasValidPercentEscapes()) return null
        val key = Uri.decode(rawKey)
        val value = Uri.decode(rawValue)
        if (key.isEmpty() ||
            key.hasControlCharacters() ||
            value.hasControlCharacters() ||
            parameters.put(key, value) != null
        ) {
            return null
        }
    }
    return parameters
}

private fun isValidVpa(vpa: String): Boolean =
    vpa.count { it == '@' } == 1 &&
        vpa.substringBefore('@').isNotEmpty() &&
        vpa.substringAfter('@').isNotEmpty() &&
        vpa.none { character ->
            character.isWhitespace() || character.isControlCharacter() || character in "?#&"
        }

private fun String.hasValidPercentEscapes(): Boolean {
    var index = 0
    while (index < length) {
        if (this[index] == '%') {
            if (index + 2 >= length ||
                !this[index + 1].isHexDigit() ||
                !this[index + 2].isHexDigit()
            ) {
                return false
            }
            index += 3
        } else {
            index++
        }
    }
    return true
}

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private fun Char.isControlCharacter(): Boolean = code in 0..31 || code == 127

private fun String.hasControlCharacters(): Boolean = any(Char::isControlCharacter)

private fun String.codePointCount(): Int = codePointCount(0, length)

private const val MaximumPayloadLength = 4 * 1024
private const val MaximumPayeeNameLength = 100
private const val MaximumTransactionNoteLength = 80
private const val UpiScheme = "upi"
private const val PayAuthority = "pay"
private const val PayeeAddressKey = "pa"
private const val PayeeNameKey = "pn"
private const val MerchantCategoryKey = "mc"
private const val TransactionNoteKey = "tn"
private const val CurrencyKey = "cu"
private const val InrCurrency = "INR"
private val AllowedKeys = setOf(
    PayeeAddressKey,
    PayeeNameKey,
    MerchantCategoryKey,
    TransactionNoteKey,
    CurrencyKey
)
private val BindingParameterKeys = setOf("am", "mam", "tr", "tid", "url", "sign")
private val MerchantCategoryPattern = Regex("[0-9]{4}")
