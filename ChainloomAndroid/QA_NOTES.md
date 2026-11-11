# Five-pass QA / redesign log

## Pass 1 — core loop / runaway-session check
**Found:** Gravity plus a 5-cell color threshold made the board self-clean too easily; random play could run for thousands of moves.

**Changed:** Raised the initial color-cluster threshold to 6 and added escalating neutral **Knot** pressure beginning after move 18. Knots do not join color clusters and are removed by full lines or Pulse.

## Pass 2 — rescue economy and difficulty curve
**Found:** A poor random player could survive too long by automatically spending Pulse and Shift. The fairness generator was also overprotective.

**Changed:** Normal play starts with one Shift; Shift rewards are finite and banked Shifts cap at 3. Pulse charges more slowly. Knot pressure escalates with run length. The color-link requirement rises during very long runs. Dead-tray correction retries only a few times instead of guaranteeing survival.

## Pass 3 — persistence and Daily Loom reproducibility
**Checked:** Save/import round-trip preserves board, score, tray, meter, tools and move count. Two engines with the same Daily seed follow identical state transitions.

**Changed:** Daily Loom uses a UTC date seed so every player gets the same challenge worldwide.

## Pass 4 — Android UI compile and daily-score edge case
**Checked:** `MainActivity` + custom `GameView` compiled against a local Android API stub surface. Portrait geometry and touch regions were reviewed.

**Found:** Resuming yesterday's saved Daily after UTC midnight could credit the score to today's Daily key.

**Fixed:** Daily best keys are derived from the engine's saved seed, keeping resumed runs attached to the correct challenge date.

## Pass 5 — release-level determinism, rotation, and marathon stress
**Found 1:** The old save format restored the visible game state but could not restore `java.util.Random`'s exact hidden state. Future pieces could diverge after reopening the app.

**Fixed:** Replaced it with a small explicit-state deterministic generator and upgraded saves to `CL2`. Added a resume-and-continue test that compares state across future piece refills and pressure events.

**Found 2:** End-of-run checks examined only each piece's current orientation. A run could be declared over even when rotating a piece would make a legal placement.

**Fixed:** Game-over and fairness checks now consider all four free rotations.

**Found 3:** Extreme auto-rescue stress could still create marathon runs.

**Fixed:** Rescue rewards are finite, late Pulse recharge fades out, Knot frequency escalates to every move in extreme runs, and the color-chain threshold increases again in the deep endgame. This affects marathon play rather than the opening difficulty.

### Final automated checks
- Pure Java engine + harness compile: **PASS**
- Android Activity/View source compile against API stubs: **PASS**
- XML manifest/resources parse: **PASS**
- 100-game invariant suite: **PASS**
- Save round trip: **PASS**
- Same-seed Daily determinism: **PASS**
- Save/resume future-RNG determinism: **PASS**
- 1,000 random-player runs without rescue automation: average **82.5 moves**, min **8**, max **628**
- 1,000 deliberately marathon-style runs that automatically exploit every available rescue: average **620.1 moves**, max **1,663**, all finished before the 2,000-move stress ceiling
- Manifest/source scan for INTERNET permission, API keys, secrets, TODO/FIXME: **PASS**

### Environment limitation
The execution environment used for this handoff did not contain the real Android SDK/Gradle binaries and would not permit fetching binary SDK archives into the build container. Therefore I could not produce or device-install a real APK here. The Java game engine and full Android UI source were compiled/tested as described above; the final APK/AAB should be built in Android Studio and tested on physical phones before publishing.
