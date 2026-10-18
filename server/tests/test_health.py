"""Health endpoint: status + baked dataAsOf; matrix validation unit tests."""

from api.index import app


def test_health_ok_null_when_no_symbols_file() -> None:
    # server/data/ holds no symbols.json yet (pipeline bakes it in M1).
    client = app.test_client() if hasattr(app, "test_client") else None  # noqa: F841
    from fastapi.testclient import TestClient

    with TestClient(app) as client:
        res = client.get("/api/health")
    assert res.status_code == 200
    body = res.json()
    assert body["status"] == "ok"
    assert body["dataAsOf"] is None


def test_health_reads_data_as_of(tmp_path, monkeypatch) -> None:
    import api.index as mod

    (tmp_path / "symbols.json").write_text('{"asOf": "2026-10-16", "symbols": []}', encoding="utf-8")
    monkeypatch.setattr(mod, "_DATA_DIR", tmp_path)
    from fastapi.testclient import TestClient

    with TestClient(mod.create_app()) as client:
        res = client.get("/api/health")
    assert res.status_code == 200
    assert res.json()["dataAsOf"] == "2026-10-16"
