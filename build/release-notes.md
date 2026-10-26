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
