# Accountability OS — Native Android

A native Android accountability system for the user's Life Reset plan: spiritual life, routines, phone use, finance, savings, work, Coffee Quality learning, drumming, Springs of Life, Project Oracle, review and diagnostics.

## Current starter

This repository contains the native Android starter for the Life Reset system. It includes the 06:30–23:00 daily operating plan, deliberate recreation/rest, accountability states, finance and savings tracking, spiritual tracking, phone UsageStats integration, reminders and a ChatGPT share-sheet report.

## Recreation principle

Entertainment is intentionally included. Movies, games and casual rest are legitimate planned activities. The goal is deliberate recreation rather than aimless scrolling.

## Build

GitHub Actions builds a debug APK on pushes to main or manually from Actions → Build Android APK → Run workflow. The workflow installs Gradle 8.9 directly.

## Android permissions

- Android 13+: notification permission.
- UsageStats: the user must enable Usage Access in Android Settings.

## Important boundaries

Finance is currently local/manual; no bank or Mobile Money credentials are accessed. The first ChatGPT integration is a share-sheet daily report. Deeper AI integration can be added later through an authenticated backend/API.
