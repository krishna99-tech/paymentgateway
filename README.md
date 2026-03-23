# Payment Gateway App

A modern Android application demonstrating a payment gateway integration supporting various UPI methods, QR code scanning, and VPA validation.

## Features

- **Multiple Payment Methods**: Supports Google Pay, PhonePe, Paytm, and direct UPI/VPA entry.
- **VPA Validation**: Integrated VPA (UPI ID) validation before proceeding with payments.
- **UPI Intent Integration**: Seamlessly switch to installed UPI apps for transaction completion.
- **QR Code Support**: Display and handle QR code based payments.
- **Order History**: Keep track of all previous transactions.
- **Real-time Status Polling**: Automatically checks for payment completion status (PENDING, SUCCESS, FAILED).
- **Modern UI**: Built with Jetpack Compose for a smooth and responsive user experience.
- **Splash Screen**: Professional entry using the Android 12+ Splash Screen API.

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) with OkHttp & GSON
- **Architecture**: MVVM (Model-View-ViewModel)
- **Concurrency**: Kotlin Coroutines & Flow
- **Dependency Management**: Gradle (KTS) with Version Catalog
- **QR Scanning**: [ZXing Android Embedded](https://github.com/journeyapps/zxing-android-embedded)

## Getting Started

### Prerequisites

- Android Studio Koala or newer.
- Android SDK 24 (Android 7.0) or higher.

### Installation

1. Clone the repository.
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Replace placeholder API endpoints in `PaymentApiService.kt` with your backend URL.
5. Provide your Merchant ID (MID) where required in the `PaymentViewModel`.
6. Build and run the app on a physical device (recommended for testing UPI app intents).

## Project Structure

- `com.payment.app`: Main entry point and Compose themes.
- `com.payment.gateway.ui`: UI components for the payment flow.
- `com.payment.gateway.viewmodel`: ViewModel managing payment states.
- `com.payment.gateway.repository`: Data layer for API calls and local app checks.
- `com.payment.gateway.network`: Retrofit service definitions.
- `com.payment.gateway.model`: Data models for requests, responses, and UI state.

## License

This project is for demonstration purposes.

---
Created with ❤️ for Android Developers.
