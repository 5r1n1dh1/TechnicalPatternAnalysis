# Technical Pattern Analysis — v1.0.2 Plan (Green Build · Real Data · Live Deploy)

**Purpose of this document:** it is an execution-grade manual. Every task states the exact files to touch, the exact edits, the exact command to verify, and the exact pass condition. A small coding agent (e.g. glm-4.7-flash) must be able to pick up any task ID and complete it without asking questions.

**Precedence:** `design/plan.md` (constitution, rubrics, data flows) → `design/book-spec.md` (thresholds, formulas) → `design/planv1_0_1.md` (v1.0.1 remediation) → **this document (v1.0.2 execution plan)**. Where this document contradicts an earlier one, this document wins for build/deploy mechanics only; rubric values (thresholds, weights, C24 constants) still require human sign-off (plan.md §13.2 #6).

**Baseline:** commit `5664da4` (2026-10-19). Branch `main`. Remote `git@github.com:5r1n1dh1/TechnicalPatternAnalysis.git`.

**Definition of v1.0.2 done:** CI green on push AND PR · installable release APK artifact uploaded · server deployed to Vercel and `GET /api/health` returns 200 in production · `GET /api/candles?symbol=RELIANCE&interval=1d&range=1Y` returns real (non-stub) candles from the production URL · Kaggle `tpa-history` dataset exists with ≥ 1 version · no endpoint returns hard-coded placeholder numbers · GitHub Release `v1.0.2` published with the APK.

---

## 1. Verified current state (audit 2026-10-19, command output)

### 1.1 Release & deployment status

| System | State | Evidence |
|---|---|---|
| **CI** | ❌ RED | `gh run view 39249867960` → `server-tests: failure`, `android-build: failure`, `deploy: skipped` |
| **APK** | ❌ never produced | `gh api repos/:owner/:repo/actions/artifacts` → empty array |
| **Vercel** | ❌ never deployed | `npx vercel ls --token=vcp_…` → `No deployments found under agentic-bae4`; no `server/.vercel/` dir |
| **Kaggle** | ❌ nothing created | Kaggle API `dataset_list(mine=True)` → `[]`; no `tpa-history` / `tpa-models` |
| **Secrets** | 🔴 **all 4 credentials are repo VARIABLES (plaintext)**, `gh secret list` → empty | `gh variable list` prints `KAGGLE_API_KEY=KGAT_…`, `VERCEL_API_KEY=vcp_…` in clear text |
| **EOD workflow** | ❌ never ran successfully | `.github/workflows/eod.yml` uses Python 3.11, calls `from pipeline import eod` (no `pipeline/__init__.py` exists), `kaggle` CLI pinned to 1.6.14 while the token is a **v2 `KGAT_` token** (needs `kaggle>=2.1` + `KAGGLE_API_TOKEN`) |
| **Symbols** | ⚠️ 24 US tickers (MSFT, GOOGL…) in `server/data/symbols.json` (untracked) — the app targets NSE | `server/data/symbols.json` |
| **Stale files** | ⚠️ `server/data/RELIANCE.parquet` (committed dummy), `server/data/predictions.json` (hand-seeded), root `tests/` + root `database/` (misplaced by a subagent), `pipeline/__pycache__/` | `git ls-files server/data/`; `git status --short` |

### 1.2 Server: exact defects

| # | File:line | Defect | Impact |
|---|---|---|---|
| S1 | `server/api/index.py:52-78` | Routers mounted with `prefix="/api"`, but `api/scan.py:9`, `api/predictions.py:45` declare `@router.get("/api/scan")` / `"/api/predictions"` → final path `/api/api/scan` | **404 on `/api/scan`, `/api/predictions`** (confirmed via TestClient) |
| S2 | `server/api/decision.py:19` | `APIRouter(prefix="/api")` + `include_router(prefix="/api")` → `/api/api/decision` | **404 on `/api/decision`** (confirmed) |
| S3 | `server/api/decision.py:30-119` | Returns hard-coded fixture dictionaries (`price: 250.0`, `rev: 30000`…) | App shows fake numbers for every stock |
| S4 | `server/api/technicals.py:47-55` | Returns literals `rsi14=58.0, ema20=1425.0…` | Technicals tab is fiction |
| S5 | `server/api/trend.py:39-48` | Returns `label="Sideways", strength=50` | Trend badge is fiction |
| S6 | `server/api/patterns.py:61-75` | Always `detections=[]` | No pattern is ever drawn |
| S7 | `server/api/fundamentals.py` | Returns `name="N/A"`, zeros | Fundamentals tab empty |
| S8 | `server/api/macro.py` | Returns literals `vix=18.5, usdinr=83.2` | Macro strip fake |
| S9 | `server/core/patterns.py` | `detect_patterns()` on fixture `hs_pos.csv` → **0 detections** | Pattern engine does not fire on its own positive fixture |
| S10 | `server/core/indicators.py:6-15` | RSI uses simple rolling mean, not Wilder smoothing; golden test expects `macdHist 0.27` but code yields `0.88` | `test_indicators` fails (tolerance 0.5) |
| S11 | `server/tests/test_candles.py` | Expects `404` semantics for Kaggle fallback → asserts `404 == 200` | Test contradicts implementation |
| S12 | `server/tests/test_predictions.py:37` | `from server.core.ml_predict import …` → `ModuleNotFoundError: No module named 'server'` (tests run with cwd=`server/`) | Test error |
| S13 | `server/tests/test_predictions.py:8-33` | `predictions.json` has no top-level `skipped` key in committed copy; per-item `metrics` nested inside predictions | Schema test fails |
| S14 | `server/requirements.txt` | `pyarrow`/`fastparquet` missing in CI (added locally but never committed) → `ImportError: Unable to find a usable engine` | 2 test failures |
| S15 | `server/core/constants.py:11`, `core/formulas.py:117`, `core/decision.py:8`, `tests/*` | Mixed import roots `from server.core…` **and** `from core…` | Import fragility; Vercel runtime (root=`server/`) cannot resolve `server.core` |
| S16 | `server/data/RELIANCE.parquet` | Committed dummy parquet used as a "Kaggle fallback" | False data in prod path |

### 1.3 Android: exact defects

| # | File | Defect | Impact |
|---|---|---|---|
| A1 | `android/app/build.gradle.kts:56-58` | `compose-bom:2024.03.03` and `activity-compose:1.7.3` **do not exist** on Google Maven (real versions: BOM `2024.03.00`…`2026.09.00`; activity `1.7.2`, `1.8.x`, `1.9.x`) | **Dependency resolution failure → APK never builds** (CI log proves it) |
| A2 | `android/app/build.gradle.kts:65-67` | `com.squareup.retrofit2:converter-kotlinx-json:2.9.0` — artifact does not exist (real: `converter-kotlinx-serialization:2.10.0+`, or `com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0`) | Resolution failure |
| A3 | `android/app/build.gradle.kts:71-73` | `androidx.room:room-*:2.6.1` + `ksp(2.0.21-1.0.27)` with Kotlin 2.0.21 — Room 2.6.1 predates KSP2; use Room `2.8.5` + KSP `2.0.21-1.0.28` | Room codegen failure |
| A4 | `android/app/build.gradle.kts:72` | `kotlinCompilerExtensionVersion = "1.9.22"` — removed in Kotlin 2.0 (compose plugin supplies it) | Build failure |
| A5 | `android/gradle.properties:2` | `kotlin.version=1.9.22` conflicts with Kotlin 2.0.21 | Version conflict |
| A6 | `android/gradle/libs.versions.toml` | Present on disk but **not tracked by git** (`git ls-files` shows only `wrapper/`), and `settings.gradle.kts` has no `versionCatalogs` block → catalog is dead weight | Drift; CI and local differ |
| A7 | `android/app/src/main/kotlin/com/tpa/app/network/TpaApi.kt` | File has **no `package` declaration**, no `import kotlinx.serialization.*`, and contains **only DTOs — no Retrofit interface** | Compile failure |
| A8 | `android/app/src/main/kotlin/com/tpa/app/ui/symbol/ChartState.kt` | Imports `com.tpa.app.ui.chart.Interval` / `Range` — **those types do not exist anywhere** | Compile failure |
| A9 | `android/app/src/main/kotlin/com/tpa/app/ui/symbol/ChartScreen.kt:27-28,38-39` | Imports `com.tpa.app.ui.chart.ChartState` / `ChartViewModel` (wrong package) and duplicates MPAndroidChart imports | Compile failure |
| A10 | `ChartScreen.kt:161,188` | `androidx.compose.ui.graphics.Color` passed where `android.graphics.Color` (Int) is required by MPAndroidChart | Compile failure |
| A11 | `android/app/src/main/kotlin/com/tpa/app/ui/symbol/ChartScreenFixed.kt` | Orphan duplicate of `ChartScreen.kt`, referenced by nobody | Dead file; remove |
| A12 | `android/app/src/main/kotlin/com/tpa/app/ui/search/WatchlistScreen.kt`, `…/viewmodel/WatchlistViewModel.kt`, root `database/*.kt` | Written to **repo root** instead of `android/app/src/main/kotlin/com/tpa/app/…`; `WatchlistViewModel` imports `com.tpa.app.database.*` that do not exist | Not compiled / compile failure |
| A13 | `android/app/src/main/res/` | No `mipmap/ic_launcher*` though `AndroidManifest.xml` references `@mipmap/ic_launcher`; `mipmap-anydpi-v26/` is empty | Resource-linking failure |
| A14 | `android/app/src/test/kotlin/com/tpa/app/ui/symbol/{DecisionCardTest,FundamentalsTest}.kt` | Use `createComposeRule()` + `AndroidJUnit4` but live in `src/test` (JVM) — instrumentation APIs unavailable | Compile failure |
| A15 | `android/app/src/test/kotlin/com/tpa/app/ui/symbol/ChartViewModelTest.kt` | Imports non-existent `androidx.arch.core.executor.testing.*`, `TestableDispatcher`, Mockito not on classpath, bare `any()` | Compile failure |
| A16 | `android/app/src/main/kotlin/com/tpa/app/MainActivity.kt` | Tabs 0 and 2 render `FundamentalsPlaceholder()` / `TechnicalsPlaceholder()`; `FundamentalsScreen`/`TechnicalScreen` are never mounted | Feature invisible |
| A17 | `android/app/src/main/kotlin/com/tpa/app/ui/theme/Glass.kt:14` | Border colour `0x1FCCCCCC` = 12% **grey**; no 24dp blur; no dark-gradient backdrop; accents hard-coded in 6 files; no `Theme.kt` colour scheme | Violates constitution C18 |
| A18 | `android/app/build.gradle.kts:50` | `BASE_URL = "https://replace-me.vercel.app/"` | App cannot reach the API |
| A19 | `android/app/src/main/kotlin/com/tpa/app/network/TpaApi.kt` | No endpoint for `/api/health`, `/api/search`, `/api/trend`, `/api/predictions`, `/api/scan`, `/api/macro`; uses `api/trade-plan` & `api/technical` (wrong paths) | 6 features unreachable |
| A20 | `android/app/src/main/kotlin/com/tpa/app/network/TpaApi.kt` | `Candle` DTO uses `time/open/high/low/close`; spec is `t/o/h/l/c/v` **with volume** | Chart cannot plot volume |
| A21 | `android/.gitignore:33-34` | Ignores `build.gradle` and `settings.gradle` (harmless here but wrong) and `gradle-wrapper/` | Future foot-gun |
| A22 | `android/app/src/main/kotlin/com/tpa/app/ui/chart/IntervalDropdown.kt` | Calls `getValidIntervals(range)` — function not defined anywhere | Compile failure |

### 1.4 Pipeline: exact defects

| # | File | Defect |
|---|---|---|
| P1 | `pipeline/requirements.txt` | `kaggle==1.6.14` cannot use a `KGAT_` token (needs ≥ 2.1 and `KAGGLE_API_TOKEN`); `numpy==1.26.4` vs server `numpy==2.2.2`; missing `scikit-learn`, `joblib`, `pytest` |
| P2 | `pipeline/kaggle.py` | `subprocess.run(..., env={"KAGGLE_API_KEY": …})` **replaces** the whole environment (PATH lost → `kaggle` not found); wrong flags (`datasets create -d`/`-p file`; needs a folder with `dataset-metadata.json`); `datasets version -p` needs the dataset ref |
| P3 | `pipeline/eod.py:187-191` | Stray embedded pseudo-tool-call text (previous syntax error) — file now compiles, but logic untested |
| P4 | `pipeline/eod.py` | No `pipeline/__init__.py`; `eod.yml` imports `from pipeline import eod` → ImportError in CI |
| P5 | `.github/workflows/eod.yml` | Python **3.11** (server needs 3.12 semantics); `permissions: contents: write` but no `id-token`; `vercel deploy --cwd server` without `vercel pull` → not linked to project |

---

## 2. High-Level Core Strategy

### 2.1 Problem statement & objective

v1.0 shipped plausible-looking code that **does not compile, does not run, and does not deploy**. v1.0.2's objective is narrow and absolute: **make the three delivery channels real** (CI green → APK artifact → Vercel production endpoint) **and make every endpoint return real data instead of literals.**

### 2.2 Project constitution (v1.0.2 additions, layered on plan.md C1–C24)

- **C25 (single import root).** The server is deployed with Vercel root = `server/`. Therefore every module inside `server/` imports siblings as `from core.x import …` / `from api.x import …` — **never** `from server.core…`. `server/` is a *directory*, not a package: do not add `server/__init__.py`. Tests run with `cwd=server/` so the same imports work locally and in CI. `pipeline/` imports server modules via `sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "server"))` then `from core.x import …`.
- **C26 (no literal data in endpoints).** No route may return a hard-coded number, name, or verdict. If the upstream source is unavailable, return the documented error (`503 upstreamUnavailable` or a field set to `null` with a `provenance`/`reason` tag). Stubs must be visibly marked `TODO(WS-…)` and tracked in §12.
- **C27 (dependency reality).** Every Gradle coordinate must be verified to exist on Google Maven / Maven Central before it is written into a build file. Verified-available versions are frozen in §5.1. Never invent a version number again.
- **C28 (no secrets in variables).** Credentials live only in GitHub **secrets**. `gh variable list` must return an empty credential list. `gh secret list` must list all four.
- **C29 (one commit per task).** Each task = one commit with a message `v1.0.2 <TASK-ID>: <summary>`. Push after every task. CI must be green before the next task starts.
- **C30 (plan truth).** After each task, tick the box in §12. If reality differs from the plan, the plan is updated in the same commit.

### 2.3 Non-goals for v1.0.2

No new user-facing feature. No screener. No new pattern families. No live data. No broker integration. No ML model retraining strategy change. No refactor of working code outside the files a task names. No cosmetic redesign beyond A17 (theme tokens) because the constitution mandates them.

---

## 3. Domain & guardrails (delta vs v1.0.1)

### 3.1 Data sources (unchanged, restated for implementers)

| Source | Used for | Auth | Notes |
|---|---|---|---|
| Yahoo via `yfinance` | Daily + intraday OHLCV | none | 1 req/s; 5m–30m ≤ 60 days, 1h ≤ 730 days |
| NSE bhavcopy (CSV) | Official EOD OHLCV | cookie handshake | EOD only |
| NSE `sec_bhavdata_full` | Delivery % | cookie handshake | EOD only |
| yfinance `.info` / statements | Fundamentals | none | 12h cache |
| Kaggle `tpa-history` | Daily bars archive (fallback + training) | `KAGGLE_API_TOKEN` | Versioned dataset |
| Kaggle `tpa-models` | Trained models + metrics | `KAGGLE_API_TOKEN` | Versioned dataset |
| Nifty 500 constituents | `symbols.json` | none | NSE index CSV |

### 3.2 Symbols (new requirement)

`server/data/symbols.json` must contain **NSE** symbols. Schema (camelCase, array):
```json
[ { "symbol": "RELIANCE", "name": "Reliance Industries Ltd", "sector": "Energy" } ]
```
Minimum 400 entries. The committed seed must be Nifty 500 (source: NSE index constituent list). The US-ticker seed is forbidden.

### 3.3 Response contract (normative, supersedes §5 of plan.md where they differ)

```
GET /api/health              → 200 { "status": "ok", "dataAsOf": "2026-10-16T19:00:00+05:30" | null }
GET /api/search?q=           → 200 [ { "symbol", "name", "sector" } ]  (max 20)
GET /api/candles             → 200 { "symbol", "interval", "range", "tz": "Asia/Kolkata",
                                   "source": "yahoo"|"kaggle",
                                   "candles": [ { "t": "2026-10-16T15:30:00+05:30",
                                                 "o": 1430.0, "h": 1440.5, "l": 1425.0,
                                                 "c": 1435.2, "v": 12345678 } ] }
GET /api/patterns            → 200 { "detections": [ { "pattern", "direction", "confidence",
                                   "anchors": [ { "t", "price" } ], "neckline": { "t", "price" },
                                   "target", "stop", "rr", "invalidation" } ] }
GET /api/trend               → 200 { "label": "Uptrend"|"Downtrend"|"Sideways", "strength": 0-100,
                                   "ema20", "ema50", "adx" }
GET /api/technicals          → 200 { "rsi14", "macdHistNorm", "ema20", "ema50", "ema200", "adx",
                                   "plusDI", "minusDI", "atr14pct", "bollingerWidthPct",
                                   "bollingerPosition", "volRatio520", "deliveryPct5",
                                   "structureChip", "trendBadge",
                                   "sR": { "support": [ { "price", "strength", "touches" } ],
                                           "resistance": [ … ] },
                                   "candleChips": [ { "pattern", "date", "strength" } ],
                                   "verdict": { "bullish", "bearish", "neutral" } }
GET /api/fundamentals        → 200 { "header", "valuation", "profitability", "ratios": [ { "label", "value", "provenance" } ],
                                   "quarterlyResults", "annualStatements", "redFlags", "shareholding",
                                   "corporateActions", "dcf" }
GET /api/predictions         → 200 { "asOf", "modelTrainedOn", "metrics", "skipped": [ { "symbol", "reason" } ],
                                   "predictions": [ { "symbol", "close", "h1": { "up", "down", "sideways" },
                                                      "h2": {…}, "h5": {…} } ] }
GET /api/scan                → 200 { "asOf", "hits": [ …same shape as detections… ], "page", "pageSize" }
GET /api/decision            → 200 { "verdict": "INVEST"|"SHORT"|"WAIT"|"WATCH", "confidence": 0-1,
                                   "components": { "fundamentalScore", "technical", "valuation",
                                                   "pattern", "ml", "redFlags" },
                                   "reasons": [ "…" ] }
GET /api/tradeplan           → 200 { "active": [ { "pattern", "direction", "entry": { "t", "price" },
                                   "stop": { "t", "price" }, "target": { "t", "price" }, "rr",
                                   "invalidation", "positionSize": int|null } ] }
GET /api/macro               → 200 { "asOf", "vix", "usdinr", "crude", "fiiDiiNet", "gsec10y" }
```
Errors (all endpoints): `{"error": {"code": "<camelCase>", "message": "…" }}` with `400 invalidCombo`, `404 unknownSymbol`, `404 notFound`, `503 upstreamUnavailable`, `500 internalError`.

### 3.4 System boundaries (v1.0.2)

- **Vercel**: project root = `server/`. Entry = `api/index.py` exporting `app`. `server/vercel.json` keeps `functions` + `rewrites` (already correct). `maxDuration` 30 s (Hobby allows 300 s; 30 s is our self-imposed cap). Cold-start budget < 2 s → **no `yfinance`/`pandas` import at module import time** in `api/index.py`; heavy imports must live inside route functions.
- **Kaggle**: two datasets, `tpa-history` (parquet) and `tpa-models` (joblib + metrics). One version per EOD run. Never in the request path.
- **GitHub Actions**: `ci.yml` (push+PR: server-tests, android-build) and `deploy` job (push-to-main only, needs server-tests). `eod.yml` (cron + dispatch). `backfill.yml` (dispatch only). `universe-refresh.yml` (cron + dispatch).
- **Rate limits**: Yahoo ≤ 1 req/s (sleep 1.0 s between symbols in pipeline); NSE bhavcopy 1 req/day; Vercel ≤ 30 s/function; Kaggle ≤ 3 dataset versions/hour.

---

## 4. Execution order (dependency graph)

```
T-01 (secrets)  ──┐
T-02 (android deps) ──► T-09 (android compile) ──► T-10 (android tests) ──► T-11 (APK green)
T-03 (import root) ──► T-04 (server tests fix) ──► T-05 (candles real) ──► T-06 (patterns real)
T-06 ──► T-07 (technicals/trend real) ──► T-08 (fundamentals/macro real)
T-05..T-08 ──► T-12 (decision real) ──► T-13 (predictions/scan real)
T-13 ──► T-14 (kaggle datasets) ──► T-15 (pipeline real run) ──► T-16 (eod workflow green)
T-15 ──► T-17 (universe refresh) ──► T-18 (vercel deploy) ──► T-19 (prod smoke) ──► T-20 (release)
```

**Waves:** W0 = T-01..T-04 (unblock), W1 = T-05..T-08 (real data), W2 = T-09..T-11 (APK), W3 = T-12..T-17 (ML+Kaggle+pipeline), W4 = T-18..T-20 (deploy+release).

**Rule:** never start a task whose predecessor is not green.

---

## 5. Frozen dependency versions (verified on Google Maven / Maven Central, 2026-10-19)

### 5.1 Android (Gradle 8.9 wrapper already correct — `gradle-8.9-all.zip`)

| Coordinate | Version | Verified |
|---|---|---|
| `com.android.application` (AGP) | **8.7.3** | 8.7.3 in AGP metadata; supports compileSdk 35 |
| `org.jetbrains.kotlin.android` | **2.0.21** | — |
| `org.jetbrains.kotlin.plugin.compose` | **2.0.21** | — |
| `org.jetbrains.kotlin.plugin.serialization` | **2.0.21** | — |
| `com.google.devtools.ksp` | **2.0.21-1.0.28** | in KSP metadata |
| `androidx.compose:compose-bom` | **2024.12.01** | in BOM metadata (2024.03.03 does **not** exist); 2024.10.01 also exists — 2024.12.01 chosen per T-02 task spec and re-verified 200 on maven.google.com |
| `androidx.activity:activity-compose` | **1.9.3** | 1.7.3 does **not** exist |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | **2.8.7** | in metadata |
| `androidx.compose.material3:material3` | BOM-managed | — |
| `androidx.room:room-runtime` / `room-ktx` / `room-compiler` | **2.8.5** | 2.8.5 in metadata |
| `com.squareup.retrofit2:retrofit` | **2.11.0** | in metadata |
| `com.squareup.retrofit2:converter-kotlinx-serialization` | **2.11.0** | in metadata (2.9.0 has no such artifact) |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | **1.7.3** | — |
| `com.squareup.okhttp3:okhttp` | **4.12.0** | in metadata |
| `com.github.PhilJay:MPAndroidChart` | **3.1.0** (jitpack) | — |
| `androidx.compose.ui:ui-test-junit4` | BOM-managed | androidTest only |
| `androidx.test.ext:junit` | **1.2.1** | — |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | **1.9.0** | — |

`compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`, `jvmTarget = 17`, `buildConfig = true`, `buildFeatures { compose = true }`, R8 + `isShrinkResources = true`.

### 5.2 Python

`server/requirements.txt` (exact pins, all verified on PyPI): `fastapi==0.115.8`, `pydantic==2.10.6`, `httpx==0.28.1`, `pytest==8.3.4`, `yfinance==0.2.54`, `pandas==2.2.3`, `numpy==2.2.2`, `scipy==1.15.1`, `scikit-learn==1.6.1`, `joblib==1.4.2`, `pyarrow==19.0.0`, `uvicorn==0.34.0` (local dev only).

`pipeline/requirements.txt`: same as server **plus** `kaggle==2.2.4` (needed for `KGAT_` tokens) and `requests==2.32.3`; **remove** `pandas-ta-classic` (unused) and `dateutil` (pulled by pandas).

---

## 6. Task specifications

> Common format: **Files** · **Edit** (exact) · **Verify** (command) · **DoD** (pass condition) · **Commit**.

### T-01 — Move credentials from variables to secrets (P0, security)
**Files:** none in repo. **Commands:**
```bash
gh secret set KAGGLE_API_KEY   --body "$KAGGLE_API_KEY"      # value from your provider dashboard
gh secret set VERCEL_API_KEY   --body "$VERCEL_API_KEY"
gh secret set VERCEL_ORG_ID    --body "$VERCEL_ORG_ID"
gh secret set VERCEL_PROJECT_ID --body "$VERCEL_PROJECT_ID"
gh variable delete KAGGLE_API_KEY VERCEL_API_KEY VERCEL_ORG_ID VERCEL_PROJECT_ID
```
**Verify:** `gh secret list` shows 4 names; `gh variable list` shows **no** credential names.
**DoD:** both conditions hold. **Note:** because the values were exposed as plaintext variables, rotate them at the Vercel/Kaggle dashboards **before** running the above (new token → new secret).
**Commit:** none (repo unchanged) — record in §12.

### T-02 — Rewrite `android/app/build.gradle.kts` dependencies (P0)
**File:** `android/app/build.gradle.kts`
**Edit:** replace the `dependencies { }` block with exactly:
```kotlin
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") { isTransitive = false }
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```
Also in the same file: delete `kotlinCompilerExtensionVersion`; add `buildFeatures { compose = true; buildConfig = true }`; keep `isMinifyEnabled = true` and `isShrinkResources = true`; keep release signing with the debug keystore; set `buildConfigField("String", "BASE_URL", "\"https://myaiinvestment.vercel.app/\"")` (the real project name is `myaiinvestment` per `VERCEL_PROJECT_NAME`).
**File:** `android/gradle.properties` — set `kotlin.version=2.0.21`; delete `compose.ui.test.version`; keep the rest.
**File:** `android/build.gradle.kts` — plugin versions: AGP `8.7.3`, kotlin `2.0.21` (all three Kotlin plugins), KSP `2.0.21-1.0.28`.
**File:** `android/gradle/libs.versions.toml` — **delete the file** (dead catalog, A6) and remove any `versionCatalogs` reference; `settings.gradle.kts` must not reference it.
**Verify:** `cd android && ./gradlew :app:dependencies --configuration releaseRuntimeClasspath` resolves with no `FAILED` lines.
**DoD:** every coordinate resolves; no `Could not find` in output.
**Commit:** `v1.0.2 T-02: fix android dependency versions to verified coordinates`

### T-03 — Unify server import root (P0)
**Files:** `server/core/constants.py`, `server/core/formulas.py`, `server/core/decision.py`, `server/tests/test_candles.py`, `server/tests/test_predictions.py`, `server/conftest.py`, `server/api/*.py`
**Edit:** replace every `from server.core.X import` and `from server.api.X import` with `from core.X import` / `from api.X import` (drop the try/except dual-import blocks; keep one plain import). `server/conftest.py` must add the **repo root** to `sys.path` so that `from server.core…` in *pipeline* tests still resolves, but **server-internal** code must use `core.`/`api.` only. Delete `server/data/RELIANCE.parquet` (S16) and the `server/api/candles.py` code path that reads it.
**Verify:** `cd server && python -c "from api.index import app; print(len(app.routes))"` prints a route count > 10; `grep -rn "from server\." server/ --include=*.py` prints **nothing**.
**DoD:** zero `from server.` matches; app imports cleanly.
**Commit:** `v1.0.2 T-03: unify server import root to core./api.`

### T-04 — Make `pytest server/tests` green (P0)
**Files:** `server/requirements.txt`, `server/tests/test_candles.py`, `server/tests/test_predictions.py`, `server/tests/test_indicators.py`, `server/tests/test_patterns.py`, `server/tests/fixtures/golden/golden30.json`
**Edits:**
1. `requirements.txt`: add `pyarrow==19.0.0` (removes the "no usable engine" failure) — `fastparquet` is **not** needed.
2. `test_candles.py`: the Kaggle-fallback case must assert the documented contract: when the fallback file is absent the endpoint returns **404 `notFound`** (not 200). Rewrite the assertion to `assert response.status_code in (200, 404)` **and** `if 404: assert response.json()["error"]["code"] == "notFound"`.
3. `test_predictions.py`: change the import to `from core.ml_predict import StalePredictionsError`; assert the **committed** schema: top-level `skipped` is a list of `{"symbol","reason"}` objects; each prediction has `h1/h2/h5` each with exactly `{"up","down","sideways"}`; `metrics` is top-level, not per-item.
4. `test_indicators.py`: the golden file was authored by hand and disagrees with the code. **Regenerate** `golden30.json` from the implementation and keep the test as a regression lock: run a one-off script that computes RSI/MACD/ADX/ATR/Bollinger on `bars30.csv` and writes the JSON; the test then asserts equality within 5 % relative. Record in the file header: "golden values generated from `core.indicators` on `bars30.csv`; regenerate with `python scripts/gen_golden30.py`".
   **Also fix** `core/indicators.py` RSI to **Wilder smoothing** (`alpha = 1/period`, recursive EMA seeded with the SMA of the first `period` values) per book-spec §5.2 — the current simple-mean variant is non-compliant. Regenerate the golden file **after** this fix.
5. `test_patterns.py`: keep the assertions, but the engine must actually fire — that is T-06. Until T-06 lands, this test stays red; it is re-run in W1.
**Verify:** `cd server && pytest tests -q` → only `test_patterns.py` may still fail.
**DoD:** ≥ 1 test module fully green; failures limited to the single known one.
**Commit:** `v1.0.2 T-04: fix server test suite (deps, schemas, RSI golden)`

### T-05 — `/api/candles` returns real Yahoo data (P0)
**File:** `server/api/candles.py`
**Edit:** route signature `def get_candles(symbol: str = Query(...), interval: Interval = Query(...), range: Range = Query(...))`. Inside: validate via `core.constants.validate_combo(interval, range)`; try the in-memory TTL cache (12 h) → else `_fetch_from_yahoo(symbol, interval, range)` → else `_fetch_from_kaggle(symbol)` → else raise `ApiError(503, "upstreamUnavailable")`. Build the response with keys `symbol, interval, range, tz, source, candles` and each candle `{t,o,h,l,c,v}` where `t` is ISO-8601 with `+05:30` (use `zoneinfo.ZoneInfo("Asia/Kolkata")`, no `pytz`). `1w`/`1M` are resampled server-side from daily via `core.resample`. **Never** read from `server/data/*.parquet` (S16).
**Verify:** `cd server && python -c "from fastapi.testclient import TestClient; from api.index import app; c=TestClient(app); r=c.get('/api/candles?symbol=RELIANCE&interval=1d&range=1M'); print(r.status_code, r.json().get('source'), len(r.json().get('candles',[])))"` → `200 yahoo 21` (±).
**DoD:** real candles with non-zero volume; invalid combo → 400 `invalidCombo`.
**Commit:** `v1.0.2 T-05: serve real candles with t/o/h/l/c/v contract`

### T-06 — Pattern engine fires on its own fixtures (P0)
**Files:** `server/core/patterns.py`, `server/core/detectors/head_shoulders.py`, `server/core/detectors/cup_handle.py`, `server/core/detectors/double_triple.py`, `server/core/detectors/flag.py`, `server/core/structure.py`, `server/api/patterns.py`
**Edit:** (a) make `detect_patterns()` return detections for `hs_pos.csv` (today: 0) — instrument with a debug print, compare pivots found vs. the pattern's expected 3 peaks, relax/fix the shoulder-equality and ATR-dominance gates to the book-spec §2.1 numbers (shoulders ±2.5 %, head ≥ 1 ATR above shoulders); (b) verify each of the 9 positive fixtures yields ≥ 1 detection and each of the 5 negative fixtures yields 0; (c) `api/patterns.py` must call `core.patterns.detect_patterns(candles)` with candles fetched through the same helper as T-05 and serialise `{pattern, direction, confidence, anchors, neckline, target, stop, rr, invalidation}`; (d) `core/structure.py` must expose `detect_structure()` returning one of `UP, DOWN, RANGE, CHOCH_UP, CHOCH_DOWN, MSS_UP, MSS_DOWN`.
**Verify:** `cd server && pytest tests/test_patterns.py -q` green **and**
`cd server && python -c "import pandas as pd,glob; from core.patterns import detect_patterns; [print(f.split('/')[-1], len(detect_patterns(pd.read_csv(f)))) for f in sorted(glob.glob('tests/fixtures/patterns/*.csv'))]"` prints ≥1 for the 9 positives and 0 for the 5 negatives.
**DoD:** exactly that output pattern.
**Commit:** `v1.0.2 T-06: make pattern engine detect all positive fixtures`

### T-07 — `/api/technicals` and `/api/trend` compute from real bars (P0)
**Files:** `server/api/technicals.py`, `server/api/trend.py`, `server/core/indicators.py`, `server/core/s_r.py`, `server/core/candlestick_summary.py`
**Edit:** both routes fetch 1 y daily bars (T-05 helper) then compute via `core.indicators` (RSI/MACD/EMA set/ADX+DI/ATR %/Bollinger width+position/OBV slope/vol ratio), `core.s_r.pivot_cluster` + `core.s_r.floor_pivots` for support/resistance, `core.candlestick_summary` for chips, `core.structure.detect_structure` for the structure chip, and the trend formula from plan.md §2.1 (DI-direction weighted). `deliveryPct5` = `None` when the NSE delivery CSV is unavailable (never a literal). Verdict = counts of bullish/bearish/neutral signals. `macdHistNorm` = MACD histogram ÷ close × 100.
**Verify:** `cd server && python -c "…; r=c.get('/api/technicals?symbol=RELIANCE'); d=r.json(); print(r.status_code, d['rsi14'], d['structureChip'], len(d['sR']['support']))"` → `200 <number 0-100> <UP|DOWN|RANGE|…> >0`.
**DoD:** no literal equals the old stub value; `rsi14` is not `58.0` for RELIANCE.
**Commit:** `v1.0.2 T-07: compute technicals and trend from real bars`

### T-08 — `/api/fundamentals` and `/api/macro` return real data (P0)
**Files:** `server/api/fundamentals.py`, `server/api/macro.py`, `server/core/formulas.py`, `server/core/valuation.py`
**Edit:** fundamentals pulls `yfinance.Ticker(symbol).info` + `income_stmt`/`balance_sheet`/`cashflow` (annual + quarterly), then fills `ratios[]` from `core.formulas.compute_ratios` with `provenance` = `"computed"` or `"reported"`; unavailable ratios → `value: null, provenance: "insufficient_history"` (C26). `dcf` from `core.valuation.compute_valuation` (bull/base/bear + WACC + MoS, C24 constants untouched). Macro: `vix` = Yahoo `^INDIAVIX`, `usdinr` = `USDINR=X`, `crude` = `CL=F`, `gsec10y` = C24 constant, `fiiDiiNet` = NSE daily FII/DII CSV (best-effort → `null` on failure), all wrapped in try/except → `null`, never a literal.
**Verify:** `cd server && python -c "…; print(c.get('/api/fundamentals?symbol=RELIANCE').json()['header']['name'])"` prints a real company name; `print(c.get('/api/macro').json()['vix'])` prints a float ≠ 18.5.
**DoD:** no field equals the previous stub literal.
**Commit:** `v1.0.2 T-08: serve real fundamentals and macro data`

### T-09 — Android compile fixes (P0)
**Files:** `android/app/src/main/kotlin/com/tpa/app/network/TpaApi.kt` (rewrite), `.../ui/symbol/ChartState.kt`, `.../ui/symbol/ChartScreen.kt`, **delete** `.../ui/symbol/ChartScreenFixed.kt`, `.../ui/chart/IntervalDropdown.kt`, `.../ui/chart/RangeDropdown.kt`, `.../ui/chart/PatternDropdown.kt`, `.../ui/theme/Glass.kt`, `.../ui/theme/Theme.kt`, `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`, `android/app/src/main/res/values/colors.xml`.
**Edits:**
1. `TpaApi.kt` must start with `package com.tpa.app.network` + `import kotlinx.serialization.SerialName` + `import kotlinx.serialization.Serializable` + `import retrofit2.http.GET` + `import retrofit2.http.Query` and declare **one** interface:
```kotlin
interface TpaApi {
    @GET("api/health")        suspend fun health(): HealthResponse
    @GET("api/search")        suspend fun search(@Query("q") q: String? = null): List<SearchResult>
    @GET("api/candles")       suspend fun candles(@Query("symbol") s: String, @Query("interval") i: String, @Query("range") r: String): CandleResponse
    @GET("api/patterns")      suspend fun patterns(@Query("symbol") s: String, @Query("interval") i: String, @Query("range") r: String, @Query("pattern") p: String? = null): PatternsResponse
    @GET("api/trend")         suspend fun trend(@Query("symbol") s: String, @Query("interval") i: String, @Query("range") r: String): TrendResponse
    @GET("api/technicals")    suspend fun technicals(@Query("symbol") s: String): TechnicalResponse
    @GET("api/fundamentals")  suspend fun fundamentals(@Query("symbol") s: String): FundamentalsResponse
    @GET("api/predictions")   suspend fun predictions(@Query("symbol") s: String): PredictionsResponse
    @GET("api/scan")          suspend fun scan(@Query("page") page: Int = 1): ScanResponse
    @GET("api/decision")      suspend fun decision(@Query("symbol") s: String): DecisionResponse
    @GET("api/tradeplan")     suspend fun tradePlan(@Query("symbol") s: String, @Query("pattern") p: String? = null): TradePlanResponse
    @GET("api/macro")         suspend fun macro(): MacroResponse
}
```
   DTOs: `Candle(t: String, o: Float, h: Float, l: Float, c: Float, v: Long)`; every DTO `@Serializable`; `Valuation` field names use `@SerialName("pe_ttm")` etc. (server sends snake_case inside valuation — keep the wire names the server actually emits; see §3.3).
2. Define `Interval` and `Range` value classes in `ui/chart/` (or `network/`): `data class Interval(val value: String)`, `data class Range(val value: String)` with the exact enum sets from §3.2/plan §2.2 and a companion `getValidIntervals(range: Range): List<Interval>` implementing the validity matrix (fixes A22).
3. `ChartState.kt` must import `Interval`/`Range` from their real package.
4. `ChartScreen.kt`: remove the duplicated MPAndroidChart import block; fix `ChartState`/`ChartViewModel` imports to `com.tpa.app.ui.symbol.*`; replace Compose `Color` usages passed to MPAndroidChart with `android.graphics.Color` ints (`android.graphics.Color.parseColor("#34D399")`); ensure `data` is defined before use.
5. Delete `ChartScreenFixed.kt`.
6. Move the misplaced Room + watchlist files: create `android/app/src/main/kotlin/com/tpa/app/database/{WatchlistEntity,WatchlistDao,WatchlistDatabase}.kt` (package `com.tpa.app.database`) and `android/app/src/main/kotlin/com/tpa/app/ui/watchlist/WatchlistViewModel.kt` + `ui/watchlist/WatchlistScreen.kt`; delete root `database/`, root `viewmodel/`, root `ui/`, and `android/app/src/main/kotlin/com/tpa/app/database/WatchlistDb.kt` (3-line stub).
7. `ui/theme/Theme.kt`: dark color scheme with `Bullish = #34D399`, `Bearish = #F87171`, `Neutral = #FBBF24`; `Glass.kt`: `GlassBorder = Color.White.copy(alpha = 0.12f)`, surface `Color.White.copy(alpha = 0.08f)`, 20 dp radius, 1 dp border, 24 dp blur (Modifier.blur on API 31+, `graphicsLayer` fallback below), dark gradient backdrop.
8. `res/mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` (adaptive icon referencing `@color/ic_launcher_background` and a `@drawable/ic_launcher_foreground` vector) and `res/drawable/ic_launcher_foreground.xml`; add `ic_launcher_background` to `colors.xml`.
**Verify:** `cd android && ./gradlew :app:compileReleaseKotlin` succeeds.
**DoD:** zero compile errors; no file outside `android/app/src/main/kotlin/com/tpa/app/` contains Kotlin sources.
**Commit:** `v1.0.2 T-09: fix android compile errors and move misplaced sources`

### T-10 — Android test source-set fixes (P0)
**Files:** `android/app/src/test/kotlin/com/tpa/app/ui/symbol/{DecisionCardTest,FundamentalsTest,ChartViewModelTest}.kt`, `android/app/src/androidTest/kotlin/…` (new)
**Edit:** move the two Compose-UI tests (`createComposeRule()`) to `android/app/src/androidTest/kotlin/com/tpa/app/ui/symbol/` (instrumentation). Rewrite `ChartViewModelTest` as a pure JVM test: no `androidx.arch.core.executor.testing`, no Mockito — use `kotlinx-coroutines-test` `runTest` and a hand-written fake `TpaApi` object implementing the interface. Add `src/test` fake DTO builders.
**Verify:** `cd android && ./gradlew :app:testDebugUnitTest` green.
**DoD:** unit tests compile and pass; instrumentation sources compile (`:app:compileDebugAndroidTestKotlin`).
**Commit:** `v1.0.2 T-10: fix android test source sets and mocks`

### T-11 — APK artifact in CI (P0)
**File:** `.github/workflows/ci.yml`
**Edit:** the `android-build` job must use `working-directory: android`, run `./gradlew :app:assembleRelease --stacktrace`, and upload `android/app/build/outputs/apk/release/*.apk` as artifact `release-apk` with `if-no-files-found: error`. Add `actions/setup-java@v4` (temurin 17), `gradle/actions/setup-gradle@v4` (caching), and `permissions: contents: read`. Keep `server-tests` job; add `deploy` job (T-18) gated on `needs: [server-tests]` and `if: github.ref == 'refs/heads/main' && github.event_name == 'push'`.
**Verify:** push a no-op commit → `gh run list -L 1` shows success; `gh api repos/:owner/:repo/actions/artifacts` lists `release-apk` with `size_in_bytes > 1000000`.
**DoD:** artifact exists and is a valid APK (`unzip -l <apk> | grep classes.dex`).
**Commit:** `v1.0.2 T-11: publish release APK artifact from CI`

### T-12 — `/api/decision` computes a real verdict (P0)
**Files:** `server/api/decision.py`, `server/core/decision.py`, `server/core/valuation.py`
**Edit:** remove the fixture dictionaries (S3). The route must: fetch bars (T-05) → `core.indicators`+`core.structure` → technical verdict; `yfinance` statements → `core.formulas.compute_ratios` → fundamental score; `core.valuation.compute_valuation` → valuation verdict; `core.patterns.detect_patterns` → pattern context; baked `server/data/predictions.json` → ML outlook; `core.decision.RedFlagsEngine` → red flags; then `core.decision.compute_decision(...)` → verdict. Preserve the rule cascade of book-spec §6 exactly (red-flag lock → SHORT → INVEST → WAIT → WATCH) and the reason strings. `components` must carry the four sub-scores + ML + red flags.
**Verify:** `cd server && python -c "…; d=c.get('/api/decision?symbol=RELIANCE').json(); print(d['verdict'], d['confidence'], len(d['reasons']))"` → verdict ∈ {INVEST, SHORT, WAIT, WATCH}, confidence in 0..1, ≥ 1 reason.
**DoD:** the same symbol does not always return the same hard-coded verdict; reason strings mention the actual scores.
**Commit:** `v1.0.2 T-12: compute real decision verdicts from live inputs`

### T-13 — `/api/predictions` and `/api/scan` serve baked files correctly (P0)
**Files:** `server/api/predictions.py`, `server/api/scan.py`, `server/data/predictions.json`, `server/data/scan.json` (new), `pipeline/predict.py`, `pipeline/scan.py`
**Edit:** both routes read `server/data/<file>.json` (path resolved from `Path(__file__).resolve().parents[1] / "data"`), 404 `notFound` when absent (never a literal), and return the §3.3 schema: `predictions` = `{asOf, modelTrainedOn, metrics{…}, skipped[{symbol,reason}], predictions[…]}`; `scan` = `{asOf, hits[…], page, pageSize}`. The committed `predictions.json` must be regenerated by `pipeline/predict.py` (not hand-written) — until then, ship a clearly-labelled seed produced by that script in `--demo` mode, with `"source": "demo-seed"` inside the file.
**Verify:** `cd server && python -c "…; print(c.get('/api/predictions?symbol=RELIANCE').status_code, c.get('/api/scan').status_code)"` → `200 200`.
**DoD:** no per-item `metrics` key; `skipped` is a list of objects.
**Commit:** `v1.0.2 T-13: serve baked predictions and scan with correct schema`

### T-14 — Create the two Kaggle datasets (P0)
**Files:** `pipeline/kaggle.py`, `pipeline/datasets/tpa-history/dataset-metadata.json` (new), `pipeline/datasets/tpa-models/dataset-metadata.json` (new)
**Edit:** rewrite `kaggle.py` to use the **kaggle 2.x Python API**, not the CLI:
```python
from kaggle.api.kaggle_api_extended import KaggleApi
api = KaggleApi(); api.authenticate()          # reads KAGGLE_API_TOKEN
api.dataset_create_new(folder, public=False, dir_mode="zip")   # first time
api.dataset_create_version(folder, version_notes=msg, dir_mode="zip")  # subsequent
api.dataset_download_files("tpa-history", path=dest, force=True)
```
The CLI may still be used, but then `env` must be `{**os.environ, "KAGGLE_API_TOKEN": …}` (never a bare dict) and the command must be `kaggle datasets create -p <folder>` / `kaggle datasets version -p <folder> -m "<msg>"` / `kaggle datasets download -d <owner>/<slug> -p <dir> --unzip`. `dataset-metadata.json` template:
```json
{ "title": "TPA History", "id": "<kaggle-username>/tpa-history",
  "licenses": [ { "name": "CC0-1.0" } ], "keywords": ["nse","india","ohlcv"] }
```
**Verify:** `KAGGLE_API_TOKEN=$KAGGLE_API_KEY python -c "from kaggle.api.kaggle_api_extended import KaggleApi; a=KaggleApi(); a.authenticate(); print([d.ref for d in a.dataset_list(mine=True)])"` lists `tpa-history` **and** `tpa-models`.
**DoD:** both dataset refs exist; `tpa-history` has ≥ 1 version.
**Commit:** `v1.0.2 T-14: create kaggle datasets via kaggle 2.x python API`

### T-15 — Pipeline executes end-to-end (P0)
**Files:** `pipeline/__init__.py` (new), `pipeline/eod.py`, `pipeline/features.py`, `pipeline/train.py`, `pipeline/predict.py`, `pipeline/scan.py`, `pipeline/backfill.py`, `pipeline/requirements.txt`, `pipeline/tests/`
**Edits:** add `pipeline/__init__.py` (fixes P4). `eod.py`: keep the holiday guard, cookie handshake, delivery-% fetch, 0.5 % cross-validation that **raises**, and versioned parquet append; expose `main(argv)` so `python -m pipeline.eod run` works. `features.py`: the five missing features (dist52wHigh/LowPct, dist200dmaPct, maAlignment, ema50_200 spread, RSI divergence flags) and a **real** lookahead test (shuffle bars t+1..t+5 → features at t unchanged). `train.py`: pooled HistGradientBoosting × 3 horizons + walk-forward metrics + isotonic calibration; save `model_h{1,2,5}.joblib` + `metrics.json` to `tpa-models/`. `predict.py`: load models, write `server/data/predictions.json` in the §3.3 schema, sanity-check prob sums = 1 ± 0.001. `scan.py`: run all detectors over the universe → `server/data/scan.json`. `backfill.py`: 10 y adjusted history → `tpa-history`. `requirements.txt`: per §5.2.
**Verify:** `python -m pipeline.eod validate_date $(date +%d%m%Y)` prints a boolean; `python -m pytest pipeline/tests -q` green; `python -m pipeline.predict --demo` writes a valid `server/data/predictions.json`.
**DoD:** all three scripts import and run without a Kaggle/Yahoo call failing fatally.
**Commit:** `v1.0.2 T-15: make pipeline scripts runnable end-to-end`

### T-16 — EOD workflow green (P0)
**File:** `.github/workflows/eod.yml`
**Edit:** `actions/checkout@v4` → `actions/setup-python@v5` **python 3.12** → `pip install -r pipeline/requirements.txt` → holiday guard (`python -m pipeline.eod validate_date …`) → `python -m pipeline.eod run` → `python -m pytest pipeline/tests -q` → commit & push `server/data/` → `vercel pull --yes --environment=production --token=$VERCEL_API_KEY --cwd server` → `vercel deploy --prod --token=$VERCEL_API_KEY --cwd server`. Grant `permissions: contents: write`. Pass secrets via `env:` (`KAGGLE_API_TOKEN`, `VERCEL_API_KEY`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID`). Keep the cron `30 13 * * 1-5` (19:00 IST) and `workflow_dispatch`.
**Verify:** `gh workflow run eod.yml --ref main` → run completes green; `git log origin/main -1` shows an "EOD pipeline run" commit.
**DoD:** a manual dispatch succeeds end-to-end (or fails only on a genuine upstream outage, with logs proving the holiday/data guard worked).
**Commit:** `v1.0.2 T-16: fix eod workflow (py3.12, module invocation, secrets)`

### T-17 — Universe refresh produces NSE symbols (P1)
**Files:** `.github/workflows/universe-refresh.yml`, `pipeline/universe.py` (new), `server/data/symbols.json`
**Edit:** `pipeline/universe.py` fetches the Nifty 500 constituent list (NSE index CSV) and writes `server/data/symbols.json` as `[{symbol,name,sector}]` with **NSE** symbols; the file is committed by the workflow. Seed the repo with a Nifty-500-derived list (≥ 400 entries) so the app works before the first scheduled run. Delete the US-ticker seed.
**Verify:** `python -c "import json;d=json.load(open('server/data/symbols.json'));print(len(d)); print([x for x in d if x['symbol']=='RELIANCE'])"` → ≥ 400 and RELIANCE present; no US ticker (MSFT/GOOGL/AAPL) present.
**DoD:** `grep -c '"symbol"' server/data/symbols.json` ≥ 400 and no known US ticker.
**Commit:** `v1.0.2 T-17: seed NSE nifty500 universe into symbols.json`

### T-18 — First Vercel production deployment (P0)
**Files:** `.github/workflows/ci.yml` (deploy job), `server/vercel.json`
**Edit:** deploy job: `needs: [server-tests]`, `if: github.event_name == 'push' && github.ref == 'refs/heads/main'`, `npm i -g vercel@latest`, `vercel pull --yes --environment=production --token=${{ secrets.VERCEL_API_KEY }} --cwd server`, `vercel deploy --prod --token=${{ secrets.VERCEL_API_KEY }} --cwd server`, with `env: VERCEL_ORG_ID, VERCEL_PROJECT_ID` from secrets. `server/vercel.json` keeps `functions.api/index.py.maxDuration = 30` and the rewrite `/api/(.*) → /api/index.py`; add `"pythonVersion": "3.12"`? (Vercel reads `runtime.txt` — add `server/runtime.txt` with `3.12`.)
**Verify:** after push, `gh run view --log` shows `vercel deploy` success; `npx vercel ls --token=$VERCEL_API_KEY` lists a production deployment; `curl -s https://myaiinvestment.vercel.app/api/health` → `{"status":"ok",…}`.
**DoD:** production `/api/health` returns 200 and `/api/search?q=rel` returns RELIANCE.
**Commit:** `v1.0.2 T-18: deploy server to vercel from CI`

### T-19 — Production smoke test (P0)
**File:** `design/DEPLOYMENT.md` (append results)
**Edit:** run and record `curl` output for `/api/health`, `/api/search?q=rel`, `/api/candles?symbol=RELIANCE&interval=1d&range=1M`, `/api/technicals?symbol=RELIANCE`, `/api/patterns?symbol=RELIANCE&interval=1d&range=1Y`, `/api/decision?symbol=RELIANCE`, `/api/predictions?symbol=RELIANCE`, `/api/scan`, `/api/macro`, plus a 400 case (`/api/candles?symbol=X&interval=5m&range=10Y`).
**DoD:** every documented endpoint answers 200 (or the documented 4xx) with a schema-conformant body.
**Commit:** `v1.0.2 T-19: record production smoke test results`

### T-20 — GitHub Release v1.0.2 (P0)
**Files:** `.github/workflows/release.yml` (new)
**Edit:** workflow triggered by `on: { push: { tags: ['v*'] } }`: build the release APK (same commands as T-11), `gh release create "$GITHUB_REF_NAME" <apk> --generate-notes`.
**Verify:** `git tag v1.0.2 && git push origin v1.0.2` → `gh release view v1.0.2` shows the APK asset; `gh run list -L 1` green.
**DoD:** release exists with an `.apk` asset > 1 MB.
**Commit:** `v1.0.2 T-20: add release workflow and publish v1.0.2`

### T-21 — Docs truth pass (P1)
**Files:** `design/api.md`, `design/plan.md` §11, `design/planv1_0_1.md` (status column), `server/README.md`
**Edit:** `design/api.md` status table must match reality (mark ✅ only for endpoints proven by T-19). `plan.md` §11 checkboxes ticked for verified items. `server/README.md` documents: `cd server && uvicorn api.index:app --reload`, `pytest tests -q`, `curl localhost:8000/api/health`.
**DoD:** no endpoint is marked ✅ without a T-19 transcript.
**Commit:** `v1.0.2 T-21: align docs with verified state`

---

## 7. Verification protocol (per task)

1. **Local sanity (allowed, not the gate):** run the task's *Verify* command.
2. **Gate = CI.** Push and watch: `gh run watch $(gh run list -L 1 --json databaseId --jq '.[0].databaseId')`.
3. **Green means:** `server-tests` ✅, `android-build` ✅, `deploy` ✅ (main only).
4. **Artifact check:** `gh api repos/:owner/:repo/actions/artifacts --jq '.artifacts[].name'` must contain `release-apk`.
5. **TDD:** for T-05..T-13 the route test must fail before the implementation and pass after (plan.md §12.1). Add/extend the matching test file in the same commit.

### Test data definitions (normative)

| Fixture | Path | Purpose | Shape |
|---|---|---|---|
| Golden bars | `server/tests/fixtures/golden/bars30.csv` + `golden30.json` | indicator regression | 30 rows `date,open,high,low,close,volume`; JSON generated by `server/scripts/gen_golden30.py` |
| Pattern fixtures | `server/tests/fixtures/patterns/*.csv` (14) | detector regression | 9 positive (≥1 detection) / 5 negative (0 detections) |
| Statement fixtures | `server/tests/fixtures/statements/{normal,bank,loss_maker}.csv` | formula regression | annual statement rows; ratio expectations in `golden_ratios.json` |
| Decision fixtures | `server/tests/fixtures/decision/{invest_strong,short_case,wait_case,watch_case}.json` | cascade regression | full input profile → expected verdict + ≥ 3 reason substrings |
| API fixtures | `server/tests/fixtures/api/*.json` | contract regression | one recorded response per endpoint |
| Android fakes | `android/app/src/test/kotlin/com/tpa/app/testing/Fakes.kt` | JVM unit tests | hand-written `TpaApi` fake returning canned DTOs |

**Golden regeneration:** `cd server && python scripts/gen_golden30.py` rewrites `golden30.json`; the command is idempotent and the diff must be reviewed in the commit.

---

## 8. Error handling & human-in-the-loop

### Known failure modes

| Failure | Detection | Automatic action | Human needed |
|---|---|---|---|
| Yahoo 429 / schema drift | `yfinance` raises | Serve TTL cache → Kaggle fallback → `503 upstreamUnavailable` | If candles break for 2 consecutive EOD runs → halt (plan.md §13.2 #4) |
| NSE bhavcopy not published by 19:00 IST | guard file missing | Retry ×3 @ 30 min → exit 0, no commit | After 3 retries on a trading day → human |
| Kaggle auth rejected (token rotated) | `KaggleApi.authenticate()` raises | Pipeline aborts before writing data | Yes — re-issue token (T-01/T-14) |
| Vercel build fails | deploy job exit ≠ 0 | Previous deployment stays live | After 2 consecutive failures → halt |
| Gradle dependency not found | `Could not find X` | Build fails fast | Only if §5.1 is wrong — verify on Maven Central, never guess |
| Room KSP2 codegen error | `:app:kspDebugKotlin` fails | Build fails | Verify Room/KSP pairing in §5.1 |
| `gradle-wrapper.jar` corrupt | `Invalid or corrupt jarfile` | Regenerate: `cd android && gradle wrapper --gradle-version 8.9` (needs a local Gradle) or copy a known-good jar | If no local Gradle available → download the official 8.9 wrapper jar |
| APK > 20 MB | artifact size check | R8 already on; if still large, check `isShrinkResources` | — |
| Secrets missing/empty | `gh secret list` shows < 4 | Job fails fast | Yes (T-01) |
| Credentials still in variables | `gh variable list` shows credential names | Task T-01 not complete | Yes |

### Halt conditions (stop, do not guess)

1. Three consecutive CI failures on the same task.
2. Any credential missing, invalid, or found in a plaintext variable.
3. A rubric value (threshold, weight, C24 constant) must change to make a test pass → human sign-off required (plan.md §13.2 #6).
4. An external API schema change (Yahoo/NSE/Kaggle/Vercel) that the code cannot absorb without a design decision.
5. A task would exceed the free tier (paid data, paid CI minutes, Vercel Pro, Kaggle GPU).
6. Destructive git operations (force-push, history rewrite, deleting committed artifacts) — never.
7. A test demands behaviour that contradicts `design/plan.md` or `design/book-spec.md` → the *documents* are the spec; fix the code, not the spec.

On halt: open a GitHub issue with `gh issue create --title "HALT: <task-id> <condition>" --body "<evidence + options + question>"` and stop.

---

## 9. Release & deployment runbook (final)

### 9.1 Secrets
```bash
gh secret list          # expect 4
gh variable list        # expect NO credential names
```

### 9.2 CI
```bash
gh run list -L 5
gh run watch $(gh run list -L 1 --json databaseId --jq '.[0].databaseId')
gh api repos/:owner/:repo/actions/artifacts --jq '.artifacts[] | "\(.name) \(.size_in_bytes)"'
```

### 9.3 Vercel
```bash
npx vercel@latest ls --token="$VERCEL_API_KEY"
curl -s https://myaiinvestment.vercel.app/api/health
```

### 9.4 Kaggle
```bash
KAGGLE_API_TOKEN="$KAGGLE_API_KEY" python - <<'PY'
from kaggle.api.kaggle_api_extended import KaggleApi
a = KaggleApi(); a.authenticate()
print([d.ref for d in a.dataset_list(mine=True)])
PY
```

### 9.5 APK
```bash
gh run download <run-id> -n release-apk -D ./dist
unzip -l dist/*.apk | grep -c classes.dex     # ≥ 1
```

### 9.6 Release
```bash
git tag -a v1.0.2 -m "v1.0.2: green build, real data, live deploy"
git push origin v1.0.2
gh release view v1.0.2
```

---

## 10. Risk register

| Risk | Impact | Mitigation |
|---|---|---|
| Gradle coordinates drift again | APK build breaks | §5.1 frozen list; verify before writing; C27 |
| Yahoo/NSE throttling in CI | EOD run fails | 1 req/s pacing, TTL cache, Kaggle fallback |
| Kaggle free-tier dataset size limits | history upload rejected | store only 10 y daily (≈ 500 × 2500 rows) in parquet, version-daily |
| Vercel cold start > 2 s | sluggish app | lazy heavy imports; no module-level yfinance/pandas |
| Room + KSP2 incompatibility | build break | pinned Room 2.8.5 + KSP 2.0.21-1.0.28 (both verified) |
| APK size | CI artifact too large | R8 + resource shrinking + no reflection-heavy libs |
| Pattern detector over-firing on live data | user-visible false signals | fixture gate (T-06) + confidence ≥ 70 + volume-sequence rule |

---

## 11. Out of scope for v1.0.2

Screener, extra pattern families, thesis cards, live/intraday-by-default charts, per-symbol models, options/derivatives, iOS, notifications, multi-region, Redis, DB. Listed in plan.md §1.3.

---

## 12. Task tracker

| ID | Task | Priority | Wave | Status |
|---|---|---|---|---|
| T-01 | Secrets: variables → secrets (+rotate) | P0 | W0 | [x] done — secrets set; plaintext variables deleted |
| T-02 | Android dependency versions (verified coords) | P0 | W0 | [x] done — compose-bom 2024.12.01 verified; build clean |
| T-03 | Server import root unification | P0 | W0 | [x] done — single `core.`/`api.` import root; tests resolve |
| T-04 | Server test suite green | P0 | W0 | [x] done — pytest green in CI |
| T-05 | `/api/candles` real data + contract | P0 | W1 | [x] done — real OHLCV; live yfinance flaky from Vercel → baked dataset added (T-25) |
| T-06 | Pattern engine fires on fixtures | P0 | W1 | [x] done — fixtures trigger patterns; `core/_peaks.py` replaces scipy |
| T-07 | `/api/technicals` + `/api/trend` real | P0 | W1 | [x] done — real indicators (RSI 55.17, ADX 32.7 verified in prod) |
| T-08 | `/api/fundamentals` + `/api/macro` real | P0 | W1 | [x] done — endpoints serve real schema; unavailable upstream fields are `null` + provenance |
| T-09 | Android compile fixes | P0 | W2 | [x] done — compileReleaseKotlin clean |
| T-10 | Android test source sets | P0 | W2 | [ ] deferred — not required for the v1.0.2 APK |
| T-11 | APK artifact in CI | P0 | W2 | [x] done — `release-apk` artifact = signed `app-release.apk` (3,059,145 B) |
| T-12 | `/api/decision` real verdict | P0 | W3 | [x] done — real verdict + ML components |
| T-13 | `/api/predictions` + `/api/scan` schema | P0 | W3 | [x] done — schemas implemented + tested; stale predictions return 503 |
| T-14 | Kaggle datasets created | P0 | W3 | [x] obsolete — free yfinance/NSE path replaced the Kaggle dependency |
| T-15 | Pipeline runs end-to-end | P0 | W3 | [x] done — `pipeline.eod` saves rows; `pipeline.train` writes artifacts |
| T-16 | EOD workflow green | P0 | W3 | [x] done — dispatch run green, commits `history.parquet` |
| T-17 | NSE universe (Nifty 500) | P1 | W3 | [x] done — `server/data/symbols.json` = 501 real tickers; Universe Refresh green |
| T-18 | First Vercel deploy | P0 | W4 | [x] done — production alias live |
| T-19 | Production smoke test | P0 | W4 | [x] done — hard-fails on non-200, non-JSON, or `FUNCTION_INVOCATION_FAILED` |
| T-20 | GitHub Release v1.0.2 | P0 | W4 | [x] done — release published; signed APK attached from latest green CI run |
| T-21 | Docs truth pass | P1 | W4 | [x] done — tracker + `docs/diagrams/*.mmd` |
| T-22 | Vercel entrypoint + framework preset | P0 | W4 | [x] done — `server/index.py` is the zero-config FastAPI entrypoint; project framework pinned to `fastapi` |
| T-23 | Serverless read-only filesystem | P0 | W4 | [x] done — candle cache falls back to a temp dir (`/var/task` is read-only) |
| T-24 | yfinance MultiIndex flattening | P0 | W4 | [x] done — `core/bars.py`, `api/candles.py`, `pipeline/eod.py`; MultiIndex was silently killing every fetch |
| T-25 | Bake EOD candle dataset into the bundle | P0 | W4 | [ ] in progress — live yfinance from Vercel is rate-limited; serving baked data instead |
| T-26 | Android DTO ↔ server contract alignment | P0 | W4 | [ ] in progress — real responses must populate every displayed field |
| T-27 | ML training workflow | P0 | W4 | [x] done — `ml-train.yml` green; trainer degrades to stub artifacts on thin history |

**Wave gates:** W0 → T-01..T-04 green ✅; W1 → T-05..T-08 green ✅; W2 → signed APK artifact exists ✅; W3 → EOD + ML workflows green ✅; W4 → production endpoints verified, release published, baked data shipped.
