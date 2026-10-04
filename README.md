# Accountability OS — Native Android

A native Android accountability system for the user's Life Reset plan: spiritual life, routines, phone use, finance, savings, work, Coffee Quality learning, drumming, Springs of Life, Project Oracle, review and diagnostics.

## Current starter

This repository contains the native Android starter for the Life Reset system. It includes the 06:30–23:00 daily operating plan, deliberate recreation/rest, accountability states, finance and savings tracking, spiritual tracking, phone UsageStats integration, reminders, a ChatGPT share-sheet report, and a Growth area for weekly drumming plus the assistant-led Coffee Quality course.

## Weekly growth principle

### Drumming
Each week has one main skill/rudiment. The learning path is: understand and play the technique → explore groove use → build fills → apply it in a suitable song. A separate worship song is learned section-by-section during the week, with the goal of a complete play-through by Sunday. The first seeded skill is the 5-stroke roll; the worship-song title is intentionally left for the user to choose.

### Coffee Quality
The Coffee Quality course is assistant-led. The app records the current stage, lesson, status and practical results. The study method is **Learn → Practice → Apply → Review**, using the user's coffee-station work for practical application where possible. The roadmap starts with foundations and progresses through varieties/origins, harvesting, processing, fermentation/drying, green grading, sensory/cupping, roasting, brewing, quality control and advanced sensory/Q Grader preparation. The course is organized now but does not automatically force a daily study session.

## Recreation principle

Entertainment is intentionally included. Movies, games and casual rest are legitimate planned activities. The goal is deliberate recreation rather than aimless scrolling.

## Build

GitHub Actions builds a debug APK on pushes to main or manually from Actions → Build Android APK → Run workflow. The workflow installs Gradle 8.9 directly.

## Android permissions

- Android 13+: notification permission.
- UsageStats: the user must enable Usage Access in Android Settings.

## Important boundaries

Finance is currently local/manual; no bank or Mobile Money credentials are accessed. The first ChatGPT integration is a share-sheet daily report. Deeper AI integration can be added later through an authenticated backend/API.
