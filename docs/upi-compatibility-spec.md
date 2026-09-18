# SplitUPI UPI Compatibility Profile

Version: 0.1 (draft)  
Date: 2026-09-18  
Platform: Android  
Protocol: generic UPI deep link (`upi://pay`)

## 1. Purpose and status

This document defines the small, conservative UPI surface supported by the SplitUPI MVP. It covers QR ingestion, child-payment URI generation, Android app handoff, and callback parsing.

This is an application compatibility profile, not an NPCI certification or a substitute for the current NPCI UPI Linking Specification, acquiring-bank rules, or PSP onboarding. When this document conflicts with the product PRD, this document controls protocol behavior.

The words **MUST**, **MUST NOT**, **SHOULD**, and **MAY** are normative.

## 2. Non-negotiable boundary

SplitUPI MUST only split an **amount-open, unbound static QR** or a manually entered VPA. It MUST NOT split or rewrite a QR that contains any of:

- `am` (amount)
- `mam` (minimum amount)
- `tr` (merchant transaction reference)
- `tid` (transaction identifier)
- `url` (transaction URL)
- `sign` (signature)

NPCI's published merchant-QR guidance says mandatory parameters in a dynamic QR are non-editable. Changing the amount or transaction reference of such a QR can break integrity, merchant reconciliation, or both.

An ineligible QR MAY be displayed as unsupported, but MUST NOT be converted into child payment intents. The message MUST say: **“This QR fixes or binds transaction details and cannot be split safely.”**

## 3. Supported input profile

A scanned QR is eligible only when all of these conditions hold:

1. Its scheme is `upi` and authority is `pay`, compared case-insensitively.
2. Every query key is unique.
3. It does not contain `am`, `mam`, `tr`, `tid`, `url`, or `sign`, compared using ASCII case normalization.
4. `pa` and `pn` are present and valid under the rules below.
5. `cu` is absent or exactly `INR` after ASCII case normalization.

The parser MUST percent-decode each key and value exactly once, MUST reject malformed escapes, and MUST reject NUL, CR, LF, and other ASCII control characters. The raw QR payload MUST be capped at 4 KiB.

| Key | Meaning | Input rule | Child-intent rule |
|---|---|---|---|
| `pa` | Payee VPA | Required. Trim surrounding whitespace; require one `@`; reject whitespace, controls, `?`, `#`, and `&`. | Preserve exactly after validation. |
| `pn` | Payee name | Required, 1–100 Unicode characters after trimming; reject controls. | Preserve exactly after trimming. |
| `mc` | Merchant category code | Optional; if present, exactly four ASCII digits. | Preserve exactly. Never invent one. |
| `tn` | Transaction note | Optional, at most 80 Unicode characters; reject controls. | Preserve exactly. Do not append split metadata. |
| `cu` | Currency | Optional on input; if present, only `INR`. | Always emit `INR`. |

Unknown parameters MUST be retained in the parsed diagnostic model, ignored when deciding split eligibility, and omitted from every child-payment URI. This compatibility rule allows provider-specific metadata such as `aid` without claiming to understand or preserve its semantics. The known binding parameters listed above remain ineligible and MUST NOT be ignored.

Manual entry MUST collect a VPA, payee name, and amount. It uses the same `pa`, `pn`, amount, and currency validation. Manual entry MUST NOT invent an MCC.

## 4. Amount rules

- Currency is INR only.
- Amounts MUST be represented as integer paise internally.
- Each child amount MUST be at least 1 paise.
- The total MUST be positive, fit in a signed 64-bit paise value, and equal the exact sum of all child amounts.
- Serialization MUST use plain decimal rupees with exactly two fractional digits, ASCII digits, and `.` as the decimal separator: `33.34`.
- Scientific notation, grouping separators, signs, and more than two fractional digits are forbidden.

## 5. Child payment URI

Each payment attempt MUST create a fresh URI with `android.net.Uri.Builder`; string concatenation is forbidden.

The emitted allowlist is:

| Key | Requirement |
|---|---|
| `pa` | Required; copied from the eligible QR or confirmed manual input. |
| `pn` | Required; copied from the eligible QR or confirmed manual input. |
| `am` | Required; this child's exact amount. |
| `cu` | Required; `INR`. |
| `tr` | Required; fresh for every attempt. |
| `mc` | Optional; copied only when present in the eligible QR. |
| `tn` | Optional; copied only when present in the eligible QR. |

No other field may be emitted in version 0.1. In particular, `tid` is deprecated and MUST NOT be set.

`tr` MUST be an ASCII-numeric identifier of at most 35 digits, unique across all attempts made by this installation. It MUST be persisted before launching the UPI app. A retry MUST receive a new `tr`; the local attempt record MUST retain the earlier reference.

The PRD's illustrative values such as `split_8F32A1_001` are local IDs only and MUST NOT be sent as `tr`.

Example:

```text
upi://pay?pa=merchant%40bank&pn=ABC%20Restaurant&mc=5812&tr=20260918000123456789&tn=Table%2012&am=33.34&cu=INR
```

Before handoff, the confirmation screen MUST show the payee name, VPA, exact child amount, split position, and selected UPI app.

## 6. Android discovery and launch

The baseline interoperability path is a generic `Intent.ACTION_VIEW` for the generated `upi://pay` URI. The app MUST offer that generic path even if it also shows named UPI apps.

For Android 11 and later, the manifest SHOULD declare an intent-signature `<queries>` entry for `ACTION_VIEW` with scheme `upi` and host `pay`, so compatible apps can be discovered without maintaining a fragile package allowlist.

A named-app launch MAY call `setPackage()` only with a package returned by current intent resolution. A hardcoded package name alone is not evidence that the app is installed or compatible. If a named launch fails, the user MUST be allowed to return to the generic chooser.

Launch with the Activity Result API. `Activity.resultCode` is a transport signal only and MUST NOT be interpreted as payment success or failure.

## 7. Callback envelope and parsing

UPI apps are inconsistent about callback envelopes. Version 0.1 accepts a query-string-shaped response from either:

1. the returned intent string extra named `response`; or
2. the returned intent's data string, when it contains a parseable query component.

The response is capped at 8 KiB. Parsing MUST:

- split fields on `&` and each field on its first `=` only;
- percent-decode exactly once;
- compare field names and status values case-insensitively using an ASCII locale;
- reject malformed escapes, control characters, and duplicate security-critical fields;
- preserve the raw response locally with production logging disabled.

Recognized fields are `Status`, `txnId`, `txnRef`, `responseCode`, and `ApprovalRefNo`. Unknown fields are retained but do not affect status.

Only the explicit `Status` value determines the client-side outcome:

| Parsed condition | Local outcome |
|---|---|
| `Status=SUCCESS`, `txnId` is nonblank, and `txnRef` exactly matches the launched attempt | `SUCCESS` (provisional) |
| `Status=FAILURE` and `txnRef` is absent or matches | `FAILURE` |
| `Status=SUBMITTED` and `txnRef` is absent or matches | `SUBMITTED` |
| Missing/blank response, unknown status, malformed payload, duplicate critical field, mismatched `txnRef`, or incomplete success | `UNKNOWN` |

`responseCode`, `ApprovalRefNo`, Android result code, app resume, and the absence of an exception MUST NOT independently imply success. `txnRef` comparison MUST use the exact persisted reference, not a case-folded value.

Provider-specific JSON such as Google Pay's `tezResponse` is outside this generic Android profile. It may be added later as a separately versioned, signature-verified adapter; it MUST NOT be loosely flattened into the generic parser.

## 8. State and duplicate-payment policy

Before launch, persist the attempt as `LAUNCHING`; immediately after a successful handoff, persist `AWAITING_RESULT`.

`SUCCESS` in this MVP means only **“the selected UPI app reported success.”** It is not bank-confirmed settlement. Production fulfillment or merchant credit claims require server-side verification through the acquiring PSP, bank, or payment aggregator, including expected VPA, `tr`, and amount.

`SUBMITTED` and `UNKNOWN` MUST block automatic retry and progression. The user must verify the debit/credit externally before creating another attempt. The next split MUST always require an explicit tap.

## 9. Provider compatibility claims

Google Pay, PhonePe, Paytm, and BHIM are test targets, not guaranteed integrations. A provider may be marked “tested” only when the test record includes:

- app package and version;
- Android version and device/OEM;
- generic versus package-targeted launch;
- exact request-key set;
- callback envelope location and redacted shape;
- outcomes observed for success, explicit failure, cancellation, and no response.

Do not publish “works with” claims from package discovery alone.

## 10. Required conformance tests

At minimum, automated tests MUST cover:

- eligible static QR with `pa`, `pn`, `mc`, `tn`, and `cu`;
- rejection of `am`, `mam`, `tr`, `tid`, `url`, and `sign`;
- retention of unknown keys in diagnostics and proof that they are omitted from child-payment URIs;
- duplicate critical keys and double-encoded input;
- Unicode payee name, encoded VPA, malformed percent escapes, and control characters;
- exact paise serialization and split-sum invariants;
- numeric, unique, at-most-35-digit `tr` generation and retry rotation;
- URI round-trip parsing without value corruption;
- callback keys in arbitrary case and order;
- success with matching reference, success with missing ID, and success with mismatched reference;
- failure, submitted, cancellation/blank response, duplicate status, and malformed response;
- proof that `resultCode`, `responseCode`, or app resume cannot produce success;
- proof that `SUBMITTED` and `UNKNOWN` cannot auto-retry or auto-advance.

Use these reference fixtures:

```text
# Eligible input QR
upi://pay?pa=merchant%40bank&pn=ABC%20Restaurant&mc=5812&tn=Table%2012&cu=INR

# Eligible input QR with ignored provider metadata
upi://pay?pa=merchant%40bank&pn=ABC%20Restaurant&aid=opaque-provider-value

# Ineligible: amount-bound
upi://pay?pa=merchant%40bank&pn=ABC%20Restaurant&am=100.00&cu=INR

# Provisional success
Status=SUCCESS&txnId=AXI1234567890&txnRef=20260918000123456789&responseCode=00

# Ambiguous despite status text: wrong reference
Status=SUCCESS&txnId=AXI1234567890&txnRef=999&responseCode=00
```

## 11. Sources and maintenance

This profile is based on:

- [NPCI merchant QR / deep-linking compliance circular](https://www.npci.org.in/PDF/npci/upi/circular/2017/Circular18_BankCompliances_to_enbaleUPIMerchantecosystem_0.pdf)
- [Google Pay for India: Android in-app UPI payments](https://developers.google.com/pay/india/api/android/in-app-payments)
- [Google Pay for India: Android prerequisites](https://developers.google.com/pay/india/api/android/overview)
- [Android package visibility declarations](https://developer.android.com/training/package-visibility/declaring)
- [Android Activity Result contract](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.StartActivityForResult)

Re-check the current NPCI specification and provider documentation before production release. Any expansion of accepted input keys, emitted keys, callback formats, or currencies requires a new profile version and conformance tests.
