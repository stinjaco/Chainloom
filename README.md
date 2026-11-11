# Chainloom

Chainloom is a one-thumb offline Android puzzle game: an 8×8 board, a three-piece tray, row and column clears, colour-chain clears with gravity and cascades, and a daily challenge. No ads, analytics, accounts or network permission.

- Package: `com.chainloom.game`
- minSdk 23, target/compile SDK 36, JDK 17, Gradle 8.13

## Where the source is

The Android project is in `ChainloomAndroid/`. It has its own `README.md`, `PRODUCT_BRIEF.md`, `QA_NOTES.md`, `LAUNCH_GUIDE.md`, `STORE_LISTING.md` and `PRIVACY_POLICY.md`.

Until November 2026 the source was stored as a base64 zip inside the build workflow. It was unpacked into normal files unchanged; the old form is in git history.

## Build

The "Build Chainloom APK" workflow runs on every push to `main`, on pull requests, and on demand from the Actions tab. The debug APK is uploaded as the `Chainloom-debug-apk` artifact and kept for 30 days.

Locally: open `ChainloomAndroid/` in Android Studio, or run `gradle :app:assembleDebug` there with Gradle 8.13.

## Note

This repo is public.
