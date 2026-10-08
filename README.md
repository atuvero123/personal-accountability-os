# Accountability OS — Native Android

A native Android accountability system for the user's Life Reset plan: spiritual life, routines, phone use, finance, savings, work, Coffee Quality learning, drumming, Springs of Life, Project Oracle, review and diagnostics.

## Current starter

This repository contains the native Android starter for the Life Reset system. It includes the 06:30–23:00 daily operating plan, deliberate recreation/rest, accountability states, finance and savings tracking, spiritual tracking, phone UsageStats integration, reminders, a ChatGPT share-sheet report, and a Growth area for weekly drumming plus the assistant-led Coffee Quality course.

## Weekly growth principle

### Drumming
Each active skill has measurable evidence rather than relying only on a confidence score. The path is **Learn → Secure → Groove → Fills → Song → Master**. A skill can define a learning tempo, secure tempo/range, number of groove applications, number of fill variations, and a song-application requirement. Evidence is retained with the skill so mastery means demonstrated use, not simply pressing a button. Mastered skills remain in history while the next skill can be started with a fresh evidence record. A separate worship song is learned section-by-section during the week, with the goal of a complete play-through by Sunday. The first seeded skill is the 5-stroke roll; the worship-song title is intentionally left for the user to choose.

### Coffee Quality
The Coffee Quality course is assistant-led. The app records the current topic, status and practical results using **Learn → Practice → Apply → Review**. Saving a lesson creates a dated history record tied to that specific topic, clears the working form, and advances the course to the next roadmap item. Saved lessons can be reopened later, so learning accumulates instead of being overwritten by the next session. The roadmap starts with foundations and progresses through varieties/origins, harvesting, processing, fermentation/drying, green grading, sensory/cupping, roasting, brewing, quality control and advanced sensory/Q Grader preparation. The course is organized now but does not automatically force a daily study session.

## Recreation principle

Entertainment is intentionally included. Movies, games and casual rest are legitimate planned activities. The goal is deliberate recreation rather than aimless scrolling.

## Build

GitHub Actions builds a debug APK on pushes to main or manually from Actions → Build Android APK → Run workflow. The workflow installs Gradle 8.9 directly.

## Android permissions

- Android 13+: notification permission.
- UsageStats: the user must enable Usage Access in Android Settings.

## Important boundaries

Finance is currently local/manual; no bank or Mobile Money credentials are accessed. The first ChatGPT integration is a share-sheet daily report. Deeper AI integration can be added later through an authenticated backend/API.
