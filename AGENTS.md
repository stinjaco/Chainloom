# Chainloom - instructions for Codex

Same as `CLAUDE.md`. Essentials: public repo, no secrets. The repo's only file is `.github/workflows/build-chainloom-apk.yml`,
which embeds the whole Android project (`ChainloomAndroid/`, Java, package `com.chainloom.game`) as a base64 zip
(`CHAINLOOM_B64`), decodes it, and builds a debug APK with Gradle 8.13 / JDK 17 / Android 36. Do not hand-edit or print the
base64; edit in a scratch copy, or propose moving the source into normal files (needs Jake's approval, it changes CI). No tests
run in CI. Do not change the workflow or versions unasked.
