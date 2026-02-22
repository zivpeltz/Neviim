# Neviim v0.1.4 - Major UI Overhaul & Feature Parity

This release brings massive improvements to the Polymarket simulation experience, radically upgrading the UI to near-parity with Polymarket while adding robust background systems for event resolution and data handling.

## ✨ New Features & UI Overhaul
- **Polymarket UI Parity:** The app now closely resembles the real Polymarket aesthetic, with a dynamic, premium UI. Live probability bars, detailed odds displays for binary events, and optimized multi-choice presentation using clean dropdowns.
- **Dynamic Event Tags:** Replaced hardcoded topic categories with live, dynamic tags fed directly from the Polymarket API. The topic filtering chip-row automatically updates to reflect whatever tags exist in the current event list.
- **Sort & Filter System:** Easily browse markets with the new sort chips:
  - 🔥 **Trending** (Highest 24h volume)
  - ⏰ **Ending Soon**
  - 🆕 **Newest**
  - 🎯 **Close Call** (Probabilities closest to 50%)
  - 📈 **High Volume**
- **Price History Charts:** Event detail pages now always feature a 7-day price history line graph drawn natively in Compose, with intelligent fallbacks (spinners while loading, empty state indicators).
- **Auto-Refresh:** Market data automatically refreshes every 30 seconds when the app is active, keeping odds and probabilities perfectly synced with Polymarket.

## ✡️ Theming
- **Jewish/Israeli Theme Fixed:** Integrated a beautiful Israeli-flag inspired theme mode. Fixed rendering bugs causing layout pushes by introducing the deeply layered `JewishThemedBackground` composable.
- Colors accurately match **PMS 286 C** (Israeli flag blue). Crisp, clean pure-white background for light mode, stunning deep "night sky" navy (`#000D2E`) for dark mode.

## ⚙️ Event Resolution & Bug Fixes
- **Robust Event Resolution:** Completely rebuilt how user positions are settled.
  - **In-process resolver:** Resolves winning/losing positions seamlessly after every 30-second data refresh.
  - **Correct Market Targeting:** Found and fixed a critical bug where multi-choice outcomes were wrongly using the parent Event ID for resolution checks. `UserPosition` now correctly persists the `marketId` needed to query Polymarket.
  - **Optimized Background Worker:** The 2-hour backend resolution worker now cleanly bypasses redundant API initializations to strictly resolve bets mapped to Polymarket’s CLOB identifiers.

Enjoy exploring the markets in Neviim!
