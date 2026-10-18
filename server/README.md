# Server (FastAPI on Vercel)

Run locally: `cd server && uvicorn api.index:app --reload` (health: `GET /api/health`).
Test: `cd server && pytest tests -q` (CI is the gate).
Contract: `design/api.md` — all JSON keys camelCase, timestamps ISO-8601 +05:30.
Deploy: `vercel deploy --prod` from `server/` via the CI `deploy` job (secrets: VERCEL_API_KEY, VERCEL_ORG_ID, VERCEL_PROJECT_ID).
Baked data lives in `data/*.json`, written only by the EOD pipeline — never hand-edit.
