import sys
from pathlib import Path

# Ensure imports resolve for api.* / core.* when running tests in server/
REPO_ROOT = Path(__file__).resolve().parent.parent  # repo root
SERVER_DIR = Path(__file__).resolve().parent  # server dir
for _p in (str(SERVER_DIR), str(REPO_ROOT)):
    if _p not in sys.path:
        sys.path.insert(0, _p)

import pytest
from fastapi.testclient import TestClient

try:
    from api.index import app
except Exception:
    app = None


@pytest.fixture
def client():
    if app is None:
        raise RuntimeError("App not found")
    return TestClient(app)
