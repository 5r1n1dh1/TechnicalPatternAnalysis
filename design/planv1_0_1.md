# Technical Pattern Analysis — v1.0.1 Remediation Plan

**Type:** stabilisation release. v1.0 shipped code that *looks* complete but **does not build, boot, test, or deploy**. v1.0.1's entire goal: make every declared milestone actually run — green CI, installable APK, live Vercel endpoint, working daily pipeline — and correct the contract drifts the audit found. **No new features.**

Audit date: 2026-10-19. Baseline: commit `c41fe55`. Full evidence in §4.

---

## 1. Current state — verified facts

| System | Status | Evidence |
|---|---|---|
| **CI** | ❌ **RED** (1 run, failed in 7s) | `gh run view 39209239867` → `Invalid or corrupt jarfile android/gradle/wrapper/gradle-wrapper.jar` |
| **APK** | ❌ **Never produced** — zero artifacts in repo | `gh api .../actions/artifacts` → empty |
| **Vercel deploy** | ❌ **Never attempted** — `ci.yml` has no deploy job | `.github/workflows/ci.yml` (1 job: android-build) |
| **Kaggle** | ⚠️ Key exists but stored as a **repo variable (plaintext)**, never used by any running workflow | `gh variable list` shows `KAGGLE_API_KEY=KGAT_…` |
| **Secrets** | 🔴 **Security** — `VERCEL_API_KEY`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID`, `KAGGLE_API_KEY` were moved from secrets → variables; all four are now visible in plaintext to anyone with repo read access. `gh secret list` returns empty | `gh variable list` |
| **Server** | ❌ Does not import — `import fuzzywuzzy` (undeclared) at `server/api/search.py:11`, imported at startup by `index.py:12`; two incompatible import roots (`api.*` vs `server.core.*`) with no `conftest.py`/`pytest.ini` | grep |
| **Server API coverage** | ❌ 4 of 12 routers mounted (search, candles, resample, decision). `predictions`, `scan`, `technicals` exist but **unmounted**; `patterns`, `trend`, `fundamentals`, `tradeplan`, `macro` **have no router file at all** | `server/api/index.py:52-57`, `ls server/api/` |
| **Pattern engine** | ❌ Broken: only 2 of 5 detectors (`head_shoulders.py`, `cup_handle.py`), H&S geometry inverted, confidence hardcoded to 85, no double/triple, no flag, no prior-trend or volume-sequence rules | `server/core/detectors/` |
| **Pipeline** | ❌ `pipeline/eod.py` **SyntaxError** (line 137 missing operator; lines 187-191 contain embedded fake tool-call text). `train.py` and `scan.py` **missing**. `predict.py` uses an **unfitted** model with hardcoded metrics | `python3 -m py_compile` |
| **EOD workflow** | ❌ Lives at `pipeline/eod.yml` → **never runs** (must be `.github/workflows/`); no `actions/checkout` so its commit/push cannot work; `backfill` job has `schedule: null` | `pipeline/eod.yml:1,50` |
| **Android** | ❌ **Cannot compile**: no `AndroidManifest.xml`, no `res/`, no `proguard-rules.pro` (referenced at `app/build.gradle.kts:61`); gradle-wrapper.jar is **5274 bytes of ASCII text**, not a jar; `MainActivity` is a `@Composable`, not an Activity; AGP 7.4.2 vs `compileSdk 35` mismatch; Kotlin 2.x-only compose plugin on Kotlin 1.9; `ChartScreen.kt` has unresolvable imports and passes Compose `Color` to MPAndroidChart (needs `android.graphics.Color`); two JUnit tests live in `src/main/kotlin` (release build fails); `WatchlistViewModel` imports non-existent `com.tpa.app.database.*` | audit §4.2 items 1-8, 54 |
| **Android contracts** | ❌ TpaApi implements 7/12 endpoints; missing `health, search, trend, predictions, scan, macro`; wrong paths (`/api/trade-plan`, `/api/technical`); candle DTO uses `time/open/high/low/close` vs spec `t/o/h/l/c/v` (no volume); **zero `@Serializable` annotations** despite the serialization converter | `network/TpaApi.kt` |
| **Android features** | ❌ Volume sub-chart, macro strip, ML horizon accuracy/asOf, "Pattern hits today", watchlist (Room dep **absent**; `db/WatchlistDb.kt` is a 3-line stub), offline cache, dropdown menus (invoked as bare `Button(onClick=…)` statements — render nothing), capital input for position sizing, disclaimer footer — all missing | audit §4.2 items 18-21, 29-34 |
| **Git hygiene** | ⚠️ `android/.gitignore:3` ignores `gradle/` → wrapper jar + `libs.versions.toml` are gitignored; stray root `gradle/gradle-wrapper.properties` pins 9.5.1 (conflicts with android/ 8.9); filename `" SymbolScreen.kt"` has a leading space; `rootProject.name = "MyInvestinmentAssistant"` | audit §4.2 items 23-25, 53 |
| **Docs** | ⚠️ `design/api.md:437-438` falsely marks patterns/trend as implemented; `plan.md` §11 checklist still shows every task unchecked | grep |

**Bottom line:** 0 of 8 milestones are verifiably working. v1.0.1 is a build-it-for-real release, not a feature release.

---

## 2. v1.0.1 scope (in priority order)

### WS-0 — Security & release gate (do first, blocks everything)
1. Rotate + re-store all four credentials as **secrets**: `gh secret set VERCEL_API_KEY`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID`, `KAGGLE_API_KEY`; **delete** the plaintext variables (`gh variable delete …`). Rotate the Vercel token and Kaggle key at the provider first (they have been exposed).
2. `.github/workflows/ci.yml` rewritten with three jobs: `server-tests` (pytest), `android-build` (assembleRelease + upload-artifact), `deploy` (needs server-tests, push-to-main only, `vercel deploy --prod --cwd server` with secrets).
3. `server/vercel.json`: replace legacy `builds` with `{"functions": {"api/index.py": {"maxDuration": 30}}, "rewrites": [...]}`; add `server/.vercelignore`.
4. Post-merge smoke test with `curl` against the production `/api/health`; record the URL in `design/DEPLOYMENT.md`.

### WS-1 — Server bootable + testable
5. `server/conftest.py` + `pytest.ini`: one import root (`server.` package) everywhere; delete the duplicate top-level `import search/candles/resample` in `index.py`.
6. Remove `fuzzywuzzy` (implement prefix+substring boost natively) or declare it; delete `pytz` usage in favour of `zoneinfo` (no extra dep).
7. Mount **all** routers that exist; create the missing router files: `patterns.py`, `trend.py`, `fundamentals.py`, `tradeplan.py`, `macro.py`.
8. Fix `/api/candles` contract: query params (not body-model), enums exactly `{5m,15m,30m,1h,1d,1w,1M}` / `{1D,5D,1M,3M,6M,1Y,2Y,5Y,10Y}`, matrix sourced **only** from `core/constants.py`, output `{t,o,h,l,c,v}` IST, no disk cache (Vercel FS is read-only) — in-memory TTL 12h + Kaggle daily-bar fallback.
9. `requirements.txt` = declared imports exactly (add `pytest`, `httpx`, `scikit-learn`, `joblib`; remove unused `pandas-ta-classic`). `pipeline/requirements.txt` = single source (server reads it, no drift).
10. CI gate: `pytest server/tests` green **with** the new tests below.

### WS-2 — Pattern engine completed to spec
11. Fix `core/patterns.py` (`SettingsConfigDict` NameError; add SMA5 smoothing, sliding window, confidence math).
12. Add missing detectors: `double_triple.py`, `flag.py`; correct H&S shoulder/head geometry; compute stop, target, R:R, invalidation from the same code path (no hardcoded 85).
13. Add the trend-precondition and volume-sequence validity layers as shared mixins.
14. Create the **14 synthetic pattern fixtures** (`server/tests/fixtures/patterns/*.csv` + generators) and `tests/test_patterns.py` asserting: positive → exactly 1 detection with confidence ≥ 70; negative → 0.

### WS-3 — EOD pipeline made real
15. Rewrite `pipeline/eod.py` (syntax errors, holiday guard with a bundled NSE calendar JSON, NSE cookie handshake, `sec_bhavdata_full` delivery %, 0.5% cross-validation that **fails** on breach, append + versioned Kaggle parquet).
16. Create `pipeline/train.py` (pooled HistGradientBoosting × 3 horizons + walk-forward metrics + isotonic calibration) and `pipeline/scan.py` (universe-wide → `scan.json`).
17. Complete `pipeline/features.py`: add `dist52wHighPct/LowPct`, `dist200dmaPct`, `maAlignment`, EMA50/200 spread, RSI-divergence flags; write the real lookahead-bias test (shuffle future bars → features at t unchanged).
18. Move `eod.yml` → `.github/workflows/eod.yml`; add `actions/checkout` + `contents: write`; drop the invalid `backfill` schedule job into a separate manual-dispatch workflow.
19. Create `.github/workflows/universe-refresh.yml` (monthly Nifty 500 constituents → `symbols.json`) and `backfill.yml` (manual, one-time history load).

### WS-4 — Android builds and installs
20. Add `android/app/src/main/AndroidManifest.xml` (INTERNET only), `res/values/{themes,strings,colors}.xml`, the referenced `res/drawable/ic_trend_up|down.xml`, and `proguard-rules.pro` (keep Retrofit/kotlinx/MPAndroidChart model rules).
21. Regenerate the Gradle wrapper **for real** (`gradle wrapper --gradle-version 8.9` or copy a genuine `gradle-wrapper.jar`); remove the text-file jar. Fix `android/.gitignore` (do not ignore `gradle/`); delete the stray root `gradle/` dir; rename `" SymbolScreen.kt"`.
22. Align versions: AGP 8.5.2 + Kotlin 2.0.21 + compose plugin 2.0.21 + Gradle 8.9 (or AGP 8.6.1 + KSP 2.0.21-1.0.28 if Room is added). Delete the dead version catalog or wire it — one source of truth. `rootProject.name = "TechnicalPatternAnalysis"`.
23. Move `src/main/kotlin/test/*Test.kt` → `src/test/kotlin/…`; delete or rewrite the non-compiling `ChartViewModelTest` (mockito/arch imports, bare `any()`); move Compose UI tests to `androidTest`.
24. Fix compile errors: `MainActivity` must be an `Activity` with `onCreate { setContent { … } }`; `ChartScreen` package imports, undefined `data`, `android.graphics.Color` for MPAndroidChart, duplicate imports; `WatchlistViewModel`/`WatchlistDb` stubs → real Room (or remove the dead code).
25. Add `@Serializable` to every DTO in `TpaApi.kt`; align DTOs with `design/api.md` (`t/o/h/l/c/v`, `active[]`, `metrics`, `asOf`); add missing endpoints `health, search, trend, predictions, scan, macro`; fix paths to `/api/technicals`, `/api/tradeplan`.
26. Theme tokens: glass border 12% **white** (not grey), 24dp blur, dark gradient backdrop, accent tokens in `Theme.kt` (replace the 6 hardcoded hex values); `MainActivity` applies the dark scheme.

### WS-5 — Feature completion (only after WS-4 is green)
27. Volume sub-chart fed by the `v` field; dropdown menus as real glass bottom sheets with validity-matrix grey-out.
28. ML Outlook card: 3 horizons × probability bars + per-horizon accuracy + `asOf` + disclaimer.
29. Macro strip (VIX · USD/INR · crude · FII/DII · G-Sec) on the symbol header.
30. "Pattern hits today" row on the search home from `/api/scan`; search screen wired to `/api/search` (debounce 300 ms) instead of 2 hardcoded rows.
31. Position sizing: capital input (local, Room-backed) → shares = capital × 1% ÷ per-share risk; remove the dead "Set capital in Settings" string.
32. EOD pill format `"Data as of {date} · EOD"`; fixed disclaimer footer.

### WS-6 — Docs & truth alignment
33. `design/api.md`: correct the status table (no false ✅); add `/api/macro`, `/api/tradeplan`, `/api/technicals` payload schemas.
34. `design/plan.md` §11: mark tasks honestly (✅ only for CI-verified items).
35. `design/DEPLOYMENT.md`: Vercel + Kaggle + APK runbook (below).

---

## 3. Release & deployment runbook (v1.0.1 acceptance)

| Gate | Command / check | Pass criterion |
|---|---|---|
| Server imports | `python -c "import server.api.index"` | no ImportError/NameError |
| Server tests | `pytest server/tests -q` | green; ≥ 85% coverage on `server/core/` |
| Pipeline imports | `for f in pipeline/*.py; do python -m py_compile $f; done` | zero SyntaxErrors |
| Pipeline dry-run | `python -m pytest pipeline/tests -q` | green on checked-in sample parquet |
| Android | `cd android && ./gradlew :app:assembleRelease` | APK produced |
| **APK artifact** | `gh api repos/:owner/:repo/actions/artifacts` | `release-apk` artifact, size > 0 |
| **CI** | `gh run list -L 3` | latest run `success` on push + PR |
| **Vercel deploy** | `gh secret list` shows all 4 secrets; `gh run view --log deploy` | `vercel deploy --prod` succeeded |
| **Vercel live** | `curl -s https://<project>.vercel.app/api/health` | `{"status":"ok", ...}` |
| **Kaggle** | `gh secret list` + pipeline log line `kaggle datasets version` | `tpa-history` version bumped (one per EOD run) |
| **EOD run** | `gh workflow run eod.yml` (manual dispatch after 19:00 IST) | JSONs committed; deploy re-triggered |

**Deployment state at audit time:** Vercel — never deployed (no deploy job, token only exposed as a variable). Kaggle — no dataset created, key unused. APK — never built. All three must pass their gates above before a GitHub Release is tagged (`v1.0.1`).

---

## 4. Audit evidence index (for the implementing agent)

- **Backend audit** — P0: `pipeline/eod.py:137,187-191` SyntaxError; `server/core/patterns.py:73` NameError; `server/api/search.py:11` undeclared `fuzzywuzzy`; no `.github/workflows/eod.yml`; `train.py`/`scan.py` absent; `/api/candles` contract break (`candles.py:55,16-36,136,150-165`); two import roots (no conftest); `symbols.json`/`scan.json` absent. P1: 4/7 routers mounted; `technicals.py:47-55` hardcoded; `decision.py:30-119` fixture data; `formulas.py` missing CCC/DuPont/incremental ROIC/EV/provenance; `valuation.py:59-67,153,170` fabricated defaults + stub bands; `features.py` missing 5 features; 14 pattern fixtures + 4 decision fixtures + API fixtures + sample parquet all missing; `ci.yml` lacks pytest + deploy jobs. P2: api.md false ✅; vercel.json legacy; hand-seeded `predictions.json`; numpy drift (1.26.4 vs 2.2.2); plan checklist stale.
- **Android/CI audit** — P0: no manifest/res/proguard; fake text gradle-wrapper.jar; JUnit tests in `src/main`; `ChartScreen.kt` unresolvable imports/`Color` type; `WatchlistViewModel` imports missing Room package; `MainActivity` not an Activity; CI = 1 failing job. P1: TpaApi 7/12 endpoints, no `@Serializable`, DTO↔spec drift; no Room dep; macro strip/ML accuracy/volume/scan/disclaimer absent; dropdowns render nothing. P2: `.gitignore` hides `gradle/`; stray root `gradle/` (9.5.1); dead version catalog; glass border grey not white; Compose UI tests in wrong source set; no release or universe-refresh workflow; `" SymbolScreen.kt"` leading space; project name typo.

---

## 5. Work protocol (unchanged from plan.md)

1. Work **WS-0 → WS-1 → WS-2 → WS-3 → WS-4 → WS-5 → WS-6**, strictly in order; within a WS, tasks in listed order.
2. One task ≈ one file/component. TDD: failing test first (plan.md §12.1).
3. Definition of Done per WS = the gate table in §3 for that WS.
4. Halt conditions unchanged (plan.md §13.2): 3 consecutive CI failures, secret problems, schema drift in Yahoo/NSE, or any rubric change → stop, open `gh issue` with evidence.
5. **Security halt (new):** any file containing a credential value, or any credential moved to a plaintext variable → stop and rotate. Credentials live only in GitHub secrets.
6. v1.0.1 ships as a **GitHub Release** with the CI-built APK once every §3 gate is green.

## 6. Out of scope for v1.0.1

New features of any kind: screener, extra pattern families (triangles/wedges/pennants/rectangles/rounding-bottom), thesis cards, options/derivatives, live data, broker integrations. These stay in the v1.1+ backlog per plan.md §1.3.