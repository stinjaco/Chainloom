# AGENTS.md — Chainloom

Instructions for any AI agent (Claude Code, Codex, others) working in this repo. `CLAUDE.md` points here.

- Read `README.md` first. The Android source is not checked in as files: it is a base64 zip inside `.github/workflows/build-chainloom-apk.yml`.
- Editing that zip by hand is error-prone. If you need to change the game, unpack the source, and ask Jacob before committing it as normal files and rewriting the workflow to build from the checkout.
- This repo is **public**. Never commit keys, keystores, tokens or personal details.
- Version 1 has no ads, analytics, accounts or network permission. Do not add any without Jacob's OK.
- Toolchain: JDK 17, Gradle 8.13, Android SDK 36.
- End commit messages with `Agent: Claude` or `Agent: Codex`.
- Public repo: no internal IP addresses, hostnames, home-network or hub file paths, tokens or keys in any file, commit message or PR text. Use generic wording.
