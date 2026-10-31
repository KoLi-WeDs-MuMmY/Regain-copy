#!/usr/bin/env bash
set -euo pipefail

echo "=== Preparing Regain Zen Build Environment ==="

# 1. Restore debug.keystore from debug.keystore.base64 (since debug.keystore is gitignored)
if [ ! -f "debug.keystore" ]; then
  if [ -f "debug.keystore.base64" ]; then
    echo "Decoding debug.keystore from debug.keystore.base64..."
    base64 -d debug.keystore.base64 > debug.keystore
  else
    echo "Generating standard Android debug.keystore..."
    keytool -genkeypair -v \
      -keystore debug.keystore \
      -storepass android \
      -alias androiddebugkey \
      -keypass android \
      -keyalg RSA \
      -keysize 2048 \
      -validity 10000 \
      -dname "CN=Android Debug,O=Android,C=US"
  fi
fi

# 2. Ensure .env exists for Secrets Gradle Plugin
if [ ! -f ".env" ] && [ -f ".env.example" ]; then
  echo "Copying .env.example to .env..."
  cp .env.example .env
fi

# 3. Ensure Android SDK licenses are accepted if ANDROID_HOME or ANDROID_SDK_ROOT is set
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -n "$SDK_ROOT" ] && [ -d "$SDK_ROOT" ]; then
  mkdir -p "$SDK_ROOT/licenses"
  printf "\n8933bad161af4178b1185d1a37fbf41ea5269c55\nd56f5187479451eabf01fb78af6dfcb131a6481e\n24333f8a63b6825ea9c5514f83c2829b004d1fee\n" > "$SDK_ROOT/licenses/android-sdk-license"
  printf "\n84831b9409646a918e30573bab4c9c91346d8abd\n504667f4c0de7af1a06de9f4b1727b84351f2910\n" > "$SDK_ROOT/licenses/android-sdk-preview-license"
fi

echo "Environment ready. Run 'gradle :app:assembleDebug' and 'gradle :app:testDebugUnitTest' to build and test."
