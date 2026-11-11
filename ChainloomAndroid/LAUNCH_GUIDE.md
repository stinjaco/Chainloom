# Chainloom launch guide — Android / Google Play

## 1. Build and install it on your phone

### Recommended tools
Use current stable Android Studio Quail 3 (2026.1.3 Patch 1 or newer). This project targets Android 16 / API 36 and uses AGP 8.12.2, Gradle 8.13 and JDK 17.

### First setup
1. Install Android Studio.
2. In Android Studio > SDK Manager, install **Android 16 / API 36** SDK Platform. Let Studio install the matching build tools it recommends.
3. Open the `ChainloomAndroid` folder as a project.
4. If Gradle wrapper files are not yet present, run one included bootstrap script from the project root. It downloads the official Gradle 8.13 distribution from `services.gradle.org` and asks Gradle itself to create standard wrapper files.
5. Re-open/sync the project if Android Studio asks.
6. Connect an Android phone with Developer options + USB debugging enabled, or create an emulator.
7. Press **Run**. For a file build, run `./gradlew assembleDebug` (`.\gradlew.bat assembleDebug` on Windows).

Debug APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## 2. Physical-device QA before publishing
Test at minimum:
- New game, rotate, place, line clear, color clear, cascade.
- Pulse and Shift.
- Close app mid-run, reopen, Resume, and continue several tray refills.
- Daily Loom on two devices: same day should produce the same underlying sequence for the same actions.
- Cross UTC midnight with an old Daily save and verify its best score stays on the original Daily.
- Lose a run where the only legal move requires rotation; it must not show game over early.
- Small phone and large phone portrait layouts.
- Android back button / gesture, app pause/resume, sound/haptics.
- Airplane mode: entire game should still work.
- At least 30–60 minutes of repeated play for crashes, heat, battery drain, and UI fatigue.

## 3. Decide branding before the first Play upload
`Chainloom` is a working name. Web search found unrelated uses of ChainLoom/Chainloom, including a domain/AI name and an item name in another game, but no obvious matching Android puzzle game. That is **not** legal trademark clearance.

Before public release:
- Search Google Play for the exact name.
- Search USPTO / relevant trademark databases for software/game marks.
- Pick the permanent Android package ID before the first production upload; package IDs are painful to change later.

If renaming, replace:
- app label in `AndroidManifest.xml`
- visible strings in `GameView.java`
- `applicationId` and namespace in `app/build.gradle.kts`
- Java package folders/declarations

## 4. Create the Play developer account
For full public distribution, Google's current Android Developer Console documentation lists a one-time **US$25** registration fee and identity verification. Organization accounts require organization verification information such as a D-U-N-S number.

## 5. Create a signed Android App Bundle
In Android Studio:
1. **Build > Generate Signed App Bundle or APK**.
2. Select **Android App Bundle (AAB)**.
3. Create a release keystore and strong password.
4. Back up the keystore and credentials securely. Losing signing access can create a serious release-management problem.
5. Build `release`.

Expected bundle location is under:
`app/build/outputs/bundle/release/`

The project already targets API 36. Google says that starting **August 31, 2026**, new apps and updates submitted to Google Play must target Android 16 / API 36 or higher.

## 6. Play Console setup
Create the app as a **Game** and complete:
- App access.
- Ads declaration: v1 has no ads.
- Content rating questionnaire.
- Target audience / children declarations based on the audience you actually choose.
- Data safety: v1 source contains no network permission or data-collection SDK, so the intended declaration is no user data collected/shared. Verify the final binary and every SDK before submitting.
- Privacy policy URL. Google currently requires a privacy policy for every app, including apps that collect no data. Use `PRIVACY_POLICY.md` as the starting draft and host the finalized version publicly.
- Category, tags and developer contact information.

## 7. Store listing assets
Google currently requires:
- 512×512 32-bit PNG icon, max 1024 KB.
- 1024×500 feature graphic, JPEG or 24-bit PNG without alpha.
- At least two screenshots to publish.

For a game, aim for at least three real 9:16 portrait gameplay screenshots at 1080×1920 or better. Capture the actual app rather than making ads that show a different game. `STORE_LISTING.md` contains starter copy.

## 8. Testing track
Use **Internal testing** first for quick installs.

If the Play developer account is a personal account created after November 13, 2023, Google currently requires a **closed test with at least 12 testers opted in continuously for at least 14 days** before you can apply for production access. Recruit more than 12 so one dropout does not reset your launch plan.

Collect feedback on:
- “Did you understand the game without help?”
- “When did the first run stop being fun?”
- “Did a loss feel fair?”
- “Did Pulse/Shift feel like strategy or a bailout?”
- “Would you play the Daily tomorrow?”
- crashes / device model / Android version.

## 9. Metrics for the next build
This v1 intentionally has no analytics. For the first closed test, manual feedback is safer and preserves the privacy promise. If the game tests well and you later add analytics, update the privacy policy/Data safety declaration first.

The most useful product metrics would be:
- tutorial/help open rate and completion
- first-run moves and minutes
- first-session number of runs
- D1 / D7 return rate
- Daily Loom participation
- score distribution
- failure reason (no fit, Knot pressure, tool unused)

## 10. Monetization only after retention
Do not add forced interstitials to prove revenue before proving the game is worth returning to. Safer first experiments if retention is strong:
- one-time cosmetic theme pack
- optional supporter purchase
- cosmetic-only seasonal board skins
- rewarded ad for a cosmetic reroll, only if it does not affect competitive Daily fairness

Keep Daily leaderboards fair by never selling gameplay power.

## 11. Production release
After the test requirement is satisfied and feedback issues are fixed:
1. Increment versionCode/versionName.
2. Re-run the five-pass test suite and physical-device checklist.
3. Generate a new signed AAB.
4. Upload to Production.
5. Review Play's automated/pre-launch reports.
6. Fix blocking compatibility or policy issues.
7. Roll out gradually rather than immediately to 100% if Play Console offers staged rollout.

## Important handoff limitation
I could not build a real APK/AAB inside this execution environment because it does not contain the Android SDK/Gradle binary toolchain and blocks binary SDK archive downloads. The source has been compiled at the Java/API-stub level and stress-tested, but **a physical-device build/test is still mandatory** before you publish it.
