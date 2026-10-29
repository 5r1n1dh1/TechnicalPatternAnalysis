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
