# Regain Zen v1.1 — Real System Floating Pop-Up Overlay & Background App Blocker

This update resolves the two core issues preventing Regain Zen from displaying pop-ups over other apps and blocking apps while running in the background. We will implement a real Android `WindowManager` (`TYPE_APPLICATION_OVERLAY`) **Floating Pop-Up Card Window** and a persistent **Background Blocker Monitor Service** (`UsageStatsManager` + `AccessibilityService`) that actively intercepts blocked apps even when Regain Zen is minimized or closed.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed User Preferences (Phase 1)**
> - **Overlay Presentation Style**: **Floating Pop-Up Card Window** centered directly on top of blocked apps (with a dimmed backdrop, animated 4-4-4 breathing lotus orb, and interactive controls).
> - **Primary Exit Action**: Tapping **"Close Blocked App & Return to Sanctuary"** dismisses the floating overlay, rewards **+15 Leaf Coins**, and navigates directly back inside the **Regain Zen Focus Sanctuary** screen.

---

## 1. Overview & Core Root-Cause Fix

- **Why Pop-Ups Weren't Appearing Over Other Apps**: Previously, the mindful intercept modal was rendered as an in-app Jetpack Compose dialog inside `MainActivity`. On Android 10+, background processes are restricted from launching full activities over other apps unless they draw directly through Android's system `WindowManager` using `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` (granted by the *Display Over Other Apps* permission).
- **Why Blocking Only Worked in the Foreground**: Without a dedicated background `ForegroundService` polling `UsageStatsManager.queryEvents()` alongside the `AccessibilityService`, Android suspends app checks as soon as Regain Zen is minimized.
- **What This Update Delivers**:
  1. **True System Floating Pop-Up Card (`TYPE_APPLICATION_OVERLAY`)**: Draws a centered, animated Zen card directly over any blocked app (Instagram, YouTube, TikTok, or any user-selected app) with a real-time 60fps pulsing lotus breathing orb and countdown timer.
  2. **Continuous Background Monitoring Service**: A lightweight foreground service that monitors foreground app switches every 1.2 seconds using `UsageStatsManager` and coordinates with `AccessibilityService` so blocked apps are intercepted immediately while Regain Zen is in the background.
  3. **5-Minute Grace Window Memory**: Tracks temporary 5-minute intentional bypass windows per app so granted grace periods are respected across background checks.

---

## 2. User Experience & Visual Design

- **Floating Pop-Up Card Window Over Blocked Apps**:
  - **Visual Identity**: Warm Alabaster Cream (`#FBF9F5`) floating card with rounded 28dp corners, Deep Forest Sage (`#2E5A44`) primary accents, and a soft 65% dimmed scrim over the underlying blocked app.
  - **Animated Breathing Lotus Orb**: A custom hardware-accelerated canvas view inside the floating window that smoothly expands and contracts on a 4-4-4 breathing cycle (`Inhale 4s` → `Hold 4s` → `Exhale`) while counting down from `8s` to `Ready`.
  - **Interactive Actions Inside the Floating Window**:
    - **Primary Button ("Close [App Name] & Return to Sanctuary · +15 Coins")**: Immediately closes the floating overlay, records +15 Leaf Coins and +8m reclaimed time in SQLite, and brings the user straight into the **Regain Zen Focus Sanctuary** screen.
    - **Secondary Button ("Allow 5m Intentional Grace")**: Unlocks after the 8-second breathing animation completes (and after entering the reflection phrase if Strict Mode is enabled on that app), granting a 5-minute uninterrupted window before the blocker re-arms.
- **Live Floating Pop-Up Test & Background Shield Toggle**:
  - Users can tap **"Test Floating Pop-Up Over Screen"** on the Sanctuary Home screen or App Blocker screen to immediately spawn the real `TYPE_APPLICATION_OVERLAY` floating window on their screen (and even press Home or switch apps to verify it floats over other apps).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual-Engine Background Detection (`UsageStatsManager` Foreground Service + `AccessibilityService`)**
  - *Chosen Approach*: Run a persistent Foreground Service (`specialUse` type) that polls `UsageStatsManager.queryEvents()` every 1.2 seconds whenever *Display Over Other Apps* and *Usage Access* are granted, while also triggering from `AccessibilityService` when enabled.
  - *Why*: Many users grant *Display Over Other Apps* and *Usage Access* first without finding the deeper Accessibility menu, or vice versa. Supporting both ensures reliable background app blocking and pop-up notifications in all configurations.
- **Decision 2: Native Canvas + View-Based `WindowManager` Floating Card**
  - *Chosen Approach*: Build the system `WindowManager` floating pop-up card using a self-contained themed Android `View` hierarchy with a custom `ValueAnimator` Lotus Breathing `View` rather than requiring an Activity-hosted Compose tree.
  - *Why*: `WindowManager` views attached from a background `Service` or `AccessibilityService` do not have an `Activity` `ViewTreeLifecycleOwner` or `SavedStateRegistryOwner` by default. A self-contained animated `View` + custom `Canvas` orb is 100% crash-free from any background service thread, renders at 60fps, and works over every app on Android 8 through Android 16.

---

## 4. Technical Architecture & Data Strategy

```
┌───────────────────────────────────────────────────────────────────────────┐
│                 Background App & Feed Detection Layer                     │
│                                                                           │
│  ┌──────────────────────────────────┐  ┌───────────────────────────────┐  │
│  │   RegainBlockerMonitorService    │  │ RegainAccessibilityBlockerSvc │  │
│  │ (ForegroundService + UsageEvents │  │ (WindowStateChanged + Content │  │
│  │  1.2s foreground package ticker) │  │  inspection for Reels/Shorts) │  │
│  └────────────────┬─────────────────┘  └───────────────┬───────────────┘  │
└───────────────────┼────────────────────────────────────┼──────────────────┘
                    │                                    │
                    ▼                                    ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                     RegainOverlayWindowManager (Singleton)                │
│  • Checks 5-minute grace window map (packageName -> graceExpiryMillis)    │
│  • Fires High-Priority Heads-Up Notification (RegainNotificationHelper)   │
│  • Attaches centered Floating Pop-Up Card via WindowManager               │
│    (TYPE_APPLICATION_OVERLAY + FLAG_DIM_BEHIND, Gravity.CENTER)           │
│  • Runs 60fps ValueAnimator Lotus Breathing Orb + 8s Countdown            │
└───────────────────┬────────────────────────────────────┬──────────────────┘
                    │                                    │
     [Close App & Return to Sanctuary]        [Allow 5m Intentional Grace]
                    │                                    │
                    ▼                                    ▼
┌──────────────────────────────────────┐  ┌─────────────────────────────────┐
│  1. Awards +15 Leaf Coins in SQLite  │  │  1. Stores 5m grace timestamp   │
│  2. Removes Floating Overlay Window  │  │  2. Increments Mindful Breaths  │
│  3. Launches MainActivity directly   │  │  3. Removes Floating Window so  │
│     into Focus Sanctuary Screen      │  │     user can use app for 5 mins │
└──────────────────────────────────────┘  └─────────────────────────────────┘
```

### Interactive Component & State Mapping
- **Background Protection Service Lifecycle**:
  - Automatically started from `MainActivity` and `RegainViewModel` whenever `SYSTEM_ALERT_WINDOW` or `PACKAGE_USAGE_STATS` is granted, displaying a quiet ongoing status notification (*"Regain Zen Shield Active — Protecting your focus in the background"*).
  - Continuously compares the active foreground app package against `RegainDatabase.getBlockingRuleForPackageSync(pkg)`.
- **Floating Pop-Up Card State Machine**:
  - **Initial State (`8s -> 1s`)**: Breathing Lotus Orb pulses with *"Inhale deeply (4s)..."* → *"Hold gently (4s)..."* → *"Exhale slowly..."*. The 5-minute grace button shows the live countdown and remains locked.
  - **Ready State (`0s`)**: If the app is in Strict Mode, validates the reflection phrase input; otherwise enables **"Allow 5m Intentional Grace"**.
  - **Sanctuary Return Flow**: Tapping **"Close [App] & Return to Regain Zen"** immediately removes the `WindowManager` view, updates `UserProgressEntity` and `DailyQuestEntity` in SQLite, and launches `MainActivity` into the Sanctuary screen.
