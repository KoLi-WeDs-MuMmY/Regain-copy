# Regain Zen — Version 1.2 Implementation Plan

## 1. Visual Theme & Aesthetic Direction: *Nocturnal Ocular Sanctuary & Disciplined Wavelengths*

Version 1.2 refines the app's visual system and motion language around ocular comfort and biological color discipline:

- **Deeply Muted, Shadowy Canvas & Layered Elevated Panels**:
  - **Primary Canvas**: Deep obsidian-moss shadow (`#0D1110` / `#111614`) with zero harsh glare to relax ocular muscles during extended deep-work sessions.
  - **Elevated Sanctuary Panels**: Subtle tiered surfaces (`#171E1B` and `#1E2723`) with muted hairline borders (`#26312C`) so secondary metadata melts quietly into dark borders while primary data sits in softened, low-glare alabaster (`#E6ECE8`).
- **Strict Wavelength Discipline**:
  - **Active Study & Positive Milestones (Mid-Spectrum Calming Frequency)**: Grounding bioluminescent sage-emerald (`#4E9F76` / `#68B68E`) that signals safety, focus, and steady rhythm.
  - **Interception & Habit-Shock Alerts (Long-Wavelength Alarm Signal)**: Intense crimson-amber long-wavelength pulse (`#E0533C` / `#FF6B4A`) reserved strictly for the blocking intercept overlay to halt unconscious muscle memory without polluting the everyday study workspace.
- **New Aesthetic Adaptive Logo (`ic_launcher`)**:
  - A custom vector adaptive icon pairing a deeply muted obsidian-slate background (`#0E1412`) with a sleek geometric **Sanctuary Shield & Hourglass Aperture** foreground. Inside the shield's protective frame, a calming mid-spectrum emerald focus core (`#52A77C`) balances above a warm amber horizon arc—instantly communicating *screen-time shield, focus timer, and mindful digital sanctuary*.

---

## 2. Decoupled Motion Container (`BlockerOverlayContainer`)

We will create a dedicated, presentation-agnostic animation wrapper in `com.example.ui.components.BlockerOverlayContainer`:

- **Composable API**:
  ```kotlin
  @Composable
  fun BlockerOverlayContainer(
      visible: Boolean,
      onDismissComplete: () -> Unit,
      modifier: Modifier = Modifier,
      content: @Composable (requestDismiss: () -> Unit) -> Unit
  )
  ```
- **Entry & Exit Transitions (Native Compose `AnimatedVisibility`)**:
  - **Entry**: `fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.95f, animationSpec = tween(250)) + slideInVertically(animationSpec = tween(250), initialOffsetY = { it / 4 })`
  - **Exit**: `fadeOut(animationSpec = tween(200)) + slideOutVertically(animationSpec = tween(200), targetOffsetY = { it / 4 })`
- **State Coordination & Post-Exit Routing**:
  - Controlled via an internal boolean state (`isVisible`) backed by `MutableTransitionState`.
  - Calling `requestDismiss()` sets `isVisible = false` so the 200ms exit animation plays smoothly inside Compose.
  - A `LaunchedEffect` waits until the exit transition finishes (`!transitionState.currentState && transitionState.isIdle`) before invoking `onDismissComplete()`, which then triggers `Intent.CATEGORY_HOME` (and removes the overlay view)—guaranteeing zero abrupt cut-offs or frame stutter.
- **Strict Separation of Concerns**:
  - No XML animations, no `overridePendingTransition`, and no `WindowManager.updateViewLayout` frame-stuttering calls.
  - The container manages only the motion envelope, adaptive sizing constraints, and backdrop scrim fade, delegating all visual UI to the modular `content` slot.

---

## 3. Production-Ready Modular Intercept Slot (`BlockerInterceptCardSlot`)

Passed as the child slot into `BlockerOverlayContainer`, wrapped in a cleanly padded, modular card (`widthIn(max = 460.dp)`) for easy restyling:

1. **Breathing Long-Wavelength Warning Shield Icon**:
   - Centered at the top of the card, pulsing smoothly with the long-wavelength crimson-amber signal (`#E0533C` -> `#FF6B4A`) to interrupt automatic scrolling muscle memory.
2. **5-Second Mindfulness Countdown Timer**:
   - Counts down from `5s` to `0s` with a smooth progress ring/bar upon interception.
   - Holds override interaction until the 5-second mindfulness pause completes.
3. **Two Full-Width Action Buttons**:
   - **Primary Button (`"Return to Focus"`)**: Triggers `requestDismiss()`, playing the 200ms slide-and-fade exit animation and immediately routing to `Intent.CATEGORY_HOME` upon completion.
   - **Secondary Button (`"Take a break"`)**:
     - Enabled once the 5-second mindfulness countdown completes (and when Strict Mode is not locking overrides).
     - Clicking `"Take a break"` smoothly reveals **4 break-duration chips below it: `5m`, `10m`, `15m`, and `30m`**.
     - **Remaining-Time Glow Logic**: Each option (`5`, `10`, `15`, `30` min) is compared against the user's remaining daily allowance for that blocked app (`remainingMinutes = max(0, dailyLimitMinutes - usedTodayMinutes)`).
     - Options where `remainingMinutes >= optionMinutes` **glow** with a vibrant border and illuminated surface; options exceeding the remaining time stay dim/disabled so the user can only take a break within their available budget. Selecting a glowing option plays the exit transition and grants that exact break duration.

---

## 4. Choreographed Animations & Screen Size Compatibility

### 3a. App Opening Animation (1.2s – 1.5s Clean Sequence)
- Implemented in pure Jetpack Compose (`RegainOpeningTransition`):
  - **0ms – 600ms**: Central Sanctuary Shield emblem fades in (`fadeIn(tween(600))`) and gently scales from `0.95f` to `1.0f` (`scaleIn(initialScale = 0.95f, animationSpec = tween(600))`).
  - **200ms – 1100ms**: A single-cycle mid-spectrum emerald breathing glow softly accents the emblem.
  - **300ms – 700ms**: Title typography glides upward by `12.dp` with a soft fade-in (`tween(400)`).
  - **1150ms – 1450ms**: Seamlessly cross-dissolves (`fadeOut(tween(300))`) directly into the main Sanctuary dashboard with no tap-to-skip clutter or artificial loading delay.

### 3b. Blocking Intercept Animation
- Replaces the instant static pop-up in `RegainOverlayWindowManager` and in-app shield modal with `BlockerOverlayContainer` + `BlockerInterceptCardSlot`.

### 3c. In-App Tab Transitions (`SANCTUARY`, `FOCUS`, `SHIELD`, `INSIGHTS`)
- Uses directional `AnimatedContent` in `MainActivity.kt` based on tab index order:
  - Smooth horizontal slide (`{ it / 6 }` / `{ -it / 6 }`) combined with `fadeIn(tween(220))` and `fadeOut(tween(180))` when switching between tabs.

### Universal Screen-Size Compatibility
- Uses `BoxWithConstraints` and `WindowWidthSizeClass` (`COMPACT`, `MEDIUM`, `EXPANDED`) across all animations.
- Relative percentage offsets (`{ it / 4 }`, `{ it / 6 }`), density-independent `12.dp` typography glide, and `widthIn(max = 460.dp)` with vertical scroll safety ensure crisp rendering on compact phones, tall 21:9 screens, foldables, and tablets in portrait or landscape.

---

## 5. Verification Plan
- Compile the project using `compile_applet` to ensure zero build or type errors.
- Run unit and Robolectric tests (`gradle :app:testDebugUnitTest`) verifying the break-duration eligibility calculation (`5, 10, 15, 30` min glow states), countdown state machine, and persistence.
