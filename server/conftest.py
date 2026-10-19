import sys
from pathlib import Path

# Ensure imports resolve for api.* when running tests in server/
SERVER_DIR = Path(__file__).resolve().parent  # server dir
if str(SERVER_DIR) not in sys.path:
    sys.path.insert(0, str(SERVER_DIR))

import pytest
from fastapi.testclient import TestClient

try:
    from api.index import app
except Exception:
    try:
        from server.api.index import app
    except Exception:
        app = None


@pytest.fixture
def client():
    if app is None:
        raise RuntimeError("App not found")
    return TestClient(app)
