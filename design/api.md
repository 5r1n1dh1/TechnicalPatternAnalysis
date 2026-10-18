# Technical Pattern Analysis API Contract

This document defines the REST API endpoints for the server. All timestamps are in IST (+05:30) unless otherwise noted. JSON keys are camelCase.

## 1. Endpoint: `/api/health`

**Method:** `GET`

**Description:** Returns server health status.

**Query parameters:** None

**Success response (200 OK):**
```json
{
  "status": "ok",
  "dataAsOf": "2026-10-17T19:00:00+05:30" // null if no data yet
}
```

**Error responses:**
- `500 Internal Server Error` (if unexpected error)

## 2. Endpoint: `/api/search?q={prefix}`

**Method:** `GET`

**Description:** Search for stock symbols by prefix.

**Query parameters:**
- `q` (string, required): search prefix (fuzzy match)

**Success response (200 OK):**
```json
[
  {
    "symbol": "RELIANCE",
    "name": "Reliance Industries",
    "sector": "Energy"
  },
  ...
]
```

**Error responses:**
- `400 Bad Request` if `q` missing
- `500 Internal Server Error` if database issue

## 3. Endpoint: `/api/candles`

**Method:** `GET`

**Description:** Get historical OHLCV candles.

**Query parameters:**
- `symbol` (string, required): NSE symbol (e.g., "RELIANCE")
- `interval` (enum: "5m", "15m", "30m", "1h", "1d", "1w", "1M")
- `range` (enum: "1D", "5D", "1M", "3M", "6M", "1Y", "2Y", "5Y", "10Y")

**Validity matrix (enforced server-side):**

| Interval ↓ / Range → | 1D | 5D | 1M | 3M | 6M | 1Y | 2Y | 5Y | 10Y |
|---|---|---|---|---|---|---|---|---|---|
| 5m / 15m / 30m | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| 1h | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| 1d | — | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 1w / 1M | — | — | — | — | — | ✅ | ✅ | ✅ | ✅ |

**Success response (200 OK):**
```json
{
  "symbol": "RELIANCE",
  "interval": "1d",
  "range": "1Y",
  "tz": "Asia/Kolkata",
  "source": "yahoo", // or "kaggle" if fallback
  "candles": [
    {
      "t": "2026-10-15T09:15:00+05:30",
      "o": 1430.0,
      "h": 1440.5,
      "l": 1425.0,
      "c": 1435.2,
      "v": 12345678
    },
    ...
  ]
}
```

**Error responses:**
- `400 Bad Request` if invalid `interval` or `range`
- `404 Not Found` if symbol unknown
- `503 Service Unavailable` if data source unavailable

## 4. Endpoint: `/api/patterns`

**Method:** `GET`

**Description:** Detect chart patterns over a range.

**Query parameters:**
- `symbol` (string, required)
- `interval` (enum: same as above)
- `range` (enum: same as above)
- `pattern` (enum: "head_and_shoulders", "cup_and_handle", "double_triple", "flag", optional)

**Success response (200 OK):**
```json
{
  "detections": [
    {
      "pattern": "head_and_shoulders",
      "direction": "bearish",
      "confidence": 85,
      "anchors": [
        {"t": "2026-10-10T09:15:00+05:30", "price": 1450.0},
        {"t": "2026-10-12T09:15:00+05:30", "price": 1460.0},
        {"t": "2026-10-14T09:15:00+05:30", "price": 1445.0},
        {"t": "2026-10-16T09:15:00+05:30", "price": 1430.0}
      ],
      "neckline": {
        "t": "2026-10-13T09:15:00+05:30",
        "price": 1435.0
      }
    },
    ...
  ]
}
```

**Error responses:**
- `400 Bad Request` if invalid parameters
- `404 Not Found` if symbol unknown
- `503 Service Unavailable` if data insufficient

## 5. Endpoint: `/api/trend`

**Method:** `GET`

**Description:** Get trend verdict (Uptrend/Downtrend/Sideways).

**Query parameters:**
- `symbol` (string, required)
- `interval` (enum: same as above)
- `range` (enum: same as above)

**Success response (200 OK):**
```json
{
  "label": "Uptrend",
  "strength": 75,
  "ema20": 1425.0,
  "ema50": 1410.0,
  "adx": 28.0
}
```

**Error responses:**
- `400 Bad Request` if invalid parameters
- `404 Not Found` if symbol unknown
- `503 Service Unavailable` if data insufficient

## 6. Endpoint: `/api/technicals`

**Method:** `GET`

**Description:** Get technical indicators and support/resistance.

**Query parameters:** None (uses 1y daily bars)

**Success response (200 OK):**
```json
{
  "rsi14": 58,
  "macdHistNorm": -0.12,
  "ema20": 1425.0,
  "ema50": 1410.0,
  "adx": 28.0,
  "atr14pct": 2.3,
  "bollingerWidthPct": 0.8,
  "volRatio520": 1.2,
  "deliveryPct5": 0.68,
  "s_r": {
    "support": [
      {"price": 1420.0, "strength": 8, "touches": 5, "volume": 1.1e7},
      {"price": 1410.0, "strength": 6, "touches": 4, "volume": 9.5e6}
    ],
    "resistance": [
      {"price": 1445.0, "strength": 7, "touches": 6, "volume": 1.3e7},
      {"price": 1455.0, "strength": 5, "touches": 3, "volume": 1.0e7}
    ]
  },
  "candleChips": [
    {"pattern": "bullish_engulfing", "date": "2026-10-10", "strength": 7},
    {"pattern": "bearish_doji", "date": "2026-10-12", "strength": 5}
  ],
  "verdict": {
    "bullish": 6,
    "bearish": 4,
    "neutral": 2
  }
}
```

**Error responses:**
- `404 Not Found` if symbol unknown
- `503 Service Unavailable` if data insufficient

## 7. Endpoint: `/api/fundamentals`

**Method:** `GET`

**Description:** Get financial statements and ratios.

**Query parameters:** None (uses latest available statements)

**Success response (200 OK):**
```json
{
  "header": {
    "name": "Reliance Industries",
    "sector": "Energy",
    "mcap": 2.1e11,
    "52wRange": "1300-1500",
    "beta": 1.2
  },
  "valuation": {
    "pe_ttm": 18.5,
    "pe_forward": 17.2,
    "pb": 2.3,
    "dividendYield": 2.1,
    "eps": 45.0,
    "bookValue": 150.0
  },
  "profitability": {
    "roa": 1.8,
    "roce": 12.5,
    "roic": 11.2,
    "grossMargin": 35.0,
    "netMargin": 9.5
  },
  "quarterlyResults": [
    {"q": "Q3 2026", "rev": 1.2e10, "pat": 1.1e9, "eps": 35.0, "yoyRev": 5.0},
    ...
  ],
  "annualStatements": {
    "pnl": {"revenue": 5.0e10, "pat": 9.0e9},
    "balanceSheet": {"totalAssets": 3.5e11, "totalDebt": 1.2e11, "equity": 2.3e11},
    "cashFlow": {"ocf": 1.5e10, "capex": 8.0e9, "fcf": 7.2e9}
  },
  "redFlags": {
    "earningsQuality": false,
    "leverage": false,
    "growth": false,
    "management": false
  },
  "shareholding": {
    "promoter": 57.0,
    "fii": 12.0,
    "dii": 8.0,
    "public": 23.0
  }
}
```

**Error responses:**
- `404 Not Found` if symbol unknown
- `503 Service Unavailable` if data insufficient

## 8. Endpoint: `/api/predictions`

**Method:** `GET`

**Description:** Get ML trend probabilities.

**Query parameters:** None (uses baked JSON)

**Success response (200 OK):**
```json
{
  "asOf": "2026-10-17",
  "modelTrainedOn": "2026-10-17",
  "metrics": {
    "h1": {"accuracy": 0.54, "logLoss": 1.02, "brier": 0.61},
    "h2": {"accuracy": 0.52, "logLoss": 1.05, "brier": 0.62},
    "h5": {"accuracy": 0.53, "logLoss": 1.03, "brier": 0.60}
  },
  "predictions": [
    {
      "symbol": "RELIANCE",
      "close": 1437.90,
      "h1": {"up": 0.42, "down": 0.31, "sideways": 0.27},
      "h2": {"up": 0.38, "down": 0.35, "sideways": 0.27},
      "h5": {"up": 0.45, "down": 0.30, "sideways": 0.25}
    }
  ]
}
```

**Error responses:**
- `500 Internal Server Error` if JSON malformed

## 9. Endpoint: `/api/scan`

**Method:** `GET`

**Description:** Get today's universe-wide pattern hits.

**Query parameters:**
- `page` (int, optional): pagination

**Success response (200 OK):**
```json
{
  "asOf": "2026-10-17",
  "hits": [
    {
      "symbol": "HDFCBANK",
      "pattern": "cup_and_handle",
      "direction": "bullish",
      "confidence": 78,
      "anchors": [
        {"t": "2026-10-10", "price": 620.0},
        {"t": "2026-10-12", "price": 615.0},
        {"t": "2026-10-14", "price": 620.0},
        {"t": "2026-10-16", "price": 625.0}
      ],
      "neckline": {"t": "2026-10-13", "price": 620.0}
    },
    ...
  ]
}
```

**Error responses:**
- `500 Internal Server Error` if JSON malformed

## 10. Endpoint: `/api/decision`

**Method:** `GET`

**Description:** Get the final INVEST/SHORT/WAIT verdict with reasoning.

**Query parameters:** None (uses cached data)

**Success response (200 OK):**
```json
{
  "verdict": "INVEST",
  "confidence": 0.85,
  "components": {
    "fundamentalScore": 78,
    "technicalVerdict": "bullish",
    "valuationVerdict": "cheap",
    "patternContext": "active bullish cup&handle",
    "mlOutlook": {
      "h1": {"up": 0.45, "down": 0.30, "sideways": 0.25},
      "h2": {"up": 0.42, "down": 0.35, "sideways": 0.23}
    },
    "redFlags": ["earningsQuality", "leverage"]
  },
  "reasons": [
    "Fundamental score 78 (high quality) + valuation cheap (MoS 22%) + bullish structure + ML up > down h1/h5",
    "No red flags triggered"
  ]
}
```

**Error responses:**
- `500 Internal Server Error` if JSON malformed

## 11. Endpoint: `/api/tradeplan`

**Method:** `GET`

**Description:** Get trade plan for an active pattern.

**Query parameters:**
- `symbol` (string, required)
- `pattern` (enum: same as patterns)

**Success response (200 OK):**
```json
{
  "active": [
    {
      "pattern": "cup_and_handle",
      "entry": {"t": "2026-10-17", "price": 625.0},
      "stop": {"t": "2026-10-17", "price": 615.0},
      "target": {"t": "2026-10-24", "price": 640.0},
      "rr": 2.33,
      "direction": "bullish",
      "invalidation": "neckline recapture on 2026-10-19",
      "positionSize": 100
    }
  ]
}
```

**Error responses:**
- `404 Not Found` if no active pattern

## 12. Endpoint: `/api/macro`

**Method:** `GET`

**Description:** Get macro context indicators.

**Query parameters:** None

**Success response (200 OK):**
```json
{
  "asOf": "2026-10-17T19:00:00+05:30",
  "vix": 18.5,
  "usdinr": 83.2,
  "crude": 85.0,
  "fiiDiiNet": 1.2e9,
  "gsec10y": 7.25
}
```

**Error responses:**
- `500 Internal Server Error` if data unavailable

## 13. Implementation status

| Endpoint | M0 implemented? | Planned features |
|---|---|---|
| health | ✅ | — |
| search | ✅ | — |
| candles | ✅ | — |
| patterns | ✅ | — |
| trend | ✅ | — |
| technicals | — | M4 |
| fundamentals | — | M6 |
| predictions | ✅ | — |
| scan | — | M7 |
| decision | — | M7 |
| tradeplan | — | M7 |
| macro | — | M7 |

All M0 endpoints are present with minimal functionality; the server is ready for CI testing.