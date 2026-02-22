#!/usr/bin/env bash
set -euo pipefail

# ── Neviim Dev Build Script ────────────────────────────────────────
# Builds a debug APK and uploads it to the persistent "dev" pre-release
# on GitHub (creating it if it doesn't exist, updating if it does).
# The APK is always available at a stable URL for quick device testing.
# Usage: ./scripts/dev-build.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

DEV_TAG="dev"
APK_SRC="app/build/outputs/apk/debug/app-debug.apk"
APK_OUT="neviim-dev.apk"

# ── Build ──────────────────────────────────────────────────────────
echo "🔨 Building debug APK..."
export JAVA_HOME="${JAVA_HOME:-$HOME/tools/jdk17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/tools/android-sdk}"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew assembleDebug --no-daemon -q

cp "$APK_SRC" "$APK_OUT"
echo "✅ APK built: $APK_OUT"

# ── Upload to the persistent "dev" pre-release ─────────────────────
COMMIT_SHA=$(git rev-parse --short HEAD)
COMMIT_MSG=$(git log -1 --pretty=format:"%s")
TIMESTAMP=$(date -u "+%Y-%m-%d %H:%M UTC")

DEV_NOTES="**⚠️ Dev build — not a stable release**

Latest commit: \`${COMMIT_SHA}\` — ${COMMIT_MSG}
Built: ${TIMESTAMP}

Install this APK to test the latest changes on your phone."

if gh release view "$DEV_TAG" &>/dev/null; then
    # Update existing dev release
    echo "🔁 Updating existing dev pre-release..."
    # Delete old APK asset if present
    gh release delete-asset "$DEV_TAG" "$APK_OUT" --yes 2>/dev/null || true
    # Re-upload
    gh release upload "$DEV_TAG" "$APK_OUT" --clobber
    # Refresh the release notes
    gh release edit "$DEV_TAG" \
        --title "Dev Build (${COMMIT_SHA})" \
        --notes "$DEV_NOTES" \
        --prerelease
else
    # Create fresh dev pre-release
    echo "🚀 Creating dev pre-release..."
    gh release create "$DEV_TAG" \
        "$APK_OUT" \
        --title "Dev Build (${COMMIT_SHA})" \
        --notes "$DEV_NOTES" \
        --prerelease \
        --target main
fi

echo ""
echo "✅ Dev APK uploaded!"
echo "   https://github.com/zivpeltz/Neviim/releases/tag/dev"
