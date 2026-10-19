# DEPLOYMENT.md

Runbook for deployments, secrets, and release gates.

## 1) Secrets rotation (GitHub Secrets)

**Do not store plaintext secrets in files or variables.** All four credentials must be stored as GitHub Secrets only. Rotate each at the provider first before updating in GitHub. Never write actual secret values into source files.

Required secrets (env names):
- `VERCEL_API_KEY` — Vercel API token (rotate at vercel.com)
- `VERCEL_ORG_ID` — Vercel organization ID (rotate if compromised)
- `VERCEL_PROJECT_ID` — Vercel project ID (rotate if compromised)  
- `KAGGLE_API_KEY` — Kaggle API key (rotate at kaggle.com)

GitHub Actions use `${{ secrets.* }}` (never `${{ vars.* }}`) for these values.

## 2) Workflow gates

- CI (`.github/workflows/ci.yml`): runs server-tests, android-build, and deploy. Deploy runs only on push to `main` after server-tests pass. APK uploaded as `release-apk` artifact.
- EOD (`.github/workflows/eod.yml`): runs Monday–Friday at 30 13 * * 1-5 (UTC). Does not commit/push in CI.
- Universe Refresh (`.github/workflows/universe-refresh.yml`): monthly cron + manual dispatch.
- Backfill (`.github/workflows/backfill.yml`): manual dispatch only.

## 3) Vercel deployment gate

Server deploy uses Vercel CLI in CI: `vercel pull --yes --environment=production --token=${{ secrets.VERCEL_API_KEY }} --cwd server` then `vercel deploy --prod --token=${{ secrets.VERCEL_API_KEY }} --cwd server`. Required envs: `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID` from secrets.

## 4) Kaggle gate

Kaggle operations require `KAGGLE_API_KEY` secret. Never log or commit key material.

## 5) APK release gate

Android build produces `app-release.apk` in `android/app/build/outputs/apk/release/`. Uploaded as artifact `release-apk` with `if-no-files-found: error`. Only installable release APK is produced.

## 6) Curl smoke test

Post-deploy sanity check (example):
```bash
curl -f https://<your-deploy>.vercel.app/api/health
```
Expect 200 OK with JSON payload. Adjust endpoint as per `server/api/index.py`.

## 7) Notes

- Secrets must be rotated at providers before updating GitHub.
- Never write actual secret values into files.
- Do not run git commit/push from workflows in this gate.
- CI is the source of truth for build/test/verify.
