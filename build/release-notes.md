v1.3.0 — versionCode 130, versionName 1.3.0

Intraday charts
- 5m, 15m, 30m and 1h candles are back. They come from Yahoo's chart
  endpoint first, then Groww's delayed charting endpoint; both need no
  key or cookie, so they work from Vercel where yfinance fails.
- Groww reports cumulative volume per session, so the server converts it
  to per-bar volume before returning candles.
- The interval/range picker mirrors the server matrix exactly: minutes
  with 1D-1M, hourly with 1D-1Y, daily with 5D-10Y, weekly/monthly with
  1Y-10Y. Invalid pairs are clamped to the first supported value; choosing
  1D while on daily switches to 5m.
- Intraday candle data is cached for 60 seconds; daily data keeps the
  12-hour cache. Intraday pairs skip the on-disk candle cache.
- X-axis labels show HH:mm for 1D, dd MMM HH:mm for longer intraday
  ranges, and dd MMM for daily and coarser candles.

Pivot points
- Detection payloads now include the bar index for every anchor and
  neckline, and a timestamp for every pivot. The chart uses the index, so
  intraday markers land on the right candle regardless of timezone.
- High and low pivots are separate scatter series: amber for highs, blue
  for lows, with matching legend entries.
- New pivot summary card under the chart for the selected detection: total
  count, high/low counts, and a row per pivot with date/time, kind and
  price. Its guide explains how pivots feed pattern detection.
- Guide text for interval, range and pivots updated.

Release
- APK asset is uploaded as TPA.apk.

v1.2.0 — versionCode 120, versionName 1.2.0

Search
- Typing is handled by local field state, so every keystroke renders
  immediately even while a request is in flight.
- 300 ms debounce, late responses for old text are ignored, and a failed
  search now shows a Retry button.

Charts: intervals and ranges
- Only combinations the server can actually serve are offered: daily candles
  with 5D-10Y, weekly/monthly with 1Y-10Y. The intraday choices that returned
  404 are gone.
- Changing one side clamps the other, so an invalid pair can never be sent.
- Error screen has "Reset 1d · 1Y"; pattern errors have Retry and Reset.
- Every range/interval change clears the old overlay and re-runs pattern
  detection for the new pair, with a section progress bar and label.

Progress
- New global progress bar under the status bar for every network call.
- Section bars for candles, pattern scan, trade plan, technicals,
  fundamentals, decision and search, each with a short caption.

Education
- Every screen has a guide button that opens a scrollable plain-language
  glossary of the identifiers shown there (chart, detections, trade plan,
  technicals, decision, fundamentals, wishlist, search).

Chart overlay
- Anchor path is colored by direction with visible dots.
- Neckline extends to the latest candle; neckline, target and stop are labeled
  horizontal lines with prices.
- Compose legend above the chart explains each color and line.
- Candle, grid and axis colors follow the light/dark theme.

Release
- APK asset is uploaded as TPA.apk.

v1.1.0 — versionCode 110, versionName 1.1.0

Android refactor (app review fixes)

Navigation
- Bottom tabs now always work, including while a symbol is open; selecting a
  tab closes the symbol detail.
- New navigation state (`NavState`) is a pure value with reducers, saved across
  configuration changes, and covered by unit tests.
- Symbol detail has working segmented tabs: Chart / Fundamentals / Technicals.

Landing screen
- Home is a real landing page: search field with 300 ms debounce, inline
  results, quick symbols, and a wishlist preview with "See all".
- Wishlist cards open the symbol; remove action and notification toggle work.
- Charts tab keeps a symbol switcher and uses the last opened symbol instead of
  starting blank.

Glassmorphism
- Backdrop blur is now done with Haze: the content layer is blurred behind the
  bottom bar instead of blurring the bar itself.
- Glass cards use translucent theme tokens (light and dark) rather than opaque
  surfaces; light theme is fully legible.
- Soft gradient/aurora background behind all screens.

Cleanup
- One Retrofit API instance and one Room database per process (`AppContainer`
  owned by `TpaApplication`).
- Removed dead screens/view-models and duplicated wishlist stack.
- Notification preference and periodic worker cannot drift apart.

Build
- CI now runs Android unit tests in addition to release compile + assemble.
- Two Compose UI tests moved to `androidTest`; stale Mockito-based
  `ChartViewModelTest` deleted (Mockito was never on the test classpath, so it
  could not run).

APK
- Built and uploaded by GitHub Actions (`release-apk`).
- Still signed with the throwaway debug key; replace before public distribution.

---

v1.0.4 — versionCode 104, versionName 1.0.4

Shipped scope (completes the v1.0.3 plan, T-28..T-52):

Server
- Pattern engine fixed: ATR is now computed over the same window as the pivot
  prices; before this, `detect_patterns` always raised internally and returned
  zero detections, so /patterns never reported a pattern.
- /patterns honours the documented error contract (400 invalidCombo,
  404 unknownSymbol) and accepts the pattern filter values the app sends.
- Head-and-shoulders fixture rebuilt: it now encodes a real H&S (equal
  shoulders, higher head, neckline troughs, breakdown) instead of an uptrend.

Android
- 3-tab bottom navigation (Search | Wishlist | Charts) with stacked symbol
  detail and global back.
- System-follow light/dark theme with an in-app override and semantic tokens.
- Live fuzzy search, Room-backed wishlist, WorkManager trend notifications.
- Candlestick + volume chart, real pattern overlay (anchors, neckline, zone,
  target, stop) and pivot markers on one CombinedChart.
- Adaptive high-DPI launcher icon; flicker-free view-model scoping.

APK
- Built by GitHub Actions: R8 + resource shrinking enabled, baseline profile
  included, four small native libs (androidx.graphics.path).
- 3.37 MB APK (1.58 MB as a compressed CI artifact).
- Signed with the throwaway debug key: no production keystore exists in repo
  secrets yet. Swap in a keystore-backed signingConfig before any public
  distribution.
