# AGENTS.md — Chainloom

Instructions for any AI agent (Claude Code, Codex, others) working in this repo. `CLAUDE.md` points here.

- Read `README.md` first. The Android source is in `ChainloomAndroid/`; the workflow in `.github/workflows/build-chainloom-apk.yml` builds it from the checkout.
- Make changes on a branch and open a pull request: the workflow builds every pull request, so wait for it to pass before merging.
- This repo is **public**. Never commit keys, keystores, tokens or personal details.
- Version 1 has no ads, analytics, accounts or network permission. Do not add any without Jacob's OK.
- Toolchain: JDK 17, Gradle 8.13, Android SDK 36.
- End commit messages with `Agent: Claude` or `Agent: Codex`.
