# AI-Assisted Development

**Audience:** everyone using an AI coding assistant (Claude Code, Copilot, Cursor, …) on this repository.
**Principle:** AI assists; the human author owns and is accountable for every committed line.
A tool wrote it, but you are signing off on it.

medTimer is a medication reminder app.
User data stays on the device — there is no backend — but the same data can land on a developer's test device, in an exported CSV, or in a logcat dump.
Treat that data, and the release-signing material, with the discipline this document describes.

## Context files (how agents learn this repo)

Keep these current — they are how an agent understands the project before it touches code:

- [`AGENTS.md`](../../AGENTS.md) — the machine-facing "README for agents": commands, architecture, conventions.
  Keep it concise; link to these guidelines for detail instead of growing it.
- [`CLAUDE.md`](../../CLAUDE.md) — imports `@AGENTS.md`; do not duplicate content into it.

When starting a task, point the agent at the right files (the relevant module, the guide for the area you are touching).
Good context in beats correction later.

## Review discipline

AI-generated code is held to the **same bar as hand-written code** — no lower, no higher.

- It must pass the same gates before commit: `./gradlew assembleDebug` (both flavors), `./gradlew testFullDebugUnitTest testFossDebugUnitTest`, and
  `./gradlew lint`.
  Android Lint is configured with `abortOnError = true` and `warningsAsErrors = true`; a warning fails the build.
- Review for the context the model could not see: behavior across both `full` (GMS) and `foss` flavors, Room migration safety, scheduling/alarm correctness,
  translation coverage across the configured locales, and whether it actually matches existing patterns rather than inventing new ones.
- Be skeptical of confident-but-wrong output: invented APIs, plausible-looking but unused build flags, and tests that assert nothing.
  If you can't explain why a line is there, don't commit it.
- Read the diff, not just the summary.
  The agent's prose description is not the change.

## Boundaries

A three-tier model.
When in doubt, move an action up a tier (toward "Ask first").

### Never (without explicit human action)

- Commit secrets or credentials: real `keystore.jks` / `*.jks`, `keystore.properties`, Google Play service-account JSONs, Firebase service-account keys,
  `fastlane/*.json` upload credentials, or `local.properties` content.
  Only `*.example` templates belong in git.
- Put **real user medication data** from a test device — names, schedules, stock, reminder history, exported CSV/PDF — into a prompt, a test fixture, a log
  line, a commit, or any external service.
  Use synthetic data instead.
  See [Privacy and data](#privacy-and-data).
- Disable, weaken, or `@Suppress` an Android Lint warning or SonarQube issue to make code "pass".
  Fix the underlying code.
  If a suppression is genuinely needed, comment *why* and reference the rule.
- Add a dependency that the agent "remembered" without verifying it exists, is maintained, and is the intended package (
  see [Supply-chain hygiene](#ai-output-security)).

### Ask first (get a human decision before proceeding)

- **Room schema changes or new migrations.**
  Schemas in `app/schemas/` are user-visible; a bad migration corrupts data already on devices in the field.
- **Dependency, AGP, Kotlin, KSP, or Hilt upgrades** — especially across majors.
  These cascade into build issues, Kotlin metadata mismatches, and lint surprises.

### Always

- Run the gates above before pushing.
- Use the project's commit format (see [Attribution and commits](#attribution-and-commits)).
- Keep changes scoped to the task; surface unrelated problems separately rather than "fixing" them silently.

## Privacy and data

medTimer is local-first — there is no server — but the data is still personal medical information.

- **Never** paste real user data into an AI prompt, public issue, or any third-party service.
  Models may retain, cache, or train on input.
  This includes data captured on a developer's own device while testing.
- Use **synthetic or anonymized** medicines, schedules, and stock values in prompts, examples, and tests.
  Made-up names ("Vitamin X 500 mg", "Medicine A") are fine; real prescription names from a real patient are not.
- Don't log medication data, and don't ask an agent to "print the stored reminders to debug" — reproduce with fabricated data.
- Exported CSV/PDF artifacts and `adb backup` output are user data — keep them out of bug reports, test fixtures, and commits.

## Release secrets

- The signing keystore, upload key, Play Store service-account JSON, and Firebase service-account key live **outside the repo**.
  Reference them by environment variable / Gradle property name, never by value.
- `local.properties`, `keystore.properties`, and any real `*.json` credential files are in `.gitignore`; keep them there.
- CI signs releases via GitHub Actions secrets — don't move that material into the repo "for convenience".

## AI-output security

Generated code can be subtly insecure even when it compiles.
Treat the [OWASP Top 10 for LLM Applications](https://genai.owasp.org/) risks as real:

- **Insecure output / hallucinations** — validate logic against the system, not just the type checker.
  Lint and SonarQube are necessary but not sufficient.
- **Sensitive-information disclosure** — watch for the model echoing context (file paths from your machine, environment values, tokens) into code or comments.
- **Prompt injection** — be cautious when an agent processes untrusted content (issue text, fetched web pages); don't let it act on instructions embedded in
  data.
- **Supply-chain hygiene** — verify every suggested Gradle dependency: correct group/artifact (guard against typo-squats and hallucinated packages), maintained,
  and pinned via `gradle/libs.versions.toml`.
  New dependencies are an "Ask first" change (see above).

## Attribution and commits

- Match existing history: `#<issue-number> <short description>` for issue-linked work, sentence-case descriptions otherwise.
  Example: `#1432 create the :feature:reminders module`.
  Imperative or past tense is fine — match the surrounding history of the branch.
- Branches follow `<issue-number>-<kebab-description>` (e.g. `1432-extract-feature-reminders-module`).
- For **substantial** AI contributions (a full feature, function, or the bulk of a change), add a co-author trailer so the assist is visible in history:

  ```
  Co-Authored-By: Claude <noreply@anthropic.com>
  ```

  Trivial completions and small edits don't need attribution.
  The human author is always the primary author regardless.

## Sources

- [agents.md](https://agents.md/) and
  GitHub, ["How to write a great AGENTS.md"](https://github.blog/ai-and-ml/github-copilot/how-to-write-a-great-agents-md-lessons-from-over-2500-repositories/) —
  2025, the AGENTS.md convention and the three-tier boundary pattern.
- OWASP, [Top 10 for LLM Applications / GenAI Security](https://genai.owasp.org/) — 2024–2026.
- Android Developers, [App signing](https://developer.android.com/studio/publish/app-signing) — keystore management.

_Last reviewed: 2026-05-26._
