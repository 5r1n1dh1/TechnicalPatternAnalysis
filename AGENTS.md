# AGENTS.md

Operating rules for all agents working in this repository. Follow every rule. Rule 1 overrides everything else when in conflict.

## 1. Token Efficiency (MOST IMPORTANT)

- Consume the fewest tokens possible. Read only files you need. Search before reading whole files. Never dump file contents into responses.
- Keep all responses, commits, PRs, and docs as short as possible while staying complete.
- No filler, no pleasantries, no restating the task, no "conversation filters" (no "I'd be happy to", no progress narration).
- Batch independent tool calls in parallel.
- Do not re-read unchanged files. Do not re-explain previous work.

## 2. Communication

- Output a jargon-free summary only. Plain language. No buzzwords.
- No conversational padding. State what was done, what changed, and what's next — nothing else.
- Always interview for open questions or when in need user input.

## 3. Workflow

- **Research and plan before execution.** Understand the existing code and confirm the approach before writing code. No guessing.
- **Small-Model-Ready Implementation Plans:** When producing a plan or guide, create an exhaustive, granular breakdown containing exact file paths, precise function/class signatures, explicit data flows, edge cases, and step-by-step instructions. Every requirement must be concrete enough that a smaller model (10–30B parameters) can execute and recreate it fully without ambiguity or missing context.
- **Never overengineer.** Simplest solution that works. No speculative abstractions, no unused config, no extra layers, no premature scaling.
- **Always sync with GitHub.** Commit and push after each unit of work. Keep the remote current.

## 4. Repository Structure

- Monorepo with two apps:
  - `android/` — the Android app.
  - `server/` — Python server, deployed on Vercel.
- Keep boundaries clean. Shared contracts (API schemas) live in one place; no cross-app code duplication.

## 5. Build, Test, Verify (CI/CD only)

- **Always use GitHub Actions** for build, test, and verification. Never build or test locally as the source of truth — local checks are fine for quick sanity, but CI is the gate.
- CI must build, test, and verify **every** push and PR.
- Always use installed `gh` app for repo actions such as actions trigger, variables fetch etc.,

## 6. Android App

- **Always output an APK.** Every CI run that touches `android/` must produce an installable release APK artifact.
- **Build the most optimized app possible**: R8/Minify enabled, resource shrinking, small native libs, minimal permissions, fast cold start, low memory use.
- **Design: glassmorphism.** Frosted-glass surfaces, translucent cards, soft blur, subtle borders, depth via layering. Consistent across all screens.
- **Design work must always produce flowcharts and sequence diagrams** (Mermaid). Every feature or design change ships with:
  - a user-flow flowchart
  - a sequence diagram of the calls involved
- Diagrams live next to the feature docs (e.g. `docs/diagrams/`).

## 7. Releases

- **GitHub Release when major changes exist** — new features, breaking changes, or significant fixes. Minor fixes: regular commits/PRs only.
- Releases attach the release APK built by CI.

## 8. Server (Vercel)

- Python server stays deployable on Vercel with zero manual steps. Merge to main = deployed.
- Keep functions cold-start friendly and lightweight. No heavy framework bloat.

## Priority Order

1. Token efficiency
2. Research and plan first
3. Simplest working solution (no overengineering)
4. CI/CD as the only build/test/verify gate
5. APK output + optimized Android build (glassmorphism)
6. GitHub sync + releases on major changes
