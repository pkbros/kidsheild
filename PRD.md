# KidShield — Product Requirements Document

**Version:** 2.0  
**Date:** March 2026  
**Author:** Product & Engineering  
**Status:** V1 MVP ✅ · V1.5 Complete ✅ · V2.0 Planned 📋  
**Platform:** Android (Flutter + Kotlin)  
**Distribution:** Sideloaded APK → Google Play Store (V2)  
**Business Model:** Freemium

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem & Vision](#2-problem--vision)
3. [Target Audience & Personas](#3-target-audience--personas)
4. [Business Model](#4-business-model)
5. [Product Architecture — Age-Tiered System](#5-product-architecture--age-tiered-system)
6. [Current Progress (V1 + V1.5)](#6-current-progress-v1--v15)
7. [V2.0 Feature Specification](#7-v20-feature-specification)
8. [Page-by-Page UI Specification](#8-page-by-page-ui-specification)
9. [Technical Architecture](#9-technical-architecture)
10. [Data Model](#10-data-model)
11. [Implementation Roadmap & Timeline](#11-implementation-roadmap--timeline)
12. [Risk Assessment](#12-risk-assessment)
13. [Appendix A — V1 Rectifications](#13-appendix-a--v1-rectifications)
14. [Appendix B — Glossary](#14-appendix-b--glossary)

---

## 1. Executive Summary

**KidShield** is an Android parental control app that combines face-recognition-based app blocking with an age-adaptive motivational system designed to progressively build healthy screen-time habits in children aged 0–11.

Unlike traditional parental controls that offer a binary block/allow toggle, KidShield deploys a **three-tier developmental framework** that adapts its strategy based on the child's age:

| Tier | Ages | Strategy | Core Mechanic |
|------|------|----------|---------------|
| **Tier 1** | 0–4 | Sensory Redirection | Calming full-screen videos/animations replace blocked content |
| **Tier 2** | 4–8 | Gamified Management | Buddy virtual pet that grows when child stays off blocked apps |
| **Tier 3** | 8–11 | Advanced Earning System | Time-banking + educational flashcards to earn screen time |

Each tier includes a **Progressive Tapering Engine** — an automated system that gradually reduces daily allowed screen time based on configurable schedules, teaching children to self-regulate rather than simply being restricted.

**Current status:** V1 (full MVP with face recognition, app blocking, device admin protection) and V1.5 (Buddy mascot, video overlay, usage analytics, parent self-awareness) are complete and running. V2.0 introduces the age-tiered system, tapering engine, and Play Store readiness.

---

## 2. Problem & Vision

### 2.1 Problem Statement

Children aged 0–11 are exposed to smartphones earlier than ever. Screen addiction manifests differently by developmental stage:

- **Toddlers (0–4)** lack the cognitive ability to self-regulate and need immediate sensory substitution
- **Young children (4–8)** respond to play, motivation, and emotional connection with characters
- **Pre-teens (8–11)** need structured incentives, autonomy, and logical reward systems

Current parental controls are binary — block or allow — offering no developmental progression, no positive motivation, and no strategy for gradual reduction. Parents themselves often model the same phone dependency they want to curb in their children.

### 2.2 Vision

KidShield is **not just a lock — it is a growth companion** that adapts to each child's developmental stage, actively motivates healthy behaviour, and progressively builds screen-time self-management skills — all while working 100% offline and preserving family privacy.

### 2.3 Key Differentiators

| Differentiator | Description |
|---------------|-------------|
| **Age-adaptive** | Three tiers with developmentally appropriate strategies — not one-size-fits-all |
| **Progressive tapering** | Automated gradual reduction — the app actively works toward being needed less |
| **Not just a wall — a guide** | Buddy mascot character motivates, teaches, and redirects |
| **Family-first** | Tracks parent screen time too — digital wellbeing is a shared family effort |
| **Impossible to bypass** | Face recognition + device admin + service monitoring = no workaround for young children |
| **100% offline** | No cloud, no accounts, no data leaving the device |
| **Earn-back system** | Children can earn screen time via educational activities (Tier 3) |

---

## 3. Target Audience & Personas

### 3.1 Segments

| Segment | Description |
|---------|-------------|
| **Primary** | Parents/guardians of children aged 0–11 |
| **Secondary** | Children themselves (active participants in building healthier habits) |
| **Tertiary** | Family unit as a whole (shared digital wellbeing) |

### 3.2 Personas

**Persona 1 — Priya (Mother of a toddler, age 3)**
> "My son grabs my phone and watches YouTube Kids for hours. I need something that gently replaces the screen with something calming, not just a black screen that makes him scream."

- Tier 1 user (Sensory Redirection)
- Wants zero child interaction — fully parent-controlled
- Values calming, educational replacement content

**Persona 2 — David (Father of twin daughters, age 6)**
> "My girls love games and cartoons. I can't just block everything — they'll throw a tantrum. I need something that makes NOT using the phone feel rewarding."

- Tier 2 user (Gamified Management)
- Wants motivation, not just restriction
- Values fun, engaging experience that children want to participate in

**Persona 3 — Aisha (Mother of a son, age 10)**
> "My son is old enough to earn his screen time. I want him to learn that time is valuable — do something educational, get rewarded. Build responsibility."

- Tier 3 user (Advanced Earning System)
- Wants structured earning mechanics
- Values educational content and measurable progress

**Persona 4 — Marcus (Father, self-aware about own usage)**
> "I tell my kids to put down the phone, but I'm on mine constantly. I need something that holds a mirror up to my own habits too."

- Uses parent self-awareness features
- Values honest, non-judgmental usage insights

---

## 4. Business Model

### 4.1 Freemium Structure

| Feature | Free | Premium |
|---------|------|---------|
| Face-recognition app blocking | ✅ | ✅ |
| PIN fallback | ✅ | ✅ |
| Manual app block-list | ✅ | ✅ |
| Buddy mascot (basic — static + text bubbles) | ✅ | ✅ |
| Configurable re-verification interval | ✅ | ✅ |
| Device admin uninstall protection | ✅ | ✅ |
| **Age-Tiered System (Tiers 1–3)** | ❌ | ✅ |
| **Progressive Tapering Engine** | ❌ | ✅ |
| **Virtual Pet Buddy (Tier 2)** | ❌ | ✅ |
| **Time Banking + Flashcards (Tier 3)** | ❌ | ✅ |
| **App Category Intelligence** | ❌ | ✅ |
| **Dashboard Report Card** | Basic (today only) | ✅ Full (trends, weekly, charts) |
| **Parent Self-Awareness** | ❌ | ✅ |
| **Screen-Free Streak Tracking** | ❌ | ✅ |
| **Weekly Summary & Insights** | ❌ | ✅ |
| **Scheduled Free-Time Windows** | ❌ | ✅ |
| **Custom Buddy Content Packs** | ❌ | Future |

### 4.2 Pricing (Planned)

| Plan | Price | Notes |
|------|-------|-------|
| Free | $0 | Classic blocker. Full V1 experience |
| Premium Monthly | $4.99/mo | All tiers + analytics + tapering |
| Premium Annual | $39.99/yr | ~33% savings vs monthly |
| Family (3+ children) | $59.99/yr | Multi-profile support (V3) |

### 4.3 Revenue Projections

Not included in this document. See separate financial model.

---

## 5. Product Architecture — Age-Tiered System

### 5.1 Overview

The age-tiered system is KidShield's core V2.0 innovation. During onboarding, the parent completes a **Child Profile Questionnaire** that determines the appropriate tier. The parent can also manually override the tier selection.

```
┌─────────────────────────────────────────────────────┐
│                 CHILD PROFILE                        │
│                                                      │
│  Age: ___    Name: ___________                       │
│                                                      │
│  ┌─────────────────────────────────────────────────┐ │
│  │  TIER ASSIGNMENT (auto or manual)               │ │
│  │                                                  │ │
│  │  0-4 → Tier 1: Sensory Redirection             │ │
│  │  4-8 → Tier 2: Gamified Management             │ │
│  │  8-11 → Tier 3: Advanced Earning               │ │
│  └─────────────────────────────────────────────────┘ │
│                                                      │
│  ┌─────────────────────────────────────────────────┐ │
│  │  PROGRESSIVE TAPERING ENGINE                    │ │
│  │  Base daily limit → Reduction schedule          │ │
│  │  → Weekly check-in → Adjust or hold            │ │
│  └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

### 5.2 Tier 1 — Sensory Redirection (Ages 0–4)

**Philosophy:** Toddlers cannot reason about screen time. Instead of blocking with a wall, replace addictive content with calming, age-appropriate sensory experiences.

**How it works:**
1. Parent selects apps to block (or blocks all apps except a whitelist)
2. When a blocked app is detected → full-screen calming video/animation plays immediately
3. Videos auto-loop; child stays engaged with replacement content instead of blocked app
4. No buttons, no text, no interaction required from the child
5. Parent verification (face/PIN) exits the redirection and returns to home screen

**Content library:**
- Nature scenes (fish swimming, rain on leaves, clouds)
- Gentle animations (abstract shapes, colour gradients)
- Lullaby visuals (stars, moon, soft movement)
- Educational segments (animals, colours, counting — silent or gentle audio)
- Custom: parents can add their own videos from device gallery

**Tapering in Tier 1:**
- Parent sets a maximum daily screen time (e.g., 60 minutes)
- Timer counts down during all app usage (blocked or allowed)
- When limit reached → all apps trigger redirection
- Weekly tapering: reduce daily limit by a parent-configured amount (e.g., 5 min/week)

### 5.3 Tier 2 — Gamified Management (Ages 4–8)

**Philosophy:** Young children respond to motivation, emotional connection, and play. The Buddy mascot evolves from a static character into a virtual pet that grows, changes outfits, and celebrates when the child builds healthy habits.

**How it works:**
1. Buddy becomes a **virtual pet** with visible happiness, energy, and growth states
2. Staying off blocked apps earns **stars** and makes Buddy happy (visible animations)
3. Opening blocked apps makes Buddy sad and costs stars
4. Stars unlock **cosmetic rewards**: outfits, accessories, backgrounds for Buddy
5. Daily **challenges** appear: "Can you stay off games for 30 minutes?" → Bonus stars

**Buddy Virtual Pet States:**

| State | Trigger | Visual |
|-------|---------|--------|
| Happy | Child below screen limit, streak active | Buddy dancing, glowing, big smile |
| Neutral | Normal usage, no streak | Buddy standing, calm expression |
| Tired | Approaching daily limit | Buddy yawning, droopy eyes |
| Sad | Blocked app opened, streak broken | Buddy sitting down, small frown |
| Celebrating | Challenge completed, milestone hit | Buddy jumping, confetti, sparkles |
| Sleeping | Night mode / device should be off | Buddy in bed, moon and stars |

**Star Economy (Tier 2):**

| Action | Stars |
|--------|-------|
| 30 minutes screen-free | +5 ⭐ |
| 1 hour screen-free | +15 ⭐ |
| Complete daily challenge | +20 ⭐ |
| Full day under screen limit | +50 ⭐ |
| Opened blocked app | −10 ⭐ |
| Streak broken | −5 ⭐ |

**Rewards (Unlockable with Stars):**

| Reward | Cost |
|--------|------|
| New hat for Buddy | 50 ⭐ |
| New outfit for Buddy | 100 ⭐ |
| New background scene | 75 ⭐ |
| Special animation | 150 ⭐ |
| Golden badge | 200 ⭐ |

**Tapering in Tier 2:**
- Displayed as Buddy's "energy bar" — when energy runs out, Buddy needs to sleep (screen time is over)
- Energy bar starts at the parent's configured daily limit
- Weekly tapering reduces the energy bar maximum
- Visual metaphor: "Buddy needs more rest this week — let's help!"

### 5.4 Tier 3 — Advanced Earning System (Ages 8–11)

**Philosophy:** Pre-teens respond to autonomy, logic, and tangible rewards. This tier introduces a **time-banking** system where screen time is a currency that must be earned through educational activities.

**How it works:**
1. Child starts each day with **zero** available screen time (or a parent-configured base amount)
2. To earn minutes, child completes **flashcard quizzes** or **educational challenges**
3. Earned minutes are deposited into a **time bank**
4. Using a blocked app withdraws from the time bank
5. When the bank reaches zero → blocked apps trigger the overlay
6. Educational apps (categorised by parent) do **not** withdraw from the bank

**App Categories:**

| Category | Examples | Bank Effect |
|----------|----------|-------------|
| **Educational** | Khan Academy, Duolingo, reading apps | No withdrawal (free to use) |
| **Neutral** | Camera, clock, calculator | No withdrawal |
| **Distracting** | YouTube, TikTok, Instagram | 1:1 withdrawal (1 min used = 1 min deducted) |
| **Addictive** | Games, short-form video apps | 2:1 withdrawal (1 min used = 2 min deducted) |

**Flashcard System:**
- Bundled flashcard packs: Math (by grade level), Vocabulary, Science, General Knowledge
- Each correct answer earns screen time:
  - Easy: +2 minutes
  - Medium: +4 minutes
  - Hard: +8 minutes
- Streak bonus: 5 correct in a row = +5 bonus minutes
- Daily flashcard cap: parent-configurable (default 60 minutes max earnable)
- Wrong answers earn nothing (no punishment)
- Parents can create custom flashcard packs

**Time Bank Rules:**
- Minimum balance to open a distracting app: 5 minutes
- Maximum daily bank: parent-configurable (default 120 minutes)
- Rollover: unused minutes do NOT carry to next day (prevents hoarding)
- Bonus: completing all daily flashcards = +15 minute bonus
- Parent override: parent can gift bonus minutes via face/PIN verification

**Tapering in Tier 3:**
- Reduce the daily earnable maximum over time (e.g., 120 min → 110 min → 100 min)
- Alternatively: increase the cost of distracting apps (e.g., 1:1 → 1.5:1 withdrawal ratio)
- Child sees their progress: "Your daily target this week: 90 minutes. Last week: 100 minutes. Great improvement!"

### 5.5 Progressive Tapering Engine (All Tiers)

The Progressive Tapering Engine is a cross-tier automated system that gradually reduces screen time based on a parent-configured schedule.

**Configuration:**
- `baseLimit`: Starting daily screen time (minutes)
- `reductionAmount`: Minutes to reduce per period
- `reductionPeriod`: Weekly / biweekly / monthly
- `minimumLimit`: Floor — never reduce below this
- `pauseTapering`: Parent can pause/resume at any time
- `adjustOnSuccess`: If child consistently stays under limit, optionally accelerate tapering

**Example Schedule:**
```
Week 1:  120 min/day  (base)
Week 2:  115 min/day  (-5)
Week 3:  110 min/day  (-5)
Week 4:  105 min/day  (-5)
...
Week 12:  65 min/day  (-5, approaching minimum)
Week 13:  60 min/day  (minimum reached, hold)
```

**Weekly Check-In:** Each Sunday evening, the parent receives a summary with a recommendation:
- "Great week! [Child] stayed under the limit 6/7 days. Ready to reduce by 5 minutes?"
- "Tough week — [Child] exceeded the limit 4 days. Consider holding the current limit."

---

## 6. Current Progress (V1 + V1.5)

### 6.1 V1 MVP — ✅ COMPLETE

| Feature | Status |
|---------|--------|
| Face registration (multiple adults, MobileFaceNet 192-d embeddings) | ✅ |
| PIN fallback verification | ✅ |
| Permission acquisition wizard (camera, accessibility, overlay, device admin, battery, notifications) | ✅ |
| Manual app block-list selection with search | ✅ |
| Background accessibility-based monitoring (foreground service + auto-restart) | ✅ |
| Full-screen blocking overlay with face verification + PIN | ✅ |
| Configurable re-verification interval (seconds-based presets) | ✅ |
| Device admin uninstall protection with PIN challenge | ✅ |
| 100% offline operation | ✅ |
| Debug mode (7-tap activation) | ✅ |
| ProGuard/R8 release build | ✅ |

**11 rectification bugs (R-01 through R-11) identified and resolved.** See [Appendix A](#13-appendix-a--v1-rectifications).

### 6.2 V1.5 — ✅ COMPLETE

| Feature | Status |
|---------|--------|
| Buddy mascot character (4 vector drawable poses) | ✅ |
| Content library (105 messages × 5 categories, bundled JSON) | ✅ |
| BuddyContentEngine (time-of-day bucketing, shuffle-bag rotation) | ✅ |
| Redesigned blocking overlay (child-friendly layout, Buddy + speech bubble) | ✅ |
| Video overlay mode (3 bundled videos, TextureView + MediaPlayer) | ✅ |
| 3-way overlay mode toggle (video → static Buddy → classic) | ✅ |
| Usage tracking (SQLite — block_events, daily_aggregates, streaks, screen_time) | ✅ |
| Screen-free streak tracking + milestone notifications | ✅ |
| Dashboard report card (daily stats, trends, most-attempted apps) | ✅ |
| Weekly summary view (7-day trends) | ✅ |
| Parent self-awareness (opt-in screen time tracking + gentle insights) | ✅ |
| Parent nudge notifications (configurable threshold) | ✅ |

### 6.3 UsageStatsManager Pivot — 🔄 Feature Branch

A reviewer-suggested architectural pivot from AccessibilityService-primary to UsageStatsManager-polling-primary is implemented on the `feature/usagestats-polling` branch:

| Change | Status |
|--------|--------|
| AppBlockingController (source-agnostic singleton) | ✅ Built |
| UsageStatsPollingEngine (300ms adaptive polling) | ✅ Built |
| AccessibilityService → optional accelerator | ✅ Refactored |
| Dashboard detection mode indicator | ✅ Built |
| Permission screen (Usage Stats required, Accessibility optional) | ✅ Updated |
| Build verified (106.6 MB release APK) | ✅ |
| Pushed to GitHub | ✅ |

**Rationale:** UsageStatsManager is a standard Android API with no Play Store restrictions, while AccessibilityService requires justification and may trigger rejection. The hybrid approach preserves instant detection (when Accessibility is granted) while guaranteeing functionality via polling alone.

### 6.4 Build & Repository

| Item | Value |
|------|-------|
| GitHub | https://github.com/pkbros/kidsheild |
| Branches | `main` (V1.5), `feature/usagestats-polling` |
| Release APK size | 106.6 MB |
| compileSdk | 36 |
| minSdk | 26 (Android 8.0) |
| targetSdk | 35 |
| Flutter | Dart SDK ^3.11.0 |
| Native | Kotlin + Java 17 |

---

## 7. V2.0 Feature Specification

### 7.1 Child Profile & Onboarding

| ID | Requirement | Tier |
|----|-------------|------|
| F-100 | Parent completes a Child Profile Questionnaire during onboarding (name, age/DOB, avatar selection) | All |
| F-101 | Age auto-assigns a tier (0–4→T1, 4–8→T2, 8–11→T3) with manual override option | All |
| F-102 | Parent can manage multiple child profiles (V3 — single profile in V2) | V3 |
| F-103 | Profile stores: name, age, tier, avatar, tapering config, star balance (T2), time bank balance (T3) | All |
| F-104 | Onboarding includes app categorisation step (parent classifies installed apps into categories) | T3 |
| F-105 | Parent configures initial daily screen time limit during profile setup | All |
| F-106 | Parent configures tapering schedule during profile setup (or skips for manual control) | All |

### 7.2 Tier 1 — Sensory Redirection

| ID | Requirement |
|----|-------------|
| F-110 | When a blocked app is detected, immediately display a full-screen calming video/animation |
| F-111 | Video auto-loops until parent verifies (face/PIN) |
| F-112 | No child-interactive UI elements on the redirection screen (no buttons, no text, no dismiss) |
| F-113 | Video selection rotates through library (shuffle-bag, no immediate repeats) |
| F-114 | Parent can add custom videos from device gallery to the redirection library |
| F-115 | Daily screen time counter runs during all app usage; when limit reached, ALL apps trigger redirection |
| F-116 | Parent can configure a whitelist of always-allowed apps (excluded from blocking and timer) |
| F-117 | Gentle audio (nature sounds, lullaby) optional and toggleable by parent |
| F-118 | Night mode: after a parent-configured bedtime, all apps trigger redirection |

### 7.3 Tier 2 — Gamified Management

| ID | Requirement |
|----|-------------|
| F-120 | Buddy becomes a virtual pet with visible happiness, energy, and hunger states |
| F-121 | Buddy has at least 6 animated states: happy, neutral, tired, sad, celebrating, sleeping |
| F-122 | Buddy animations use Lottie files for smooth, lightweight rendering |
| F-123 | Star economy: child earns stars for screen-free time and loses stars for opening blocked apps |
| F-124 | Stars are displayed prominently on the Buddy home screen and on the blocking overlay |
| F-125 | Stars can be spent on cosmetic rewards: hats, outfits, backgrounds, special animations |
| F-126 | Reward shop screen where child browses and purchases cosmetics with stars |
| F-127 | Daily challenges appear each morning (e.g., "Stay off games for 1 hour") with bonus star reward |
| F-128 | Challenge completion triggers Buddy celebration animation |
| F-129 | Energy bar represents remaining daily screen time; depletes with usage, refills overnight |
| F-130 | When energy bar empties, Buddy "falls asleep" and all blocked apps trigger the overlay |
| F-131 | Buddy displays contextual messages based on current state and child's behaviour |
| F-132 | Parent can view Buddy state, star balance, and challenge progress from dashboard |

### 7.4 Tier 3 — Advanced Earning System

| ID | Requirement |
|----|-------------|
| F-140 | Time bank: child starts each day with zero (or parent-configured base) screen time |
| F-141 | Flashcard quiz system with multiple-choice and free-text answer modes |
| F-142 | Bundled flashcard packs: Math (Grade 1–5), Vocabulary, Science, General Knowledge |
| F-143 | Each correct answer earns screen time: Easy +2min, Medium +4min, Hard +8min |
| F-144 | Streak bonus: 5 correct in a row = +5 bonus minutes |
| F-145 | Daily earnable cap: parent-configurable (default 120 minutes) |
| F-146 | Time bank auto-resets to zero (or base amount) at midnight |
| F-147 | Using a distracting app withdraws from the bank at 1:1 ratio (configurable) |
| F-148 | Using an addictive app withdraws at 2:1 ratio (configurable) |
| F-149 | Educational and neutral apps do not withdraw from bank |
| F-150 | Parent categorises each installed app into: Educational, Neutral, Distracting, Addictive |
| F-151 | When bank reaches zero, blocked apps trigger the overlay |
| F-152 | Parent can gift bonus minutes via face/PIN verification |
| F-153 | Child can view their time bank balance, today's earnings, and spending breakdown |
| F-154 | Parents can create custom flashcard packs (question + answer + difficulty) |
| F-155 | Flashcard session screen: swipe-based card UI with progress indicator |
| F-156 | Minimum bank balance of 5 minutes required to open a distracting app |

### 7.5 Progressive Tapering Engine

| ID | Requirement |
|----|-------------|
| F-160 | Parent configures: base limit, reduction amount, reduction period, minimum limit |
| F-161 | Tapering runs automatically — reduces the child's daily limit per the schedule |
| F-162 | Parent can pause, resume, or reset tapering at any time |
| F-163 | Weekly check-in notification with summary and recommendation (reduce / hold / ease back) |
| F-164 | Visual progress graph showing limit reduction over time |
| F-165 | Tapering adapts per tier: raw minutes (T1), energy bar max (T2), earnable cap (T3) |
| F-166 | Parent can manually adjust the current limit at any time (overrides tapering for that period) |

### 7.6 App Category Intelligence

| ID | Requirement |
|----|-------------|
| F-170 | App categorisation UI during onboarding and in settings |
| F-171 | Four categories: Educational, Neutral, Distracting, Addictive |
| F-172 | Pre-populated suggestions based on Play Store category metadata (if available), else manual |
| F-173 | Parent can recategorise any app at any time from settings |
| F-174 | Categories determine: withdrawal rate (T3), star impact (T2), blocking behaviour (all) |

### 7.7 Scheduled Free-Time Windows

| ID | Requirement |
|----|-------------|
| F-180 | Parent can configure time windows where blocking is suspended (e.g., weekends 10am–12pm) |
| F-181 | Multiple windows per day supported |
| F-182 | Separate weekend and weekday schedules |
| F-183 | Free-time windows still count toward daily screen time limit |
| F-184 | Visual indicator on child's screen when in a free-time window |

---

## 8. Page-by-Page UI Specification

Each screen is described with enough detail to generate a UI mockup using Google Stitch or similar AI tools. Design language: **rounded, warm, child-safe aesthetic with soft gradients, generous padding, large touch targets (min 48dp), and the Nunito font family.**

**Colour palette:**
- Primary: `#4A90D9` (calm blue)
- Secondary: `#7ED321` (positive green)
- Accent: `#F5A623` (warm amber/gold — stars, rewards)
- Danger: `#E74C3C` (soft red — warnings, blocked)
- Background: `#F7F9FC` (light off-white)
- Card surface: `#FFFFFF`
- Text primary: `#2C3E50` (dark blue-grey)
- Text secondary: `#7F8C8D` (medium grey)

---

### S-01 · Splash Screen

**Purpose:** App launch branding.

**Layout (top to bottom, centred):**
- Full-screen background: vertical gradient from `#4A90D9` (top) to `#7ED321` (bottom)
- Centre: KidShield logo — a shield icon with a smiling child face silhouette inside, white, 120×120dp
- Below logo (8dp gap): App name "KidShield" in Nunito Bold 28sp, white
- Below name (4dp gap): Tagline "Growing Healthy Habits" in Nunito Regular 14sp, white, 60% opacity
- Bottom 40dp: Circular progress indicator, white, 24dp diameter
- Duration: 2 seconds, auto-navigates to Welcome or Dashboard (if already set up)

---

### S-02 · Welcome / First Launch

**Purpose:** Introduce the app to a new parent user. Gate to onboarding.

**Layout:**
- Background: `#F7F9FC`
- Top 30%: Illustration of a parent and child smiling at a phone together — warm, hand-drawn style, full-width
- Below illustration: "Welcome to KidShield" in Nunito Bold 24sp, `#2C3E50`, centre-aligned
- 12dp gap: Body text in Nunito Regular 16sp, `#7F8C8D`, centre-aligned, max-width 300dp:
  > "Build healthy screen habits for your child — step by step, age by age. Let's set things up in about 5 minutes."
- 32dp gap: Primary button, full-width (horizontal padding 24dp), height 56dp, corner radius 16dp, `#4A90D9` fill, white text "Get Started" Nunito SemiBold 18sp
- 16dp gap: Text link "Already set up? Restore" in `#4A90D9`, Nunito Regular 14sp, centre-aligned
- Bottom safe area: 24dp padding

---

### S-03 · Child Profile Setup

**Purpose:** Collect child's name, age, and assign a tier. First step of onboarding.

**Layout:**
- Top app bar: "Child Profile" in Nunito SemiBold 20sp, back arrow left, step indicator right "Step 1 of 6"
- Background: `#F7F9FC`
- Scrollable content area with 24dp horizontal padding:

**Section 1 — Avatar (top, centre-aligned):**
- Circular avatar picker, 96dp diameter, light grey border 2dp
- Default: generic child silhouette icon
- Tap to open a horizontal scrollable row of 12 pre-made cartoon avatars (animals, stickers) with selection ring (`#4A90D9` border 3dp)

**Section 2 — Name (below avatar, 24dp gap):**
- Label: "Child's Name" Nunito SemiBold 14sp, `#7F8C8D`
- Text input field: outlined style, corner radius 12dp, height 52dp, Nunito Regular 16sp, placeholder "e.g., Mia"

**Section 3 — Age (below name, 16dp gap):**
- Label: "Age" Nunito SemiBold 14sp, `#7F8C8D`
- Horizontal number picker: large circular buttons for ages 0–11 in a scrollable row, selected age highlighted with `#4A90D9` fill and white number, unselected: white fill with `#2C3E50` number, each circle 48dp

**Section 4 — Tier Assignment (below age, 24dp gap):**
- Card (white, corner radius 16dp, elevation 2dp, 16dp padding):
  - Title: "Recommended Mode" Nunito SemiBold 16sp
  - Icon + label for the assigned tier:
    - Tier 1: 🌊 "Sensory Redirection" — "Calming content replaces blocked apps"
    - Tier 2: ⭐ "Gamified Buddy" — "Earn stars and grow your virtual pet"
    - Tier 3: 📚 "Time Banking" — "Earn screen time through learning"
  - Subtitle: "Based on age [X]. You can change this anytime." Nunito Regular 13sp, `#7F8C8D`
  - "Change Mode" text button, `#4A90D9`
  - If tapped: expand to show all 3 tier options as radio cards

**Bottom — fixed:**
- Primary button "Continue" full-width, 56dp height, `#4A90D9`, Nunito SemiBold 18sp white

---

### S-04 · Screen Time Configuration

**Purpose:** Set initial daily screen time limit and tapering preferences.

**Layout:**
- App bar: "Screen Time" + "Step 2 of 6"
- Background: `#F7F9FC`, 24dp horizontal padding

**Section 1 — Daily Limit:**
- Label: "Daily Screen Time Limit" Nunito SemiBold 16sp
- Large circular dial/picker in centre: 200dp diameter, `#4A90D9` arc showing selected time, white centre with large number (e.g., "120") in Nunito Bold 36sp + "minutes" below in 14sp
- Below dial: preset chips in a row: "30 min" "60 min" "90 min" "120 min" — rounded chips, 36dp height, `#E8F0FE` fill, `#4A90D9` text, selected chip: `#4A90D9` fill, white text

**Section 2 — Tapering (below, 24dp gap):**
- Card (white, rounded 16dp, 16dp padding):
  - Toggle row: "Enable Progressive Tapering" + Switch (on = `#7ED321`)
  - When enabled, expand to show:
    - "Reduce by" → dropdown: 5 / 10 / 15 minutes
    - "Every" → dropdown: Week / 2 Weeks / Month
    - "Minimum limit" → number input, default 30 minutes
    - Preview text: "120 min → 30 min over ~18 weeks" Nunito Regular 13sp, `#7F8C8D`

**Bottom — fixed:**
- "Continue" primary button

---

### S-05 · App Selection & Categorisation

**Purpose:** Parent selects which apps to block and (for Tier 3) categorises them.

**Layout:**
- App bar: "Manage Apps" + "Step 3 of 6"
- Search bar below app bar: rounded, 44dp height, magnifying glass icon, placeholder "Search apps..."
- Background: `#F7F9FC`

**For Tiers 1 & 2 — Simple block list:**
- Scrollable list of installed apps, each row:
  - App icon (40dp, rounded 8dp), app name (Nunito Regular 16sp), package subtitle (Nunito Regular 12sp, `#7F8C8D`)
  - Right side: toggle switch (blocked = `#E74C3C`, unblocked = grey)
- "Select All" / "Deselect All" text buttons at top

**For Tier 3 — Categorised list:**
- Same app list, but right side shows a **category chip** instead of a toggle:
  - Tap chip to cycle through: "Skip" (grey) → "Educational" (green `#7ED321`) → "Neutral" (blue `#4A90D9`) → "Distracting" (amber `#F5A623`) → "Addictive" (red `#E74C3C`)
- Filter tabs at top: "All" | "Educational" | "Neutral" | "Distracting" | "Addictive" | "Uncategorised"
- Counter badge on each tab showing count

**Bottom — fixed:**
- Summary text: "12 apps blocked · 5 educational · 3 distracting"
- "Continue" primary button

---

### S-06 · Face Registration (Existing — minor redesign)

**Purpose:** Register parent/guardian faces for verification.

**Layout:** Existing V1 face registration screen. Changes for V2:
- Step indicator updated: "Step 4 of 6"
- Skip button available if at least 1 face already registered
- Minor visual refresh to match new colour palette and font

---

### S-07 · PIN Setup (Existing — minor redesign)

**Purpose:** Set a 4–6 digit PIN as fallback.

**Layout:** Existing V1 PIN setup screen with updated step indicator "Step 5 of 6" and visual refresh.

---

### S-08 · Permissions (Existing — updated)

**Purpose:** Guide parent through required Android permissions.

**Layout:** Existing permission wizard. Changes for V2:
- Step indicator: "Step 6 of 6"
- **Usage Stats permission** now listed as **required** (not optional)
- **Accessibility** listed as **recommended** (optional with warning)
- Visual refresh

---

### S-09 · Onboarding Complete

**Purpose:** Confirm setup is done and introduce the child's experience.

**Layout:**
- Background: `#F7F9FC`
- Top 40%: Full-width illustration showing Buddy waving with confetti
- "All Set!" Nunito Bold 28sp, `#2C3E50`, centre
- Body text: "KidShield is now protecting [Child's Name]. Here's what happens when [he/she] opens a blocked app..." Nunito Regular 16sp, `#7F8C8D`, 300dp max-width, centre
- 16dp gap: Three small preview cards in a horizontal scroll (each 140×180dp, rounded 12dp, white, shadow):
  - Card 1: Mini screenshot of overlay → "See Buddy appear"
  - Card 2: Mini screenshot of stars/time bank → "Earn rewards"
  - Card 3: Mini screenshot of dashboard → "Track progress"
- 32dp gap: "Go to Dashboard" primary button

---

### S-10 · Dashboard (Redesigned for V2)

**Purpose:** Central hub showing child's status, daily stats, quick actions.

**Layout:**
- Bottom navigation bar with 4 tabs: **Dashboard** (home icon, selected), **Buddy** (pet icon), **Reports** (chart icon), **Settings** (gear icon)
- App bar: "KidShield" left, child avatar + name right (tappable to switch profiles in V3)

**Content (scrollable, 16dp horizontal padding):**

**Section 1 — Status Banner (top):**
- Full-width card, corner radius 16dp, gradient fill: light blue to light green
- Left side: Protection status icon (shield with checkmark, green) + "Active" or "Paused"
- Right side: Detection mode badge (Hybrid / Polling / Accessibility)
- Below (inside card): "Tier [1/2/3]: [Tier Name]" subtitle

**Section 2 — Today's Summary (below, 12dp gap):**
- Row of 3 metric cards (equal width, 8dp gaps between):
  - Card 1: "Screen Time" — circular progress ring (used/limit), large number "47 min", subtitle "of 120 min"
  - Card 2 (Tier 2): "Stars" — star icon, large number "135 ⭐", subtitle "+25 today"
  - Card 2 (Tier 3): "Time Bank" — clock icon, large number "42 min", subtitle "earned today"
  - Card 3: "Streak" — flame icon, large number "2h 15m", subtitle "current streak"

**Section 3 — Tapering Progress (below, 12dp gap):**
- Horizontal progress bar: showing current week's limit vs starting limit
- Labels: "Week 4 of 18 · 105 min/day" Nunito Regular 13sp
- Small "Adjust" text button right-aligned

**Section 4 — Daily Challenge (Tier 2 only, below, 12dp gap):**
- Card with Buddy illustration (small, 48dp) left, challenge text right
- "Stay off YouTube for 1 hour" Nunito SemiBold 14sp
- Progress indicator: "32 min / 60 min"
- Star reward badge: "+20 ⭐"

**Section 5 — Quick Actions (below, 12dp gap):**
- Row of action chips: "Start Flashcards" (T3), "View Buddy" (T2), "Gift Time" (T3), "Free-Time Window"

**Section 6 — Top Blocked Apps (below, 12dp gap):**
- Card with list of top 3 most-attempted apps today, each row: app icon + name + attempt count

**Section 7 — Parent Screen Time (if opt-in, below, 12dp gap):**
- Subtle card, `#F0F0F0` background: "Your screen time today: 2h 45m" with gentle insight text

---

### S-11 · Buddy Home Screen (Tier 2)

**Purpose:** Virtual pet home — child's primary interaction with Buddy.

**Layout:**
- Bottom nav: Dashboard, **Buddy** (selected), Reports, Settings
- Background: Illustrated scene (park, bedroom, space — based on selected background)
- No app bar — fully immersive

**Centre (60% of screen):**
- Buddy character displayed as Lottie animation, approximately 200dp tall
- Current state animation plays (happy, neutral, tired, sad, celebrating, sleeping)
- Speech bubble above Buddy with rotating text from BuddyContentEngine
- Subtle particle effects when celebrating (confetti, sparkles)

**Top bar (overlaid, semi-transparent):**
- Left: Star counter "⭐ 135" in rounded pill badge, `#F5A623` fill, white text
- Right: Energy bar (horizontal, 120dp wide, 12dp tall) — fills from `#7ED321` (full) to `#E74C3C` (low)
- Energy label: "45 min left"

**Bottom area (overlaid):**
- Horizontal row of 4 circular action buttons (56dp each, white fill, shadow):
  - 🏪 "Shop" → Reward shop
  - 🎯 "Challenge" → Daily challenge detail
  - 👕 "Dress Up" → Outfit selection
  - 📊 "Stats" → Quick stats view

**Interactions:**
- Tap Buddy: Buddy does a random reaction animation (wave, spin, laugh)
- Swipe left/right on background: cycle through owned backgrounds
- Pull down: refresh state / show today's challenge

---

### S-12 · Reward Shop (Tier 2)

**Purpose:** Spend stars on cosmetic items for Buddy.

**Layout:**
- App bar: "Buddy Shop" with back arrow. Star balance displayed right: "⭐ 135"
- Background: `#F7F9FC`

**Tab bar below app bar:**
- Tabs: "Hats" | "Outfits" | "Backgrounds" | "Animations"
- Selected tab: `#4A90D9` underline

**Grid content (2 columns, scrollable):**
- Each item card (equal width, 12dp gap, corner radius 12dp, white fill, shadow):
  - Item preview image (square, top, full-width of card, rounded top corners)
  - Item name below image: Nunito SemiBold 14sp
  - Price: "⭐ 50" in `#F5A623` or "Owned ✓" in `#7ED321`
  - If not enough stars: price text greyed out, card slightly dimmed
  - Tap → purchase confirmation bottom sheet

**Purchase confirmation bottom sheet:**
- Item preview (large), name, price
- "Buy for ⭐ 50?" Nunito SemiBold 16sp
- Two buttons: "Cancel" (outlined) and "Buy" (primary, `#F5A623`)
- On purchase: confetti animation, "You got it!" text, Buddy wearing the item

---

### S-13 · Daily Challenge Screen (Tier 2)

**Purpose:** Show today's challenge with progress.

**Layout:**
- App bar: "Today's Challenge" + back arrow
- Background: `#F7F9FC`

**Centre card (white, rounded 20dp, 24dp padding, 80% width):**
- Buddy illustration at top (celebrating pose), 100dp
- Challenge text: "Stay off YouTube for 1 hour" Nunito Bold 18sp, centre
- Circular progress ring below: large (140dp), `#4A90D9` arc, "42 min / 60 min" in centre
- Below ring: "18 minutes to go!" Nunito SemiBold 14sp, `#7F8C8D`
- Reward badge at bottom of card: "Reward: ⭐ 20" in amber pill

**Below card:**
- "Previous Challenges" section header
- List of past 7 days challenges: date, challenge text, ✅ completed or ❌ missed, stars earned

---

### S-14 · Flashcard Session Screen (Tier 3)

**Purpose:** Child answers flashcards to earn screen time. Card-swipe interface.

**Layout:**
- App bar: "Earn Time" + back arrow. Right side: "Bank: 42 min" in green pill badge
- Background: `#F7F9FC`

**Progress bar (below app bar):**
- Thin horizontal bar (4dp) showing progress through current pack: `#4A90D9` fill

**Centre — Flashcard (takes up ~60% of screen):**
- Card (300dp wide × 400dp tall, white, rounded 20dp, elevation 4dp, centre-aligned)
- **Front state (question):**
  - Top label: difficulty chip — "Easy" (green) / "Medium" (amber) / "Hard" (red), 8dp from top
  - Centre: Question text in Nunito SemiBold 20sp, `#2C3E50`, centre-aligned, max 3 lines
  - Bottom: "Tap to flip" hint, Nunito Regular 13sp, `#7F8C8D`
  - Reward preview: "+2 min" / "+4 min" / "+8 min" pill at bottom of card
- **Back state (answer — after tap/swipe):**
  - Answer options (multiple choice): 4 buttons stacked vertically, each full-width within card, 48dp height, rounded 12dp
    - Default: white fill, `#2C3E50` text
    - Selected correct: `#7ED321` fill, white text, checkmark icon
    - Selected wrong: `#E74C3C` fill, white text, X icon
  - Free-text mode: text input field with "Submit" button

**Below card:**
- Streak indicator: "🔥 3 in a row!" Nunito SemiBold 14sp, `#F5A623` (appears after 2+ correct)
- Session stats: "5 correct · 1 wrong · +18 min earned" Nunito Regular 13sp

**Bottom — fixed:**
- "End Session" outlined button (left) + "Next Card →" primary button (right)
- On session end: summary card with total earned, streaks, animated "+X min added to bank"

---

### S-15 · Time Bank Screen (Tier 3)

**Purpose:** Show the child's current time bank balance, earnings, and spending.

**Layout:**
- App bar: "Time Bank" + back arrow
- Background: `#F7F9FC`

**Section 1 — Balance display (top, centre):**
- Large circular graphic (160dp): bank vault / piggy bank illustration
- Inside circle: current balance "42 min" Nunito Bold 32sp, `#2C3E50`
- Below circle: "of 120 min daily maximum" Nunito Regular 14sp, `#7F8C8D`
- Animated coin/minute icon floating in when balance increases

**Section 2 — Today's Activity (below, 16dp gap):**
- Two side-by-side cards (equal width, 8dp gap):
  - "Earned" card: green left border, "+48 min" large text, "15 flashcards" subtitle
  - "Spent" card: amber left border, "−6 min" large text, "YouTube (4m), Games (2m)" subtitle

**Section 3 — Spending Breakdown (below, 16dp gap):**
- List of apps used today with time withdrawn:
  - Each row: app icon (32dp) + app name + category chip (colour-coded) + "−X min" right-aligned
  - Row for educational apps shows "Free" badge instead of withdrawal

**Section 4 — Action Buttons (below, 16dp gap):**
- "Start Earning" primary button → navigates to flashcard session
- "Ask Parent for Bonus" outlined button → triggers parent verification flow

---

### S-16 · Blocking Overlay — Tier 1 (Sensory Redirection)

**Purpose:** Full-screen calming video replaces blocked app for toddlers.

**Layout:**
- **Full-screen video player**, no chrome, no UI elements visible to child
- Video fills entire screen (centre-crop to fit aspect ratio)
- Video auto-loops
- Audio: muted by default (parent can enable gentle audio in settings)

**Hidden parent area:**
- Triple-tap bottom-right corner (within 1 second) reveals semi-transparent bottom bar (fade in)
- Bottom bar (80dp height, black 60% opacity, rounded top corners 16dp):
  - Small text: "Parent: verify to exit" Nunito Regular 14sp, white
  - Two small buttons (40dp height): "🔒 Face Verify" and "🔑 PIN" — white outlined
  - Auto-hides after 10 seconds of no interaction

---

### S-17 · Blocking Overlay — Tier 2 (Buddy Overlay)

**Purpose:** Buddy appears with motivational message when child opens a blocked app.

**Layout:**
- Background: soft gradient `#F7F9FC` to `#E8F0FE`
- Top (subtle): blocked app name + icon, small text "This app is blocked" Nunito Regular 12sp, `#7F8C8D`

**Centre:**
- Buddy animation (sad or pointing pose), 180dp tall, Lottie
- Speech bubble above Buddy (white, rounded 16dp, subtle shadow, arrow pointing to Buddy):
  - Message text from BuddyContentEngine, Nunito Regular 16sp, `#2C3E50`, max 3 lines
- Star penalty notice below Buddy: "−10 ⭐" in red pill badge (appears after 3 seconds)

**Bottom (fixed, 24dp padding):**
- Energy bar: showing remaining daily time
- Three buttons (full-width, stacked, 12dp gaps):
  - "Go Back" — large primary button, `#7ED321`, 56dp height, "🏠 Go Back" white text
  - "Face Verify" — outlined, 48dp height, `#4A90D9` border
  - "Use PIN" — text button, 44dp height, `#7F8C8D`

---

### S-18 · Blocking Overlay — Tier 3 (Time Bank Gate)

**Purpose:** Shows time bank balance and options when child opens a blocked app.

**Layout:**
- Background: `#F7F9FC`
- Top bar: blocked app icon + name + category chip (e.g., "Distracting" amber)

**Centre card (white, rounded 20dp, 24dp padding):**
- Time bank icon (piggy bank / vault, 64dp)
- "Your Time Bank" Nunito SemiBold 18sp
- Balance: "42 min remaining" Nunito Bold 24sp, green if >20min, amber if 5–20min, red if <5min
- Withdrawal rate: "This app costs 1 min per 1 min used" or "2 min per 1 min" (for addictive), Nunito Regular 14sp, `#7F8C8D`
-  "Time will be deducted while you use this app."

**Below card:**
- If balance >= 5 min: "Continue to [App Name]" primary button, `#4A90D9` — tapping starts deduction timer and allows app
- If balance < 5 min: "Not enough time. Earn more!" message, "Start Flashcards" primary button, `#7ED321`
- "Go Back" outlined button always visible
- "Parent Override" text button at very bottom → face/PIN verification to gift time

---

### S-19 · Reports / Weekly Summary

**Purpose:** Detailed usage analytics for parents.

**Layout:**
- Bottom nav: Dashboard, Buddy, **Reports** (selected), Settings
- App bar: "Reports" + date range selector (dropdown: "This Week" / "Last Week" / "This Month")

**Section 1 — Weekly Overview Cards (horizontal scroll):**
- 7 day cards (each 80dp wide, 120dp tall, rounded 12dp):
  - Day name at top (Mon, Tue, ...)
  - Circular mini-chart showing screen time vs limit
  - Status icon: ✅ (under limit) or ⚠️ (over limit)
  - Screen time number below

**Section 2 — Trends Chart (below, 16dp gap):**
- Line/bar chart (220dp height) showing daily screen time over the selected period
- Limit line shown as dashed horizontal
- Tap a day to see details

**Section 3 — Key Metrics Table (below, 16dp gap):**
- Card with rows:
  - "Avg Daily Screen Time: 68 min" (with ↓ trend arrow if improving)
  - "Block Attempts: 23" (with trend)
  - "Verified Access: 8" (with trend)
  - "Best Streak: 4h 12m"
  - "Challenges Completed: 5/7" (T2)
  - "Flashcards Answered: 142" (T3)
  - "Time Earned: 210 min" (T3)

**Section 4 — Most Used Apps (below, 16dp gap):**
- Horizontal bar chart or list: top 5 apps by time spent, colour-coded by category

**Section 5 — Parent Screen Time (if opt-in):**
- Separated card: "Your Screen Time This Week: 14h 30m" with daily breakdown

---

### S-20 · Settings (Redesigned for V2)

**Purpose:** All configuration options.

**Layout:**
- Bottom nav: Dashboard, Buddy, Reports, **Settings** (selected)
- App bar: "Settings"
- Scrollable list of setting groups:

**Group 1 — Child Profile:**
- Row: child avatar + name + tier badge → tap to edit profile
- "Change Tier" button

**Group 2 — Screen Time:**
- "Daily Limit: 105 min" → tap to adjust
- "Tapering: Active (Week 4)" → tap for tapering settings
- "Free-Time Windows" → tap for schedule editor
- "Night Mode: 8:30 PM – 7:00 AM" → tap to configure

**Group 3 — Apps:**
- "Manage Blocked Apps" → S-05
- "App Categories" (T3) → category editor
- "Re-verification Interval" → picker

**Group 4 — Buddy (T2):**
- "Buddy Overlay Mode: Video / Animated / Classic" → 3-way toggle
- "Star Balance: 135 ⭐" (view only)
- "Reset Buddy" → confirmation dialog

**Group 5 — Flashcards (T3):**
- "Manage Flashcard Packs" → pack list
- "Daily Earn Cap: 120 min" → picker
- "Create Custom Pack" → pack editor

**Group 6 — Parent Self-Awareness:**
- "Track My Screen Time" toggle
- "Nudge Threshold: 60 min" → picker

**Group 7 — Security:**
- "Manage Faces" → face list
- "Change PIN"
- "Device Admin: Active ✅"

**Group 8 — Detection Engine:**
- "Mode: Hybrid" badge
- Status rows: "UsageStats: Active ✅" / "Accessibility: Active ✅"

**Group 9 — About:**
- Version, licenses, debug mode (7-tap)

---

### S-21 · Flashcard Pack Manager (Tier 3)

**Purpose:** Browse, enable/disable, and create flashcard packs.

**Layout:**
- App bar: "Flashcard Packs" + back arrow
- Background: `#F7F9FC`

**List of packs (scrollable):**
- Each pack card (full-width, rounded 12dp, white, 16dp padding):
  - Left: pack icon (emoji: 🔢 Math, 📖 Vocab, 🔬 Science, 🌍 General, ✏️ Custom)
  - Centre: pack name (Nunito SemiBold 16sp) + card count "48 cards" + difficulty range "Easy – Hard"
  - Right: toggle switch (enabled/disabled for this child)

**Bottom — fixed:**
- "Create Custom Pack" button, `#4A90D9` outlined, full-width

---

### S-22 · Custom Flashcard Editor (Tier 3)

**Purpose:** Parent creates custom flashcards.

**Layout:**
- App bar: "New Flashcard Pack" + back arrow + "Save" button right
- Background: `#F7F9FC`

**Pack info:**
- "Pack Name" text input
- "Description" text input (optional)

**Card list:**
- Each card shown as a mini-card (full-width, white, rounded 8dp, 12dp padding):
  - Question text (bold), answer text (regular), difficulty chip
  - Edit (pencil) and delete (trash) icon buttons right
- "Add Card" button at bottom of list → opens add/edit dialog

**Add/Edit Card Dialog (bottom sheet):**
- "Question" text input (multiline)
- "Correct Answer" text input
- "Wrong Answer 1" text input
- "Wrong Answer 2" text input
- "Wrong Answer 3" text input
- "Difficulty" segmented control: Easy | Medium | Hard
- "Save Card" primary button

---

### S-23 · Free-Time Window Editor

**Purpose:** Configure scheduled periods when blocking is suspended.

**Layout:**
- App bar: "Free-Time Windows" + back arrow
- Background: `#F7F9FC`

**Toggle at top:** "Enable Scheduled Free Time" switch

**When enabled:**
- Two sections with headers: "Weekdays" and "Weekends"
- Each section contains a list of time windows:
  - Each window row: start time – end time (tappable to edit) + delete button
  - "Add Window" button below each section

**Time picker:** Standard Material time picker dialog when adding/editing

**Preview at bottom:**
- Visual weekly timeline (Mon–Sun, horizontal bar for each day) with free-time windows highlighted in green

---

### S-24 · Tapering Progress Screen

**Purpose:** Detailed view of the Progressive Tapering Engine status and history.

**Layout:**
- App bar: "Tapering Progress" + back arrow
- Background: `#F7F9FC`

**Section 1 — Current Status Card (top):**
- "Week 4 of 18" Nunito Bold 20sp
- "Current Limit: 105 min/day" large text
- "Started at 120 min · Target: 30 min" subtitle
- Reducing by: "5 min / week"
- Status badge: "On Track" green or "Paused" amber

**Section 2 — Progress Graph (below, 16dp gap):**
- Line chart (250dp height): X-axis = weeks, Y-axis = minutes
- Blue line = limit over time (stepping down)
- Green dots = days child stayed under limit
- Red dots = days child exceeded limit
- Current week highlighted

**Section 3 — Actions (below, 16dp gap):**
- "Pause Tapering" / "Resume Tapering" toggle button
- "Adjust Current Limit" button → number picker
- "Reset to Start" text button (with confirmation dialog)

**Section 4 — Weekly Check-In:**
- Latest recommendation card: "Great week! [Name] stayed under limit 6/7 days. Ready to reduce?"
- "Accept Reduction" primary button + "Hold Current Limit" outlined button

---

### S-25 · Parent Verification Flow (Face + PIN)

**Purpose:** Reusable verification screen for parent-only actions.

**Layout:** Existing V1 face verification / PIN entry screen. Used when:
- Exiting blocking overlay
- Gifting bonus time (T3)
- Accessing settings
- Changing block list

Minor V2 updates: visual refresh to match new palette.

---

### S-26 · App Category Editor (Tier 3)

**Purpose:** Reclassify apps into categories outside of onboarding.

**Layout:**
- App bar: "App Categories" + back arrow
- Filter tabs: "All" | "Educational" | "Neutral" | "Distracting" | "Addictive"
- Search bar
- List of installed apps (same as S-05 Tier 3 layout but without onboarding context)
- Each app row: icon + name + current category chip (tappable to change)
- Bottom summary: "5 educational · 8 neutral · 12 distracting · 3 addictive"

---

## 9. Technical Architecture

### 9.1 System Architecture (Updated for V2)

```
┌──────────────────────────────────────────────────────────────┐
│                        FLUTTER UI LAYER                       │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────┐   │
│  │Dashboard│ │  Buddy   │ │ Reports  │ │   Settings     │   │
│  │ Screen  │ │  Home    │ │  Screen  │ │   Screen       │   │
│  └────┬────┘ └────┬─────┘ └────┬─────┘ └───────┬────────┘   │
│       │           │            │                │            │
│  ┌────┴───────────┴────────────┴────────────────┴────────┐   │
│  │              AppState (ChangeNotifier)                  │   │
│  │  child_profile · tier · stars · time_bank · tapering   │   │
│  └──────────────────────┬─────────────────────────────────┘   │
│                         │ MethodChannel                       │
│  ┌──────────────────────┴─────────────────────────────────┐   │
│  │             PlatformChannelHandler                      │   │
│  └──────────────────────┬─────────────────────────────────┘   │
└─────────────────────────┼─────────────────────────────────────┘
                          │
┌─────────────────────────┼─────────────────────────────────────┐
│                   KOTLIN NATIVE LAYER                          │
│                         │                                      │
│  ┌──────────────────────┴─────────────────────────────────┐   │
│  │              AppBlockingController (Singleton)          │   │
│  │  manages: block list, tier logic, time bank, tapering   │   │
│  └───┬──────────┬──────────┬──────────┬───────────────────┘   │
│      │          │          │          │                        │
│  ┌───┴───┐  ┌──┴───┐  ┌──┴───┐  ┌──┴──────────────────┐    │
│  │Usage  │  │Acces.│  │Fore- │  │  Tier Engines        │    │
│  │Stats  │  │Svc   │  │ground│  │  ├ SensoryEngine (T1)│    │
│  │Polling│  │(opt) │  │Svc   │  │  ├ GamifyEngine (T2) │    │
│  │Engine │  │      │  │      │  │  └ BankEngine (T3)   │    │
│  └───────┘  └──────┘  └──────┘  └──────────────────────┘    │
│                                                               │
│  ┌────────────────────┐  ┌────────────────────────────────┐   │
│  │ FaceRecognition    │  │  UsageDatabase (SQLite)        │   │
│  │ Engine             │  │  + child_profiles              │   │
│  │ (ML Kit + TFLite)  │  │  + app_categories              │   │
│  └────────────────────┘  │  + star_transactions            │   │
│                          │  + time_bank_transactions       │   │
│  ┌────────────────────┐  │  + tapering_schedule            │   │
│  │ BuddyContent       │  │  + flashcard_packs             │   │
│  │ Engine             │  │  + flashcard_results            │   │
│  └────────────────────┘  │  + challenges                   │   │
│                          │  + block_events (existing)       │   │
│  ┌────────────────────┐  │  + daily_aggregates (existing)  │   │
│  │ BlockingOverlay    │  │  + streaks (existing)           │   │
│  │ Activity           │  │  + screen_time (existing)       │   │
│  │ (tier-aware)       │  └────────────────────────────────┘   │
│  └────────────────────┘                                       │
└───────────────────────────────────────────────────────────────┘
```

### 9.2 New V2 Kotlin Components

| Component | Responsibility |
|-----------|---------------|
| `TierEngine` (interface) | Common interface for tier-specific logic: `onBlockedAppDetected()`, `getDailyStatus()`, `getOverlayConfig()` |
| `SensoryRedirectionEngine` | Tier 1: manages video library, playback rotation, daily timer |
| `GamifiedBuddyEngine` | Tier 2: manages star economy, Buddy state machine, challenges, rewards |
| `TimeBankEngine` | Tier 3: manages balance, deposits (flashcards), withdrawals (app usage), caps |
| `TaperingScheduler` | Cross-tier: reads schedule, adjusts daily limits, triggers weekly check-ins |
| `FlashcardManager` | Tier 3: loads packs, serves questions, tracks results, calculates earnings |
| `AppCategoryManager` | Tier 3 (used by others): stores/retrieves per-app category assignments |
| `ChildProfileManager` | All: stores/retrieves child profile, tier assignment, avatar |
| `FreeTimeWindowManager` | All: evaluates whether current time is within a free-time window |

### 9.3 Lottie Animations (Tier 2)

Buddy's 6 states will each have a Lottie JSON file (~50-200KB each):
- `buddy_happy.json` — bouncing, waving, glowing
- `buddy_neutral.json` — standing, blinking, subtle idle motion
- `buddy_tired.json` — yawning, droopy eyes, slow movement
- `buddy_sad.json` — sitting, head down, small frown
- `buddy_celebrating.json` — jumping, confetti, sparkles, arms up
- `buddy_sleeping.json` — lying down, moon and stars, breathing animation

**Lottie dependency:** `com.airbnb.android:lottie:6.x` (~300KB)

### 9.4 Key Dependencies (New in V2)

| Library | Version | Purpose |
|---------|---------|---------|
| Lottie | 6.x | Buddy animations (Tier 2) |
| MPAndroidChart or Vico | latest | Charts for reports/tapering graphs |
| Room (optional) | 2.6.x | If migrating from raw SQLite for type safety |

### 9.5 Platform Channel Methods (New in V2)

| Method | Direction | Parameters | Return |
|--------|-----------|------------|--------|
| `getChildProfile` | Dart → Kotlin | — | Profile JSON |
| `saveChildProfile` | Dart → Kotlin | Profile JSON | success bool |
| `getTierStatus` | Dart → Kotlin | — | Tier-specific status JSON |
| `getStarBalance` | Dart → Kotlin | — | int |
| `addStars` / `removeStars` | Dart → Kotlin | amount: int, reason: String | new balance |
| `getTimeBankBalance` | Dart → Kotlin | — | int (minutes) |
| `depositTime` | Dart → Kotlin | minutes: int, source: String | new balance |
| `withdrawTime` | Dart → Kotlin | minutes: int, app: String | new balance |
| `getFlashcardPack` | Dart → Kotlin | packId: String | Pack JSON |
| `submitFlashcardAnswer` | Dart → Kotlin | cardId, answer, correct: bool | earned minutes |
| `getTaperingStatus` | Dart → Kotlin | — | Tapering JSON |
| `updateTaperingConfig` | Dart → Kotlin | Config JSON | success bool |
| `getAppCategories` | Dart → Kotlin | — | Map<String, String> |
| `setAppCategory` | Dart → Kotlin | pkg: String, cat: String | success bool |
| `getFreeTimeWindows` | Dart → Kotlin | — | List<Window> JSON |
| `saveFreeTimeWindows` | Dart → Kotlin | List<Window> JSON | success bool |
| `giftBonusMinutes` | Dart → Kotlin | minutes: int | new balance |
| `getChallengeStatus` | Dart → Kotlin | — | Challenge JSON |

---

## 10. Data Model

### 10.1 New Tables (V2)

```sql
-- Child profile (single child in V2, multi in V3)
CREATE TABLE child_profiles (
    id          INTEGER PRIMARY KEY,
    name        TEXT NOT NULL,
    age         INTEGER NOT NULL,
    avatar_id   TEXT NOT NULL DEFAULT 'default',
    tier        INTEGER NOT NULL CHECK(tier IN (1,2,3)),
    created_at  INTEGER NOT NULL,  -- epoch ms
    updated_at  INTEGER NOT NULL
);

-- App categories for Tier 3 earn/spend logic
CREATE TABLE app_categories (
    package_name TEXT PRIMARY KEY,
    category     TEXT NOT NULL CHECK(category IN ('educational','neutral','distracting','addictive')),
    updated_at   INTEGER NOT NULL
);

-- Star economy for Tier 2
CREATE TABLE star_transactions (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    amount      INTEGER NOT NULL,  -- positive = earn, negative = spend/penalty
    reason      TEXT NOT NULL,      -- 'streak_30min', 'challenge_complete', 'blocked_app_opened', 'purchase_hat_01'
    balance     INTEGER NOT NULL,  -- running balance after this transaction
    created_at  INTEGER NOT NULL
);

-- Time bank for Tier 3
CREATE TABLE time_bank_transactions (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    amount      INTEGER NOT NULL,  -- positive = deposit (flashcards), negative = withdrawal (app usage)
    source      TEXT NOT NULL,      -- 'flashcard', 'parent_gift', 'app_youtube', 'daily_base'
    balance     INTEGER NOT NULL,  -- running balance in minutes
    created_at  INTEGER NOT NULL
);

-- Tapering schedule
CREATE TABLE tapering_schedule (
    id                  INTEGER PRIMARY KEY,
    base_limit          INTEGER NOT NULL,  -- starting daily limit in minutes
    reduction_amount    INTEGER NOT NULL,  -- minutes to reduce per period
    reduction_period    TEXT NOT NULL CHECK(reduction_period IN ('weekly','biweekly','monthly')),
    minimum_limit       INTEGER NOT NULL,
    start_date          TEXT NOT NULL,      -- YYYY-MM-DD
    is_paused           INTEGER NOT NULL DEFAULT 0,
    current_limit       INTEGER NOT NULL    -- today's effective limit
);

-- Flashcard packs
CREATE TABLE flashcard_packs (
    id          TEXT PRIMARY KEY,    -- e.g., 'math_grade3', 'custom_01'
    name        TEXT NOT NULL,
    description TEXT,
    is_custom   INTEGER NOT NULL DEFAULT 0,
    is_enabled  INTEGER NOT NULL DEFAULT 1,
    card_count  INTEGER NOT NULL DEFAULT 0,
    created_at  INTEGER NOT NULL
);

-- Individual flashcards
CREATE TABLE flashcards (
    id              TEXT PRIMARY KEY,
    pack_id         TEXT NOT NULL REFERENCES flashcard_packs(id),
    question        TEXT NOT NULL,
    correct_answer  TEXT NOT NULL,
    wrong_answer_1  TEXT,
    wrong_answer_2  TEXT,
    wrong_answer_3  TEXT,
    difficulty      TEXT NOT NULL CHECK(difficulty IN ('easy','medium','hard')),
    times_answered  INTEGER NOT NULL DEFAULT 0,
    times_correct   INTEGER NOT NULL DEFAULT 0
);

-- Flashcard session results
CREATE TABLE flashcard_results (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    card_id         TEXT NOT NULL REFERENCES flashcards(id),
    was_correct     INTEGER NOT NULL,
    time_earned     INTEGER NOT NULL DEFAULT 0,  -- minutes earned from this answer
    answered_at     INTEGER NOT NULL              -- epoch ms
);

-- Daily challenges (Tier 2)
CREATE TABLE challenges (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    date            TEXT NOT NULL,       -- YYYY-MM-DD
    description     TEXT NOT NULL,
    target_minutes  INTEGER NOT NULL,    -- e.g., 60 (stay off YouTube for 60 min)
    target_app      TEXT,                -- package name, null = any blocked app
    star_reward     INTEGER NOT NULL,
    progress_min    INTEGER NOT NULL DEFAULT 0,
    is_completed    INTEGER NOT NULL DEFAULT 0,
    completed_at    INTEGER              -- epoch ms, null if not completed
);

-- Buddy cosmetics inventory
CREATE TABLE buddy_inventory (
    item_id     TEXT PRIMARY KEY,
    item_type   TEXT NOT NULL CHECK(item_type IN ('hat','outfit','background','animation')),
    item_name   TEXT NOT NULL,
    cost        INTEGER NOT NULL,          -- star cost
    is_owned    INTEGER NOT NULL DEFAULT 0,
    is_equipped INTEGER NOT NULL DEFAULT 0,
    preview_url TEXT                        -- asset path
);

-- Free-time windows
CREATE TABLE free_time_windows (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    day_type    TEXT NOT NULL CHECK(day_type IN ('weekday','weekend')),
    start_time  TEXT NOT NULL,   -- HH:MM
    end_time    TEXT NOT NULL,   -- HH:MM
    is_enabled  INTEGER NOT NULL DEFAULT 1
);
```

### 10.2 Existing Tables (Unchanged from V1.5)

```
block_events       — individual block/verify events
daily_aggregates   — per-day rollup stats
streaks            — screen-free streak records
screen_time        — daily screen time per context (child/parent)
```

---

## 11. Implementation Roadmap & Timeline

### Overview

```
                    V2.0 DEVELOPMENT ROADMAP
    ─────────────────────────────────────────────────
    
    PHASE A: Foundation & Architecture     4 weeks
    PHASE B: Tier 1 (Sensory Redirection)  3 weeks
    PHASE C: Tier 2 (Gamified Buddy)       5 weeks
    PHASE D: Tier 3 (Time Banking)         5 weeks
    PHASE E: Tapering Engine               2 weeks
    PHASE F: Reports & Polish              3 weeks
    PHASE G: Play Store & Launch           2 weeks
                                          ─────────
                                    TOTAL: 24 weeks
    
    LAUNCH STRATEGY:
    ├─ Tier 1 Launch (MVP+)    → end of Phase B (Week 7)
    ├─ Tier 2 Launch (Growth)  → end of Phase C (Week 12)
    ├─ Tier 3 Launch (Full)    → end of Phase F (Week 22)
    └─ Play Store Launch       → end of Phase G (Week 24)
```

---

### Phase A — Foundation & Architecture (Weeks 1–4)

**Goal:** Build shared infrastructure that all tiers depend on.

| Week | Task | Deliverable |
|------|------|-------------|
| 1 | Merge `feature/usagestats-polling` branch into main | Unified codebase with hybrid detection |
| 1 | Design and implement `ChildProfileManager` + database schema | Profile CRUD, tier assignment |
| 1 | Create `TierEngine` interface and registry pattern | Extensible tier architecture |
| 2 | Implement `TaperingScheduler` (shared across tiers) | Tapering config store + daily limit calculation |
| 2 | Implement `FreeTimeWindowManager` | Schedule evaluation logic |
| 2 | Implement `AppCategoryManager` + database table | Category CRUD per package |
| 3 | Build child profile onboarding screens (S-03, S-04) in Flutter | Working profile setup flow |
| 3 | Refactor app selection screen for tier-aware mode (S-05) | Block-list (T1/T2) + category (T3) modes |
| 4 | Update `BlockingOverlayActivity` to dispatch to tier-specific overlay | Tier-aware overlay routing |
| 4 | Update `AppBlockingController` to consult `ChildProfileManager` for tier | Tier-aware blocking decisions |
| 4 | New platform channel methods for profile, tier, tapering | End-to-end data flow |

**Milestone:** App knows the child's tier and routes to the correct overlay.

---

### Phase B — Tier 1: Sensory Redirection (Weeks 5–7)

**Goal:** Fully working toddler-mode with calming video replacement.

| Week | Task | Deliverable |
|------|------|-------------|
| 5 | Implement `SensoryRedirectionEngine` | Video library management, rotation logic |
| 5 | Build Tier 1 blocking overlay (S-16) | Full-screen video player, no child UI, hidden parent area |
| 6 | Implement daily time limit counter in Tier 1 mode | Timer that blocks ALL apps when limit reached |
| 6 | Add custom video import (parent adds from gallery) | Video picker + copy to app storage |
| 6 | Integrate with `FreeTimeWindowManager` | Blocking pauses during free windows |
| 7 | Night mode implementation (all tiers) | After bedtime → all apps redirect |
| 7 | Testing and polish for Tier 1 | Edge cases, device testing, UX polish |

**🚀 TIER 1 LAUNCH — Sideload release. Usable product for toddler parents.**

---

### Phase C — Tier 2: Gamified Buddy (Weeks 8–12)

**Goal:** Virtual pet Buddy with star economy, challenges, and cosmetic rewards.

| Week | Task | Deliverable |
|------|------|-------------|
| 8 | Design 6 Buddy Lottie animations (commission or create) | JSON animation files |
| 8 | Implement `GamifiedBuddyEngine` — state machine + star economy | Buddy state transitions, star CRUD |
| 9 | Build Buddy Home screen (S-11) | Animated Buddy, star counter, energy bar |
| 9 | Build Tier 2 blocking overlay (S-17) | Buddy sad state, star penalty, motivational message |
| 10 | Implement daily challenge system | `ChallengeGenerator`, progress tracking, completion |
| 10 | Build Daily Challenge screen (S-13) | Challenge display, progress ring, history |
| 11 | Build Reward Shop (S-12) | Grid layout, purchase flow, equip cosmetics |
| 11 | Implement `buddy_inventory` table + cosmetic asset loading | Item ownership, equipped state |
| 12 | Energy bar integration (T2 screen time = energy metaphor) | Visual depletion + "Buddy sleeping" state |
| 12 | Testing and polish for Tier 2 | Star balance edge cases, animation performance |

**🚀 TIER 2 LAUNCH — Full gamified experience for ages 4–8.**

---

### Phase D — Tier 3: Time Banking + Flashcards (Weeks 13–17)

**Goal:** Earn-to-use screen time via educational flashcard system.

| Week | Task | Deliverable |
|------|------|-------------|
| 13 | Implement `TimeBankEngine` — balance, deposits, withdrawals, caps | Core banking logic |
| 13 | Implement `FlashcardManager` — pack loading, question serving, result tracking | Flashcard service |
| 14 | Bundle flashcard content packs (Math G1–G5, Vocab, Science, General Knowledge) | JSON data files (~500–1000 cards total) |
| 14 | Build Flashcard Session screen (S-14) | Card flip UI, answer selection, streak tracking |
| 15 | Build Time Bank screen (S-15) | Balance display, earnings/spending breakdown |
| 15 | Build Tier 3 blocking overlay (S-18) | Bank balance gate, deduction info, earn CTA |
| 16 | Implement real-time time deduction while using categorised apps | Background timer that withdraws from bank |
| 16 | Build Custom Flashcard Editor (S-22) + Pack Manager (S-21) | Parent content creation |
| 17 | Parent bonus gift flow (face/PIN → deposit minutes) | S-25 integration with time bank |
| 17 | Testing and polish for Tier 3 | Balance sync, deduction accuracy, edge cases |

**🚀 TIER 3 LAUNCH — Full earning system for ages 8–11.**

---

### Phase E — Progressive Tapering Engine (Weeks 18–19)

**Goal:** Automated weekly limit reduction across all tiers.

| Week | Task | Deliverable |
|------|------|-------------|
| 18 | Implement `TaperingScheduler` run logic (daily limit adjustment) | Auto-reducing limits per schedule |
| 18 | Build Tapering Progress screen (S-24) | Graph, status, pause/resume/adjust |
| 19 | Weekly check-in notification with recommendation | Smart notification: reduce / hold / ease |
| 19 | Integrate tapering with all 3 tiers | T1: raw minutes, T2: energy cap, T3: earn cap |

---

### Phase F — Reports, Freemium & Polish (Weeks 20–22)

**Goal:** Analytics overhaul, paywall, and full polish pass.

| Week | Task | Deliverable |
|------|------|-------------|
| 20 | Build redesigned Reports screen (S-19) | Weekly view, charts, trends, tier-specific metrics |
| 20 | Build Free-Time Window Editor (S-23) | Schedule editor UI |
| 21 | Implement freemium paywall | Feature gating: free vs premium |
| 21 | Build redesigned Dashboard (S-10) with tier-aware sections | Unified dashboard |
| 22 | Full integration testing across all tiers | End-to-end flows |
| 22 | Performance optimisation (animations, database, battery) | Profiling and fixes |
| 22 | UX polish pass (all 26 screens) | Consistent design language |

---

### Phase G — Play Store Preparation & Launch (Weeks 23–24)

**Goal:** Google Play Store listing and public launch.

| Week | Task | Deliverable |
|------|------|-------------|
| 23 | Play Store compliance audit (accessibility service justification, permissions) | Compliance report |
| 23 | Build Android App Bundle (AAB) with asset delivery | Optimised distribution |
| 23 | Create store listing assets (screenshots, feature graphic, description) | Marketing materials |
| 24 | Closed beta testing via Play Console | Beta feedback |
| 24 | Address beta feedback, final bug fixes | Stable release |
| 24 | Public launch on Google Play Store | 🎉 **V2.0 LIVE** |

---

### Effort Summary

| Phase | Duration | Cumulative | Launch Event |
|-------|----------|------------|--------------|
| A — Foundation | 4 weeks | Week 4 | — |
| B — Tier 1 | 3 weeks | Week 7 | 🚀 Tier 1 (sideload) |
| C — Tier 2 | 5 weeks | Week 12 | 🚀 Tier 2 (sideload) |
| D — Tier 3 | 5 weeks | Week 17 | 🚀 Tier 3 (sideload) |
| E — Tapering | 2 weeks | Week 19 | — |
| F — Reports & Polish | 3 weeks | Week 22 | — |
| G — Play Store | 2 weeks | Week 24 | 🎉 Public launch |

**Total: 24 weeks (~6 months) for full V2.0**

---

## 12. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Android OS kills background service | High | Critical | Foreground service + WorkManager + BOOT_COMPLETED + battery exemption (already mitigated in V1) |
| Child holds photo of parent to camera | Medium | High | Liveness detection (blink/head movement) — v2.1 feature |
| Google Play rejects Accessibility Service | Medium | High | UsageStatsManager pivot makes Accessibility optional; justify via app description |
| Lottie animations performance on low-end devices | Medium | Medium | Test on budget devices; provide static fallback |
| Flashcard content quality/accuracy | Medium | Medium | Curate from verified educational sources; community review |
| Star/time bank economy imbalances | Medium | Medium | Configurable rates; parent can adjust; analytics to detect outliers |
| APK size bloat from video + Lottie + flashcards | High | Medium | AAB + asset packs; compress all assets; lazy load flashcard packs |
| Child factory-resets device | Low | High | Out of scope; document as limitation |
| Tapering too aggressive → child frustration | Medium | Medium | Conservative defaults; parent override; pause option; weekly check-in |
| Free tier too limited → low conversion | Medium | High | Free tier includes full blocking (core value); premium adds convenience and engagement |
| Parent finds self-tracking intrusive | Low | Medium | Strictly opt-in; gentle tone; easy disable |
| Concurrent tier usage if age changes | Low | Low | Manual tier change by parent; data migrates with warning |

---

## 13. Appendix A — V1 Rectifications

During V1 and V1.5 development, 11 critical bugs were discovered and resolved:

| # | Issue | Root Cause | Fix |
|---|-------|-----------|-----|
| R-01 | Face detection silently failing | `BitmapFactory.decodeByteArray()` skips EXIF rotation | Added ExifInterface parsing + Matrix rotation |
| R-02 | Embedding extraction crash | MobileFaceNet outputs 192-d, not 128-d | Changed `EMBEDDING_SIZE` to 192 |
| R-03 | Battery optimization permission crash | Missing `FLAG_ACTIVITY_NEW_TASK` for non-Activity context | Added flag to intent |
| R-04 | Onboarding always restarts | No check for existing faces/PIN on launch | Added completion check in `initState()` |
| R-05 | Verification loop — overlay reappears | `resetLastDetected()` cleared tracking → re-detection | Replaced with `overlayActive` flag + grace period |
| R-06 | PIN screen UI overflow | Fixed layout without scroll | Added SafeArea + SingleChildScrollView |
| R-07 | Guardians screen UI overflow | `Spacer()` overflow on small screens | Replaced with scrollable + pinned buttons |
| R-08 | Release build stuck on loading | R8 strips Gson/TFLite/ML Kit classes | Comprehensive ProGuard rules + try/catch |
| R-09 | Accessibility ungrantable on Android 13+ | "Restricted settings" blocks sideloaded apps | Added instructional text for manual override |
| R-10 | No way to skip onboarding steps | All steps required completion | Added skip buttons with confirmation |
| R-11 | Accessibility stops detecting | overlayActive flag stuck + recheck timer dies + no watchdog | Staleness timeout + self-sustaining timer + foreground service ping |

---

## 14. Appendix B — Glossary

| Term | Definition |
|------|-----------|
| **Accessibility Service** | Android API to observe UI events (window changes) across the system |
| **App Category** | Classification of an app (Educational, Neutral, Distracting, Addictive) determining its impact on the time bank and star economy |
| **Buddy** | KidShield's mascot character — evolves from static illustration (V1.5) to virtual pet with states and cosmetics (V2 Tier 2) |
| **Cosine Similarity** | Metric measuring angle between face embedding vectors (1.0 = identical) |
| **Daily Challenge** | A Tier 2 task (e.g., "stay off YouTube for 1 hour") that awards bonus stars on completion |
| **Energy Bar** | Tier 2 visual metaphor for remaining daily screen time — Buddy's energy depletes with usage |
| **Flashcard Pack** | A collection of educational question-answer cards used in Tier 3 to earn screen time |
| **Foreground Service** | Android service with persistent notification, resistant to OS killing |
| **Free-Time Window** | A parent-configured schedule period when blocking is suspended |
| **Lottie** | Open-source animation library (Airbnb) for rendering After Effects animations as JSON |
| **MobileFaceNet** | Lightweight face recognition neural network; outputs 192-d embedding per face |
| **Platform Channel** | Flutter mechanism for Dart ↔ native Android communication |
| **Progressive Tapering** | Automated gradual reduction of daily screen time limits over weeks |
| **Sensory Redirection** | Tier 1 strategy: replacing blocked content with calming videos instead of a blocking wall |
| **Shuffle-Bag Algorithm** | Randomization ensuring all items shown once before repeats |
| **Star Economy** | Tier 2 virtual currency: earned for healthy habits, spent on Buddy cosmetics |
| **Tier** | One of three age-adapted strategies: T1 (0–4), T2 (4–8), T3 (8–11) |
| **Time Bank** | Tier 3 virtual balance of earned screen time in minutes |
| **Time-of-Day Bucketing** | Content selection based on current time period (morning/afternoon/evening/night) |
| **UsageStatsManager** | Android API for device usage history; primary detection method post-pivot |

---

## 15. Scope Boundaries

### ✅ Completed (V1 + V1.5)

- Face recognition blocking + PIN fallback
- Permission wizard + accessibility-based monitoring
- Manual app block-list with search
- Full-screen blocking overlay (3 modes: video, Buddy, classic)
- Device admin uninstall protection
- Configurable re-verification interval
- Buddy mascot (105 messages, 4 poses, video overlay)
- Usage tracking (SQLite, block events, daily aggregates)
- Screen-free streak tracking + milestone notifications
- Dashboard report card + weekly summary
- Parent self-awareness (opt-in screen time)
- 100% offline, ProGuard release build
- UsageStatsManager hybrid detection (feature branch)

### 📋 V2.0 Scope

- Child profiling + onboarding questionnaire
- Age-tiered system (Tier 1, 2, 3)
- Progressive Tapering Engine
- App category intelligence
- Scheduled free-time windows
- Buddy virtual pet + star economy (Tier 2)
- Time banking + flashcard system (Tier 3)
- Freemium paywall
- Redesigned dashboard + reports with charts
- Google Play Store distribution

### 🔮 Future (V3+)

| Feature | Target |
|---------|--------|
| Multiple child profiles | V3 |
| iOS support | V3 |
| Remote management (parent phone → child phone) | V3 |
| Cloud sync / backup | V3 |
| Liveness detection (anti-spoofing) | V2.1 |
| AI-powered contextual Buddy responses | V3 |
| Voice lines / TTS for Buddy | V3 |
| Family challenges ("Tech-free dinner streak") | V3 |
| Community flashcard marketplace | V3 |

---

*End of document. V1 (MVP): Complete. V1.5: Complete. V2.0: Implementation plan ready.*
