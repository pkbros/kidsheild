# KidShield — Product Requirements Document (PRD)

**Version:** 1.5
**Date:** March 3, 2026
**Author:** Product Team
**Status:** Active — V1 (MVP) Complete, V1.5 In Development

---

## 1. Overview

### 1.1 Problem Statement

Young children (ages 5–10) are increasingly exposed to addictive apps — social media, short-form video, and games — leading to excessive screen time, reduced physical activity, and attention issues. Existing parental control solutions are either too complex, easily bypassed, or rely on simple PINs that children quickly learn. Worse, most solutions only **block** — they create friction without offering guidance, motivation, or alternatives. Parents themselves often model the very phone addiction they're trying to curb in their children.

### 1.2 Solution

**KidShield** is an Android app that combines **face-recognition-based app blocking** with **positive behavioral guidance** for the whole family. When a child attempts to open a blocked app, KidShield displays a friendly, character-driven overlay that not only restricts access but actively **motivates, teaches, and redirects** the child toward healthier activities. For parents, the app provides **usage insights and self-awareness** — surfacing their own screen time alongside their child's, making digital wellbeing a shared family effort rather than a one-sided restriction.

**Key differentiators:**
- **Not just a wall — a guide.** The blocking overlay features "Buddy," a relatable child mascot character who encourages, teaches, and suggests alternatives
- **Family-first design.** Tracks and surfaces *parent* screen time too — "Kids mirror what they see"
- **Positive reinforcement.** Screen-free streaks, daily report cards, and encouraging messages reward good habits
- **Impossible to bypass.** Face recognition + device admin + accessibility service make circumvention virtually impossible for young children

### 1.3 Target Audience

| Segment | Description |
|---------|-------------|
| **Primary** | Parents/guardians of children aged 5–10 |
| **Secondary** | The children themselves (active participants in building healthier habits) |
| **Tertiary** | The family unit as a whole (shared digital wellbeing) |

### 1.4 Platform & Distribution

- **Platform:** Android only (minimum SDK 26 / Android 8.0)
- **Distribution:** Sideloaded APK initially; Google Play Store in a later phase
- **Tech Stack:** Flutter (UI) + Native Android / Kotlin (system services via platform channels)

---

## 2. Goals & Success Metrics

### 2.1 V1 (MVP) Goals — ✅ COMPLETE

| # | Goal | Status |
|---|------|--------|
| G1 | Block access to parent-selected apps via face-verified overlay | ✅ Done |
| G2 | Run reliably in background without being killed by the OS | ✅ Done |
| G3 | Provide a simple, child-proof setup and management experience | ✅ Done |
| G4 | Achieve < 2 second face verification latency on mid-range devices | ✅ Done |

### 2.2 V1.5 Goals

| # | Goal |
|---|------|
| G5 | Transform the blocking overlay from a cold restriction into a warm, character-driven guidance experience |
| G6 | Track and surface usage statistics (block attempts, screen time) for both child and parent |
| G7 | Introduce positive reinforcement through screen-free streaks and encouraging messages |
| G8 | Provide parents with daily/weekly report cards showing family digital wellness trends |
| G9 | Gently surface parent's own screen time to promote family-wide awareness |

### 2.3 Success Metrics

| Metric | Target | Version |
|--------|--------|---------|
| Overlay trigger latency (app open → overlay visible) | < 500 ms | V1 ✅ |
| Face recognition accuracy | ≥ 97% true-positive, < 1% false-positive | V1 ✅ |
| Background service uptime | ≥ 99% (auto-restart on kill) | V1 ✅ |
| Setup completion rate | ≥ 90% of onboarding starts | V1 ✅ |
| Average screen-free streak length | > 2 hours | V1.5 |
| Daily blocked app attempt reduction (week-over-week) | ≥ 15% decrease | V1.5 |
| Parent engagement with report card | ≥ 3x/week views | V1.5 |
| Parent self-awareness feature opt-in rate | ≥ 50% | V1.5 |

---

## 3. User Personas

### 3.1 Parent (Admin User)

- Registers their face (and optionally other guardians' faces) during setup
- Selects which installed apps to block
- Verifies identity via face or fallback PIN to grant temporary app access
- Configures settings such as re-verification interval
- **V1.5:** Reviews daily/weekly report cards showing child's blocked app attempts and screen time trends
- **V1.5:** Optionally sees their own screen time surfaced alongside their child's — a gentle nudge toward modeling good behavior

### 3.2 Child (Active Participant)

- Uses the device normally for allowed apps
- Encounters a **friendly, character-driven** blocking overlay when opening a restricted app
- **V1.5:** Sees "Buddy" (mascot character) who offers encouragement, fun facts, activity suggestions, and moral lessons
- **V1.5:** Earns recognition for screen-free streaks — building positive habits rather than just hitting walls
- Cannot bypass, uninstall, or disable KidShield

---

## 4. Functional Requirements

### 4.1 Onboarding & Face Registration

| ID | Requirement | Priority |
|----|-------------|----------|
| F-01 | First-launch wizard guides the parent through full setup | P0 |
| F-01a | **Smart onboarding resume**: If the user has already completed certain steps (e.g., face registered, PIN set), the wizard detects this on launch and **skips to the first incomplete step** instead of restarting from the beginning | P0 |
| F-02 | Parent registers their face by capturing multiple angles (front, slight left, slight right) using the front camera | P0 |
| F-02a | Front camera images must have **EXIF rotation applied** before processing — `BitmapFactory.decodeByteArray()` does not apply EXIF rotation from JPEG, so rotation must be read via `ExifInterface` and applied via `Matrix` transformation | P0 |
| F-02b | **Skip option**: If faces are already registered, a "Skip" button is available in the AppBar to bypass re-registration | P0 |
| F-03 | Face embeddings generated via **MobileFaceNet (mobilefacenet.tflite)** and stored **locally on-device** (encrypted). Model outputs **192-dimensional** embeddings (not 128 as originally documented) | P0 |
| F-04 | Support registering **multiple authorized adults** (up to 4 faces) | P0 |
| F-05 | Parent sets a **fallback PIN** (6-digit minimum) during setup, used when face detection is unavailable (e.g., poor lighting, broken camera) | P0 |
| F-06 | Parent can re-register or delete faces from settings | P1 |

### 4.2 Permission Acquisition

| ID | Requirement | Priority |
|----|-------------|----------|
| F-07 | App requests and guides the user to grant the following permissions sequentially with clear explanation screens: | P0 |
| | — **Accessibility Service** (to detect foreground app changes) | P0 |
| | — **Display Over Other Apps** (SYSTEM_ALERT_WINDOW, for blocking overlay) | P0 |
| | — **Device Administrator** (to prevent uninstall) | P0 |
| | — **Camera** (for face recognition) | P0 |
| | — **Usage Stats Access** (PACKAGE_USAGE_STATS, backup detection) | P1 |
| | — **Ignore Battery Optimizations** (to prevent background kill) | P0 |
| | — **Notification Permission** (for persistent foreground service notification) | P0 |
| F-07a | **Battery optimization intent** must use `FLAG_ACTIVITY_NEW_TASK` when launched from a non-Activity context (e.g., platform channel handler), otherwise it crashes on some Android versions | P0 |
| F-07b | **Skip option**: A "Skip for now" button with confirmation dialog allows the parent to proceed without granting all permissions (with a warning that protection won't be fully active) | P0 |
| F-07c | **Restricted settings guidance (Android 13+)**: On sideloaded installs, Android 13+ blocks accessibility service grants due to "restricted settings." The permission screen includes instructions to enable via App Info → "Allow restricted settings" | P0 |
| F-08 | If any critical permission is missing, show a persistent banner in-app and block activation of the protection service | P0 |
| F-09 | Provide a "permission health check" screen showing the status of each permission with quick-fix buttons | P1 |

### 4.3 App Selection (Block List)

| ID | Requirement | Priority |
|----|-------------|----------|
| F-10 | Display a scrollable list of **all installed apps** (excluding system-critical apps like Settings, Phone, Messages) with icons and names | P0 |
| F-11 | Parent toggles apps ON/OFF to mark them as blocked | P0 |
| F-12 | Provide a **search bar** to filter apps by name | P1 |
| F-13 | Show a count of currently blocked apps on the main dashboard | P1 |
| F-14 | Persist the block list locally using SharedPreferences or local database | P0 |
| F-15 | Newly installed apps default to **unblocked**; parent is notified of new installs to review | P2 |

### 4.4 Background Monitoring Service

| ID | Requirement | Priority |
|----|-------------|----------|
| F-16 | A **foreground service** (Kotlin) runs persistently in the background, showing a non-dismissible notification | P0 |
| F-17 | The service uses the **Accessibility Service API** to detect when a blocked app's activity moves to the foreground | P0 |
| F-18 | On detection of a blocked app, the service **immediately launches the overlay activity** (< 500 ms) | P0 |
| F-18a | **Overlay stacking prevention**: The service maintains an `overlayActive` flag. A new overlay is **never launched** while one is already displayed. This prevents the verification loop where a successful dismiss immediately re-triggers the overlay because the blocked app briefly re-enters foreground | P0 |
| F-18b | **Overlay dismissed callback**: When the overlay is dismissed (via verification, PIN, or navigation), it calls `onOverlayDismissed()` on the accessibility service to clear the `overlayActive` flag and record a grace-period timestamp | P0 |
| F-18c | **Periodic re-check timer**: After the overlay is dismissed and the blocked app is allowed, a `Handler`-based recheck timer periodically verifies the blocked app is still in the foreground. If the user leaves the app, the timer stops. If the re-verification interval expires while the app is still active, the overlay re-appears | P0 |
| F-18d | **Grace period for "every time" mode**: When the re-verification interval is set to 0 (every time), a **3-second grace period** is enforced after each successful verification to prevent immediate re-triggering | P0 |
| F-19 | The service **auto-restarts** if killed by the OS (via START_STICKY, WorkManager periodic health check, and a broadcast receiver on BOOT_COMPLETED) | P0 |
| F-20 | The service maintains a flag/timestamp for recently verified sessions to avoid re-prompting within the configured interval | P0 |

### 4.5 Blocking Overlay & Face Verification

| ID | Requirement | Priority |
|----|-------------|----------|
| F-21 | Overlay is a **full-screen, system-level activity** displayed on top of the blocked app | P0 |
| F-22 | Overlay shows: app icon, app name, message ("This app is restricted. Parent verification required."), and a "Verify" button | P0 |
| F-23 | Pressing "Verify" activates the front camera and performs **real-time face detection + recognition** using MobileFaceNet | P0 |
| F-24 | If the detected face matches any registered parent/guardian (cosine similarity ≥ threshold), the overlay **dismisses** and the blocked app is allowed to continue | P0 |
| F-25 | If verification **fails** (face not recognized, no face detected within 10 seconds, or 3 failed attempts), overlay remains and shows a "Use PIN" option | P0 |
| F-26 | The overlay **cannot be dismissed** by pressing Back, Home, or Recent Apps — it re-appears immediately | P0 |
| F-26a | Overlay uses `imageProxy.imageInfo.rotationDegrees` from CameraX to correctly orient camera frames for face detection, regardless of device sensor orientation | P0 |
| F-27 | If the user navigates away (Home button), the blocked app is sent to background and is not accessible; overlay clears | P0 |
| F-28 | After successful verification, the blocked app is **unlocked for a configurable interval** (default: 1800 seconds / 30 minutes) before re-prompting | P0 |
| F-29 | PIN fallback: 6+ digit PIN entry screen shown as alternative to face verification | P0 |

### 4.6 Settings & Configuration

| ID | Requirement | Priority |
|----|-------------|----------|
| F-30 | Settings screen is **protected** — requires face verification or PIN to access | P0 |
| F-31 | **Re-verification interval**: Configurable time (in seconds) after which the overlay re-appears for a previously unlocked app. Options: 0 (every time) / 30s / 60s / 120s / 300s / 900s / 1800s / 3600s, or custom value | P0 |
| F-32 | **Manage blocked apps**: Navigate to the app selection screen (F-10) | P0 |
| F-33 | **Manage faces**: Add, preview, or delete registered faces | P0 |
| F-34 | **Change PIN**: Requires current PIN or face verification first | P1 |
| F-35 | **Enable/Disable protection**: Master toggle to pause all blocking (requires verification) | P1 |
| F-36 | **Debug/Testing mode**: A hidden developer toggle (accessible via 7-tap on version number) that sets the re-verification interval to custom values (e.g., 10 seconds) for testing | P1 |

### 4.7 Mascot Character & Guidance System (V1.5)

| ID | Requirement | Priority | Status |
|----|-------------|----------|--------|
| F-37 | Introduce **"Buddy"** — a relatable human child mascot character that appears on the blocking overlay instead of a cold restriction message | P0 | ✅ V1.5a |
| F-38 | ~~Buddy is displayed as a static illustration with rotating text speech bubbles~~ → **Replaced by F-38a** | P0 | ✅ → Superseded |
| F-38a | The blocking overlay plays a **full-screen looping video** (9:16 format) randomly selected from a bundled library of 3 videos. A **semi-transparent text bubble** from the content library is overlaid on top of the video | P0 | V1.5b |
| F-39 | Maintain a **local content library** of 80–120 pre-written messages across the following categories: | P0 | ✅ V1.5a |
| | — **Motivational** (e.g., "You're stronger than any screen! Go be awesome!") | P0 | ✅ |
| | — **Activity Suggestions** (e.g., "How about drawing something cool right now?") | P0 | ✅ |
| | — **Fun Facts / Did You Know** (e.g., "Did you know octopuses have 3 hearts?") | P0 | ✅ |
| | — **Moral Micro-Lessons** (e.g., "Being kind to someone today makes the world better.") | P0 | ✅ |
| | — **Gentle Humor** (e.g., "Even superheroes take breaks from screens!") | P0 | ✅ |
| F-40 | Messages are selected **randomly without immediate repeats** (shuffle-bag algorithm) so the child sees variety | P0 | ✅ V1.5a |
| F-41 | Messages are **time-of-day aware** where appropriate: morning messages suggest active play, evening messages suggest reading/winding down, weekend messages suggest family activities | P1 | ✅ V1.5a |
| F-42 | ~~The overlay layout changes to warm child-friendly design featuring Buddy prominently~~ → **Replaced by F-42a** | P0 | ✅ → Superseded |
| F-42a | The overlay is a **full-screen video player** with verification controls (face verify + PIN + go back) rendered as **small, semi-transparent buttons pinned to the bottom** of the screen over a gradient scrim. The blocked app name is shown subtly at the top | P0 | V1.5b |
| F-43 | ~~Buddy's illustration has multiple poses/expressions~~ → **Replaced by F-43a** | P1 | ✅ → Superseded |
| F-43a | On each overlay appearance, one of **3 bundled videos** is selected at random (no immediate repeat of the same video) | P0 | V1.5b |
| F-44 | The content library is stored as a **bundled JSON asset** — no internet required, fully offline | P0 | ✅ V1.5a |
| F-44a | Videos are stored as **bundled raw assets** in the APK — no internet required, fully offline | P0 | V1.5b |
| F-45 | Parents can optionally **disable** the video overlay in settings (reverting to static Buddy or classic overlay) | P2 | ✅ V1.5a |
| F-45a | Video overlay is **muted by default** (no audio playback) to avoid surprises in public/school settings | P0 | V1.5b |
| F-45b | When face verification camera activates, the video **pauses** and the camera preview replaces the video area. On retry/cancel the video **resumes** | P0 | V1.5b |

### 4.8 Usage Statistics & Tracking (V1.5)

| ID | Requirement | Priority |
|----|-------------|----------|
| F-46 | Track **block events**: each time a blocked app triggers the overlay, record {app_package, timestamp, outcome (face_verified / pin_verified / navigated_away)} | P0 |
| F-47 | Track **daily screen time** (total device unlock duration) using UsageStatsManager API | P0 |
| F-48 | Track **per-app usage time** for blocked apps (time between successful verification and next lock/app switch) | P1 |
| F-49 | Calculate and persist **daily aggregates**: total block attempts, total verified accesses, total screen time, most-attempted app | P0 |
| F-50 | Store up to **90 days** of historical usage data locally | P1 |
| F-51 | All tracking data stored **locally only** — no cloud upload, no analytics SDK | P0 |

### 4.9 Screen-Free Streaks & Positive Reinforcement (V1.5)

| ID | Requirement | Priority |
|----|-------------|----------|
| F-52 | Track **screen-free streaks**: continuous duration where no blocked app was opened | P0 |
| F-53 | Show the **current streak** prominently on the parent dashboard (e.g., "2h 15m without blocked apps") | P0 |
| F-54 | Send an **encouraging notification** when streak milestones are hit: 1 hour, 2 hours, 4 hours, "all day" | P0 |
| F-55 | Buddy character appears in streak notifications with a celebratory message | P1 |
| F-56 | Track and display **longest streak ever** and **today's best streak** on the dashboard | P1 |
| F-57 | Maintain a **weekly streak summary** (average daily best streak) for trend visibility | P2 |

### 4.10 Parent Dashboard & Report Card (V1.5)

| ID | Requirement | Priority |
|----|-------------|----------|
| F-58 | Redesign the dashboard to show a **daily report card** with: blocked app attempts (count), successful verifications, total screen time, current streak, and trend indicators (↑↓ vs. yesterday) | P0 |
| F-59 | Show a **"Top Blocked Apps"** section listing the most-attempted blocked apps with attempt counts | P0 |
| F-60 | Provide a **weekly summary view** accessible from the dashboard: bar chart or simple visual showing daily block attempts over the past 7 days | P1 |
| F-61 | Show Buddy on the dashboard with a **contextual status message**: "Great day! Only 3 attempts" or "Tough day — 15 attempts, but we'll do better tomorrow!" | P1 |
| F-62 | **Nudge notifications** for parents: "Your child tried to open TikTok 8 times in the last hour" (configurable threshold) | P1 |

### 4.11 Parent Self-Awareness (V1.5)

| ID | Requirement | Priority |
|----|-------------|----------|
| F-63 | **Opt-in feature** (enabled during setup or in settings): track and display the parent's own total daily screen time | P0 |
| F-64 | Show parent screen time on the dashboard alongside the child's metrics: "You: 3h 20m · Child: 1h 45m" | P0 |
| F-65 | Provide a **gentle, non-judgmental insight** when parent screen time is high: "You've been on for 4+ hours today. Kids learn by watching!" (shown as a Buddy tip, not a nag) | P1 |
| F-66 | Allow parent to **set a personal daily screen time goal** (optional) and track progress toward it | P2 |
| F-67 | Include parent's screen time in the **weekly summary** for family-wide trend visibility | P1 |
| F-68 | Parent self-tracking can be **disabled at any time** from settings with no data loss (data is retained but hidden) | P0 |

---

## 5. Non-Functional Requirements

| ID | Requirement | Priority | Version |
|----|-------------|----------|---------|
| NF-01 | **Performance:** Face recognition inference must complete in < 1.5 seconds on devices with Snapdragon 600-series or equivalent | P0 | V1 ✅ |
| NF-02 | **Battery:** Background service must consume < 3% battery per hour during idle monitoring | P0 | V1 ✅ |
| NF-03 | **Storage:** Face embeddings + app data must use < 20 MB total (excluding usage history DB) | P1 | V1 ✅ |
| NF-04 | **Privacy:** All face data stored locally on-device only; no cloud upload; embeddings stored (not raw images) | P0 | V1 ✅ |
| NF-05 | **Security:** PIN and face embeddings encrypted at rest (AES-256 via Android Keystore) | P0 | V1 |
| NF-06 | **Reliability:** Service must survive device reboot, app force-stop attempts, and battery optimization | P0 | V1 ✅ |
| NF-07 | **Compatibility:** Support Android 8.0 (API 26) through Android 15 (API 35). Note: Android 13+ (API 33) restricts sideloaded apps from granting accessibility service — requires user to manually "Allow restricted settings" from App Info | P0 | V1 ✅ |
| NF-08 | **Offline:** App must work 100% offline — no internet required (including mascot content and usage tracking) | P0 | V1 ✅ |
| NF-08a | **Release build resilience:** R8/ProGuard minification must preserve Gson TypeToken generics, TFLite interpreter classes, ML Kit face detection, CameraX, and Play Core deferred component classes. A comprehensive `proguard-rules.pro` is required. App initialization (`AppState.initialize()`) must be wrapped in try/catch/finally to prevent the app from getting stuck on a loading screen if any platform channel call fails | P0 | V1 ✅ |
| NF-09 | **Content freshness:** Mascot message library must feel non-repetitive for at least 2 weeks of daily use (minimum 80 unique messages) | P0 | V1.5 ✅ |
| NF-09a | **APK size:** Video assets should be compressed (H.264, medium bitrate) to keep total APK size under 200 MB. Videos should be ≤ 30 seconds each to limit storage impact | P1 | V1.5b |
| NF-10 | **Usage data storage:** 90 days of usage history must use < 5 MB storage | P1 | V1.5 |
| NF-11 | **Dashboard load time:** Report card and stats must render in < 1 second | P1 | V1.5 |
| NF-12 | **Streak accuracy:** Screen-free streak tracking must be accurate within ±30 seconds | P0 | V1.5 |

---

## 6. Technical Architecture

### 6.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Flutter UI Layer                       │
│  ┌──────────┐  ┌──────────────┐  ┌────────────────────┐ │
│  │ Onboarding│  │  Dashboard   │  │  Settings Screen   │ │
│  │  Wizard   │  │ + Report Card│  │                    │ │
│  └──────────┘  └──────────────┘  └────────────────────┘ │
│  ┌──────────────────────┐  ┌───────────────────────────┐ │
│  │  Face Registration   │  │   App Selection List      │ │
│  │    Camera Screen     │  │                           │ │
│  └──────────────────────┘  └───────────────────────────┘ │
│  ┌──────────────────────┐  ┌───────────────────────────┐ │
│  │  Weekly Summary View │  │  Parent Self-Awareness    │ │
│  │  (Stats + Trends)    │  │  (Screen Time Tracker)    │ │
│  └──────────────────────┘  └───────────────────────────┘ │
├─────────────────────────────────────────────────────────┤
│              Platform Channels (MethodChannel)           │
├─────────────────────────────────────────────────────────┤
│              Native Android Layer (Kotlin)               │
│  ┌───────────────────────────────────────────────────┐  │
│  │         Accessibility Service                      │  │
│  │   (Foreground app detection + event tracking)      │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │        Foreground Service                          │  │
│  │   (Persistent monitoring + streak tracking)        │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │       Overlay Activity / Window                    │  │
│  │   (Buddy mascot + face verify + guidance content)  │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │       Face Recognition Engine                      │  │
│  │  ┌─────────────┐    ┌──────────────────────┐      │  │
│  │  │ ML Kit Face │    │  MobileFaceNet TFLite │      │  │
│  │  │  Detection  │    │  (Embedding Extract)  │      │  │
│  │  └─────────────┘    └──────────────────────┘      │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │       Usage Tracking Engine                        │  │
│  │   (Block events, screen time, streaks, aggregates) │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │       Device Admin Receiver                        │  │
│  │   (Uninstall protection + PIN challenge)           │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 6.2 Key Technical Components

| Component | Technology | Purpose | Version |
|-----------|-----------|---------|---------|
| UI Framework | Flutter 3.x + Dart | All user-facing screens | V1 ✅ |
| Platform Bridge | MethodChannel / EventChannel | Flutter ↔ Kotlin communication | V1 ✅ |
| Accessibility Service | Android AccessibilityService (Kotlin) | Detect foreground app changes + log block events | V1 ✅ |
| Background Service | Android Foreground Service (Kotlin) | Keep monitoring alive + streak tracking | V1 ✅ |
| Overlay | System Alert Window / Activity (TYPE_APPLICATION_OVERLAY) | Block screen with Buddy mascot | V1 ✅ / V1.5 |
| Face Detection | Google ML Kit Face Detection | Locate face in camera frame | V1 ✅ |
| Face Recognition | MobileFaceNet (mobilefacenet.tflite) via TFLite Interpreter | Generate 192-d face embedding | V1 ✅ |
| Face Matching | Cosine Similarity (Kotlin) | Compare embeddings (threshold ~0.6–0.7) | V1 ✅ |
| Local Storage | SharedPreferences + Encrypted File | Block list, embeddings, settings | V1 ✅ |
| Auto-restart | BOOT_COMPLETED receiver + WorkManager | Service resilience | V1 ✅ |
| Uninstall Protection | DeviceAdminReceiver + PIN challenge | Prevent app removal | V1 ✅ |
| Mascot Content | Bundled JSON asset | Buddy's message library (80-120 messages) | V1.5 |
| Usage Database | SQLite / Drift (local) | Block events, screen time, streaks, daily aggregates | V1.5 |
| Screen Time Tracking | UsageStatsManager API | Device-wide screen time for parent & child context | V1.5 |
| Streak Engine | Kotlin (in-memory + persisted) | Track continuous screen-free periods | V1.5 |
| Notification Engine | Android NotificationManager | Streak milestones, parent nudges | V1.5 |

### 6.3 Face Recognition Pipeline

```
Camera Frame (CameraX)
        │
        ▼
ML Kit Face Detection
  (locate face bounding box)
        │
        ▼
Crop & Preprocess
  (resize to 112x112, normalize, apply EXIF rotation)
        │
        ▼
MobileFaceNet TFLite Inference
  (output: 192-dimensional embedding)
        │
        ▼
Cosine Similarity vs. stored embeddings
        │
        ▼
  similarity ≥ threshold? ──Yes──▶ GRANT ACCESS
        │                          (log: face_verified)
       No
        ▼
  DENY / Prompt PIN
  (log: face_failed → pin_verified / navigated_away)
```

### 6.4 App Detection Flow

```
Accessibility Service
  onAccessibilityEvent(TYPE_WINDOW_STATE_CHANGED)
        │
        ▼
Extract package name from event
        │
        ▼
Check against blocked apps list
        │
   Not blocked ──▶ Ignore
        │              │
     Blocked           ▼
        │         Update screen-free
        ▼         streak (no reset)
Check last verification timestamp for this app
        │
   Within interval ──▶ Allow (already verified)
        │
   Expired / Never verified
        │
        ▼
Log block event {app, timestamp}
Reset screen-free streak
        │
        ▼
Launch Blocking Overlay (with Buddy)
```

### 6.5 Buddy Content Selection Flow (V1.5)

```
Overlay triggered for blocked app
        │
        ▼
Get current time-of-day bucket
  (morning / afternoon / evening / night)
        │
        ▼
Select content category (weighted random):
  ├── Motivational (25%)
  ├── Activity Suggestion (30%) ← time-aware
  ├── Fun Fact (20%)
  ├── Moral Lesson (15%)
  └── Humor (10%)
        │
        ▼
Pick message from category
  (shuffle-bag: no immediate repeats)
        │
        ▼
Select Buddy pose (rotate through 4 poses)
        │
        ▼
Select video (random, no immediate repeat of same)
        │
        ▼
Render overlay:
  ┌──────────────────────────┐
  │                          │
  │   [Full-screen looping   │
  │    video background]     │
  │                          │
  │                          │
  │  ┌────────────────────┐  │
  │  │ 💬 "Did you know   │  │
  │  │ dolphins sleep with│  │
  │  │ one eye open?"     │  │
  │  └────────────────────┘  │
  │                          │
  │   ─── YouTube ───        │
  │                          │
  │  ▓▓▓▓▓▓▓ gradient ▓▓▓▓▓ │
  │  [🔐 Verify] [PIN] [←]  │
  └──────────────────────────┘
```

### 6.6 Usage Tracking Architecture (V1.5)

```
┌─────────────────────────────────────────┐
│         Event Sources                    │
│  ┌─────────────┐  ┌──────────────────┐  │
│  │Accessibility │  │ UsageStatsManager│  │
│  │  Service     │  │  (Screen Time)   │  │
│  │(block events)│  │                  │  │
│  └──────┬──────┘  └────────┬─────────┘  │
│         │                  │             │
│         ▼                  ▼             │
│  ┌──────────────────────────────────┐   │
│  │     Usage Tracking Engine        │   │
│  │  ┌────────┐ ┌────────┐ ┌──────┐ │   │
│  │  │ Events │ │Streaks │ │Agg.  │ │   │
│  │  │ Logger │ │ Tracker│ │Engine│ │   │
│  │  └────────┘ └────────┘ └──────┘ │   │
│  └──────────────┬───────────────────┘   │
│                 │                        │
│                 ▼                        │
│  ┌──────────────────────────────────┐   │
│  │     Local SQLite Database        │   │
│  │  ├── block_events table          │   │
│  │  ├── daily_aggregates table      │   │
│  │  ├── streaks table               │   │
│  │  └── screen_time table           │   │
│  └──────────────────────────────────┘   │
│                 │                        │
│                 ▼                        │
│  ┌──────────────────────────────────┐   │
│  │     Flutter Dashboard            │   │
│  │  (Report Card + Weekly Summary)  │   │
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

---

## 7. User Flows

### 7.1 First-Time Setup Flow

```
Install App
    │
    ▼
Welcome Screen ("Protect your child's screen time")
    │
    ▼
Face Registration (capture parent's face from 3 angles)
    │
    ▼
Add Additional Guardians? (optional, up to 3 more)
    │
    ▼
Set Fallback PIN (minimum 6 digits)
    │
    ▼
Grant Permissions (step-by-step guided flow)
  ├── Accessibility Service
  ├── Display Over Other Apps
  ├── Device Administrator
  ├── Camera
  ├── Battery Optimization Exemption
  └── Notification
    │
    ▼
Select Apps to Block (scrollable list of installed apps)
    │
    ▼
Setup Complete → Dashboard
    │
    ▼
Background Service Starts
```

### 7.2 Child Opens Blocked App (V1.5 — with Buddy)

```
Child taps on YouTube (blocked)
    │
    ▼
Accessibility Service detects YouTube in foreground
    │
    ▼
Log block event {youtube, timestamp, pending}
Reset screen-free streak
    │
    ▼
Check: Was YouTube verified in the last [interval]?
    │
   No ──▶ Launch full-screen overlay
    │          │
    │          ▼
    │     Buddy appears with a message:
    │     "How about building something with LEGO instead? 🧱"
    │     ─────────────────────
    │     YouTube is restricted
    │     [Verify with Face] [Use PIN]
    │          │
    │     ┌────┴────────────┐
    │     │                 │
    │   Face Match        No Match
    │     │              (3 attempts)
    │     ▼                 │
    │   Log: face_verified  ▼
    │   Dismiss overlay   Show PIN option
    │   Start timer         │
    │     │            ┌────┴────┐
    │     ▼            │         │
    │   YouTube      PIN OK    PIN Fail
    │   opens          │         │
    │              Log: pin_   Remain
    │              verified   blocked
    │               Dismiss    │
    │               overlay  Log: navigated_away
    │                        (if user goes Home)
    │
   Yes ──▶ Allow through (no overlay)
```

### 7.3 Parent Views Report Card (V1.5)

```
Parent opens KidShield app
    │
    ▼
Dashboard shows:
  ┌──────────────────────────────────────┐
  │ 🛡️ Protection: ON                   │
  │                                      │
  │ TODAY'S REPORT CARD                  │
  │ ├── Block attempts: 12 (↓ vs. yesterday)
  │ ├── Verified access: 4              │
  │ ├── Screen time: 2h 15m            │
  │ ├── Best streak: 3h 40m            │
  │ └── Most attempted: YouTube (7x)   │
  │                                      │
  │ YOUR SCREEN TIME: 3h 20m           │
  │ "Kids learn by watching! 📱"        │
  │                                      │
  │ Buddy says: "Good progress today!   │
  │ Only 12 attempts — down from 18!"   │
  │                                      │
  │ [Weekly Summary] [Manage Apps]       │
  │ [Settings]                           │
  └──────────────────────────────────────┘
```

### 7.4 Parent Accesses Settings

```
Parent opens KidShield app
    │
    ▼
Dashboard shows status + blocked app count
    │
    ▼
Taps "Settings"
    │
    ▼
Face verification or PIN required
    │
    ▼
Settings menu:
  ├── Manage Blocked Apps
  ├── Manage Faces
  ├── Change PIN
  ├── Re-verification Interval
  └── Enable/Disable Protection
```

---

## 8. Screen Inventory

| # | Screen | Description | Version |
|---|--------|-------------|---------|
| S-01 | **Welcome / Splash** | App logo, tagline, "Get Started" button | V1 ✅ |
| S-02 | **Face Registration** | Camera viewfinder with face alignment guide, capture button, progress indicator (3 captures) | V1 ✅ |
| S-03 | **Add More Guardians** | Option to register additional faces or skip | V1 ✅ |
| S-04 | **PIN Setup** | 6-digit PIN entry with confirmation | V1 ✅ |
| S-05 | **Permission Guide** | Step-by-step permission granting with illustrations and "Grant" buttons | V1 ✅ |
| S-06 | **App Selection** | List of installed apps with toggle switches, search bar, "Done" button | V1 ✅ |
| S-07 | **Dashboard + Report Card** | Protection status, blocked app count, daily report card (attempts, screen time, streaks, trends), parent screen time, Buddy status message | V1 ✅ / V1.5 |
| S-08 | **Blocking Overlay (Buddy)** | Full-screen overlay featuring Buddy mascot with speech bubble, app restriction info, "Verify" and "Go Back" buttons | V1 ✅ → V1.5 redesign |
| S-09 | **Face Verification** | Camera viewfinder with real-time face detection feedback, countdown timer | V1 ✅ |
| S-10 | **PIN Entry** | 6-digit PIN input, "Forgot PIN" info text | V1 ✅ |
| S-11 | **Settings** | List of configurable options + parent self-awareness toggle + mascot toggle | V1 ✅ / V1.5 |
| S-12 | **Manage Faces** | Grid/list of registered faces with add/delete options | V1 ✅ |
| S-13 | **Interval Config** | Text input for seconds + preset chips | V1 ✅ |
| S-14 | **Weekly Summary** | 7-day view of daily block attempts (bar chart or list), screen time trends, streak records | V1.5 |
| S-15 | **Streak Notification** | System notification with Buddy character celebrating streak milestones | V1.5 |
| S-16 | **Parent Nudge Notification** | System notification alerting parent of high block-attempt activity | V1.5 |

---

## 9. Data Model

### 9.1 Local Storage Schema

```
KidShieldData/
├── face_embeddings.enc          # AES-256 encrypted file
│   ├── parent_1: {name, embedding[192], created_at}
│   ├── parent_2: {name, embedding[192], created_at}
│   ├── parent_3: {name, embedding[192], created_at}
│   └── parent_4: {name, embedding[192], created_at}
│
├── preferences (SharedPreferences)
│   ├── pin_hash: String              # SHA-256 hash of PIN
│   ├── is_protection_enabled: Boolean
│   ├── reverification_interval_seconds: Int (default: 1800)
│   ├── is_setup_complete: Boolean
│   ├── debug_mode: Boolean
│   ├── mascot_enabled: Boolean       # V1.5 (default: true)
│   ├── parent_tracking_enabled: Boolean  # V1.5 (default: false, opt-in)
│   ├── parent_screen_time_goal_minutes: Int  # V1.5 (optional)
│   └── nudge_threshold: Int          # V1.5 (default: 5 attempts/hour)
│
├── blocked_apps (SharedPreferences / JSON)
│   └── List<String>             # Package names (e.g., "com.google.youtube")
│
├── verification_sessions (in-memory + SharedPreferences)
│   └── Map<String, Long>        # {package_name: last_verified_timestamp}
│
├── buddy_content.json (bundled asset)        # V1.5
│   └── categories:
│       ├── motivational: [{id, message, time_of_day: "any"}]
│       ├── activity: [{id, message, time_of_day: "morning|afternoon|evening"}]
│       ├── fun_fact: [{id, message, time_of_day: "any"}]
│       ├── moral: [{id, message, time_of_day: "any"}]
│       └── humor: [{id, message, time_of_day: "any"}]
│
├── usage.db (SQLite)                         # V1.5
│   ├── block_events table
│   │   ├── id: INTEGER PRIMARY KEY
│   │   ├── app_package: TEXT
│   │   ├── timestamp: INTEGER (epoch ms)
│   │   └── outcome: TEXT (face_verified / pin_verified / navigated_away)
│   │
│   ├── daily_aggregates table
│   │   ├── date: TEXT (YYYY-MM-DD) PRIMARY KEY
│   │   ├── total_block_attempts: INTEGER
│   │   ├── total_verified: INTEGER
│   │   ├── total_screen_time_minutes: INTEGER
│   │   ├── parent_screen_time_minutes: INTEGER (nullable)
│   │   ├── best_streak_minutes: INTEGER
│   │   └── most_attempted_app: TEXT
│   │
│   ├── streaks table
│   │   ├── id: INTEGER PRIMARY KEY
│   │   ├── start_time: INTEGER (epoch ms)
│   │   ├── end_time: INTEGER (epoch ms, null if active)
│   │   └── duration_minutes: INTEGER
│   │
│   └── screen_time table
│       ├── date: TEXT (YYYY-MM-DD)
│       ├── context: TEXT (child / parent)
│       └── total_minutes: INTEGER
│
└── buddy_state (SharedPreferences)            # V1.5
    ├── last_shown_messages: List<Int>    # IDs of recently shown (prevent repeats)
    ├── current_pose_index: Int
    └── messages_shown_count: Int
```

---

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation | Version |
|------|-----------|--------|------------|---------|
| Android OS kills background service | High | Critical | Foreground service + WorkManager + BOOT_COMPLETED + battery optimization exemption | V1 ✅ |
| Child holds photo of parent to camera | Medium | High | ML Kit liveness detection (blink/head movement) — **V2 feature** | V2 |
| Google Play rejects due to Accessibility Service usage | Medium | High | Clearly document accessibility use; comply with Google's Accessibility API policy; defer Play Store to V2 | V2 |
| Face recognition fails in poor lighting | Medium | Medium | PIN fallback always available; guide user to improve lighting | V1 ✅ |
| Device Admin prevents normal uninstall by parent | Low | Medium | PIN challenge on admin disable; clear in-app "Remove Protection" flow | V1 ✅ |
| MobileFaceNet accuracy varies across ethnicities | Medium | Medium | Test broadly; allow cosine similarity threshold tuning in debug mode | V1 |
| Child factory-resets device | Low | High | Out of scope for MVP; document as known limitation | — |
| Buddy content feels repetitive after extended use | Medium | Medium | Minimum 80 unique messages across 5 categories; shuffle-bag prevents immediate repeats; expand library over time | V1.5 |
| Video assets inflate APK size significantly | High | Medium | Compress videos (H.264, 1-2 Mbps, ≤30s each). Target < 200 MB total APK. Consider AAB + asset packs if Play Store distribution is pursued in V2 | V1.5b |
| Accessibility service silently stops working | High | Critical | R-11 fix: overlay-active staleness timeout, self-sustaining recheck timer, foreground service watchdog ping. Documented in Rectifications | V1.5b |
| UsageStatsManager permission denied or unavailable | Medium | Low | Graceful degradation — screen time shows "unavailable" but all other features work. Block event tracking uses accessibility service (already granted) | V1.5 |
| Parent finds self-tracking intrusive or naggy | Low | Medium | Strictly opt-in; gentle/humorous tone; easy to disable anytime; no guilt-tripping language | V1.5 |
| Usage database grows too large | Low | Low | Auto-prune data older than 90 days; daily aggregates are compact (~100 bytes/day) | V1.5 |

---

## 11. V1 Rectifications & Lessons Learned

During V1 development and testing, several critical bugs were discovered and resolved. These are documented here for engineering context and to prevent regression.

| # | Issue | Root Cause | Fix Applied | Affected Files |
|---|-------|-----------|-------------|----------------|
| R-01 | **Face detection silently failing** — front camera images processed by ML Kit returned no faces | `BitmapFactory.decodeByteArray()` does not apply EXIF rotation from JPEG data. Front camera images were sideways/upside-down, making faces undetectable | Added `ExifInterface` parsing + `Matrix` rotation in `PlatformChannelHandler.decodeBitmapWithExifRotation()`. Overlay uses `imageProxy.imageInfo.rotationDegrees` for CameraX frames | PlatformChannelHandler.kt, BlockingOverlayActivity.kt |
| R-02 | **Embedding extraction crash** — TFLite output buffer shape mismatch `[1,192]` vs `[1,128]` | MobileFaceNet model actually outputs 192-dimensional embeddings, not 128 as documented in the original architecture | Changed `EMBEDDING_SIZE` constant from 128 to 192 throughout | FaceRecognitionEngine.kt |
| R-03 | **Battery optimization permission crash** — app crashed when requesting battery optimization exemption | `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent was missing `FLAG_ACTIVITY_NEW_TASK` when launched from a non-Activity context (platform channel handler) | Added `Intent.FLAG_ACTIVITY_NEW_TASK` to the battery optimization intent | PlatformChannelHandler.kt |
| R-04 | **Onboarding always restarts** — every app launch showed face registration even if already completed | Welcome screen was a stateless widget that always navigated to step 1. No check for existing faces or PIN | Converted to StatefulWidget. `initState()` checks existing faces/PIN via platform channel and skips to the first incomplete onboarding step | welcome_screen.dart |
| R-05 | **Verification loop** — after successful face verify, overlay immediately re-appeared for the same app | On overlay dismiss, `resetLastDetected()` cleared the tracked package, causing the accessibility service to re-detect the same blocked app in foreground and re-trigger the overlay | Replaced `resetLastDetected()` with `overlayActive` flag + `onOverlayDismissed()` callback pattern. Added 3-second grace period for "every time" mode. Added `Handler`-based recheck timer instead of immediate re-detection | KidShieldAccessibilityService.kt, BlockingOverlayActivity.kt |
| R-06 | **UI overflow on PIN screen** — keyboard pushed content off-screen | PIN entry screen used a fixed layout without scroll wrapping | Wrapped in `SafeArea` + `SingleChildScrollView` | pin_setup_screen.dart |
| R-07 | **UI overflow on guardians screen** — buttons pushed off-screen with long face list | Layout used `Spacer()` between content and buttons. On small screens with multiple registered faces, this caused RenderFlex overflow | Replaced `Spacer()` with scrollable content area + pinned bottom buttons using `Column` + `Expanded` + `SingleChildScrollView` | add_guardians_screen.dart |
| R-08 | **Release build stuck on loading** — app showed infinite loading spinner in release mode | R8 minification stripped Gson `TypeToken` generics, TFLite interpreter classes, and ML Kit classes. Platform channel calls threw exceptions silently. `AppState.initialize()` had no error handling, so `isLoading` stayed `true` forever | Created comprehensive `proguard-rules.pro` with keep rules for Gson, TFLite, ML Kit, CameraX, Flutter, Play Core. Wrapped `AppState.initialize()` in try/catch/finally so loading completes even on failure | proguard-rules.pro, build.gradle.kts, app_state.dart |
| R-09 | **Accessibility service ungrantable on Android 13+** — sideloaded APK could not enable accessibility service | Android 13 (API 33) introduced "restricted settings" that blocks sideloaded apps from granting accessibility/device admin permissions | Added instructional text on the permission screen guiding users to App Info → "Allow restricted settings." This is an OS-level limitation with no programmatic workaround | permission_screen.dart |
| R-10 | **No way to skip onboarding steps** — user forced through all steps even if partially complete or wanting to skip | All onboarding screens required completion to proceed | Added skip buttons to face registration (if faces exist) and permission screen (with confirmation dialog warning about reduced protection) | face_registration_screen.dart, permission_screen.dart |
| R-11 | **Accessibility service stops detecting blocked apps** — overlay stops appearing after some time, even when reverification interval has expired | Three root causes: (1) `overlayActive` flag stuck `true` if `BlockingOverlayActivity` is killed without calling `onOverlayDismissed()` — `onDestroy()` never resets the flag, blocking all future overlays. (2) Recheck timer `Runnable` exits without re-posting itself when `currentForegroundPackage` briefly changes (e.g., notification shade, system dialog), causing the timer to die permanently. (3) No watchdog to verify accessibility service is still alive after Android restarts it | Fix: (1) Call `onOverlayDismissed()` in `BlockingOverlayActivity.onDestroy()` as safety net. Add `overlayActiveTimestamp` and reset `overlayActive` if stale for > 60 seconds. (2) Make recheck runnable always re-post itself — only stop on explicit `stopRecheckTimer()`. (3) Add periodic health-check ping from foreground service to accessibility service | KidShieldAccessibilityService.kt, BlockingOverlayActivity.kt, KidShieldForegroundService.kt |

---

## 12. Scope Boundaries

### Completed (V1 / MVP) ✅

- Face registration (multiple adults, MobileFaceNet 192-d embeddings) + PIN fallback
- Permission acquisition wizard (camera, accessibility, overlay, device admin, battery, notifications)
- Manual app block-list selection with search
- Background accessibility-based monitoring (foreground service + auto-restart)
- Full-screen blocking overlay with face verification + PIN fallback
- Configurable re-verification interval (seconds-based, with presets)
- Device admin uninstall protection with PIN challenge
- 100% offline operation
- Debug mode (7-tap)
- ProGuard/R8 release build configuration

### In Scope (V1.5 — Current Phase)

- **Buddy mascot character** on blocking overlay (static illustration + rotating text bubbles)
- **Content library** (80-120 messages: motivational, activities, fun facts, moral lessons, humor)
- **Time-of-day-aware** content selection
- **Usage tracking** (block events, outcomes, per-app stats)
- **Screen time tracking** (device-wide via UsageStatsManager)
- **Screen-free streak** tracking + milestone notifications
- **Dashboard report card** (daily stats, trends, most-attempted apps)
- **Weekly summary view** (7-day trends)
- **Parent self-awareness** (opt-in screen time tracking + gentle insights)
- **Parent nudge notifications** (configurable threshold for high activity alerts)

### Out of Scope (Future Versions)

| Feature | Target Version |
|---------|---------------|
| Liveness detection (anti-spoofing) | V2 |
| ~~Animated Buddy mascot (Lottie/Rive)~~ → Superseded by video overlay (V1.5b) | ~~V2~~ ✅ |
| AI-powered contextual Buddy responses | V2 |
| Voice lines / TTS for Buddy | V2 |
| Scheduled free-time windows | V2 |
| Per-app time limits | V2 |
| "Earn screen time" via offline challenges | V2 |
| Family challenges ("Tech-free dinner streak") | V2 |
| New app install notifications | V2 |
| Parent screen time goal tracking | V2 |
| Detailed per-app analytics with charts | V2 |
| Google Play Store distribution | V2 |
| iOS support | V3+ |
| Remote management (parent's phone → child's phone) | V3+ |
| Multiple child profiles | V3+ |
| Cloud sync / backup | V3+ |

---

## 13. Development Phases

### Phase 1 — Foundation (Week 1–2) ✅ COMPLETE

- [x] Flutter project setup with Kotlin native module
- [x] Platform channel architecture (MethodChannel / EventChannel)
- [x] Permission request flow (all required permissions)
- [x] Local storage layer (SharedPreferences, face embeddings)

### Phase 2 — Face Recognition (Week 3–4) ✅ COMPLETE

- [x] CameraX integration for face capture
- [x] ML Kit face detection integration
- [x] MobileFaceNet TFLite integration (192-d embedding extraction)
- [x] EXIF rotation handling for front camera
- [x] Face registration flow (capture, process, store)
- [x] Face verification flow (capture, compare, threshold)
- [x] PIN setup and verification

### Phase 3 — Core Blocking (Week 5–6) ✅ COMPLETE

- [x] Accessibility Service implementation (foreground app detection)
- [x] Foreground Service (persistent background monitoring)
- [x] Blocking overlay (system alert window, undismissable)
- [x] App selection screen (list installed apps, toggle block, search)
- [x] Verification session management (interval-based re-prompting, seconds-based)

### Phase 4 — Resilience & Settings (Week 7–8) ✅ COMPLETE

- [x] Device Admin Receiver (uninstall protection + PIN challenge)
- [x] Auto-restart mechanisms (BOOT_COMPLETED + START_STICKY)
- [x] Settings screen (interval config, manage faces, manage apps)
- [x] Dashboard screen with protection status
- [x] Debug/testing mode (7-tap)
- [x] ProGuard/R8 configuration for release builds

### Phase 5 — Polish & Testing (Week 9–10) ✅ COMPLETE

- [x] End-to-end testing (face detection, overlay, verification loop)
- [x] Edge case handling (permissions, camera failure, EXIF rotation)
- [x] UI polish (scrollable layouts, keyboard overflow fixes, skip options)
- [x] Release APK build and sideload testing
- [x] Restricted settings guidance for Android 13+

---

### Phase 6a — Buddy Mascot & Overlay Redesign (Week 11–12) ✅ COMPLETE

- [x] Design Buddy character illustrations (human child, 4 poses: waving, thinking, pointing, encouraging) — vector drawables
- [x] Create and curate content library JSON (105 messages across 5 categories)
- [x] Implement BuddyContentEngine: time-of-day bucketing, weighted category selection, shuffle-bag algorithm
- [x] Redesign blocking overlay: warm child-friendly layout with Buddy + speech bubble + app info + rounded buttons
- [x] Add mascot enable/disable toggle in settings (with classic fallback)
- [x] Platform channel methods for mascot state (isMascotEnabled / setMascotEnabled)
- [x] Release APK build verified (93.2 MB)

### Phase 6b — Video Overlay & Accessibility Bug Fix (Week 12–13)

- [ ] **R-11 Fix:** Add `onOverlayDismissed()` safety call in `BlockingOverlayActivity.onDestroy()`
- [ ] **R-11 Fix:** Add `overlayActiveTimestamp` + staleness check (reset if > 60s) in accessibility service
- [ ] **R-11 Fix:** Make recheck timer self-sustaining (always re-post, only explicit stop)
- [ ] **R-11 Fix:** Add foreground service watchdog ping to verify accessibility service health
- [ ] Replace static Buddy illustrations with full-screen video playback (VideoView/MediaPlayer)
- [ ] Bundle 3 videos (9:16 format) as raw assets in APK
- [ ] Implement random video selection (no immediate repeat of same video)
- [ ] Overlay verification buttons (face verify + PIN + go back) small and semi-transparent at bottom with gradient scrim
- [ ] Show text bubble from BuddyContentEngine overlaid on video (semi-transparent)
- [ ] Show blocked app name subtly at top of screen
- [ ] Pause video during face verification camera, resume on retry/cancel
- [ ] Mute video by default (no audio)
- [ ] Settings: 3-way overlay mode toggle (video → static Buddy → classic)
- [ ] Test on various screen sizes and devices

### Phase 7 — Usage Tracking & Streak Engine (Week 13–14)

- [ ] Set up local SQLite database (Drift or sqflite) with schema: block_events, daily_aggregates, streaks, screen_time
- [ ] Instrument accessibility service to log block events with outcomes
- [ ] Implement screen-free streak tracking (start/stop/reset logic in foreground service)
- [ ] Integrate UsageStatsManager for device-wide screen time (with graceful fallback)
- [ ] Build daily aggregation engine (runs at midnight or on app open)
- [ ] Implement 90-day data retention with auto-pruning
- [ ] Add platform channel methods for Flutter to query usage data

### Phase 8 — Dashboard Report Card & Notifications (Week 15–16)

- [ ] Redesign dashboard with daily report card (attempts, verified, screen time, streak, trends)
- [ ] Build "Top Blocked Apps" section with attempt counts
- [ ] Add trend indicators (↑↓ vs. yesterday for each metric)
- [ ] Build weekly summary view (7-day bar chart or list view)
- [ ] Add Buddy contextual status message on dashboard
- [ ] Implement streak milestone notifications (1h, 2h, 4h, all-day) with Buddy character
- [ ] Implement parent nudge notifications (configurable threshold)

### Phase 9 — Parent Self-Awareness (Week 17–18)

- [ ] Add opt-in parent screen time tracking toggle (setup + settings)
- [ ] Integrate UsageStatsManager for parent context (track when app is in foreground after face/PIN verify)
- [ ] Display parent screen time on dashboard alongside child metrics
- [ ] Implement gentle insight messages for high parent screen time
- [ ] Include parent data in weekly summary
- [ ] Add disable option in settings (retain data, just hide display)

### Phase 10 — V1.5 Polish & Release (Week 19–20)

- [ ] End-to-end testing of all V1.5 features
- [ ] Performance profiling (database queries, dashboard render time)
- [ ] Battery impact assessment with new tracking features
- [ ] Content review: ensure all 80-120 Buddy messages are appropriate, diverse, and engaging
- [ ] UI/UX polish across new screens
- [ ] Release APK build and testing

---

## 14. Glossary

| Term | Definition |
|------|-----------|
| **Accessibility Service** | Android API that allows an app to observe UI events like window changes across the system |
| **Buddy** | KidShield's mascot character — a relatable human child illustration that appears on the blocking overlay, providing encouragement, fun facts, activity suggestions, and moral lessons |
| **Content Library** | A bundled JSON asset containing 80-120 pre-written messages across 5 categories (motivational, activities, fun facts, moral lessons, humor) that Buddy displays on the overlay |
| **Cosine Similarity** | A metric measuring the angle between two vectors; used to compare face embeddings (1.0 = identical, 0.0 = unrelated) |
| **Daily Aggregate** | A summarized row of usage data per day: total block attempts, verified accesses, screen time, best streak, most-attempted app |
| **Device Admin** | Android API that grants an app elevated privileges, including preventing its own uninstall |
| **Foreground Service** | An Android service that shows a persistent notification and is less likely to be killed by the OS |
| **MobileFaceNet** | A lightweight face recognition neural network optimized for mobile devices; outputs a 192-d embedding per face |
| **Overlay / SYSTEM_ALERT_WINDOW** | Permission allowing an app to draw on top of other apps |
| **Parent Self-Awareness** | An opt-in feature that tracks and surfaces the parent's own daily screen time alongside the child's metrics, encouraging family-wide digital wellness |
| **Platform Channel** | Flutter mechanism for calling native Android/iOS code from Dart |
| **Screen-Free Streak** | A continuous period during which no blocked app was opened; tracked and celebrated with milestone notifications |
| **Shuffle-Bag Algorithm** | A randomization technique that ensures all items in a set are shown once before any repeats — used for Buddy's message rotation to maximize variety |
| **Time-of-Day Bucketing** | Categorizing content selection based on the current time: morning (6am–12pm), afternoon (12pm–5pm), evening (5pm–9pm), night (9pm–6am) — used to make Buddy's activity suggestions contextually appropriate |
| **UsageStatsManager** | Android API that provides access to device usage history data, including app usage durations and device unlock events |

---

*This document was last updated on March 3, 2026. V1 (MVP) is complete. V1.5a (Buddy mascot + static overlay) is complete. V1.5b (video overlay + accessibility fix) is in progress.*
