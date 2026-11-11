# Chainloom — Android game prototype

Chainloom is a one-thumb offline puzzle/strategy game built to combine the immediate readability of block puzzles with deeper color-chain planning, cascades, and limited emergency tools.

## What is playable
- 8×8 board and three-piece tray.
- Tap-to-place polyomino pieces; rotate freely.
- Full rows/columns clear.
- Large connected same-color groups clear and collapse under gravity, enabling cascades.
- Gray **Knots** add escalating late-run pressure.
- **Pulse** is a charged emergency clear.
- **Shift** redraws the remaining tray; rewards are deliberately finite so a run cannot create an infinite rescue economy.
- Normal high-score mode and a globally deterministic UTC **Daily Loom** challenge.
- Local high scores and save/resume.
- Haptics, simple sound cues, particles, screen shake, and flash feedback.

## Privacy / monetization
Version 1 contains no advertising SDK, analytics SDK, account system, network permission, in-app purchase, energy timer, loot box, or pay-to-win mechanic. Game state and scores are stored locally in Android SharedPreferences.

## Android configuration
- Package: `com.chainloom.game`
- minSdk: 23
- targetSdk / compileSdk: 36
- Android Gradle Plugin: 8.12.2
- Required Gradle: 8.13
- Required JDK: 17

## Fastest build path
1. Install current stable Android Studio (Quail 3 / 2026.1.3 Patch 1 or newer).
2. Open this folder.
3. Install Android SDK Platform 36 if Android Studio prompts you.
4. This handoff does not include the binary `gradle-wrapper.jar`. Either let Android Studio configure Gradle 8.13, or run the included bootstrap script once:
   - Windows PowerShell: `powershell -ExecutionPolicy Bypass -File .\bootstrap-gradle.ps1`
   - macOS/Linux: `bash ./bootstrap-gradle.sh`
5. After the wrapper is created: `./gradlew assembleDebug` (Windows: `.\gradlew.bat assembleDebug`).
6. Install the debug APK from `app/build/outputs/apk/debug/app-debug.apk`, or press **Run** in Android Studio with a phone attached.

For a signed Google Play bundle, use **Build > Generate Signed App Bundle or APK > Android App Bundle** in Android Studio. See `LAUNCH_GUIDE.md` for the complete release checklist.

## QA
See `QA_NOTES.md`. The engine has deterministic, persistence, invariant, balance, and resume-continuation stress tests. Android UI source was also compiled against an Android API stub surface in the build environment to catch Java/signature mistakes.

## Naming note
“Chainloom” is a working product name. A web search did not reveal a matching Android game, but unrelated uses of “ChainLoom/Chainloom” exist. Perform Play Store and trademark clearance before commercial launch and rename the package before the first public upload if you choose a different brand.
