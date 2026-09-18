SplitUPI — Android MVP PRD

Version: 0.1
Platform: Android
Primary technology: Kotlin + Jetpack Compose
Architecture: Single Android application, local-first
Initial payment mechanism: UPI Intent / upi://pay
Initial supported payment apps: Google Pay, PhonePe, Paytm, BHIM where installed
Backend: None for MVP
Authentication: None
Account creation: None

Protocol compatibility: [SplitUPI UPI Compatibility Profile](./upi-compatibility-spec.md). The compatibility profile controls URI, QR eligibility, handoff, and callback behavior where it is more restrictive than this PRD.

UX reference: [Interactive SplitUPI UX Blueprint](./ux/splitupi-ux-blueprint.html).

⸻

1. Product Overview

SplitUPI allows a user to make a large UPI payment as a sequence of smaller UPI payments.

Example:

Merchant bill: ₹10,000
Split:
5 payments
Payment 1 → ₹2,000
Payment 2 → ₹2,000
Payment 3 → ₹2,000
Payment 4 → ₹2,000
Payment 5 → ₹2,000

The application does not process or hold money.

Instead, it acts as an orchestration layer:

SplitUPI
↓
Creates UPI payment intent
↓
Launches user's selected UPI application
↓
User authorizes payment
↓
UPI app returns payment response
↓
SplitUPI updates transaction state
↓
User explicitly starts next split

⸻

2. MVP Goal

Validate the following hypothesis:

Users are willing to use a separate Android application to divide one UPI payment into multiple smaller UPI transactions.

The MVP should answer:

1. Can users reliably enter payment details as text?
2. Can we reliably scan common UPI QR codes with the camera?
3. Can we reliably read a UPI QR from an image selected from phone media?
4. Can we extract and validate the payment information from all three input methods?
5. Can we modify an eligible payment amount for the split?
6. Can we launch the selected UPI application?
7. Can we receive and parse the payment response?
8. Can we reliably maintain payment state across app lifecycle events?
9. Can the user sequentially complete multiple payments without accidentally duplicating a payment?

⸻

3. Explicit Non-Goals

Do not implement these in V1:

* Bank integration
* PSP integration
* Payment aggregation
* UPI AutoPay
* Credit/EMI
* User accounts
* Cloud synchronization
* Payment processing by SplitUPI
* Storing UPI PIN
* Automatic UPI PIN entry
* Accessibility automation
* Automatically clicking the Pay button inside another UPI app
* Automatically launching the next payment
* Background payment execution
* Merchant settlement
* Payment reconciliation with a bank/PSP
* iOS
* Web

The app must never attempt to bypass the user’s authorization inside the UPI application.

⸻

4. Payment Input Methods and Primary Flow

The home screen must provide exactly three ways to enter payment information:

1. Text entry
   * VPA
   * Merchant name
   * Amount
2. Scan QR code
   * Opens the device camera.
3. Scan QR code from phone media
   * Opens the Android system Photo Picker so the user can select one image from Photos or another media provider.

Flow A — Text entry

Home
↓
Enter payment details
↓
Enter VPA, merchant name, and amount
↓
Validate payment details
↓
Payment Details

Flow B — Scan QR with camera

Home
↓
Scan QR
↓
Request camera permission if needed
↓
Open camera
↓
Scan and parse UPI QR
↓
Payment Details

Flow C — Scan QR from phone media

Home
↓
Scan QR from Photos
↓
Open system Photo Picker
↓
User selects one image
↓
Detect and parse UPI QR in the image
↓
Payment Details

All three flows then converge:

Payment Details
↓
Calculate automatic ₹2,000 splits
↓
Split Preview
↓
Select UPI App
↓
Pay Split #1
↓
External UPI app
↓
Return to SplitUPI
↓
Process response
↓
Payment Result
↓
[Pay Next]
↓
Repeat

Cancelling the camera or Photo Picker must return to the home screen without creating a split session or changing entered data.

⸻

5. Text Input Requirements

The text-entry screen must contain three required fields:

VPA
[ restaurant@upi ]

Merchant name
[ ABC Restaurant ]

Amount
[ ₹10,000 ]

The user cannot continue until all three values pass the validation rules in the UPI compatibility profile. After validation, the app automatically creates payments capped at ₹2,000 and shows the calculated split.

The text-entry screen must also show up to two recent valid payment details, newest first. Each item contains the VPA, merchant name, and total amount. Selecting an item autofills all three fields. A payment is added to this local-only history when the user starts its payment session; selecting the same VPA again replaces its older entry instead of creating a duplicate.

Example:

Automatic split
Maximum ₹2,000 per payment

Result
₹2,000 × 5

⸻

6. QR Acquisition and Parsing Requirements

The application must accept a QR from either the live camera or one image selected through the Android system Photo Picker. Both sources must use the same QR decoder, UPI parser, validation rules, and compatibility profile.

Camera requirements:

* Open the camera only after the user taps **Scan QR**.
* Request camera permission only when it is needed.
* Stop scanning after the first valid, eligible UPI QR is detected.

Phone-media requirements:

* Open the system Photo Picker only after the user taps **Scan QR from Photos**.
* Allow selection of exactly one image.
* Decode the selected image locally. Never upload it.
* Do not request broad photo-library or storage permission.
* Release access to the selected image after decoding; persistent media access is unnecessary.

Only QR payloads representing eligible UPI payment information may proceed to payment setup.

Amount-open example:

upi://pay?pa=merchant@upi&pn=ABC%20Restaurant&mc=5812&cu=INR

The parser must extract all known fields and retain unknown parameters internally. Unknown provider-specific parameters such as `aid` are ignored for split eligibility and never copied into child-payment intents. Known binding parameters remain ineligible under the UPI compatibility profile; preserving a field does not mean it is safe to modify or re-emit.

If the image contains no QR, show:

No QR code found in this image.

If a QR is not a valid UPI payment payload, show:

This QR code is not a supported UPI payment QR.

If a UPI QR fixes or binds transaction details and is therefore ineligible for splitting, show the compatibility-profile error instead of treating it as a malformed QR.

Do not launch any external application after any QR error.

⸻

7. UPI Data Model

Create:

data class UpiPaymentData(
val payeeVpa: String,
val payeeName: String?,
val merchantCategoryCode: String?,
val amount: BigDecimal?,
val currency: String?,
val transactionReference: String?,
val transactionNote: String?,
val transactionUrl: String?,
val additionalParameters: Map<String, String>
)

Do not represent money internally as Double.

Use:

BigDecimal

or integer paise.

⸻

8. Payment Type

The application should distinguish:

enum class UpiPaymentType {
P2P,
P2M,
UNKNOWN
}

For V1, the parser may infer P2P/P2M only where the QR provides sufficient information.

Do not invent an MCC.

If mc is present, preserve it exactly.

If mc is absent, do not fabricate a merchant category.

⸻

9. Split Calculation

The application must split the total automatically. Each payment must be no more than ₹2,000. Create ₹2,000 payments until the remaining amount is ₹2,000 or less, then create one final payment for that remainder.

Examples:

₹3,500 → ₹2,000 + ₹1,500

₹4,000 → ₹2,000 + ₹2,000

₹10,001 → ₹2,000 + ₹2,000 + ₹2,000 + ₹2,000 + ₹2,000 + ₹1

The number of payments is derived and is not user-selectable. The sum must always exactly equal the original amount, and no payment may exceed ₹2,000.

Algorithm in paise:

maximumSplitAmount = 200000
fullPaymentCount = total / maximumSplitAmount
remainder = total % maximumSplitAmount

Create `fullPaymentCount` payments of `maximumSplitAmount`. If `remainder > 0`, append one final payment containing the remainder.

All calculations should operate in the smallest currency unit.

For INR:

₹10.00 → 1000 paise

⸻

10. Split Session

Every payment operation creates a local SplitSession.

data class SplitSession(
val id: String,
val payee: UpiPaymentData,
val originalAmount: Long,
val numberOfSplits: Int,
val createdAt: Instant,
val status: SplitSessionStatus
)

Each split is a separate transaction.

data class SplitTransaction(
val id: String,
val sessionId: String,
val sequenceNumber: Int,
val amount: Long,
val transactionReference: String,
val status: PaymentStatus
)

⸻

11. Transaction Reference

Every split must have a unique transaction reference.

Example:

split_8F32A1_001
split_8F32A1_002
split_8F32A1_003

Never reuse a transaction reference for a different payment attempt.

If the user retries a failed payment, create a new attempt/reference or follow the applicable UPI semantics rather than blindly reusing the old transaction.

⸻

12. Payment State Machine

This is the most important part of the application.

sealed interface PaymentStatus {
data object NotStarted : PaymentStatus
data object Launching : PaymentStatus
data object AwaitingResult : PaymentStatus
data object Success : PaymentStatus
data object Failure : PaymentStatus
data object Submitted : PaymentStatus
data object Unknown : PaymentStatus
}

Lifecycle:

NOT_STARTED
↓
LAUNCHING
↓
AWAITING_RESULT
↓
┌───┼───────────┐
↓   ↓           ↓
SUCCESS FAILURE SUBMITTED
↓
UNKNOWN

Do not treat:

app resumed

as:

payment successful

⸻

13. UPI Intent Creation

Create a dedicated component:

interface UpiIntentBuilder {
fun build(
payment: SplitTransaction,
paymentData: UpiPaymentData
): Uri
}

The generated URI should follow:

upi://pay

and include the appropriate payment parameters.

For a split transaction:

pa = original payee VPA
pn = original payee name
am = split amount
cu = INR
tr = unique split transaction reference

Preserve applicable merchant information from the scanned QR.

Do not blindly copy the original transaction reference into every split.

⸻

14. UPI App Selection

Create:

data class UpiApp(
val packageName: String,
val displayName: String
)

The app should detect installed UPI applications.

Initial known applications:

Google Pay
PhonePe
Paytm
BHIM

The implementation should not make the core payment engine dependent on one provider.

Architecture:

UpiPaymentLauncher
│
├── Google Pay
├── PhonePe
├── Paytm
└── BHIM

If the selected application isn’t installed:

This payment app is not installed.

⸻

15. Payment Launch

Create:

interface UpiPaymentLauncher {
fun launch(
activity: Activity,
app: UpiApp,
paymentUri: Uri
)
}

The Android implementation should use an appropriate Intent with:

ACTION_VIEW

and the UPI URI.

The app must not attempt to automate interaction with the external UPI application.

⸻

16. Payment Callback

The application must register for the result of the launched UPI activity.

The callback parser should support responses containing fields such as:

Status
txnId
txnRef
responseCode
ApprovalRefNo

and provider-specific response payloads where applicable.

Create:

data class UpiPaymentResponse(
val status: UpiResponseStatus,
val transactionId: String?,
val transactionReference: String?,
val responseCode: String?,
val approvalReferenceNumber: String?,
val rawResponse: String?
)
enum class UpiResponseStatus {
SUCCESS,
FAILURE,
SUBMITTED,
UNKNOWN
}

The raw provider response should be retained locally for debugging during MVP development.

Do not log sensitive payment information in production logs.

⸻

17. Callback → State Mapping

UPI response
│
├── SUCCESS
│      ↓
│   Success
│
├── FAILURE
│      ↓
│   Failure
│
├── SUBMITTED
│      ↓
│   Submitted
│
└── anything else
↓
Unknown

For SUCCESS, show:

Payment successful
₹2,000
1 of 5 payments completed
[ Pay Next ₹2,000 ]

For FAILURE:

Payment failed
₹2,000
[ Try Again ]

For SUBMITTED:

Payment submitted
We're unable to confirm the final status yet.
[ Check Again ]

For UNKNOWN:

Payment status unavailable
Please verify the payment in your UPI app/bank before retrying.
[ Check Status ]
[ Return Home ]

⸻

18. Critical Duplicate-Payment Protection

The app must never automatically retry a transaction whose status is:

SUBMITTED
UNKNOWN

Example:

Payment #2
₹2,000
UNKNOWN

The user must explicitly decide what to do.

This is essential because the original transaction may have succeeded even though the app did not receive a definitive response.

⸻

19. Next Payment

The next split should only be initiated after an explicit user action.

Never do:

SUCCESS
↓
automatically launch next UPI payment

Instead:

SUCCESS
↓
show result
↓
user presses [Pay Next]
↓
launch next UPI transaction

This is a product safety requirement.

⸻

20. Session Progress

The main payment screen should show:

₹10,000 total
██████░░░░░░░░░░ 40%
2 / 5 payments completed
✓ ₹2,000
✓ ₹2,000
○ ₹2,000
○ ₹2,000
○ ₹2,000

The user can clearly see:

* Total amount
* Number of splits
* Completed payments
* Current payment
* Remaining amount

⸻

21. App Lifecycle

The app must persist the active session locally.

Potential scenario:

SplitUPI
↓
Launch GPay
↓
Android kills SplitUPI
↓
User returns later

When the application starts:

Load active SplitSession
↓
Restore transaction state
↓
Show current state

Do not assume that process death means payment failure.

⸻

22. Local Persistence

For MVP:

Room

Database:

split_sessions
split_transactions

Suggested schema:

split_sessions
-------------------------
id
payee_vpa
payee_name
mcc
original_amount
currency
number_of_splits
created_at
status
split_transactions
-------------------------
id
session_id
sequence_number
amount
transaction_reference
status
upi_transaction_id
response_code
approval_reference
created_at
updated_at

⸻

23. Home Screen

Minimal MVP:

SplitUPI
Split one payment into
multiple smaller UPI payments.
[ Enter Payment Details ]
[ Scan QR ]
[ Scan QR from Photos ]
────────────
Recent Payments

Do not build a complicated dashboard.

⸻

24. Payment Setup Screen

For a QR scanned with the camera or selected from phone media:

ABC Restaurant
UPI ID
restaurant@upi
Total amount
[ ₹10,000 ]
[ Continue ]

Because the MVP only splits amount-open QRs, the user enters the total amount after a successful QR scan.

For text entry:

VPA
[ restaurant@upi ]
Merchant name
[ ABC Restaurant ]
Amount
[ ₹10,000 ]
[ Continue ]

After either input flow, show:

Automatic split
Maximum per payment  ₹2,000
Payments             5
Final payment        ₹2,000
[ Review split ]

⸻

25. Split Preview

Before launching the first transaction:

Review split
Total
₹10,000
5 payments
1   ₹2,000
2   ₹2,000
3   ₹2,000
4   ₹2,000
5   ₹2,000
Payment app
Google Pay
[ Start Payment ]

This is the last confirmation before money leaves the app.

⸻

26. Payment App Selection

Show installed UPI apps:

Choose payment app
◉ Google Pay
○ PhonePe
○ Paytm
○ BHIM
[ Continue ]

Persist the user’s last selection as a convenience.

⸻

27. Camera and Phone Media Access

Request camera permission only when the user taps:

Scan QR

If denied:

Camera permission is required
to scan a UPI QR.
[ Open Settings ]
[ Cancel ]

When the user taps **Scan QR from Photos**, launch the Android system Photo Picker with image-only selection. Do not request `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE`, or broad storage access. The app only needs temporary read access to the single URI returned by the picker.

If the user cancels the camera or Photo Picker, return without showing an error and without creating a session.

⸻

28. Security Requirements

The application must:

* Never request or store UPI PIN.
* Never attempt to read UPI PIN.
* Never automate taps inside GPay/PhonePe/etc.
* Never use accessibility services to automate payment.
* Never store bank credentials.
* Never modify unrelated QR information.
* Validate the payee VPA before creating a payment.
* Require explicit user confirmation before each split.
* Never automatically retry ambiguous payments.
* Never claim a payment is successful solely because the app resumed.

⸻

29. Logging

During development, provide structured logs:

SplitSessionCreated
UpiQrScanned
UpiPaymentPrepared
UpiAppLaunchRequested
UpiAppReturned
UpiResponseParsed
PaymentStateChanged
SplitCompleted

Example:

PaymentStateChanged(
transactionId=local_123,
from=AwaitingResult,
to=Success
)

Never log:

UPI PIN
bank account number
authentication credentials

⸻

30. Error Handling

The following cases must have explicit handling:

No UPI app installed

No compatible UPI payment app found.

Selected app unavailable

Google Pay is no longer available.
Please select another UPI app.

User backs out of UPI app

Payment was not confirmed.
[ Try Again ]
[ Cancel ]

UPI app returns no response

We couldn't determine the payment status.
Do not retry until you verify whether the payment was deducted.

QR is invalid

Invalid UPI QR.

Amount is invalid

Enter a valid amount.

Split count invalid

Constraints:

minimum = 2
maximum = 20

For MVP, I’d cap it at 20 to prevent ridiculous payment sequences.

⸻

31. Architecture

Use a clean, boring architecture.

UI
│
▼
ViewModel
│
▼
Use Cases
│
├── ScanUpiQr
├── CreateSplitSession
├── CalculateSplit
├── CreateUpiPayment
├── LaunchUpiPayment
├── HandleUpiResponse
└── ResumeSplitSession
│
▼
Repositories
│
├── SplitRepository
├── UpiRepository
└── InstalledAppsRepository
│
▼
Room / Android APIs

Suggested package structure:

com.splitupi
├── data
│   ├── local
│   ├── repository
│   └── upi
│
├── domain
│   ├── model
│   ├── repository
│   └── usecase
│
├── feature
│   ├── home
│   ├── scanner
│   ├── setup
│   ├── split
│   └── payment
│
└── platform
└── upi

⸻

32. Testing Requirements

Codex should generate unit tests for:

QR parser

valid UPI QR
missing pa
encoded pn
amount present
amount absent
MCC present
P2P QR
dynamic merchant QR
unknown parameters
malformed URI
non-UPI QR

QR image input

image containing one eligible UPI QR
image containing no QR
image containing a non-UPI QR
image containing an ineligible amount-bound UPI QR
corrupt or unreadable image
Photo Picker cancellation
camera cancellation
the same payload produces the same parsed result from camera and phone media

Split calculation

₹1 → ₹1
₹2,000 → ₹2,000
₹2,000.01 → ₹2,000 + ₹0.01
₹3,500 → ₹2,000 + ₹1,500
₹4,000 → ₹2,000 + ₹2,000
₹10,001 → five ₹2,000 payments + ₹1
smallest possible amount

Invariants:

sum(splitAmounts) == originalAmount
every splitAmount <= ₹2,000
every splitAmount > ₹0

Transaction state machine

Test:

NotStarted → Launching
Launching → AwaitingResult
AwaitingResult → Success
AwaitingResult → Failure
AwaitingResult → Submitted
AwaitingResult → Unknown

Ensure:

Submitted → automatic retry

is impossible.

⸻

33. Definition of Done

The MVP is complete when a user can:

1. Open the Android app.
2. Enter a VPA, merchant name, and amount as text.
3. Alternatively, scan a valid UPI QR with the camera.
4. Alternatively, select an image containing a valid UPI QR from phone media.
5. See the same normalized payment details regardless of input method.
6. Enter/select the total amount.
7. See the automatic payment count and exact split amounts capped at ₹2,000.
8. Select an installed UPI application.
9. Launch the UPI payment with the split amount.
10. Complete the payment in the external UPI application.
11. Return to SplitUPI.
12. See the payment result.
13. Manually initiate the next split.
14. Complete all splits.
15. Close/reopen the application and recover the active split session.
16. Avoid accidental duplicate payment when a transaction is SUBMITTED or UNKNOWN.

⸻

34. Important MVP boundary

There is one thing I would not let Codex assume:

SUCCESS from the client-side UPI response = bank-confirmed payment.

For V1 we’re using the UPI-app response for UX/state experimentation only. A production version needs server-side transaction verification/reconciliation through the appropriate PSP/bank/payment-partner arrangement. Google explicitly recommends server-side verification rather than relying solely on the client response.

So I’d put this directly into the code as documentation:

/**
* MVP payment status.
*
* IMPORTANT:
* Client-side UPI responses are not authoritative
* settlement confirmation. Production must add
* PSP/bank-side transaction verification.
  */

⸻

Recommended V1 tech stack

Given that you’re an Android engineer, I’d keep it aggressively native:

Kotlin
Jetpack Compose
Material 3
Coroutines
StateFlow
ViewModel
Room
CameraX
ML Kit Barcode Scanning
Android Photo Picker
Android Activity Result APIs
UPI Intent / ACTION_VIEW
JUnit
Turbine

No backend. No KMP. No Flutter.

Build the Android version first. The core experiment is whether the UPI handoff + callback + sequential split UX actually works reliably across GPay/PhonePe/Paytm. Once that works, the business/product questions become much easier to answer.

One thing I would do before giving this PRD to Codex, though, is create a small UPI compatibility specification for the exact upi://pay parameters and callback formats we intend to support. That should be a separate technical spec referenced by the PRD; otherwise Codex will make assumptions around mc, tr, callback parsing, and provider behavior, which is exactly where payment implementations become brittle.
