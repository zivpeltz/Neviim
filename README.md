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

Neviim (Hebrew: **נביאים** — *Prophets*) is a binary prediction market app where users wager virtual **ShekelPoints (SP)** on Yes/No outcome events. Prices are driven by an **Automated Market Maker (AMM)** — you don't set the odds, the market does.

Think [Polymarket](https://polymarket.com), but with falafel money.

---

## 📱 Screenshots

> *Coming soon — enable SVM in your BIOS first 😅*

---

## 🏗️ Architecture

```
app/src/main/java/com/neviim/market/
├── data/
│   ├── amm/          # AMM pricing engine (constant-product formula)
│   ├── model/        # Event, UserPosition, UserProfile
│   └── repository/   # In-memory reactive repository (StateFlow)
├── ui/
│   ├── components/   # ProbabilityBar, PriceLineChart, StatCard
│   ├── navigation/   # Bottom nav + Jetpack Navigation routes
│   ├── screen/       # Explore, EventDetail, Portfolio, Account
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

Prices are calculated using a **pool-ratio formula**:

```
Price(Yes) = NoPool / (YesPool + NoPool)
Price(No)  = YesPool / (YesPool + NoPool)
```

When a user buys **Yes** shares:
1. Their SP is added to the Yes pool
2. Yes price automatically increases (more demand → higher price)
3. Shares received = `amount / priceAtExecution`
4. Each share pays **1 SP** if the outcome resolves Yes

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

- **Explore** — Scrollable feed of active events with search & tag filters
- **Event Detail** — Probability chart, colored split bar, Buy Yes/No trade panel
- **My Bids** — Active & resolved positions with entry price, current price, and P&L
- **My Account** — Balance, win rate, total bets, and a +1,000 SP refill button
- **Error Handling** — Can't bet more than your balance

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (recommended) or JDK 17 + Android SDK 34
- Android device or emulator (API 26+)

### Build & Run

```bash
# Clone the repo
git clone <your-repo-url>
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

The app comes pre-loaded with 5 events:

| Event | Tag | Starting Yes % |
|-------|-----|:--------------:|
| Will it rain in Tel Aviv tomorrow? | 🔬 Science | 60% |
| Bitcoin > $100k by end of 2026? | 💰 Crypto | 65% |
| Will elections be held before 2027? | 🏛️ Politics | 50% |
| Will Netta win Eurovision 2026? | 🎭 Pop Culture | 80% |
| Israel wins gold at 2028 Olympics? | ⚽ Sports | 85% |

Starting balance: **5,000 SP**

---

## 📂 Key Files

| File | Purpose |
|------|---------|
| [`AmmEngine.kt`](app/src/main/java/com/neviim/market/data/amm/AmmEngine.kt) | Market math — pricing, trade execution |
| [`MarketRepository.kt`](app/src/main/java/com/neviim/market/data/repository/MarketRepository.kt) | Single source of truth, mock data seeding |
| [`ExploreScreen.kt`](app/src/main/java/com/neviim/market/ui/screen/ExploreScreen.kt) | Home feed with search + filters |
| [`EventDetailScreen.kt`](app/src/main/java/com/neviim/market/ui/screen/EventDetailScreen.kt) | Trading UI with chart + trade panel |
| [`NavGraph.kt`](app/src/main/java/com/neviim/market/ui/navigation/NavGraph.kt) | Navigation routes + bottom bar |

---

## 🛣️ Roadmap

- [ ] Persistent storage with Room DB
- [ ] Event resolution + automatic payout
- [ ] User authentication
- [ ] Real-time price updates via WebSocket
- [ ] Social feed & comments on events
- [ ] Custom event creation
- [ ] Dark/light theme toggle

---

## 📄 License

This project is for educational and prototyping purposes.

---

<div align="center">

*Built with ☕ and mass amounts of chutzpah*

**שקלפוינטס לא שווים כלום, אבל הנבואה — אין לה מחיר** 🕎

</div>
