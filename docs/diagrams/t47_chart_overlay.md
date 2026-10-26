# T-47/T-48/T-49/T-50 — Chart Overhaul, Real Overlay, Pivot Markers, No Flicker

## Flowchart (user-facing)
```mermaid
flowchart TD
    U[User opens Charts & Patterns tab] --> VM[ChartViewModel loads state]
    VM --> API[GET candles + trend + patterns<br/>symbol, interval, range, pattern?]
    API --> CH[CombinedChart<br/>CandleData price + BarData volume]
    API --> SEL[Detection chips list]
    SEL --> PICK{User picks a detection}
    PICK -->|yes| MAP[ISO anchor/pivot time → candle index]
    PICK -->|no| CH
    MAP --> OV[PatternOverlayRenderer draws<br/>anchor polyline, dashed neckline,<br/>anchor zone, target + stop lines]
    MAP --> PM[PivotMarker scatter dots<br/>HIGH green / LOW red]
    OV --> CH
    PM --> CH
    CH --> U
    VM --> CHG{range / interval / pattern changed?}
    CHG -->|yes| CANCEL[Cancel previous fetch job]
    CANCEL --> API
    CHG -->|no| U
```

## Sequence: chart load, overlay draw, re-trigger
```mermaid
sequenceDiagram
    participant U as User
    participant CS as ChartScreen
    participant VM as ChartViewModel (NavBackStackEntry-scoped)
    participant API as /api/{candles,patterns}
    participant CH as CombinedChart
    participant OR as PatternOverlayRenderer
    participant PM as PivotMarker
    U->>CS: open Charts tab (symbol set)
    CS->>VM: collect state (symbol, interval, range, pattern)
    VM->>API: GET candles + GET patterns
    API-->>VM: candles[ISO o,h,l,c,v] + detections[anchors ISO, pivots, neckline]
    VM-->>CS: state emits (same VM instance → no flicker)
    CS->>CH: set CombinedData(CandleData, BarData volume)
    CS->>OR: drawPatternOverlay(anchor polyline, neckline, zone, target, stop)
    CS->>PM: scatter entries for pivots (HIGH/LOW)
    CH-->>U: candles + volume + overlay + pivot markers
    U->>CS: change range / interval / pattern
    CS->>VM: updateRange / updateInterval / selectPattern
    VM->>VM: cancel in-flight fetch, start new one
    VM-->>CS: fresh state → chart redraws in place
```

## Implementation notes
- Chart host: `android/app/src/main/kotlin/com/tpa/app/ui/symbol/ChartScreen.kt` — one `CombinedChart` carrying price candles, volume bars and pivot scatter (`chart.extraData`).
- Overlay: `android/app/src/main/kotlin/com/tpa/app/ui/chart/PatternOverlayRenderer.kt`; pivots: `android/app/src/main/kotlin/com/tpa/app/ui/chart/PivotMarker.kt`; volume: `android/app/src/main/kotlin/com/tpa/app/ui/chart/VolumeBars.kt`.
- Anchor/pivot timestamps are ISO strings; `indexForTime` maps them to candle indices (exact match, then same-day fallback).
- Flicker elimination (T-50): view models are scoped to the `NavBackStackEntry`, so a recomposition or tab switch reuses the loaded state instead of refetching into an empty screen.
