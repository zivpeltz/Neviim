<div align="center">

# 🕎 Neviim — נביאים

**A play-money prediction market for prophets-in-training**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](#license)

*Bet ShekelPoints on the future. No real money. Just prophecy.*

</div>

---

## ✨ What is Neviim?

Neviim (Hebrew: **נביאים** — *Prophets*) is a prediction market app where users wager virtual **ShekelPoints (SP)** on outcome events. It supports both classic **Yes/No** binary markets and **multi-choice** markets (Polymarket-style) with multiple possible outcomes. Prices are driven by an **Automated Market Maker (AMM)** — you don't set the odds, the market does.

Think [Polymarket](https://polymarket.com), but with falafel money.

---

## 📱 Screenshots

> *Coming soon 😅*

---

## 🏗️ Architecture

```
app/src/main/java/com/neviim/market/
├── data/
│   ├── amm/          # AMM pricing engine (pool-ratio + inverse-pool)
│   ├── model/        # Event, EventOption, UserPosition, UserProfile
│   └── repository/   # In-memory reactive repository (StateFlow)
├── ui/
│   ├── components/   # ProbabilityBar, PriceLineChart, StatCard
│   ├── navigation/   # Bottom nav + Jetpack Navigation routes
│   ├── screen/       # Explore, EventDetail, CreateEvent, Portfolio, Account
│   ├── theme/        # Material 3 dark/light color schemes
│   └── viewmodel/    # MVVM ViewModels per screen
├── MainActivity.kt
└── NeviimApp.kt
```

| Layer | Tech |
|-------|------|
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM |
| **Navigation** | Jetpack Navigation (Bottom Nav) |
| **State** | Kotlin StateFlow / Coroutines |
| **Data** | In-memory repository (Room-ready) |
| **Min SDK** | 26 (Android 8.0) |

---

## 📊 AMM Pricing Logic

### Binary Events (Yes / No)

Prices are calculated using a **pool-ratio formula**:

```
Price(Yes) = NoPool / (YesPool + NoPool)
Price(No)  = YesPool / (YesPool + NoPool)
```

### Multi-Choice Events

For events with multiple outcomes, probability is calculated via **inverse-pool weighting**:

```
P(option_i) = (1 / pool_i) / Σ(1 / pool_j)
```

When a user buys shares of any option:
1. Their SP is added to that option's pool
2. The option's price automatically increases (more demand → higher price)
3. Shares received = `amount / priceAtExecution`
4. Each share pays **1 SP** if the event resolves to that option

---

## 🌍 Localization (RTL Support)

Neviim fully supports **English (LTR)** and **Hebrew (RTL)**:

- `res/values/strings.xml` — English
- `res/values-he/strings.xml` — Hebrew (עברית)
- `android:supportsRtl="true"` in manifest
- Compose layouts auto-mirror with system locale

To test Hebrew: **Settings → System → Languages → Add Hebrew → drag to top**

---

## 🎯 Features

### Core
- **Explore** — Scrollable feed of active events with search & tag filters
- **Event Detail** — Rich info (description, end date, traders, liquidity, per-option pool breakdown), probability chart, and trade panel
- **My Bids** — Active & resolved positions with entry price, current price, and P&L
- **My Account** — Balance, win rate, total bets, and a +1,000 SP refill button

### Event Types
- **Binary (Yes/No)** — Classic prediction markets with probability split bar
- **Multi-Choice** — Multiple mutually-exclusive outcomes (e.g. "Who will be the next PM?"), each with separate AMM pools and probability tracking

### Event Creation
- Create both **binary** and **multi-choice** events
- Set **end dates** via Material 3 date picker
- Add **descriptions** (resolution criteria)
- Manage **custom options** (add up to 8, each with English + Hebrew labels)
- Choose **category tags** and set initial probability (binary) or equal-weight pools (multi-choice)

### Event Detail Data
- 📊 **Volume** — Total SP traded on the event
- 💧 **Liquidity** — Total SP across all option pools
- 👥 **Traders** — Number of unique trades
- ⏰ **End Date** — Days remaining countdown
- 📈 **Pool Breakdown** — SP amount and probability per option, with selectable rows for trading

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

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`

---

## 🎲 Mock Data

The app comes pre-loaded with **8 events** (5 binary + 3 multi-choice):

### Binary Events

| Event | Tag | Starting Yes % |
|-------|-----|:--------------:|
| Will it rain in Tel Aviv tomorrow? | 🔬 Science | 60% |
| Bitcoin > $100k by end of 2026? | 💰 Crypto | 65% |
| Will elections be held before 2027? | 🏛️ Politics | 50% |
| Will Netta win Eurovision 2026? | 🎭 Pop Culture | 80% |
| Israel wins gold at 2028 Olympics? | ⚽ Sports | 85% |

### Multi-Choice Events

| Event | Tag | Options |
|-------|-----|:-------:|
| Who will be the next Prime Minister? | 🏛️ Politics | 5 candidates |
| Ethereum price range end of 2026? | 💰 Crypto | 4 ranges |
| Eurovision 2026 winner country? | 🎭 Pop Culture | 5 countries |

Starting balance: **5,000 SP**

---

## 📂 Key Files

| File | Purpose |
|------|---------|
| [`Models.kt`](app/src/main/java/com/neviim/market/data/model/Models.kt) | Event, EventOption, EventType, UserPosition |
| [`AmmEngine.kt`](app/src/main/java/com/neviim/market/data/amm/AmmEngine.kt) | Market math — binary & multi-option pricing and trade execution |
| [`MarketRepository.kt`](app/src/main/java/com/neviim/market/data/repository/MarketRepository.kt) | Single source of truth, seed data, event creation |
| [`ExploreScreen.kt`](app/src/main/java/com/neviim/market/ui/screen/ExploreScreen.kt) | Home feed with search + filters + multi-choice previews |
| [`EventDetailScreen.kt`](app/src/main/java/com/neviim/market/ui/screen/EventDetailScreen.kt) | Trading UI with rich event info, pool breakdown, chart |
| [`CreateEventScreen.kt`](app/src/main/java/com/neviim/market/ui/screen/CreateEventScreen.kt) | Event creation form (binary + multi-choice) |
| [`NavGraph.kt`](app/src/main/java/com/neviim/market/ui/navigation/NavGraph.kt) | Navigation routes + bottom bar |

---

## 🛣️ Roadmap

- [x] ~~Custom event creation~~
- [x] Multi-choice prediction markets
- [ ] Persistent storage with Room DB
- [ ] Event resolution + automatic payout
- [ ] User authentication
- [ ] Real-time price updates via WebSocket
- [ ] Social feed & comments on events
- [ ] Dark/light theme toggle

---

## 📄 License

This project is for educational and prototyping purposes.

---

<div align="center">

*Built with ☕ and mass amounts of chutzpah*

**שקלפוינטס לא שווים כלום, אבל הנבואה — אין לה מחיר** 🕎

</div>
