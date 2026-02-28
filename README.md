<div align="center">

# 🕎 Neviim — נביאים

**A play-money Polymarket simulator for prophets-in-training**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](#license)

*Bet ShekelPoints on the future. No real money. Just prophecy.*

</div>

---

## ✨ What is Neviim?

Neviim (Hebrew: **נביאים** — *Prophets*) is a prediction market app where users wager virtual **ShekelPoints (SP)** on outcome events.

We've evolved into a **play-money Polymarket Simulator**. The app pulls real, live data (events, choices, probabilities, and resolution outcomes) directly from the public Polymarket API.

A background synchronization worker periodically checks your active bets against the live market state. If an event resolves and you predicted correctly, the app automatically rewards you with your simulated winnings!

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
- **Explore** — Scrollable feed of active live markets fetched from Polymarket. Deep-links directly to markets.
- **Dynamic Tagging** — Uses live tags (trending, new, domains) straight from the blockchain.
- **Event Detail** — Rich UI with accurate outcome probabilities, "Ends At" countdowns, full event rules, and volume indicators. Beautiful multi-choice dropdowns and intuitive binary trade panels.
- **My Bids (Portfolio)** — View real-time value changes, un-realized percent gains/losses, and completely split history between Active and Resolved trades.
- **My Account** — Profile dashboard exhibiting your aggregate win rate, gross winnings, total trades, and a quick "Ask for a Miracle" +1,000 SP refill button.

### Simulated Trading
- Zero financial risk play-money environment. Trade freely utilizing *ShekelPoints (SP)*.
- Price histories mimic Polymarket probabilies, storing trade states independently on your device.
- Robust in-app background event resolution logic securely queries Polymarket gamma APIs and settles your open trades instantly as events resolve in the real world.
- Supports both simple binary ("Will X happen by Y?") and complex multi-choice ("Who will win the election?") events.

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
