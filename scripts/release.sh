#!/usr/bin/env bash
set -euo pipefail

# ── Neviim Release Script ─────────────────────────────────────────
# Creates a GitHub Release with the debug APK attached.
# Usage: ./scripts/release.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

# ── Read version from build.gradle ─────────────────────────────────
VERSION_NAME=$(grep 'versionName' app/build.gradle | head -1 | sed 's/.*"\(.*\)".*/\1/')
VERSION_CODE=$(grep 'versionCode' app/build.gradle | head -1 | sed 's/[^0-9]//g')

if [ -z "$VERSION_NAME" ]; then
    echo "❌ Could not read versionName from app/build.gradle"
    exit 1
fi

TAG="v${VERSION_NAME}"
echo "📦 Releasing Neviim ${TAG} (versionCode ${VERSION_CODE})"

# ── Check if tag already exists ────────────────────────────────────
if gh release view "$TAG" &>/dev/null; then
    echo "❌ Release ${TAG} already exists on GitHub."
    echo "   Bump the version in app/build.gradle first."
    exit 1
fi

# ── Build ──────────────────────────────────────────────────────────
echo "🔨 Building APK..."
export JAVA_HOME="${JAVA_HOME:-$HOME/tools/jdk17}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/tools/android-sdk}"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew assembleDebug --no-daemon -q

APK_SRC="app/build/outputs/apk/debug/app-debug.apk"
APK_OUT="neviim-v${VERSION_NAME}.apk"

cp "$APK_SRC" "$APK_OUT"
echo "✅ APK built: $APK_OUT"

# ── Generate release notes from commits since last tag ─────────────
PREV_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
if [ -n "$PREV_TAG" ]; then
    NOTES=$(git log "${PREV_TAG}..HEAD" --pretty=format:"- %s" --no-merges)
else
    NOTES=$(git log --pretty=format:"- %s" --no-merges -20)
fi

if [ -f "RELEASE_NOTES.md" ]; then
    NOTES=$(<RELEASE_NOTES.md)
fi

if [ -z "$NOTES" ]; then
    NOTES="Release ${TAG}"
fi

# ── Create GitHub Release ──────────────────────────────────────────
echo "🚀 Creating GitHub Release ${TAG}..."
gh release create "$TAG" \
    "$APK_OUT" \
    --title "Neviim ${TAG}" \
    --notes "$NOTES" \
    --latest

echo ""
echo "✅ Release ${TAG} published!"
echo "   https://github.com/zivpeltz/Neviim/releases/tag/${TAG}"
