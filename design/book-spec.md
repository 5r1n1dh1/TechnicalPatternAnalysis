# Book Spec — Distilled Investment Rules (from user's curated book)

Source: `/home/srinidhi/Documents/AI-Invest` (~140 chapters). This file is the **authoritative extraction** the codebase implements; agents never need to re-read the book. Reviewed: Part 2 (Fundamentals, 52 ch), Part 3 (Technicals, 28 ch), Parts 4/6/7/9/10/11 (selected). Every rule below is implementable from free data (yfinance statements + NSE bhavcopy); see reconciliation notes where the book says otherwise.

## 1. Reconciliation: Book vs Plan v3 (inconsistencies → resolutions)

| # | Book | Old plan | Resolution (implemented) |
|---|---|---|---|
| 1 | Every pattern family carries a measured-move target formula + invalidation rule | Overlay only, no targets, no stop | Trade-plan card: entry/stop/target/R:R/position size (§7, §8); chart draws target + stop lines |
| 2 | Prior-trend requirement is core to H&S, double top/bottom, flags | Not specified | Added as mandatory detector precondition |
| 3 | Volume contraction inside pattern + expansion on break is a core validity rule (Dow tenet) | Volume only a confidence boost | Detectors reject patterns violating the volume sequence at default thresholds (relaxed only when bars < 20) |
| 4 | RSI: 30/70 not auto-sell; ideal bullish zone 55–65; divergence = warning | Flat zones only | Technicals tab zones + divergence detection; RSI 55–65 treated as bullish momentum regime |
| 5 | ADX directionless; use ±DI for direction | ADX only as trend-score bonus | Trend rubric uses +DI/−DI direction × ADX bands (§5.2) |
| 6 | Market structure (BOS/ChoCH/MSS) precedes pattern logic | Structure absent | Added structure module (§5.1); feeds technical verdict + ML features |
| 7 | Breakout quality rules (close near high, RVOL ≥ 3× significant, follow-through 1–2 sessions) | Not specified | Breakout quality score added to pattern confidence & trade-plan R:R |
| 8 | Decision = quality×valuation matrix + red flags, not data alone | ML-outlook card only | Composite Decision Engine (§6): INVEST / SHORT / WAIT with visible breakdown |
| 9 | Valuation via DCF/DDM + margin of safety, sector-specific multiples | Relative display fields only | Valuation engine (§4): sector multiples vs own 5y band + peers + 3-scenario DCF + MoS |
| 10 | Missing metrics often not published by Yahoo/NSE (CFO ratios, ROIC, EV, PEG…) | "Compute when present" | New rule C22: **compute via formulas** in `core/formulas.py` (§3), golden-tested |
| 11 | Delivery % (delivered/traded) is India-specific conviction signal; NSE publishes it EOD | Not included | New data input `sec_bhavdata_full` CSV → Technicals + ML feature |
| 12 | Screener recipes (quality/value/GARP/momentum) | v1 excluded | Post-v1 backlog (§10); decision engine re-uses the same formulas |
| 13 | Banks/insurers need GNPA/CASA/EV-VNB from filings | Generic ratios only | Sector path: banks/NBFC = P/B vs sustainable ROE (+RoA); non-computable sector metrics render "needs filing data" |
| 14 | Not implementable from sources | — (flagged to user) | Credit ratings, insider-trades feed quality, annual-report text (auditor churn, RPTs, pledging %) — no free API; shown as manual checklist items, never auto-scored |

## 2. Ratio formulas & threshold bands (book-cited; all computable)

All from annual + quarterly statements; `Avg X` = mean of t−1 and t. Industry-context bands only override where listed (bank/NBFC path, §4.3).

**Liquidity:** Current = CA/CL (<1 weak, 1–1.5 adequate, 1.5–2.5 healthy, >3 inefficient); Quick = (CA−Inv)/CL (<1 concern, ≥1.5 strong); Cash = Cash/CL.
**Efficiency:** DSO = AvgRecv/Rev·365; DIO = AvgInv/COGS·365; DPO = AvgPay/COGS·365; CCC = DSO+DIO−DPO (negative = moat, e.g. FMCG); AssetTurn = Rev/AvgAssets.
**Profitability:** GM = (Rev−COGS)/Rev; EBITDA M = EBITDA/Rev; OM = EBIT/Rev; NM = PAT/Rev. Industry GM bands: software 70–90, FMCG 40–60, auto 20–35, steel 10–25, refining 5–15, retail 10–30; NM: software 20–35, FMCG 10–20, banks 15–30, auto 5–12, retail 2–8.
**Returns:** ROE = PAT/AvgEquity; ROCE = EBIT/CapitalEmployed (CE = Equity + total debt = TA − CL); ROA = PAT/AvgAssets; ROIC = EBIT·(1−T)/AvgInvestedCapital (IC = equity + interest-bearing debt − excess cash); IncrementalROIC = ΔNOPAT/ΔIC. Bands: <5% weak, 5–10 avg, 10–15 good, 15–20 very good, >20 excellent. Value rule: ROCE/ROIC > cost of capital.
**DuPont:** ROE = NM × AssetTurn × FinLeverage (decompose in UI for diagnosis).
**Solvency:** D/E = Debt/Equity (<0.3 conservative, 0.3–1.0 comfortable, 1–2 high, >2 risky; ~0 software; high normal for telecom/infra/airlines); Debt/EBITDA (≤1 excellent, 1–2 healthy, 2–3 moderate, >4 elevated); InterestCov = EBIT/Interest (<1 distress, 1–2 weak, 2–5 acceptable, >5 strong); NetDebt = Debt − Cash.
**Growth:** CAGR(X,n) = (Xend/Xbegin)^(1/n) − 1 for Rev, PAT, EPS, OCF, FCF; volume-led growth flagged separately from price-only; SGR = ROE × Retention (1−payout); growth > SGR must be funded (debt/dilution → label).
**Valuation multiples:** P/E = Price/EPS; EarningsYield = 1/PE (compare vs G-Sec ~7%); PEG = PE/growth% (<1 cheap/bottom-fish, 1–1.5 GARP-acceptable, >2 expensive); P/B = Price/BVPS; EV = MktCap + Debt − Cash; EV/EBITDA; P/S & EV/S (loss-making/early); DividendYield = DPS/Price; Payout = Div/PAT (<60–70% sustainable band); FCFYield = FCF/MktCap; FCF = OCF − CapEx.
**Earnings quality:** CFO/PAT ratio (≥ 0.8 healthy; < 0.5 persistent = flag); receivables growth vs revenue growth; inventory growth vs sales; share-count CAGR vs EPS CAGR (dilution); recurring "other income"/one-off share of PAT.
**Bank/NBFC path:** RoA (1.8% vs 0.8% compounds massively); RoE checked for leverage; NIM (derived: NII/AvgAdvances); skip D/E · EV/EBITDA · CCC; P/B anchored to ROE (≈10%→1.2×, 18%→3.5×, 20%→4.5×).

## 3. Value computation rule (C22)

`core/formulas.py` — every value derivable from statements/price history is computed, never displayed as "N/A" if inputs exist:

```
EBITDA   = OI - OII + D&A  → fallback: EBIT + D&A, or OCF - ΔWC + tax + interest paid if D&A missing
T        = stated taxExpense / PAT, fallback 25%
EPS      = PAT / diluted share count
FCF      = OCF - CapEx;  FCFYield = FCF / MktCap
WACC     = CAPM: Re = Rf(7%) + β·ERP(6%); after-tax Rd = Rd·(1−T); weights from book+mkt blend
Growth   = CAGR over ≥3 available years, else "insufficient history"
```

Golden tests: hand-verified fixture statement set (§plan 12.2). Display: every derived cell shows the formula result + provenance tag (computed vs reported).

## 4. Valuation engine (rule-based; M8)

1. **Rank multiple matrix** by sector (primary → secondary): Banks/NBFC P/B(→ROE anchor) / P-E; Insurance P/(P-E fallback); IT P/E→PEG, FCF yield; FMCG P/E→FCF yield/PEG; Cement/Steel/Chem EV/EBITDA (+EV/EBIT); Telecom EV/EBITDA; Retail EV/Sales; Utilities P/B, div yield, EV/EBITDA; Realty P/B→NAV(note); CapitalGoods/Precision EV/EBITDA→P/E; Commodity EV/EBITDA (mid-cycle norm), P/B; AerospaceDefence P/E, EV/OrderBook(note); Power EV/EBITDA, P/B; Pharma/Healthcare P/E→EV/EBITDA.
2. **Banding** per multiple: current = own 5-yr / peer-5 (%) → band in [cheapest ≤0, 0-2 fair 2-3, ≥3 expensive]; realized only if ≥3 years of clean quarterly statements remain; else stale-flag.
3. **DCF (3 scenarios)** — FCFF: base = recent FCF margin; bull 40% gap ↑, bear −25%; growth starts at min(revCAGR, 18%) decaying linearly to terminal g (bull 5.0%, base 4.0%, bear 2.5%, all p < discount); horizon 10y; TV = lastFCF·(1+g)/(WACC−g); EquityValue = EV − netDebt; per-share = /dilutedShares; WACC = CAPM (Rf 7%, ERP 6%, β or sector beta fallback 1.0); tax 25%. Output: upside% per scenario = (value − price)/price.
4. **Margin of Safety** required band by quality tier (ROCE/ROIC + CFO conversion): tier1 (>20%, CFO/PAT≥0.8) ≥ 15–20%; tier2 ≥20–30%; tier3 avg ≥30–40%; cyclical ≥40% vs normalized mid-cycle earnings average.
5. **Bank/NBFC path**: skip P/E-first logic; sustainable ROE = 3-yr median RoE (checked < 6% leverage inflated); sustainable P/B interpolated from book table (RoE 10→1.2×, 18→3.5×, 20→4.5×); verdict vs current P/B; MoS = (sustainable P/B − current P/B)/sustainable P/B via table; ROA<0.8% note.
6. **Verdict inputs**: matrix(quality × valuation) classification:
   - High+Che = INVEST-strong (MoS met), High+Fair = hold/Accumulate, High+Exp = WAIT.
   - Avg+Che = analyze (engine surfaces, no auto-invest), Avg+Exp = avoid.
   - Poor+Che = **value-trap quarantine** (book Ch48 rules: declining Rev/Margins/ROE + rising debt/dilution); Poor+Exp = avoid.
7. **Cyclicals**: normalize earnings = mean of last 5-yr EPS before banding multiples; flag "at cycle peak" if current PAT > 1.8× 5-yr mean.

## 5. Technical additions (book; upgrades M3/M4)

### 5.1 Market structure engine (daily bars, 150-bar lookback)
- Swing highs/lows = Late-known pivts (window 5, highs>surrounding ±5). Uptrend = HH+HL sequence (last 3); Down = LH+LL; else range.
- **BOS** = close beyond last confirmed swing high in an up-leg (low in down-leg) → structure continuation.
- **ChoCH** = close beyond last confirmed swing *low* during up-leg (beyond last swing high in down-leg) → warning.
- **MSS** = ChoCH + impulsive displacement candle (range ≥ 1.5× ATR) in break direction.
- Structure state {UP, DOWN, RANGE, CHoCH_UP_pending, CHoCH_DOWN_pending} → Technicals tab chip + ML feature + decision input.

### 5.2 Indicator rubric (replaces flat zones)
- RSI(14): <30 oversold (only with structure context); 55–65 "bullish momentum regime"; >70 overbought but never auto-sell. Divergences: last 40 bars pivot compare — bullish (price LL, RSI HL), bearish (price HH, RSI LH).
- ADX(14): <20 weak; 20–25 developing; 25–50 strong; >50 very strong. Direction = +DI vs −DI. Never standalone.
- Bollinger(20,2): squeeze (width < 0.5× 120-bar median) → pre-breakout; upper-band close in trend = continuation.
- Volume: RVOL = today / 20-d avg (≥ 3 significant); breakout candle must close top-25%-in-range; delivery % = DELIVQTY/DELIVPER (NSE CSV) — high delivery + up move = accumulation; high + down = supply.
- MA ladder: price > 20 EMA > 50 EMA > 200 SMA = bull alignment; inverse bear; price > 30% above 200-DMA = overextension flag (mean reversion risk). Golden/death cross events surfaced.

### 5.3 S/R (strength ranking → scores 0–10)
Touches (4–5 ideal; >6 = exhausted, watch for break); time span (older = stronger); volume-at-touch; reaction magnitude; distance-to-mean (clustering 0.75×ATR); **role reversal** after break; **psychological round levels** (x00/x000; India lakhs marks). Confluence badge = ≥ 2 methods in ±1%.

## 6. Composite Decision Engine (rule-based, transparent; M8)

Inputs (0–100 each): F = fundamental score (earnings quality 25: CFO/PAT, receivables-vs-rev, inventory-vs-sales, dilution; returns 25: ROCE/ROIC band + ROIC−WACC; leverage 20: D/E, Debt/EBITDA, IntCov; growth 15: Rev/PAT CAGR ≥ 10% & P/E-neutral; capital 15: FCF+, payout<70%, screener-quality subset). T = technical verdict (structure ∈{UP,DOWN}, MA alignment, momentum regime, delivery + RVOL confirm, higher-timeframe). V = valuation verdict (§4 band + MoS gap; banker path bank). P = pattern context (live/failed bullish-bearish pattern conf, R:R from §7). M = ML up/down dominance per horizon.

**Verdict rules (evaluated top-down, first match wins):**
1. RED FLAG LOCK: ≥ 3 hard bull-side red flags (below) → verdict "SHORT_VETO_UP" any other label → WAIT-review.
2. SHORT: T=bearish-confirmed AND (bearish pattern ≥60 conf OR M.down > 0.5 for h2) AND (V=expensive-with-ownband-high OR F.earningsQuality < 45). Requires R:R ≥ 2 bearish plan.
3. INVEST: F ≥ 65 AND V = cheap/tier MoS met AND T not-bearish AND (M.up > M.down h1/h5) → INVEST. If additionally T bullish + M.up>0.55 → INVEST-strong.
4. WAIT: F≥65 but V fair/expensive, or T bearish with F<65 → WAIT (with reason list: "Wait for cheaper multiple", "Fragmented structure").
5. PASS(WATCH): everything else.
UI: each component card shows sub-scores AND the failing rubric names; nothing opaque. Confidence label (Strong/Normal/Weak) from gap-to-thresholds.

### 6.1 Red flags engine (computable subset; each = boolean, listed on card)
- PAT↑ while OCF flat/↓ (2 consecutive years)
- Receivables growth > 1.5× revenue growth
- Inventory growth > 1.5× revenue growth
- Persistent negative OCF with positive PAT
- Dilution: share-count CAGR 5y > 2% AND EPS CAGR < PAT CAGR
- Interest coverage < 2 (non-bank)
- D/E > 2 or Debt/EBITDA > 4 & rising
- Declining ROCE/ROIC for 3 consecutive years
- P/E < 2 with revenue declining ("cheapness trap" marker)
- Book-keeping manual-slot (display only): auditor churn, RPTs size, promoter pledge %, qualified opinion — from NSE/annual-report RSS pending; render as checkbox list user ticks self.

## 7. Trade plan card (attached to every active pattern detection)

| Field | Rule |
|---|---|
| Entry | Pattern breakout close; retest variant flagged (higher-probability per book) |
| Stop | Structure: just beyond neckline/opposite pedestal − 0.5 ATR buffer; fallback 2×ATR below entry |
| Target | Measured move minimum: H&S/IHS = neckline ± (head−neckline); Double/Triple = breakout − (extreme−breakoutLevel); **C&H = rim + cup depth**; Flag = pole projected from breakout |
| R:R | (target−entry)/(entry−stop); badge red if < 2, amber 2–3, green ≥ 3 |
| Position size | user-entered capital (local, Room) × 1% ÷ per-share risk → shares; no server side |
| Invalidation | book failure rule: neckline recapture post-breakout + new extreme → pattern failed chip (turns failed-bearish into bullish scan note for shorts) |

## 8. Technical verdict extension (Technicals tab)
Adds rows: structure chip, breakout-quality index (0–100 from §5.2 composite), delivery-% + RVOL stem (last 5 closes), S/R confluence badges.

## 9. Macro strip (minimal 5; EOD, free)
India VIX, USD/INR, crude (CL=F), FII/DII net (NSE daily CSV, best-effort), G-Sec 10y (config constant updated quarterly). Render as strip on symbol details header; drive nothing numerically (context only, book-compliant).

## 10. Post-v1 backlog (book items deferred)
- Screener screens: Quality (ROCE>20, ROE>18, D/E<0.5, FCF+, IntCov>5, stable promoter), Value (P/E below sector, FCF yield high), GARP (rev CAGR 12–25, ROCE>18, PEG ≤1.5), Momentum (52w-high-lite + 200DMA + rel-strength + delivery confirm), Coffee-can (10–25 tags from same formulas), Red-flag scan reverse.
- Extra pattern families: triangles (asc/desc/sym), wedges (rising/falling), pennants, rectangles, rounding bottom (book Ch12–15 rules; pennant = flag with converging bounds).
- Thesis + sell-checklist cards (Ch4 selling rules: 6 valid reasons; temporary-vs-permanent test).
- Derivative overlays (PCR bands, OI build-up matrix — low priority per book).

## 11. Not implementable (mark "needs filing data"; never auto-score)
Credit rating rationale PDFs; annual-report text fields (auditor opnion, RPT schedule detail, BRSR); con-call transcripts; FII/DII quarterly granular by stock (NSE aggregates only); insurance EV/VNB detail; PSU disinvestment news.
