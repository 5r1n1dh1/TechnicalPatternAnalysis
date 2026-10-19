"""Fetch Nifty 500 universe from NSE index CSV and bake server/data/symbols.json."""
import csv
import json
import os
from datetime import datetime

NSE_URL = "https://nsearchives.nseindia.com/content/indices/ind_nifty500list.csv"
OUTPUT = os.path.join(os.path.dirname(__file__), "..", "server", "data", "symbols.json")
OUTPUT = os.path.abspath(OUTPUT)


def fetch_and_write():
    import requests
    session = requests.Session()
    headers = {"User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36"}
    session.get("https://www.nseindia.com/", headers=headers, timeout=30)
    resp = session.get(NSE_URL, headers=headers, timeout=30)
    resp.raise_for_status()
    lines = resp.text.splitlines()
    reader = csv.DictReader(lines)
    items = []
    for row in reader:
        sym = row.get("Symbol", "").strip()
        if not sym:
            continue
        items.append({
            "symbol": sym,
            "name": row.get("Company Name", sym).strip(),
            "sector": row.get("Industry", "NSE").strip() or "NSE",
        })
    items.sort(key=lambda x: x["symbol"])
    seen = set()
    unique = []
    for it in items:
        if it["symbol"] not in seen:
            seen.add(it["symbol"])
            unique.append(it)
    os.makedirs(os.path.dirname(OUTPUT), exist_ok=True)
    with open(OUTPUT, "w") as f:
        json.dump(unique, f, indent=2)
    return len(unique)


if __name__ == "__main__":
    count = fetch_and_write()
    print(f"Wrote {count} symbols to {OUTPUT}")
