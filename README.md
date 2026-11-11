# Chainloom

Chainloom is a one-thumb offline Android puzzle game: an 8×8 board, a three-piece tray, row and column clears, colour-chain clears with gravity and cascades, and a daily challenge. No ads, analytics, accounts or network permission.

- Package: `com.chainloom.game`
- minSdk 23, target/compile SDK 36, JDK 17, Gradle 8.13

## Where the source is

There are no source files checked in. The whole Android project (`ChainloomAndroid/`) is stored as a base64 zip inside `.github/workflows/build-chainloom-apk.yml`. The workflow unpacks it and builds a debug APK.

To get the source out locally, copy the block between the `CHAINLOOM_B64` markers into a file and run:

```
base64 -d chainloom.b64 > chainloom.zip && unzip chainloom.zip
```

The unpacked project has its own `README.md`, `PRODUCT_BRIEF.md`, `QA_NOTES.md`, `LAUNCH_GUIDE.md`, `STORE_LISTING.md` and `PRIVACY_POLICY.md`.

## Build

Run the "Build Chainloom APK" workflow (Actions tab, or any push to `main`). The APK is uploaded as the `Chainloom-debug-apk` artifact and kept for 30 days.

## Note

This repo is public.
