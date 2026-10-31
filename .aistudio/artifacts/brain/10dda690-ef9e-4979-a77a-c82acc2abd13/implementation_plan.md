# Regain Zen — Version 1.3.0 UI & UX Implementation Plan

## 1. Visual Direction & Design System (Hybrid Obsidian Sanctuary + Tactile Chrono)
Version **1.3.0** (`versionName = "1.3.0"`, `versionCode = 4`) upgrades the entire visual appearance of **Regain Zen** by fusing the **Obsidian Sanctuary** and **Tactile Chrono & Focus** design specifications while keeping 100% of existing features, database persistence, background blocking services, and actions intact:

- **Color Architecture**:
  - **Base Void Canvas**: `#0B0C0E` / `#111318` deep low-luminance obsidian backdrop.
  - **Elevated Bento Modules**: `#14161B` (`surface-container-low`) and `#1C1F26` (`surface-container`) with `24dp`–`28dp` rounded geometry and hairline specular borders (`1dp` `rgba(255, 255, 255, 0.06)`).
  - **Inset Recessed Troughs**: `#0D0E10` for segmented mode switchers, progress tracks, and radar bars.
  - **Primary Action Accent (Radiant Ember / Burnt Vermilion)**: `#FF5E4D` / `#E75A44` for primary pill CTAs (`Begin Meditative Flow`, `Pause Flow`, `Synthesize Daily Focus`, `Test Friction Ritual`) with `#0B0C0E` dark obsidian label contrast.
  - **Secondary Restorative Accent (Celestial Opal)**: `#4EBEA5` / `#6BD9BF` for balanced cognitive load badges, active shield telemetry (`AEGIS GRID ACTIVE`, `SURGICAL`, `STRIPPED`), and 432Hz binaural indicators.
  - **Tactile Chrono Dial Accent (Warm Cream Ivory)**: `#EADECF` for the mechanical countdown dial ring, precision radial ticks, and tabular numerals.
- **Typography (`Plus Jakarta Sans`)**:
  - Bundle local `Plus Jakarta Sans` font files (`.ttf`) in `app/src/main/res/font/` for crisp geometric headers, tabular countdown numerals, and uppercase micro-labels (`11sp`, `+0.08em` tracking).
- **Static In-App UI for v1.3.0**:
  - In-app screen switching, tab navigation, and app launch open directly without theme/tab animations (reserving general UI animations for the **v1.3.1** update).
  - **Only the Block Pop-Up (Interception Overlay)** features the new multi-stage liquid circle animation.

---

## 2. Screen-by-Screen UI & UX Redesign (Zero Feature Loss)

### Global Top Header & Floating Pill Navigation
- **Unified Top Bar**: Displays the Regain Zen brand mark & active screen title, live streak/coins pill (`14D` / Focus Coins), Shield/Permissions status indicator button, and quick goal/settings trigger.
- **Navigation Bar & Rail**: Floating dark pill dock (`Focus`, `Sanctuary`, `Shield`, `Rooms`, `Coach`) with subtle active pill indicator and adaptive `NavigationRail` on tablet/landscape layouts.

### 1. Sanctuary Screen (`SanctuaryHomeScreen.kt`)
- **Twilight Sanctuary Hero**: Centered `"STATE OF MIND / Twilight Sanctuary"` header with the circular **Reclaimed Time** ember ring gauge (`#FF5E4D` glowing arc, reclaimed vs. daily cap readout) and `"Cognitive load balanced"` opal status pill.
- **2-Column Bento Telemetry**: Side-by-side cards for **Pickups Avoided / Blocks** and **Deep Flow Span** with delta badges.
- **Active Horizon Card**: Displays the active or upcoming focus directive/schedule, DND/Shield status badge, progress bar, and remaining duration.
- **Ritual / Daily Quests**: Interactive quest cards with XP/coin rewards, completion checkmarks, and one-tap quest actions.
- **Primary Pill CTA & Quick Controls**: Full-width Radiant Ember `"Begin Meditative Flow"` button plus clean access to Daily Goal tuning, Permission diagnostics, and Reels/Shorts intercept preview.

### 2. Focus Studio (`FocusStudioScreen.kt`)
- **Recessed Mode Selector**: Top pill bar switching between `Deep Work 25m`, `Ultra Flow 50m`, `Custom`, and `Stopwatch`.
- **Tactile Chrono Radial Dial**: Precision radial tick ring and Warm Cream Ivory / Radiant Ember progress arc with centered tabular countdown readout (`REMAINING`, cycle indicator, and `100% FOCUS` badge).
- **Tactile Transport Controls**: Circular Reset button, Radiant Ember `"Start / Pause Flow"` pill CTA, and Save/Complete session action (preserving exact-second study time recording).
- **Active Directive Card**: Interactive subject/task tag selector (`Study`, `Coding`, `Reading`, `Design`, `Writing`) with elapsed pace bar.
- **Acoustic Field / Focus Soundscape Card**: `432 HZ BINAURAL` badge, ambient sound selector (`Rain`, `White Noise`, `Forest`, `Cafe`, `Silent`), visual waveform bars, and tactile volume slider.
- **Rhythm Constellation & Recent Sessions**: Weekly focus consistency matrix (`M T W T F S S`) alongside recorded session history.

### 3. Shield & Limits (`ShieldLimitsScreen.kt`)
- **Aegis Grid Hero Radar**: `"AEGIS GRID ACTIVE"` status pill, concentric shield radar ring displaying total intrusions deflected, and posture selector (`Zen Fortress / Strict Mode`, `Surgical`, `Monk`) plus real-time background blocker permission status.
- **Impulse Radar Window**: 24-hour vulnerability bar highlighting peak urge windows and active shield coverage.
- **Monitored Portals**: Bento cards for **Short-Form Shields** (Instagram Reels `SURGICAL`, YouTube Shorts `STRIPPED`, TikTok `ABSOLUTE LOCK`) and **Per-App Daily Allowances** with emerald/ember progress bars, +/-5m steppers, and block toggles.
- **4-4-4 Friction Ritual Preview Card**: Interactive preview card with `"TEST FRICTION RITUAL"` button to launch and test the new Block Pop-Up animation on demand.
- **Add App / Focus Schedules**: Full app limit management and scheduled focus windows (`Add Schedule`, toggle, delete).

### 4. Study Rooms (`StudyRoomsScreen.kt`)
- **Obsidian Sanctuary Bento Layout**: Redesigned room selector pills, live room telemetry header, peer study cards with live timer progress bars, and interactive focus cheers.

### 5. AI Coach & Insights (`AiCoachInsightsScreen.kt`)
- **Cognitive Engine Header**: `"COGNITIVE ENGINE ACTIVE"` + `"Local Vault"` badge.
- **Rega AI Attention Coach Hero Card**: Displays `"1/1 DAILY QUOTA AVAILABLE"` + `"Offline Cache"` badges, daily cognitive synthesis text, Radiant Ember `"Synthesize Daily Focus ->"` pill CTA, and an expandable `"Inspect Privacy-Safe Prompt Payload"` accordion.
- **Weekly Attention Delta**: 7-day stacked bar chart comparing Deep Focus vs. Impulse Drift with flow ratio telemetry.
- **Tactile Shield Audit & Neural Telemetry Feed**: Per-app intervention breakdown (`YouTube`, `Instagram`, `TikTok`), behavioral insight cards, and the `"AXIOM OF THE DAY"` wisdom card.

---

## 3. Fresh Block Pop-Up Animation (Black Liquid Circle Sequence)
Both the system-wide background app blocker overlay (`RegainOverlayWindowManager` + `BlockerOverlayContainer`) and in-app intercept modal (`MindfulInterceptModal`) will feature the new **Black Liquid Circle** choreography:

1. **Stage 1 — Liquid Circle Rise**: When a blocked app is opened, a sleek **Black Liquid Circle** housing the **Regain Zen emblem** rises smoothly from the bottom of the screen to the exact center.
2. **Stage 2 — Elastic Pulse (Small -> Big)**: At the center of the screen, the liquid circle contracts briefly (becomes small) and then expands organically (becomes big) with a subtle liquid ring ripple.
3. **Stage 3 — Adaptive Dock Shift**:
   - **Portrait (Vertical Screen)**: The liquid emblem glides **upward** toward the upper focal zone.
   - **Landscape (Horizontal Screen)**: The liquid emblem glides **to the left** so the content unfolds cleanly beside it.
4. **Stage 4 — Liquid Message Emergence**: A mindfulness intervention message emerges directly out of the liquid circle (showing the intercepted app name, attempt count, and conscious habit prompt).
5. **Stage 5 — Guided Breath Exercise**: A tactile breathing ring (`Inhale / Hold / Exhale` countdown) guides the user through a brief neuro-pause before override options unlock.
6. **Stage 6 — Action & Break Options**:
   - **Primary CTA ("Return to Study Sanctuary")**: Triggers the clean exit transition and routes back to Regain Zen / Home.
   - **Break Options (`5m`, `10m`, `15m`, `30m`)**: Interactive break duration pills with glowing selection feedback and temporary unlock support (or locked badge when Strict Mode is active).
