# Regain Zen — Mindful Screen Time & Deep Focus (Android Kotlin/Compose)

**Regain Zen** is an offline-first Android digital wellbeing and focus application built with **Kotlin** and **Jetpack Compose**, featuring:
- **Calm Organic Zen Theme** with adaptive layouts (`Compact`, `Medium`, `Expanded`) across phones, foldables, and tablets.
- **Deep Focus & Pomodoro Studio** (`25/5`, `50/10`, `Countdown`, `Flow Stopwatch`) with real-time synthesized offline ambient soundscapes (*Kyoto Moss Rain*, *Warm Brown Noise*, *Bamboo Forest Wind*, and *40Hz Gamma Focus*).
- **Targeted Reels & Shorts Shield** (blocks infinite vertical feeds while keeping Direct Messages & Search open), **Per-App Daily Limits**, **Strict Mode** with a 4-4-4 guided breathing intercept, and **Smart Recurring Schedules**.
- **Study Together Sanctuary Rooms** with simulated focus peers and a local leaderboard.
- **1-Prompt Capped AI Productivity Coach** with an on-device Zen heuristic engine and strict `1/1` daily request quota enforcement.

---

## Building & Debugging on GitHub (GitHub Actions APK Download)

This repository includes an automated GitHub Actions workflow at [`.github/workflows/android-debug-apk.yml`](.github/workflows/android-debug-apk.yml).

1. Push your branch to GitHub.
2. Open the **Actions** tab in your GitHub repository.
3. Click the **Build & Debug Android APK** workflow run.
4. Once completed, scroll to the **Artifacts** section at the bottom of the run summary and download **`regain-zen-debug-apk`** (which contains `app-debug.apk`) and **`debug-test-reports`**.

---

## Building Locally After Cloning from GitHub

Because `debug.keystore` and `.env` are listed in `.gitignore`, restore them once after cloning before running Gradle:

```bash
# 1. Restore debug.keystore (from debug.keystore.base64) and .env
bash scripts/prepare-github-build.sh

# 2. Run unit & Robolectric tests
gradle :app:testDebugUnitTest --stacktrace

# 3. Compile the installable Debug APK
gradle :app:assembleDebug --stacktrace
```

The compiled APK will be output to:
```
app/build/outputs/apk/debug/app-debug.apk
```
