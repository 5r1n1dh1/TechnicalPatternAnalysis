"""Universe-wide pattern scan."""
import os
import json
import pandas as pd

from pipeline.features import build_features


OUT_SCAN = "server/data/scan.json"
HISTORY_PARQUET = "tpa-history/history.parquet"


def scan():
    os.makedirs(os.path.dirname(OUT_SCAN), exist_ok=True)

    if not os.path.exists(HISTORY_PARQUET):
        if os.path.exists("history.parquet"):
            df_all = pd.read_parquet("history.parquet")
        else:
            df_all = pd.DataFrame()
    else:
        df_all = pd.read_parquet(HISTORY_PARQUET)

    hits = []
    if df_all.empty:
        with open(OUT_SCAN, "w") as f:
            json.dump({"asOf": pd.Timestamp.now().isoformat(), "hits": hits}, f, indent=2)
        return

    symbols = df_all["symbol"].unique() if "symbol" in df_all.columns else []
    for sym in symbols:
        try:
            sym_df = df_all[df_all["symbol"] == sym].sort_values("Date") if "Date" in df_all.columns else df_all
            if len(sym_df) < 100:
                continue
            feats = build_features(sym_df)
            # stub pattern detection
            hits.append(
                {
                    "symbol": sym,
                    "pattern": "range",
                    "confidence": 0.6,
                    "timestamp": sym_df["Date"].iloc[-1].isoformat() if "Date" in sym_df.columns and hasattr(sym_df["Date"].iloc[-1], "isoformat") else str(sym_df["Date"].iloc[-1]) if "Date" in sym_df.columns else "",
                }
            )
        except Exception:
            continue

    out = {
        "asOf": pd.Timestamp.now().isoformat(),
        "hits": hits,
    }
    with open(OUT_SCAN, "w") as f:
        json.dump(out, f, indent=2)


if __name__ == "__main__":
    scan()
