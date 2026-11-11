# Chainloom - instructions for Claude Code

Owner: Jacob (Jake), not a professional developer. Plain words; say what you verified and what you did not.
Public repo: never add secrets, signing keys or personal data. Codex reads `AGENTS.md`; keep the two in agreement.

## What this is
Chainloom is a one-thumb, offline Android puzzle game prototype (8x8 board, polyomino pieces, color-group clears with gravity,
Knots, Pulse/Shift tools, Daily Loom challenge). Package `com.chainloom.game`, plain Java, minSdk 23, target/compile SDK 36.

## Unusual layout (read this first)
The repo has no source files on disk. Its only file is `.github/workflows/build-chainloom-apk.yml`, which contains the whole
Android project as a base64-encoded zip (`CHAINLOOM_B64` block). On each run it decodes the zip into `ChainloomAndroid/`,
checks `settings.gradle.kts` exists, runs `gradle :app:assembleDebug` (Gradle 8.13, JDK 17, Android platform 36 / build-tools
36.0.0) and uploads `app-debug.apk` as artifact `Chainloom-debug-apk`. Triggers: manual (`workflow_dispatch`) and pushes to
main/master.

The zip contains (verified by decoding it into a scratch folder): `ChainloomAndroid/app/src/main/java/com/chainloom/game/`
(`GameEngine.java`, `GameView.java`, `MainActivity.java`), a test harness `app/src/test/.../GameEngineHarness.java`, Gradle
Kotlin-DSL files, and docs (`README.md`, `PRODUCT_BRIEF.md`, `QA_NOTES.md`, `LAUNCH_GUIDE.md`, `PRIVACY_POLICY.md`,
`STORE_LISTING.md`). Its docs mention `.github/workflows/build-apk.yml`; the real file is `build-chainloom-apk.yml`.

## Working on it
- Do not hand-edit the base64 block. To change the game, decode the zip into a new empty scratch folder, edit there, re-zip,
  and re-encode; better, propose moving the project into normal files (`ChainloomAndroid/...`) in the repo and simplifying the
  workflow - that needs Jake's approval because it changes CI.
- No tests run in CI (it only builds the debug APK). The engine harness exists in the zip but was not run here.
- Do not change the workflow, dependencies or versions without Jake's request.
- The name "Chainloom" is a working title; trademark/Play Store checks are listed in the zip's README.

## Cost and speed
Lightest model that fits: Haiku-class for searching/reading, Sonnet-class for edits. Never print the base64 block.
