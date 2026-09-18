package rahul.lohra.upisplit.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class RecentPaymentDetails(
    val vpa: String,
    val merchantName: String,
    val amountPaise: Long
)

internal class RecentPaymentStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun load(): List<RecentPaymentDetails> = runCatching {
        val storedValue = preferences.getString(RecentPaymentsKey, null)
            ?: return@runCatching emptyList()
        val json = JSONArray(storedValue)
        buildList {
            for (index in 0 until minOf(json.length(), MaximumRecentPayments)) {
                val item = json.optJSONObject(index) ?: continue
                val vpa = item.optString(VpaKey).trim()
                val merchantName = item.optString(MerchantNameKey).trim()
                val amountPaise = item.optLong(AmountPaiseKey, 0L)
                if (vpa.isNotEmpty() && merchantName.isNotEmpty() && amountPaise > 0L) {
                    add(
                        RecentPaymentDetails(
                            vpa = vpa,
                            merchantName = merchantName,
                            amountPaise = amountPaise
                        )
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

    fun save(payment: RecentPaymentDetails): List<RecentPaymentDetails> {
        val recentPayments = buildList {
            add(payment)
            addAll(load().filterNot { it.vpa.equals(payment.vpa, ignoreCase = true) })
        }.take(MaximumRecentPayments)

        val json = JSONArray().apply {
            recentPayments.forEach { recentPayment ->
                put(
                    JSONObject().apply {
                        put(VpaKey, recentPayment.vpa)
                        put(MerchantNameKey, recentPayment.merchantName)
                        put(AmountPaiseKey, recentPayment.amountPaise)
                    }
                )
            }
        }
        preferences.edit().putString(RecentPaymentsKey, json.toString()).apply()
        return recentPayments
    }

    private companion object {
        const val PreferencesName = "recent_payment_details"
        const val RecentPaymentsKey = "payments"
        const val VpaKey = "vpa"
        const val MerchantNameKey = "merchant_name"
        const val AmountPaiseKey = "amount_paise"
        const val MaximumRecentPayments = 2
    }
}
