package rahul.lohra.upisplit.ui

internal enum class AppScreen(val title: String) {
    Home("SplitUPI"),
    TextEntry("Payment details"),
    CameraScan("Scan QR"),
    PhotoImport("Phone media"),
    ConfirmDetails("Confirm details"),
    SplitSetup("Split payment"),
    Review("Review"),
    AppSelection("Payment app"),
    Payment("Payment session"),
    Result("Payment result")
}

internal enum class InputSource {
    Text,
    Camera,
    Photos
}

internal enum class PaymentOutcome {
    Success,
    Failure,
    Submitted,
    Unknown
}

internal data class PaymentProvider(
    val name: String,
    val initials: String,
    val packageName: String
)

internal val UpiPaymentProviders = listOf(
    PaymentProvider(
        name = "Google Pay",
        initials = "G",
        packageName = "com.google.android.apps.nbu.paisa.user"
    ),
    PaymentProvider(
        name = "PhonePe",
        initials = "P",
        packageName = "com.phonepe.app"
    ),
    PaymentProvider(
        name = "Paytm",
        initials = "Pt",
        packageName = "net.one97.paytm"
    ),
    PaymentProvider(
        name = "BHIM",
        initials = "B",
        packageName = "in.org.npci.upiapp"
    )
)
