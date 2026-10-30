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

echo "Environment ready. Run 'gradle :app:assembleDebug --stacktrace' to build the APK."
