# SplitUPI Android Payment Launcher Specification

Version: 0.1 (draft)  
Date: 2026-09-18  
Platform: Android  
Protocol: UPI intent (`upi://pay`)

## 1. Purpose

This document defines how SplitUPI discovers UPI-capable apps, launches a payment in a selected app, receives the client response, and handles launch failures.

It supplements the [SplitUPI UPI Compatibility Profile](./upi-compatibility-spec.md), which remains authoritative for accepted QR fields, generated payment fields, amount formatting, transaction references, and callback validation.

The words **MUST**, **MUST NOT**, **SHOULD**, and **MAY** are normative.

## 2. Integration model

Google Pay, PhonePe, Paytm, and BHIM do not require four different payment URIs for this flow. SplitUPI MUST build one standards-based `upi://pay` URI. A named-app launch is the same Android `ACTION_VIEW` intent with `Intent.setPackage()` applied.

```text
standard request URI
        |
Intent(ACTION_VIEW, uri)
        |
        +-- no package --------> Android UPI app chooser
        |
        +-- setPackage(GPay) --> Google Pay
        +-- setPackage(PhonePe) -> PhonePe
        +-- setPackage(Paytm) --> Paytm
        +-- setPackage(BHIM) ---> BHIM
```

Provider payment-gateway SDKs are a different integration. They require merchant onboarding, credentials, server-created orders or tokens, and server-side status verification. SplitUPI has none of that in the MVP and MUST NOT mix proprietary gateway SDK calls into this launcher.

## 3. Provider registry

| Display name | Android package | Direct-launch mechanism | Evidence |
|---|---|---|---|
| Google Pay | `com.google.android.apps.nbu.paisa.user` | Standard UPI intent plus `setPackage()` | Google documents the exact package and launch code. |
| PhonePe | `com.phonepe.app` | Standard UPI intent plus `setPackage()` | Paytm's official Smart Intent example lists the package and uses the same package-targeted launch for all listed UPI apps. No separate PhonePe consumer-app URI is required by this profile. |
| Paytm | `net.one97.paytm` | Standard UPI intent plus `setPackage()` | Paytm's official Smart Intent example lists the package and exact launch pattern. |
| BHIM | `in.org.npci.upiapp` | Standard UPI intent plus `setPackage()` | Paytm's official Smart Intent example lists the package. NPCI's public BHIM UPI guidance requires a prominent generic UPI-intent option but does not publish a separate BHIM-only URI contract. |
| Any UPI app | none | Standard UPI intent through an Android chooser | Required fallback. Google and NPCI both state that the generic UPI-intent path must remain available. |

Package names are configuration, not proof of readiness. A package MUST NOT be shown merely because it is in this table.

## 4. Request URI

The launcher receives a fully validated request from the compatibility layer. It MUST NOT reconstruct payment data from screen text.

The MVP request shape is:

```text
upi://pay?pa=<VPA>&pn=<NAME>&tr=<UNIQUE_REFERENCE>&am=<RUPEES>&cu=INR[&mc=<MCC>][&tn=<NOTE>]
```

Rules:

- Build the URI with `android.net.Uri.Builder`; do not concatenate or pre-encode query strings.
- `pa`, `pn`, `tr`, `am`, and `cu` are required by this app profile.
- `mc` and `tn` are copied only when validated input supplied them.
- `am` uses exactly two decimal places and no grouping separator.
- `tr` is fresh for every launch attempt, numeric, at most 35 digits, and persisted before handoff.
- `cu` is always `INR`.
- Unknown scanned-QR parameters are not forwarded.

Google's example also includes a merchant transaction URL. SplitUPI MUST NOT invent a `url`; the compatibility profile deliberately omits it because the MVP has no acquiring PSP or transaction-status endpoint.

## 5. Android manifest visibility

Android 11 and later restrict package visibility. The manifest MUST declare the generic UPI intent signature. Named package declarations MAY also be retained for explicit provider checks.

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data
            android:scheme="upi"
            android:host="pay" />
    </intent>

    <package android:name="com.google.android.apps.nbu.paisa.user" />
    <package android:name="com.phonepe.app" />
    <package android:name="net.one97.paytm" />
    <package android:name="in.org.npci.upiapp" />
</queries>
```

The current SplitUPI manifest already contains these declarations.

## 6. Discovery and selection

The app-selection screen MUST be based on current intent resolution, not on the hardcoded provider registry alone.

1. Create a probe: `Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))`.
2. Call `PackageManager.queryIntentActivities()` for that unscoped probe.
3. Collect the resolved activity package names.
4. Intersect those names with the provider registry.
5. Display only named apps that are installed **and** currently advertise that they can handle a UPI payment intent.
6. Always display **Pay with any UPI app**, which launches an unscoped chooser.

This is the Smart Intent readiness check documented by Paytm. An installed app can still be absent from the result when its UPI setup is incomplete or its relevant component is disabled. Labeling such an app as usable is wrong.

Discovery is a snapshot. The launcher MUST resolve the final intent again immediately before launch.

## 7. Launch algorithm

### 7.1 Common intent builder

Build the unscoped intent first:

```kotlin
fun buildUpiIntent(uri: Uri): Intent =
    Intent(Intent.ACTION_VIEW, uri)
```

Do not put provider names, package names, or provider-specific parameters into this builder.

### 7.2 Named provider

For Google Pay, PhonePe, Paytm, or BHIM:

```kotlin
val launchIntent = buildUpiIntent(paymentUri).apply {
    setPackage(provider.packageName)
}

if (launchIntent.resolveActivity(packageManager) == null) {
    // Do not launch. Return to app selection and offer the generic chooser.
} else {
    launcher.launch(launchIntent)
}
```

The exact package values are in section 3. There is no provider-specific URI rewrite.

### 7.3 Generic fallback

```kotlin
val baseIntent = buildUpiIntent(paymentUri)

if (baseIntent.resolveActivity(packageManager) == null) {
    // No UPI-ready handler exists.
} else {
    launcher.launch(Intent.createChooser(baseIntent, "Pay with..."))
}
```

The chooser MUST NOT be package-scoped. It is the interoperability path required by the public Google and NPCI guidance.

### 7.4 Activity Result API

Use `ActivityResultContracts.StartActivityForResult`. `startActivityForResult()` appears in older provider samples but is deprecated Android API; it does not change the wire protocol.

Before launch:

1. Generate a new `tr`.
2. Persist the attempt and expected amount as `LAUNCHING`.
3. Build and resolve the final intent.
4. Launch it.
5. Mark the attempt `AWAITING_RESULT` after successful handoff.

Catch `ActivityNotFoundException` and `SecurityException`. A launch exception MUST leave the split unpaid and MUST offer a return to app selection.

## 8. Provider-specific requirements

### 8.1 Google Pay

- Package: `com.google.android.apps.nbu.paisa.user`.
- Use the common request URI and apply `setPackage()`.
- Google requires verified merchant UPI acceptance details for a production merchant integration.
- If Google Pay returns `Submitted` or `Succeeded`, Google explicitly requires the merchant to verify the amount and status with its PSP or payment aggregator.
- Google also requires support for the generic UPI intent; a Google-Pay-only checkout is non-conformant.

### 8.2 PhonePe

- Package: `com.phonepe.app`.
- Use the common request URI and apply `setPackage()`.
- The package-targeted pattern is documented in Paytm's official cross-app Smart Intent example.
- Do not substitute PhonePe Payment Gateway SDK instructions. That flow is for onboarded merchants and is not an app-to-app generic UPI intent contract.
- Because no public PhonePe consumer-app document found during this review publishes a stronger callback guarantee, SplitUPI MUST treat its returned client status as provisional.

### 8.3 Paytm

- Package: `net.one97.paytm`.
- Use the common request URI and apply `setPackage()`.
- Paytm's Smart Intent documentation requires both installation and UPI-readiness checks before showing the named app.
- The long Paytm QR URI in Paytm's example contains Paytm merchant fields such as `mode`, `orgid`, `paytmqr`, and `sign`. Those fields belong to Paytm-issued merchant payloads. SplitUPI MUST NOT fabricate or copy them into generated child payments.
- Do not substitute Paytm Payment Gateway order/token APIs. They are a separate server-backed integration.

### 8.4 BHIM

- Package: `in.org.npci.upiapp`.
- Use the common request URI and apply `setPackage()`.
- Keep the generic chooser visible. NPCI's current public BHIM UPI guidelines say that **Pay by any UPI app** must be prominent and must show the customer's registered UPI apps.
- The public BHIM guidance does not define a special BHIM deep-link scheme. Do not invent one.

## 9. Callback handling

An Android activity result only means control returned to SplitUPI. `resultCode == RESULT_OK`, app resume, or absence of an exception MUST NOT be interpreted as payment success.

Provider callback envelopes are inconsistent. The compatibility layer SHOULD inspect, in order:

1. a string extra named `response` or `Response` containing `key=value&...`;
2. a returned data URI/query string;
3. flat extras for `Status`, `txnId`, `txnRef`, `responseCode`, and `ApprovalRefNo`.

Field names and status values are case-insensitive; transaction-reference values are not. Duplicate critical fields, malformed encoding, mismatched `txnRef`, blank responses, cancellations, and unknown statuses produce `UNKNOWN`.

The client may display `SUCCESS`, `FAILURE`, `SUBMITTED`, or `UNKNOWN`, but only server-side verification through the acquiring PSP, bank, or payment aggregator can establish settlement. SplitUPI has no backend, so it MUST describe client success as **reported by the payment app**, not **money received**.

`SUBMITTED` and `UNKNOWN` MUST block automatic progression and automatic retry. Blind retry can double-pay the merchant.

## 10. Failure behavior

| Condition | Required behavior |
|---|---|
| No generic UPI handler | Show that no UPI-ready app is available; remain in SplitUPI. |
| Named app absent from discovery | Hide it from the named provider list. |
| Named app fails final resolution | Return to selection and offer the generic chooser. |
| Launch throws | Keep the split unpaid; show a retry/change-app action. |
| User cancels or returns no payload | Record `UNKNOWN`, not failure or success. |
| Returned reference mismatches | Record `UNKNOWN`; do not advance. |
| Payment app reports success | Mark only provisional client success; never claim verified settlement. |

SplitUPI MUST NOT automatically launch another app after a failure. That would create a duplicate-payment hazard.

## 11. Current implementation audit

Status as of 2026-09-18:

| Requirement | Current state |
|---|---|
| Standards-based `upi://pay` URI | Implemented. |
| Package-targeted launch for all four named apps | Implemented. |
| Android package visibility declarations | Implemented. |
| Final `resolveActivity()` guard | Implemented. |
| Activity Result API | Implemented. |
| Show only installed and UPI-ready named apps | **Missing.** All four hardcoded providers are currently displayed. |
| Generic **Pay with any UPI app** chooser | **Missing.** |
| Persist attempt before launch | **Missing.** The expected reference is only held in Compose saved state. |
| Parse flat callback extras | **Missing.** Only response-string and data-string envelopes are parsed. |
| Server-side settlement verification | Intentionally unavailable in the backend-free MVP. Production payment claims are therefore unsafe. |

The selection-screen text currently says readiness is checked before launch, but the screen does not perform discovery. That statement is false until the missing readiness filter is implemented.

## 12. Acceptance criteria

A launcher implementation conforms to this specification only when:

- named providers come from live UPI intent resolution;
- the generic chooser is always offered when at least one handler exists;
- all providers receive the same validated URI;
- named launch differs only by `setPackage()`;
- each attempt gets a fresh, persisted transaction reference;
- launch and callback failures never advance the split;
- app-returned success is labeled provisional;
- no automatic retry or automatic next-payment launch occurs.

## 13. Official sources

- [NPCI circular 18: merchant QR and Android intent compliance](https://www.npci.org.in/PDF/npci/upi/circular/2017/Circular18_BankCompliances_to_enbaleUPIMerchantecosystem_0.pdf)
- [NPCI BHIM UPI Guidelines, June 2026](https://www.npci.org.in/uploads/BHIM_UPI_Guidelines_2026_012a0b1bce.pdf)
- [Google Pay for India: Android in-app payments](https://developers.google.com/pay/india/api/android/in-app-payments)
- [Google Pay for India: overview and Android 11 package visibility](https://developers.google.com/pay/india/api/android/overview)
- [Paytm Payments: UPI Smart Intent](https://www.paytmpayments.com/docs/upi-smart-intent/)
- [Android package visibility declarations](https://developer.android.com/training/package-visibility/declaring)
- [Android Activity Result APIs](https://developer.android.com/training/basics/intents/result)

The public provider pages are not equivalent in authority. Google documents its own exact launch. Paytm documents the cross-app package-targeted pattern and all four package names. NPCI documents the generic chooser obligation. No public provider source found in this review establishes that an app-returned callback alone proves settlement.
