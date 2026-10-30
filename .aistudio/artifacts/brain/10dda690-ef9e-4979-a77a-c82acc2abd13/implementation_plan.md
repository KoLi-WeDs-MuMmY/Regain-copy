# Regain Zen — Mindful Screen Time & Deep Focus

Regain Zen is a complete Kotlin and Jetpack Compose rebuild of the Regain (`ai.regainapp`) digital wellbeing and productivity app, reimagined with a **Calm Organic Zen** visual aesthetic, full **Adaptive Multi-Screen Architecture** across all device sizes, offline-first local persistence, and a quota-capped **AI Productivity Coach**.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and product choices reflect your confirmed preferences and feedback:

- **Confirmed Decision 1 — Calm Organic Zen UI/UX Redesign**: Replaces harsh warning screens with a restorative, tactile palette of warm cream, soft linen, deep forest sage, and terracotta accents, paired with editorial serif headings (`Lora`) and clean geometric body typography (`Plus Jakarta Sans`).
- **Confirmed Decision 2 — Universal Screen Size Compatibility & Zero Wasted Space**: Uses dynamic `BoxWithConstraints` breakpoints (`Compact`, `Medium`, `Expanded`), adaptive `NavigationBar` (portrait handhelds) vs. `NavigationRail` (tablets, foldables, and landscape), responsive multi-column grids, and full edge-to-edge `WindowInsets.safeDrawing` padding so every screen size fills available space proportionally with zero clipped controls or awkward blank margins.
- **Confirmed Decision 3 — Strictly Capped Single-Prompt AI Productivity Coach**: Provides personalized screen time analysis and actionable focus habits using a single, consolidated structured prompt (`gemini-3.5-flash-lite`) capped at **1 request per 24 hours** (and only triggered explicitly by user action). Results are cached locally in Room so subsequent views cost zero requests, and a deterministic local insight engine provides instant fallback insights when completely offline.
- **Confirmed Decision 4 — 100% Local Offline Architecture**: All data—including app timers, Reels/Shorts blocking rules, focus history, Rega companion streaks/coins, and virtual Study Together rooms with simulated global study companions—runs entirely on-device using Room SQLite without requiring accounts or server connections.

---

### 1. Overview & Core Concept

- **What It Does**: Helps users overcome doomscrolling, curb short-form video addiction (Reels, Shorts, TikTok), set mindful daily app limits with Strict Mode protection, run deep-work Pomodoro timers with procedural ambient audio, study alongside virtual accountability companions, and receive daily personalized coaching insights.
- **Target Audience / Persona**: Students, remote workers, and mindful smartphone or tablet users who want the full functional power of Regain without cloud account friction or cluttered interfaces.
- **Key Value**: Combines Regain's signature targeted intervention loops (blocking infinite feeds while keeping messaging open, mindful breathwork pauses before opening distracting apps, and gamified focus streaks) with a serene, distraction-free Zen interface that adapts fluidly to any display.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Home & Rega Zen Companion Dashboard**: Displays today's screen time vs. mindful goal, saved time metric, current focus streak, Rega companion mood/evolution stage, Daily Detox Quests, and quick-launch cards for Focus Timer and Reels/Shorts Shield.
  2. **Deep Focus & Pomodoro Studio**: Interactive circular organic timer supporting Pomodoro (25/5, 50/10, custom), Stopwatch, and Countdown modes, focus tag selection (Study, Coding, Reading, Mindful Rest), built-in synthesized Ambient Sound Mixer (Rain, Forest Wind, Brown Noise, Binaural Focus), and Strict Mode toggle.
  3. **App Limits, Reels/Shorts Shield & Smart Schedules**:
     - *Targeted Short-Form Blocker*: Individual toggles and daily minute caps for Instagram Reels, YouTube Shorts, TikTok, Snapchat Spotlight, and X/Reddit feeds—plus an interactive **"Test Mindful Intercept Overlay"** simulator demonstrating the breathwork pause & block screen.
     - *App Daily Limits & Schedules*: Per-app usage progress bars, daily limit sliders, Strict Mode lock (requiring a 4-breath mindful pause or reflection challenge before editing limits), and recurring Smart Schedules (Morning Routine, Deep Work, Bedtime Wind-Down).
  4. **Study Together Rooms (Offline Simulated Global Rooms)**: Join themed virtual study rooms (*Silent Zen Library*, *Medical & STEM Cram*, *Late Night Creators*) with realistic simulated focus peers, live session ticker, and a local milestone leaderboard.
  5. **Screen Time Insights & 1-Prompt AI Coach**: Weekly usage breakdown charts, pickup frequency, doomscroll loop detection, and the **AI Productivity Coach** card featuring a visible daily quota badge (`1/1 Free Daily AI Request Available`), one-tap consolidated analysis, and persistent local caching.

- **Adaptive & Responsive Multi-Screen Strategy (All Screen Sizes)**:
  - **Compact Width (`< 600.dp`, standard phones)**: Bottom `NavigationBar` with edge-to-edge `WindowInsets.navigationBars` safety, full-width fluid cards (`fillMaxWidth()`), and vertically scrollable sections that ensure small screens (e.g., 320dp–360dp width or split-screen mode) never clip text or buttons.
  - **Medium Width (`600.dp – 840.dp`, large phones in landscape & unfolded foldables)**: Side `NavigationRail` to maximize vertical canvas space, paired with a balanced 2-column adaptive grid (`LazyVerticalGrid` / dual-pane rows) so cards expand to utilize the full width without leaving empty gutters.
  - **Expanded Width (`> 840.dp`, tablets & desktop mode)**: Canonical two-pane and multi-column dashboard layouts (e.g., Focus Timer ring alongside the Ambient Sound Mixer & Session History; App Shield controls alongside the live Schedule & Intercept preview) utilizing 100% of the viewport intelligently.
  - **Safe-Area & Cutout Protection**: Full `enableEdgeToEdge()` configuration with `WindowInsets.safeDrawing` and `imePadding()` so camera notches, curved corners, gesture bars, and on-screen keyboards never obscure interactive elements.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: **Calm Organic Zen** — tactile, breathable layouts inspired by botanical journals and mindful stationery.
  - *Color Palette & Mood*:
    - `--background`: Warm Alabaster Cream (`#F7F5F0`) in Light Mode / Deep Moss Charcoal (`#141A16`) in Dark Mode
    - `--surface`: Soft Linen (`#EFECE4`) / Elevated Pine (`#1C241E`)
    - `--primary`: Deep Forest Sage (`#3A5A40`) / Luminous Sage (`#A3B18A`)
    - `--secondary`: Warm Eucalyptus (`#588157`)
    - `--accent`: Sunlit Terracotta (`#D47A55`) & Amber Ochre (`#E09F3E`) for streaks, coins, and gentle limit warnings
  - *Typography & Hierarchy*:
    - **Display / Headers**: `Lora` (bundled locally via `font-util`) for calm, editorial wisdom headers and timer numerals.
    - **Body / UI Controls**: `Plus Jakarta Sans` (bundled locally via `font-util`) for crisp legibility in metrics, badges, and controls.
  - *Component Styling & Layout*: Material 3 adaptive components with generous `24.dp` rounded organic cards, subtle `1.dp` botanical borders, asymmetric hero status banners, and custom `Canvas` botanical ring visualizations that scale dynamically via `DrawScope.size`.
- **Interactive Feedback & Motion**:
  - Smooth spring-physics progress rings on the Focus Timer and Screen Time gauge.
  - Expandable breathwork animation circle (4-second inhale, 4-second hold, 4-second exhale) when testing or triggering an app/Reels block intercept.
  - Real-time procedural audio synthesis (`AudioTrack`) so ambient focus soundscapes play genuinely offline without external media dependencies.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Dynamic Container-Driven Responsive Layouts (`BoxWithConstraints`)**
  - *Chosen Approach*: Drive all layout switching (`Compact` vs. `Medium` vs. `Expanded`, portrait vs. landscape) from `BoxWithConstraints` container dimensions rather than fixed device assumptions, combined with `LazyVerticalGrid(GridCells.Adaptive(minSize = 300.dp))` and proportional weights (`Modifier.weight(1f)`).
  - *Why*: Ensures that whether the app runs on a compact phone, a tall foldable split-screen, or a widescreen tablet, no screen space is wasted and no interactive control is ever pushed off-screen.

- **Decision 2 — Strict 1-Prompt Daily Rate Limiter & Offline Heuristic Fallback for AI Coach**
  - *Chosen Approach*: Consolidate all user context (7-day screen time totals, top distracting apps, Reels/Shorts intercepts, and completed focus sessions) into a **single compact prompt** using `gemini-3.5-flash-lite` with structured JSON output. Store a timestamped `AiCoachInsightEntity` in Room that enforces a hard cap of **1 API request per calendar day** (with a clear quota indicator and a default 100% Offline Local Coach mode).
  - *Why*: Guarantees the user never exceeds free-tier rate limits while still delivering tailored, high-impact coaching. When offline, the built-in Local Heuristic Coach analyzes the exact same Room metrics instantaneously on-device.

- **Decision 3 — Procedural Offline Ambient Sound Engine**
  - *Chosen Approach*: Generate soothing Rain, Brown Noise, Soft Forest Wind, and 40Hz Gamma Focus Binaural Beats directly in Kotlin using Android's native `AudioTrack` PCM synthesis.
  - *Why*: Works 100% offline with zero network calls and keeps the APK lightweight.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌───────────────────────────────────────────────────────────────────────────┐
│                Adaptive Jetpack Compose UI Shell (BoxWithConstraints)     │
│   Compact: Bottom NavigationBar  │  Medium/Expanded: Side NavigationRail  │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐  │
│  │  Home & Rega │ │ Focus Studio │ │ Shield &     │ │ Insights, Rooms  │  │
│  │  Adaptive    │ │ Two-Pane /   │ │ Adaptive Grid│ │ & 1-Req AI Coach │  │
│  │  Dashboard   │ │ Soundscape   │ │ App Limits   │ │ Multi-Column     │  │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └────────┬─────────┘  │
│         └────────────────┴────────┬───────┴──────────────────┘            │
│                                   ▼                                       │
│               ┌───────────────────────────────────────┐                   │
│               │     Mindful Intercept & Breathwork    │                   │
│               │     Responsive Full-Screen Modal      │                   │
│               └───────────────────┬───────────────────┘                   │
└───────────────────────────────────┼───────────────────────────────────────┘
                                    ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                    ViewModel & Domain State Layer                         │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │ RegainViewModel (StateFlow<RegainUiState>, Timer & Audio Jobs)      │  │
│  └────────────┬───────────────────────────┬────────────────────┬───────┘  │
└───────────────┼───────────────────────────┼────────────────────┼──────────┘
                ▼                           ▼                    ▼
┌───────────────────────────┐ ┌─────────────────────────┐ ┌─────────────────┐
│    RegainRepository       │ │ProceduralSoundSynthesizer│ │SinglePromptAi   │
│  (Room DAOs + UsageStats) │ │ (Native AudioTrack PCM) │ │CoachEngine      │
└───────────────┬───────────┘ └─────────────────────────┘ │(1/Day Cap +     │
                ▼                                         │ Offline Engine) │
┌───────────────────────────┐                             └─────────────────┘
│   Room SQLite Database    │
│  - AppLimitEntity         │
│  - ShortFormShieldEntity  │
│  - FocusSessionEntity     │
│  - ScheduleBlockEntity    │
│  - UserProgressEntity     │
│  - AiCoachInsightEntity   │
└───────────────────────────┘
```

- **Data Model & State**:
  - `AppLimitEntity`: Tracks package name, display name, category, daily limit minutes, today's used minutes, block count, and Strict Mode flag.
  - `ShortFormShieldEntity`: Dedicated rules for Instagram Reels, YouTube Shorts, TikTok, Snapchat Spotlight, and Reddit/X feeds (mode: *Block Completely*, *Daily Time Cap*, or *Mindful Breath Pause*, plus intercepted scroll count).
  - `FocusSessionEntity`: Stores completed and active Pomodoro/focus sessions, duration, tag, ambient sound used, and earned Rega Leaf Coins.
  - `ScheduleBlockEntity`: Recurring time-window schedules (days of week, start/end time, blocked categories, strict lock).
  - `UserProgressEntity`: Tracks Rega mascot level, Leaf Coins, current streak days, mindful breaths taken, and daily quest completion states.
  - `AiCoachInsightEntity`: Caches the daily AI coach summary, top habit bottleneck, 3 actionable micro-challenges, date key (`YYYY-MM-DD`), and quota usage count (`0` or `1` for the current day).

- **Interactive Component & State Mapping**:
  - **Responsive Viewport Adaptation**: Every screen reads the active `WindowLayoutInfo` (width/height breakpoints and orientation) from the root `BoxWithConstraints`. On compact portrait screens, sections stack in a smooth `LazyColumn`; on landscape, foldable, or tablet displays, sections automatically arrange into multi-column grids (`GridCells.Adaptive`) or side-by-side panes filling 100% of the safe drawing width.
  - **Focus Timer Lifecycle**: Starting a session initiates a coroutine ticker in `RegainViewModel`, activates the selected `ProceduralSoundSynthesizer` channel if enabled, locks navigation distractions when Strict Mode is active, and credits Leaf Coins + updates `FocusSessionEntity` and streak counters upon completion.
  - **Reels & App Shield Controls**: Adjusting sliders or toggles immediately persists changes to Room; tapping *"Preview Intercept"* opens the responsive Mindful Pause & Breathwork Overlay where the user can either complete a 12-second guided breathing cycle to unlock a 5-minute grace period or choose *"Back to Focus"* to earn bonus Leaf Coins.
  - **AI Productivity Coach Quota Gate**: When the user taps *"Generate Today's Insight"*, the engine checks `AiCoachInsightEntity` for today's date key. If `requestsUsedToday == 0`, it runs the single-prompt generator (with automatic fallback to the on-device heuristic analyzer when offline), saves the result to Room, and marks today's quota as `1/1 Used`.
