# v1.1.0 — App review, refactor and UX repair

**Scope:** Android app only (`android/`). Server stays unchanged.
**Goal:** working tabs, real landing screen (search + wishlist), true backdrop-blur
glassmorphism, one navigation model, no duplicated data layer.

## 1. Findings (verified in code)

| # | Sev | Finding | Evidence |
|---|-----|---------|----------|
| 1 | P0 | Search tab is a stub; real search screen is never shown | `MainActivity.kt:156` renders only an "Open RELIANCE" button; `ui/search/SearchScreen.kt` is unreferenced |
| 2 | P0 | Tabs cannot switch while a symbol is open | `MainActivity.kt:101`: `onSelect = { if (stackedSymbol == null) selectedTab = it }` |
| 3 | P0 | Symbol detail tabs are dead; content is always Fundamentals | `MainActivity.kt:206` `onClick = {}`; `SymbolContent` ignores the selected tab and unused VMs |
| 4 | P0 | Charts tab builds a VM with no symbol | `MainActivity.kt:177-183`; `ChartViewModel.init` fetches only when a symbol exists |
| 5 | P1 | Blur is applied to the bar itself (foreground), not the content behind it | `MainActivity.kt:129` `.blur(8.dp)` on `NavigationBar`; same pattern in `GlassBottomBar` |
| 6 | P1 | Glass cards are opaque, so no glass effect is visible | `MainActivity.kt:262` Surface `color = surface` covers the gradient; `Glass.kt:28-34` uses hardcoded dark values |
| 7 | P1 | Wishlist cards are not clickable; `onClick` is accepted but unused | `ui/wishlist/WishlistScreen.kt:61,69-87` |
| 8 | P1 | Duplicate wishlist stacks | `database/WatchlistDb.kt` + `viewmodel/WatchlistViewModel.kt` + `ui/search/WatchlistScreen.kt` are dead; live stack is `data/wishlist/*` + `ui/wishlist/*` |
| 9 | P1 | API + Room objects recreated per composition | `NetworkModule.provideApi()` uses `remember` inside each caller; `WishlistRepository` opens `tpa.db` per Wishlist tab |
| 10 | P2 | ~97 hardcoded colors break light theme | `SearchScreen.kt`, `TechnicalScreen.kt`, `ChartScreen.kt`, `DecisionCard.kt`, `DetectionChip.kt` |
| 11 | P2 | Dead UI files | `VolumeBars.kt`, `SymbolScreen.kt`, `TradePlanCard.kt`, `ThemeViewModel.kt` have no callers |

## 2. Target architecture

```
MainActivity
 └─ TechnicalPatternAnalysisTheme
     └─ TpaApp(container)
         ├─ NavState (tab + optional SymbolRoute)
         ├─ HazeState (backdrop blur)
         ├─ AppBackground (gradient + aurora orbs)
         ├─ screen content (scrolls under the bar)
         └─ GlassBottomBar        <-- hazeChild(state): blurs content behind it
             ├─ HomeScreen        (search + wishlist preview + quick symbols)
             ├─ WishlistScreen    (clickable cards, remove, notification toggle)
             └─ ChartsTab         (chart for last/default symbol; own symbol switch)
         └─ SymbolDetailScreen    (back from any tab)
             ├─ ChartTab
             ├─ FundamentalsTab
             └─ TechnicalsTab
```

- One API instance and one Room DB per process via `AppContainer` owned by
  `TpaApplication`.
- Navigation is a pure value (`NavState`) with reducer functions, unit tested.
- Tabs always switch; opening a tab clears the detail route.
- Glass = Haze 0.7.3: `Modifier.haze(state)` on the content layer,
  `Modifier.hazeChild(state, shape, style)` on the bottom bar. No `Modifier.blur`
  on foreground UI.
- Cards use translucent token-driven fills + 1 dp border; only the
  bottom bar uses a live backdrop blur (scroll performance).

## 3. Exact file plan

### New
| File | Contents |
|------|----------|
| `TpaApplication.kt` | `class TpaApplication : Application()` with `val container by lazy { AppContainer(this) }`; `class AppContainer(context: Context)` with `api: TpaApi`, `wishlist: WishlistRepository` |
| `ui/navigation/NavState.kt` | `enum MainTab { Home, Wishlist, Charts }`; `enum SymbolTab { Chart, Fundamentals, Technicals }`; `data class SymbolRoute(symbol, tab)`; `data class NavState(tab, detail)` with `selectTab`, `openSymbol`, `selectSymbolTab`, `back` |
| `ui/TpaApp.kt` | root composable; `HazeState`; `rememberSaveable` nav; `BackHandler`; `AppBackground`; bottom bar |
| `ui/components/AppBackground.kt` | `@Composable fun AppBackground()` gradient + two radial orbs |
| `ui/components/GlassBottomBar.kt` | `@Composable fun GlassBottomBar(nav, hazeState, onSelect)` using `hazeChild` |
| `ui/components/GlassCard.kt` | `@Composable fun GlassCard(modifier, onClick: (() -> Unit)? = null, content)` |
| `ui/components/StateViews.kt` | `LoadingView`, `ErrorView`, `EmptyView` |
| `ui/components/SymbolCard.kt` | `@Composable fun SymbolCard(symbol, name, sector, trailing, onClick)` |
| `ui/home/HomeScreen.kt` | header, `SearchField`, inline results, wishlist preview, quick symbols |
| `ui/search/SearchViewModel.kt` | `SearchUiState(query, results, loading, error)`; `onQueryChange`, `clear`; 300 ms debounce; cancellation-safe |
| `ui/search/SearchComponents.kt` | `SearchField`, `SearchResults` |
| `ui/symbol/SymbolDetailScreen.kt` | back + symbol header + working segmented tabs + content |
| `test/.../NavStateTest.kt` | tab switch clears detail; open/back; symbol tab change |

### Modified
| File | Change |
|------|--------|
| `MainActivity.kt` | keep only activity setup; `setContent { Theme { TpaApp(container) } }`; remove all screens/icons/glass helpers |
| `network/NetworkModule.kt` | add plain `fun createApi(): TpaApi`; keep a thin composable wrapper only if needed |
| `data/wishlist/WishlistRepository.kt` | singleton via container; no per-composition DB |
| `ui/wishlist/WishlistViewModel.kt` | add `add`, `remove`, `toggle`; keep `entries` |
| `ui/wishlist/WishlistScreen.kt` | clickable cards, remove action, bell toggle, empty state, bottom content padding |
| `ui/theme/Theme.kt` | complete light/dark schemes (`onSurfaceVariant`, `outline`, `surfaceContainerHigh`) |
| `ui/theme/Color.kt` | add `glassFillStrong`, `glassBorder`, `successContainer`, `warning` tokens |
| `ui/theme/Glass.kt` | token-based `glassSurface(shape, alphaScale)`, `glassCard()`, `glassBar(hazeState, shape)`; remove dark-only hardcode |
| `ui/symbol/ChartScreen.kt` | becomes `ChartTab(viewModel)`; removes back/default symbol; theme tokens replace white text; bottom padding |
| `ui/symbol/ChartViewModel.kt` | constructor `(api, initialSymbol = "")`; load in init; no-op on blank |
| `ui/symbol/TechnicalScreen.kt` | token colors; loading/error state |
| `ui/symbol/DecisionCard.kt` | token colors |
| `android/app/build.gradle.kts` | `implementation("dev.chrisbanes.haze:haze:0.7.3")`; versionCode 110 / 1.1.0 |
| `AndroidManifest.xml` | `android:name=".TpaApplication"` |

### Deleted
`database/WatchlistDb.kt`, `viewmodel/WatchlistViewModel.kt`, `viewmodel/ThemeViewModel.kt`,
`ui/search/SearchScreen.kt`, `ui/search/WatchlistScreen.kt`, `ui/chart/VolumeBars.kt`,
`ui/symbol/SymbolScreen.kt`, `ui/symbol/TradePlanCard.kt`.

## 4. Data flow

1. `MainActivity` reads `TpaApplication.container`; theme wraps `TpaApp`.
2. `TpaApp` holds `NavState` and a single `WishlistViewModel(container.wishlist)`.
3. Home: `SearchViewModel(container.api)`; typing debounces 300 ms → `api.search`;
   tapping a result calls `nav.openSymbol(symbol)`. Wishlist preview observes the
   shared VM; tapping a card opens the symbol.
4. Symbol detail: `key(symbol)` creates `ChartViewModel(api, symbol)` /
   `FundamentalsViewModel(api, symbol)`; `TechnicalsTab` calls
   `vm.load(symbol)` in `LaunchedEffect`.
5. Back: `NavState.back()` clears the detail. Bottom tabs call `selectTab`
   (always clears detail).
6. Glass: `HazeState` is created once at root; content layer applies
   `Modifier.haze`; the bar applies `Modifier.hazeChild`, so the bar samples and
   blurs the pixels behind it.

## 5. Edge cases

- Empty search query → results cleared, no request.
- Fast typing → previous debounce job cancelled; `CancellationException` rethrown.
- Network failure → inline error, previous results cleared, no crash.
- Unknown/blank symbol → detail route not opened.
- Symbol with no candles/patterns → explicit empty states, no infinite spinner.
- Configuration change → nav saved via `rememberSaveable` (tab, symbol, inner tab).
- Wishlist duplicate add → Room `REPLACE`, single row.
- Light/dark switch → all glass fills/borders derive from `LocalTpaTokens`.
- Notification permission denied → toggle stays off, worker not scheduled.
- Bottom bar overlays content → lists reserve 104 dp bottom padding; last item
  remains reachable.

## 6. Implementation order

1. Add Haze + `AppContainer` + manifest application class.
2. Add `NavState` + unit test.
3. Rewrite theme glass primitives and background.
4. Rewrite `MainActivity` → `TpaApp` + `GlassBottomBar`.
5. Build Home (search + wishlist preview) and Search VM/components.
6. Rewrite Wishlist screen and VM.
7. Build `SymbolDetailScreen` with working inner tabs; adapt Chart/Fundamentals/Technicals.
8. Delete dead files, replace hardcoded colors on remaining screens.
9. Bump 1.1.0, docs/diagrams, push; CI green; release `v1.1.0` with APK.

## 7. Verification (CI is the gate)

- `ci.yml`: `server-tests`, `android-build` (compile + assemble), `deploy` green.
- New `NavStateTest` runs in `android-build` via `:app:testReleaseUnitTest` (added step).
  Compose UI tests moved to `androidTest`; the Mockito-based `ChartViewModelTest`
  was removed because Mockito was never on the test classpath.
- APK artifact `release-apk` installed; manual check: all three tabs switch,
  detail back works, inner tabs switch, search returns results, wishlist opens
  detail, bar blurs content behind it in both themes.

## 8. Quota policy (this session)

- Free model: $0 per token, no published per-minute/day number; usage is visible
  in OpenCode Console → Usage. No 429s in local logs.
- One agent, no subagents, no polling loops, no local Gradle builds.
- Targeted reads/edits; CI is the only build gate.
