# Regain Zen v1.3.2 Implementation Plan

## 1. Overview & Goals
Update **Regain Zen** to version **1.3.2** with the following core upgrades aligned with your answers:
1. **Reference-Accurate Tactile Chrono Pomodoro Screen (`FocusStudioScreen.kt`)**:
   - Match the uploaded reference layout (`screen(1)(1).png`):
     - **Mode Selector Bar**: Scrollable/pill selector with `Deep Work 25m`, `Ultra Flow 50m`, `Short Rest`, and a `Custom` configurator pill so users can customize focus, short break, long break, and round counts at any time.
     - **Main Tactile Chrono Card**:
       - Interactive directive header pill (`DESIGN ARCHITECTURE • SPRINT 3` or active task title with coral indicator dot and edit/customizer button).
       - **Deep 3D Extruded Chrono Dial**:
         - Outer precision tick ring (12 major glowing ticks + 48 minor ticks).
         - Thick warm-cream (`#EFE6DD`) 3D neumorphic outer ring with multi-stop bevel highlights, ambient coral glow, and deep drop shadow.
         - Translucent coral pie-wedge sweep (`#FF6B52`) across the cream ring with a crisp glowing coral radial hand at the leading edge.
         - Deep recessed dark inner well (`#1C1918`) with inner shadow ring, coral stopwatch/timer icon, bold `MM:SS` countdown readout in warm cream, and `REMAINING` / `BREAK REMAINING` subtitle.
       - **Tactile Controls Row**: Raised 3D circular Reset (`↺`) button, wide 3D coral pill (`⏸ Pause Flow` / `▶ Start Flow`), and raised 3D circular Mute/Sound (`🔊`) button.
       - **Recessed Bottom Status Pill**: Full-width dark inset capsule displaying `🎯 Round X of Y` on the left and `🕒 Xh Ym today` (real studied time) on the right.
     - **Focus Soundscape Card**:
       - Header with waveform badge (`FOCUS SOUNDSCAPE`) and sound preset selector button (`•••`).
       - Recessed inner dark player box displaying track title (`Kyoto Zen Rain & Beats`), artist/subtitle (`Illmind & Burna Boy • Lo-Fi Chill`), `BINAURAL 432HZ` badge, and an animated 12-bar coral & cream waveform equalizer.
       - Transport controls (`⏮` previous preset, glowing coral `⏸`/`▶` play/pause button, `⏭` next preset, speaker icon, and custom coral volume slider).
     - **Interactive Task Checklist Card**:
       - Header (`TASK CHECKLIST`) and working `+ Add` task dialog/inline creator.
       - Interactive task rows with checkable coral circle icons, strikethrough completed text, duration pill (`25m`, `50m`) or `ACTIVE` coral badge, and live completion footer (`X of Y complete` • `Z% Done`). Selecting a task sets the active directive on the Pomodoro dial.
     - **Focus Consistency • Weekly Matrix Card**:
       - Header (`FOCUS CONSISTENCY • WEEKLY MATRIX`) and `WEEK 44` badge.
       - 7-column (`MON`–`SUN`) × 4-row tactile 3D heatmap grid with rounded cells shaded by focus intensity, plus `Less Intent` to `Deep Flow` 4-dot legend.

2. **Customizable Auto-Advancing Pomodoro Logic + Break Notifications**:
   - Upgrade `ActiveTimerUiState` and `RegainViewModel` with a full Pomodoro cycle engine:
     - **Customizable Parameters**:
       - Focus duration (`5–120 min`, default `25m`)
       - Short Break duration (`1–30 min`, default `5m`)
       - Long Break duration (`5–60 min`, default `15m`)
       - Rounds before Long Break (`2–8 rounds`, default `4`)
     - **Auto-Advance with System Notification**:
       - When a **Focus round** finishes, the exact studied time and Leaf Coins are recorded in SQLite, and the timer automatically transitions into the **Short Break** (or **Long Break** after completing the final round) and fires a high-priority Android notification (`RegainNotificationHelper.sendPomodoroPhaseTransitionNotification`) alerting the user that break time has started.
       - When a **Break round** finishes, the timer automatically advances to the next **Focus round** (`Round X + 1 of Y`) and sends a notification prompting the user that their break is over and the next focus round has begun.

3. **Cool Animations for the Pomodoro Studio**:
   - **Smooth Sweep & Glowing Radial Needle**: Animated progress sweep (`animateFloatAsState`) with a pulsing ember halo at the leading edge when the timer is running.
   - **Breathing Ambient Aura**: Subtle breathing radial coral/emerald aura behind the 3D dial synced to active flow or break mode.
   - **Live Soundscape Waveform**: 12-bar equalizer that smoothly oscillates when ambient sound or focus flow is active and settles gently when paused.
   - **Tactile Spring Press Physics**: Scale and elevation spring animations on the Start/Pause, Reset, Sound, and Mode pills.

4. **3D Tactile Depth System Across the App**:
   - **Deep 3D Dial on Focus Tab**: Multi-layer Canvas beveling, outer shadow projection, cream ring gradient, and recessed center well shadow.
   - **Subtle 3D Depth on Cards & Controls Across All Tabs**:
     - Create reusable 3D surface modifiers in `ui/theme/` (`tactileCard3D`, `tactileRecessedWell`, `tactilePillButton3D`) using top-edge specular highlight borders (`WarmCreamIvory` at low alpha), bottom-edge shadow borders, and layered elevation shadows across `SanctuaryHomeScreen`, `ShieldLimitsScreen`, `StudyRoomsScreen`, `AiCoachInsightsScreen`, and the bottom navigation bar.

5. **Multi-Provider Free-Tier AI Coach (Google Gemini, Groq, and OpenRouter)**:
   - Expand the AI layer (`GeminiAiService.kt` / `MultiProviderAiService.kt` and `SinglePromptAiCoach.kt`) to support:
     - **Groq (Free Tier API Keys)**: OpenAI-compatible endpoint (`https://api.groq.com/openai/v1/chat/completions`) using fast free-tier models (`llama-3.3-70b-versatile`, `llama-3.1-8b-instant`).
     - **OpenRouter (Free Tier API Keys)**: OpenAI-compatible endpoint (`https://openrouter.ai/api/v1/chat/completions`) using free-router models (`meta-llama/llama-3.3-70b-instruct:free`, `google/gemma-3-27b-it:free`, `deepseek/deepseek-chat-v3-0324:free`).
     - **Google Gemini**: Existing Gemini endpoint (`gemini-2.0-flash-lite`, `gemini-1.5-flash`).
   - Persist `aiProvider` (`GEMINI`, `GROQ`, `OPENROUTER`) and custom API key in `UserProgressEntity` / `RegainDatabase`.
   - Update the AI Settings dialog in `AiCoachInsightsScreen.kt` so users can pick their provider (`Groq Free Tier`, `OpenRouter Free Tier`, or `Google Gemini`), paste their API key, and use both **Daily Insights** and **Rega AI Chat** seamlessly without billing errors.

---

## 2. Planned File Changes
- `app/build.gradle.kts`: Bump `versionName` to `"1.3.2"` and `versionCode` to `5`.
- `app/src/main/java/com/example/data/RegainDatabase.kt`: Add `aiProvider` column (`GEMINI`, `GROQ`, `OPENROUTER`) and persist custom Pomodoro preferences (`focusMinutes`, `shortBreakMinutes`, `longBreakMinutes`, `totalRounds`) in `user_progress`.
- `app/src/main/java/com/example/notifications/RegainNotificationHelper.kt`: Add `sendPomodoroBreakNotification(...)` for automatic focus-to-break and break-to-focus round transitions.
- `app/src/main/java/com/example/ai/GeminiAiService.kt` & `SinglePromptAiCoach.kt`: Add multi-provider routing for `GEMINI`, `GROQ`, and `OPENROUTER` in both Chat and Daily Insight synthesis.
- `app/src/main/java/com/example/ui/RegainViewModel.kt`:
  - Add customizable Pomodoro state (`currentRound`, `totalRounds`, `customFocusMinutes`, `customShortBreakMinutes`, `customLongBreakMinutes`), interactive task checklist state (add/toggle/select task), prev/next ambient sound controls, and auto-advance round logic with break notifications.
  - Wire provider selection + API key persistence for the AI Coach.
- `app/src/main/java/com/example/ui/theme/Theme.kt`: Add reusable 3D tactile modifiers (`tactile3DCard`, `tactileRecessedWell`, `tactileRaisedPill`) for subtle 3D depth across all tabs.
- `app/src/main/java/com/example/ui/screens/FocusStudioScreen.kt`: Rebuild the Pomodoro dial, controls, soundscape player, task checklist, weekly matrix, and custom Pomodoro configuration modal to match `screen(1)(1).png` with deep 3D rendering and smooth animations.
- `app/src/main/java/com/example/ui/screens/AiCoachInsightsScreen.kt` & other screens: Add multi-provider selector dialog (`Groq`, `OpenRouter`, `Gemini`) and apply subtle 3D tactile card depth.
