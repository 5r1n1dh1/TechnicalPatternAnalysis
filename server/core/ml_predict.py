"""ML prediction loader (CI-only train; server reads baked JSON)."""
import os, json
from datetime import datetime, timezone, timedelta
from core.constants import PREDICTIONS_MAX_AGE_HRS

PRED_FILE = "server/data/predictions.json"

class StalePredictionsError(Exception):
    pass

def load_predictions():
    if not os.path.exists(PRED_FILE):
        raise StalePredictionsError("predictions.json missing")
    with open(PRED_FILE) as f:
        data = json.load(f)
    as_of = datetime.fromisoformat(data.get("asOf", "1970-01-01T00:00:00+05:30"))
    now = datetime.now(timezone(timedelta(hours=5, minutes=30)))
    if (now - as_of).total_seconds() > PREDICTIONS_MAX_AGE_HRS * 3600:
        raise StalePredictionsError(f"predictions stale since {data['asOf']}")
    return data
