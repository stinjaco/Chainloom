# Build the Chainloom APK with GitHub Actions

This project now includes `.github/workflows/build-apk.yml`.

1. Put the project in a GitHub repository.
2. Open the repository's **Actions** tab.
3. Select **Build Chainloom APK**.
4. Choose **Run workflow**.
5. When the build finishes, open that run and download the **Chainloom-debug-apk** artifact.
6. Unzip the artifact and transfer `app-debug.apk` to your Android phone.
7. Tap the APK on the phone and allow installs from that source when Android prompts you.

The APK is a debug/test build for direct installation. A Play Store release should use a separately generated release signing key and an Android App Bundle (`.aab`).
