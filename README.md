<div align="center">

# 🕎 Polymarket Simulator — נביאים

**A play-money prediction market simulator pulling live events from Polymarket**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](#license)

*Bet fake ShekelPoints on real global events. No real money. Just prophecy.*

</div>

---

## ✨ What is this?

This app is a play-money Polymarket Simulator! It pulls real, live data (events, choices, probabilities, and resolution outcomes) directly from the public Polmarket Gamma API. Users are given a fake balance of **ShekelPoints (SP)**, which they can use to place hypothetical bets on these real events. 

A background synchronization worker periodically checks the active bets against the live market state. If an event resolves and your outcome won, the app automatically rewards you with your fake winnings!

Think [Polymarket](https://polymarket.com), but with falafel money and zero financial risk.

*Note: This app was **vibecoded for fun using Antigravity**.* 🚀

---

## 🏗️ Architecture

```
app/src/main/java/com/neviim/market/
├── data/
│   ├── network/      # Retrofit Gamma API Client for Polymarket
│   ├── model/        # Event, EventOption, UserPosition, UserProfile
│   ├── repository/   # StateFlow Repository bridging API + Local Storage
│   ├── storage/      # Local JSON Storage for Fake User Balance & Bets
│   └── updater/      # WorkManager for Background Event Resolution
├── ui/
│   ├── components/   # ProbabilityBar, PriceLineChart, StatCard
│   ├── navigation/   # Bottom nav + Jetpack Navigation routes
│   ├── screen/       # Explore, EventDetail, CreateEvent, Portfolio, Account
│   ├── theme/        # Material 3 dark/light color schemes
│   └── viewmodel/    # MVVM ViewModels per screen
├── MainActivity.kt
└── NeviimApp.kt
```

---

## 🌍 Localization (RTL Support)

The simulator fully supports **English (LTR)** and **Hebrew (RTL)**:

- `res/values/strings.xml` — English
- `res/values-he/strings.xml` — Hebrew (עברית)
- `android:supportsRtl="true"` in manifest
- Compose layouts auto-mirror with system locale

---

## 🎯 Features

### Core
- **Explore** — Scrollable feed of active live markets fetched from Polymarket.
- **Event Detail** — Rich info (description, tags, volume, pools, end date) and trade panel.
- **My Bids** — Active & resolved positions with entry price, shares, and amount paid.
- **My Account** — Balance, win rate, total bets, and a +1,000 SP refill button.

### Simulated Trading
- Prices accurately reflect real-world Polymarket probabilities at the time of the bet.
- Fully local storage ensures your position data stays private.
- WorkManager routinely syncs with the Gamma API to close out resolved bets and credit your account.

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (recommended) or JDK 17 + Android SDK 34
- Android device or emulator (API 26+)

### Build & Run

```bash
# Clone the repo
git clone https://github.com/zivpeltz/Neviim.git
cd "Israel Polymarket"

# Open in Android Studio and press Run
# OR build from command line:
./gradlew assembleDebug

# Install on connected device:
./gradlew installDebug
```

---

## 📄 License

This project is for educational and prototyping purposes. 

---

<div align="center">

*Built with ☕ and mass amounts of chutzpah*

**שקלפוינטס לא שווים כלום, אבל הנבואה — אין לה מחיר** 🕎

</div>
