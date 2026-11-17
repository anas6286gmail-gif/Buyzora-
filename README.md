# Buyzora

Modern multi-category Android shopping application built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Google Sign-In (Credential Manager)**, and **Cloud Firestore**.

## Features

- **Multi-Category Catalog**: Clothes, Shoes, Fashion Accessories, Electronics, Smartphones, Gadgets, Face Washes, Skincare, Haircare, and Everyday Essentials.
- **Discovery & Details**: Live search, category filtering, sorting (Featured, Price, Top Rated, Best Deals), variant selection, discounts, and ratings.
- **Cart & Wishlist**: Real-time persistent Wishlist and Shopping Bag with quantity controls and promo codes (`BUYZORA15`, `WELCOME10`).
- **Secure Checkout**: Google Sign-In authentication and checkout support for UPI, Debit/Credit Cards (tokenized display only), Net Banking, and Cash on Delivery (COD).
- **Order Tracking & Returns**: 6-step order tracking timeline, one-tap cancellations, 14-day return & refund requests, and customer support tickets.
- **Store Admin Console**: Manage products, pricing, stock, order fulfillment/refunds, customer accounts, and support inquiries.

## Project Structure

- `app/src/main/java/com/example/` — Kotlin source files (`MainActivity.kt`, `model/`, `data/`, `viewmodel/`, `ui/`)
- `app/src/main/res/` — App icons, studio drawables, bundled fonts (`Outfit`, `Plus Jakarta Sans`), and XML resources
- `app/src/main/AndroidManifest.xml` — App manifest and permissions
- `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` — Gradle configuration
- `firestore.rules`, `firestore.test.js`, `firebase.json`, `firebase-blueprint.json` — Firestore schema, hardened security rules, and emulator test suite

## Building Locally

1. Open the project folder in **Android Studio** (AGP 9.1+, JDK 11+).
2. Ensure `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml` are present for your Firebase project.
3. Sync Gradle and run the `:app` configuration on an emulator or physical device.
