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
    val initials: String
)

internal val PrototypeProviders = listOf(
    PaymentProvider(name = "Google Pay", initials = "G"),
    PaymentProvider(name = "PhonePe", initials = "P"),
    PaymentProvider(name = "Paytm", initials = "Pt"),
    PaymentProvider(name = "BHIM", initials = "B")
)
